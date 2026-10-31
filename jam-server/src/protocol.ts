/**
 * Wire protocol between Spotube and the jam room server (version 1).
 *
 * Every frame is a JSON object with a `t` discriminator. The server never looks
 * inside `body`: queue, anchor and command payloads are owned by the app and only
 * routed (and, for queue/anchor, retained) here.
 */

export const PROTOCOL_VERSION = 1;

/** Same alphabet as the app's `JamRoomCode` (no I, O, 0, 1). */
export const ROOM_CODE = /^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$/;
/**
 * A participant's secret, chosen by the client (at least 128 random bits). The server
 * never shares it: everyone else only sees the id derived from it ([participantId]),
 * so knowing someone's id doesn't let you reconnect as them.
 */
export const CLIENT_KEY = /^[A-Za-z0-9_-]{22,64}$/;
export const MAX_NAME_LENGTH = 40;
export const MAX_PARTICIPANTS = 32;
/** A long queue with cover URLs is the largest frame, a few hundred KB at most. */
export const MAX_FRAME_BYTES = 512 * 1024;
export const MAX_BANS = 256;
/** Everyone who has been in the room, to tell a returning member from a new arrival. */
export const MAX_MEMBERS = 512;

/** Optional per-room device digest from the client (it never sends device details). */
export const DEVICE_DIGEST = /^[A-Za-z0-9_-]{16,64}$/;

/**
 * Per-socket token buckets. Every incoming message is billed, so a client that floods
 * is disconnected. Real clients stay far below: a few frames a minute, bursts of a
 * dozen when skipping quickly, and the host sends at most one queue a second.
 */
export const FRAME_BUDGET = { burst: 40, perSecond: 6 };
export const QUEUE_BUDGET = { burst: 5, perSecond: 1 };

/**
 * A host that drops keeps the room for this long before someone else takes over (long
 * enough for a phone to switch networks). Guests keep playing in the meantime.
 */
export const HOST_GRACE_MS = 30_000;
/** An empty room keeps its queue for this long, so everyone can rejoin after a blip. */
export const EMPTY_ROOM_TTL_MS = 10 * 60_000;

/**
 * Keep-alive, sent as a bare text frame. The runtime answers it without waking the room,
 * so it costs nothing; clock pings (`ping` frames) are only sent when the clock syncs.
 */
export const KEEPALIVE_PING = "ping";
export const KEEPALIVE_PONG = "pong";

export type JoinMode = "create" | "join";

export type ClientFrame =
  | { t: "ping"; id: number; c: number }
  | { t: "queue"; body: unknown }
  | { t: "anchor"; body: unknown }
  | { t: "cmd"; body: unknown }
  | { t: "kick"; target: string; ban?: boolean };

export interface Participant {
  id: string;
  name: string;
}

export type ServerFrame =
  | {
      t: "welcome";
      v: number;
      you: string;
      host: string | null;
      participants: Participant[];
      queue: unknown;
      anchor: unknown;
    }
  | { t: "pong"; id: number; c: number; s: number }
  | { t: "presence"; host: string | null; participants: Participant[] }
  | { t: "queue"; body: unknown }
  | { t: "anchor"; body: unknown }
  | { t: "cmd"; from: string; body: unknown }
  | { t: "error"; code: ErrorCode };

export type ErrorCode =
  | "bad_request"
  | "room_taken"
  | "room_not_found"
  | "room_full"
  | "banned"
  | "kicked"
  | "replaced"
  | "frame_too_large"
  | "rate_limited";

/** WebSocket close codes the app maps back to an [ErrorCode]. */
export const CLOSE_CODES: Record<ErrorCode, number> = {
  bad_request: 4000,
  room_taken: 4001,
  room_not_found: 4004,
  room_full: 4005,
  banned: 4003,
  kicked: 4008,
  replaced: 4009,
  frame_too_large: 4013,
  rate_limited: 4029,
};

/** First 96 bits of SHA-256, base64url. */
async function shortHash(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return btoa(String.fromCharCode(...new Uint8Array(digest).slice(0, 12)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

/** The public id for [key] in room [code]: a different id per room, so ids can't link rooms. */
export function participantId(code: string, key: string): Promise<string> {
  return shortHash(`${code}:${key}`);
}

/**
 * What a ban remembers about a device besides its participant id: the client's device
 * digest combined with its network, hashed again here so the room only ever stores an
 * opaque value. Null without a digest.
 */
export function deviceMark(code: string, digest: string | null, ip: string): Promise<string> | null {
  return digest ? shortHash(`${code}:${digest}:${networkOf(ip)}`) : null;
}

/** An IPv4 address, or an IPv6 /64 (one home or phone gets a whole /64). */
export function networkOf(ip: string): string {
  if (!ip.includes(":")) return ip;
  const [head, tail] = ip.split("::");
  const left = head ? head.split(":") : [];
  const right = tail ? tail.split(":") : [];
  const groups = tail === undefined ? left : [...left, ...Array(8 - left.length - right.length).fill("0"), ...right];
  return groups.slice(0, 4).map((g) => parseInt(g || "0", 16).toString(16)).join(":") + "::/64";
}

/** Names are shown to everyone: no control or formatting characters, no padding. */
export function cleanName(raw: string): string {
  return raw.replace(/[\p{C}]/gu, "").replace(/\s+/gu, " ").trim().slice(0, MAX_NAME_LENGTH);
}
