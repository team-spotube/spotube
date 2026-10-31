import { DurableObject } from "cloudflare:workers";
import {
  CLOSE_CODES,
  KEEPALIVE_PING,
  KEEPALIVE_PONG,
  EMPTY_ROOM_TTL_MS,
  FRAME_BUDGET,
  HOST_GRACE_MS,
  MAX_BANS,
  MAX_MEMBERS,
  MAX_FRAME_BYTES,
  MAX_PARTICIPANTS,
  PROTOCOL_VERSION,
  QUEUE_BUDGET,
  type ClientFrame,
  type ErrorCode,
  type JoinMode,
  type Participant,
  type ServerFrame,
} from "./protocol";

interface Bucket {
  tokens: number;
  at: number;
}

/** Takes one token; false when [bucket] is empty. */
function spend(bucket: Bucket, budget: { burst: number; perSecond: number }, now: number): boolean {
  bucket.tokens = Math.min(budget.burst, bucket.tokens + ((now - bucket.at) / 1000) * budget.perSecond);
  bucket.at = now;
  if (bucket.tokens < 1) return false;
  bucket.tokens -= 1;
  return true;
}

/** Kept on each socket so it survives hibernation. */
interface Attachment {
  id: string;
  name: string;
  joined: number;
  /** See [deviceMark]; empty when the client sent no device digest. */
  mark: string;
}

const enum Key {
  Host = "host",
  Queue = "queue",
  Banned = "banned",
  BannedMarks = "bannedMarks",
  Members = "members",
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
  // In memory: a room that hibernated starts everyone with a full budget again, which
  // only matters to a client that floods, and that one wakes the room right away.
  private frameBudgets = new WeakMap<WebSocket, Bucket>();
  private queueBudgets = new WeakMap<WebSocket, Bucket>();

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair(KEEPALIVE_PING, KEEPALIVE_PONG));
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const id = url.searchParams.get("id")!;
    const name = url.searchParams.get("name")!;
    const mode = url.searchParams.get("mode") as JoinMode;
    const mark = url.searchParams.get("mark") ?? "";

    const { 0: client, 1: server } = new WebSocketPair();
    const previous = this.ctx.getWebSockets(id);
    this.ctx.acceptWebSocket(server, [id]);
    server.serializeAttachment({ id, name, joined: Date.now(), mark } satisfies Attachment);

    // A reconnect with the same id (network change, app restart) takes over the old socket.
    for (const ws of previous) this.reject(ws, "replaced");

    const refusal = await this.admit(id, mode, mark);
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
    if (!this.withinBudget(this.frameBudgets, ws, FRAME_BUDGET)) return this.reject(ws, "rate_limited");

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
        if (!this.withinBudget(this.queueBudgets, ws, QUEUE_BUDGET)) return this.reject(ws, "rate_limited");
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
        if (frame.ban) await this.ban(frame.target);
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

  /**
   * Bans [id], and the device it's on: someone who comes back with a fresh identity from
   * the same device and network is refused too, unless they were already a member.
   */
  private async ban(id: string): Promise<void> {
    const marks = this.ctx
      .getWebSockets(id)
      .map((ws) => this.attachment(ws).mark)
      .filter((mark) => mark);
    const [banned, bannedMarks] = await Promise.all([
      this.ctx.storage.get<string[]>(Key.Banned),
      this.ctx.storage.get<string[]>(Key.BannedMarks),
    ]);
    await this.ctx.storage.put({
      [Key.Banned]: [...new Set([...(banned ?? []), id])].slice(-MAX_BANS),
      [Key.BannedMarks]: [...new Set([...(bannedMarks ?? []), ...marks])].slice(-MAX_BANS),
    });
  }

  private async admit(id: string, mode: JoinMode, mark: string): Promise<ErrorCode | null> {
    const others = this.live().filter((ws) => this.attachment(ws).id !== id);
    // An empty room past its TTL whose cleanup alarm hasn't run yet is gone.
    const emptySince = await this.ctx.storage.get<number>(Key.EmptySince);
    if (others.length === 0 && emptySince !== undefined && Date.now() - emptySince >= EMPTY_ROOM_TTL_MS) {
      await this.ctx.storage.deleteAll();
      this.anchor = null;
    }

    const [host, banned, queue] = await Promise.all([
      this.ctx.storage.get<string>(Key.Host),
      this.ctx.storage.get<string[]>(Key.Banned),
      this.ctx.storage.get(Key.Queue),
    ]);
    const exists = host !== undefined || queue !== undefined || others.length > 0;

    if (mode === "create") {
      // Only a fresh code, or the room's own host bringing it back. Anyone else
      // "creating" an existing room (even an empty one) would take it over.
      if (others.length > 0 || (exists && host !== id)) return "room_taken";
      if (!exists) await this.ctx.storage.put({ [Key.Host]: id, [Key.Members]: [id] });
      return null;
    }

    if (!exists) return "room_not_found";
    if (banned?.includes(id)) return "banned";
    const members = (await this.ctx.storage.get<string[]>(Key.Members)) ?? [];
    const isMember = members.includes(id);
    // A member who was in the room before a ban (a friend on the same Wi-Fi with the
    // same phone, say) is never caught by another person's device ban.
    if (mark && !isMember && (await this.ctx.storage.get<string[]>(Key.BannedMarks))?.includes(mark)) {
      return "banned";
    }
    if (others.length >= MAX_PARTICIPANTS) return "room_full";
    if (!isMember) await this.ctx.storage.put(Key.Members, [...members, id].slice(-MAX_MEMBERS));

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

  private withinBudget(
    buckets: WeakMap<WebSocket, Bucket>,
    ws: WebSocket,
    budget: { burst: number; perSecond: number },
  ): boolean {
    const now = Date.now();
    let bucket = buckets.get(ws);
    if (!bucket) {
      bucket = { tokens: budget.burst, at: now };
      buckets.set(ws, bucket);
    }
    return spend(bucket, budget, now);
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
