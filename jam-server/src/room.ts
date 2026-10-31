import { DurableObject } from "cloudflare:workers";
import {
  CLOSE_CODES,
  KEEPALIVE_PING,
  KEEPALIVE_PONG,
  EMPTY_ROOM_TTL_MS,
  HOST_GRACE_MS,
  MAX_FRAME_BYTES,
  MAX_PARTICIPANTS,
  PROTOCOL_VERSION,
  type ClientFrame,
  type ErrorCode,
  type JoinMode,
  type Participant,
  type ServerFrame,
} from "./protocol";

/** Kept on each socket so it survives hibernation. */
interface Attachment {
  id: string;
  name: string;
  joined: number;
}

const enum Key {
  Host = "host",
  Queue = "queue",
  Banned = "banned",
  HostGoneAt = "hostGoneAt",
  EmptySince = "emptySince",
}

/**
 * One jam room, named by its six-letter code.
 *
 * The room is a relay with memory: it tracks who is connected and who hosts, keeps
 * the host's latest queue and playback anchor for late joiners, routes guest
 * commands to the host, and serves the clock everyone syncs playback against.
 * Sockets use the hibernation API, so an idle room costs nothing between messages.
 *
 * Cost: rooms are billed per incoming message and per storage write, so the steady
 * state is kept free of both. Keep-alives are answered by the runtime without waking
 * the room, and the anchor (sent on every playback change) lives in memory only: when
 * a hibernated room wakes for a join, the host answers the join's presence frame with
 * a fresh anchor anyway.
 */
