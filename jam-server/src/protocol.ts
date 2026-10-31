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
export const CLIENT_ID = /^[A-Za-z0-9_-]{4,64}$/;
export const MAX_NAME_LENGTH = 40;
export const MAX_PARTICIPANTS = 32;
/** A long queue with cover URLs is the largest frame, a few hundred KB at most. */
export const MAX_FRAME_BYTES = 512 * 1024;

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
  | "frame_too_large";

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
};