export class JamRoom extends DurableObject<Env> {
  private anchor: unknown = null;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair(KEEPALIVE_PING, KEEPALIVE_PONG));
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const id = url.searchParams.get("id")!;
    const name = url.searchParams.get("name")!;
    const mode = url.searchParams.get("mode") as JoinMode;

    const { 0: client, 1: server } = new WebSocketPair();
    const previous = this.ctx.getWebSockets(id);
    this.ctx.acceptWebSocket(server, [id]);
    server.serializeAttachment({ id, name, joined: Date.now() } satisfies Attachment);

    // A reconnect with the same id (network change, app restart) takes over the old socket.
    for (const ws of previous) this.reject(ws, "replaced");

    const refusal = await this.admit(id, mode);
    if (refusal) {
      this.reject(server, refusal);
    } else {
      await this.ctx.storage.delete(Key.EmptySince);
      const [host, queue] = await Promise.all([
        this.ctx.storage.get<string>(Key.Host),
        this.ctx.storage.get(Key.Queue),
      ]);
      this.send(server, {
        t: "welcome",
        v: PROTOCOL_VERSION,
        you: id,
        host: host ?? null,
        participants: this.participants(),
        queue: queue ?? null,
        anchor: this.anchor,
      });
      this.broadcast(await this.presence(), server);
    }

    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(ws: WebSocket, message: string | ArrayBuffer): Promise<void> {
    const size = typeof message === "string" ? message.length : message.byteLength;
    if (size > MAX_FRAME_BYTES) return this.reject(ws, "frame_too_large");
    if (typeof message !== "string") return;

    let frame: ClientFrame;
    try {
      frame = JSON.parse(message);
    } catch {
      return;
    }
    const sender = this.attachment(ws);

    switch (frame.t) {
      case "ping":
        this.send(ws, { t: "pong", id: frame.id, c: frame.c, s: Date.now() });
        return;

      case "queue":
        if (!(await this.isHost(sender.id))) return;
        await this.ctx.storage.put(Key.Queue, frame.body);
        this.broadcast({ t: "queue", body: frame.body }, ws);
        return;

      case "anchor":
        if (!(await this.isHost(sender.id))) return;
        this.anchor = frame.body;
        this.broadcast({ t: "anchor", body: frame.body }, ws);
        return;

      case "cmd": {
        const host = await this.ctx.storage.get<string>(Key.Host);
        const out: ServerFrame = { t: "cmd", from: sender.id, body: frame.body };
        if (sender.id === host) {
          this.broadcast(out, ws);
        } else if (host) {
          for (const target of this.ctx.getWebSockets(host)) this.send(target, out);
        }
        return;
      }

      case "kick": {
        if (!(await this.isHost(sender.id)) || frame.target === sender.id) return;
        if (frame.ban) {
          const banned = (await this.ctx.storage.get<string[]>(Key.Banned)) ?? [];
          if (!banned.includes(frame.target)) {
            await this.ctx.storage.put(Key.Banned, [...banned, frame.target]);
          }
        }
        for (const target of this.ctx.getWebSockets(frame.target)) {
          this.reject(target, frame.ban ? "banned" : "kicked");
        }
        return;
      }
    }
  }

  async webSocketClose(ws: WebSocket, code: number): Promise<void> {
    try {
      ws.close(code === 1005 ? 1000 : code);
    } catch {
      // Already closed.
    }
    await this.onDeparture(ws);
  }

  async webSocketError(ws: WebSocket): Promise<void> {
    await this.onDeparture(ws);
  }

  /** Fires after a host's grace period, or when an empty room expires. */
  async alarm(): Promise<void> {
    const live = this.live();
    if (live.length === 0) {
      const emptySince = await this.ctx.storage.get<number>(Key.EmptySince);
      if (emptySince === undefined) return;
      const expiresAt = emptySince + EMPTY_ROOM_TTL_MS;
      if (Date.now() >= expiresAt) {
        await this.ctx.storage.deleteAll();
        this.anchor = null;
      } else {
        await this.ctx.storage.setAlarm(expiresAt);
      }
      return;
    }

    const host = await this.ctx.storage.get<string>(Key.Host);
    if (host && live.some((ws) => this.attachment(ws).id === host)) return;
    await this.promote();
  }

  // ---------- Membership ----------

  private async admit(id: string, mode: JoinMode): Promise<ErrorCode | null> {
    const others = this.live().filter((ws) => this.attachment(ws).id !== id);

    if (mode === "create") {
      if (others.length > 0) return "room_taken";
      // Leftovers of an expired-but-not-yet-cleaned room must not leak into a new one.
      await this.ctx.storage.deleteAll();
      this.anchor = null;
      await this.ctx.storage.put(Key.Host, id);
      return null;
    }

    const [host, banned, queue] = await Promise.all([
      this.ctx.storage.get<string>(Key.Host),
      this.ctx.storage.get<string[]>(Key.Banned),
      this.ctx.storage.get(Key.Queue),
    ]);
    if (host === undefined && queue === undefined && others.length === 0) return "room_not_found";
    if (banned?.includes(id)) return "banned";
    if (others.length >= MAX_PARTICIPANTS) return "room_full";

    // Joining a room whose host is gone (and whose grace period is over) promotes someone.
    const hostLive = host === id || others.some((ws) => this.attachment(ws).id === host);
    if (!hostLive) {
      const goneAt = await this.ctx.storage.get<number>(Key.HostGoneAt);
      if (goneAt === undefined || Date.now() - goneAt >= HOST_GRACE_MS) {
        await this.promote(id);
      }
    } else if (host === id) {
      await this.ctx.storage.delete(Key.HostGoneAt);
    }
    return null;
  }

  private async onDeparture(ws: WebSocket): Promise<void> {
    const { id } = this.attachment(ws);
    const live = this.live().filter((other) => other !== ws);
    // Replaced by a newer socket of the same participant: nothing changed.
    if (live.some((other) => this.attachment(other).id === id)) return;

    if (live.length === 0) {
      await this.ctx.storage.put(Key.EmptySince, Date.now());
      await this.ctx.storage.setAlarm(Date.now() + EMPTY_ROOM_TTL_MS);
      return;
    }

    if (id === (await this.ctx.storage.get<string>(Key.Host))) {
      await this.ctx.storage.put(Key.HostGoneAt, Date.now());
      await this.ctx.storage.setAlarm(Date.now() + HOST_GRACE_MS);
    }
    this.broadcast(await this.presence(), ws);
  }

  /** The longest-connected participant hosts next (or [fallback] in an empty room). */
  private async promote(fallback?: string): Promise<void> {
    const next = this.participants()[0]?.id ?? fallback;
    if (!next) return;
    await this.ctx.storage.put(Key.Host, next);
    await this.ctx.storage.delete(Key.HostGoneAt);
    this.broadcast(await this.presence());
  }

  private async isHost(id: string): Promise<boolean> {
    return (await this.ctx.storage.get<string>(Key.Host)) === id;
  }

  private async presence(): Promise<ServerFrame> {
    return {
      t: "presence",
      host: (await this.ctx.storage.get<string>(Key.Host)) ?? null,
      participants: this.participants(),
    };
  }

  // ---------- Sockets ----------

  private live(): WebSocket[] {
    return this.ctx.getWebSockets().filter((ws) => ws.readyState === WebSocket.OPEN);
  }

  /** Connected participants, longest-connected first, one entry per id. */
  private participants(): Participant[] {
    const byId = new Map<string, Attachment>();
    for (const ws of this.live()) {
      const a = this.attachment(ws);
      const existing = byId.get(a.id);
      if (!existing || a.joined < existing.joined) byId.set(a.id, a);
    }
    return [...byId.values()]
      .sort((a, b) => a.joined - b.joined || a.id.localeCompare(b.id))
      .map(({ id, name }) => ({ id, name }));
  }

  private attachment(ws: WebSocket): Attachment {
    return ws.deserializeAttachment() as Attachment;
  }

  private send(ws: WebSocket, frame: ServerFrame): void {
    try {
      ws.send(JSON.stringify(frame));
    } catch {
      // The socket closed under us; its close handler cleans up.
    }
  }

  private broadcast(frame: ServerFrame, except?: WebSocket): void {
    const text = JSON.stringify(frame);
    for (const ws of this.live()) {
      if (ws === except) continue;
      try {
        ws.send(text);
      } catch {
        // See send().
      }
    }
  }

  private reject(ws: WebSocket, code: ErrorCode): void {
    this.send(ws, { t: "error", code });
    try {
      ws.close(CLOSE_CODES[code], code);
    } catch {
      // Already closed.
    }
  }
}
