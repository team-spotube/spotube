import { CLIENT_ID, MAX_NAME_LENGTH, PROTOCOL_VERSION, ROOM_CODE } from "./protocol";

export { JamRoom } from "./room";

const JSON_HEADERS = { "content-type": "application/json" };

/**
 * Routes:
 * - `GET /v1/health` → `{ ok, v, now }` (used by the app's "Test connection")
 * - `GET /v1/rooms/{CODE}?id=&name=&mode=create|join` (WebSocket) → the room's Durable Object
 */
export default {
  async fetch(request, env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/v1/health") {
      return Response.json({ ok: true, v: PROTOCOL_VERSION, now: Date.now() });
    }

    const match = url.pathname.match(/^\/v1\/rooms\/([^/]+)$/);
    if (!match) return new Response("Not found", { status: 404 });

    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return new Response("Expected a WebSocket upgrade", { status: 426 });
    }

    const code = match[1].toUpperCase();
    const id = url.searchParams.get("id") ?? "";
    const name = (url.searchParams.get("name") ?? "").trim().slice(0, MAX_NAME_LENGTH);
    const mode = url.searchParams.get("mode");
    if (!ROOM_CODE.test(code) || !CLIENT_ID.test(id) || !name || (mode !== "create" && mode !== "join")) {
      return new Response(JSON.stringify({ error: "bad_request" }), { status: 400, headers: JSON_HEADERS });
    }

    const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
    const { success } = await env.CONNECT_LIMITER.limit({ key: ip });
    if (!success) return new Response("Too many connections", { status: 429 });

    const forwarded = new URL(request.url);
    forwarded.search = new URLSearchParams({ id, name, mode }).toString();
    const room = env.JAM_ROOM.get(env.JAM_ROOM.idFromName(code));
    return room.fetch(new Request(forwarded, request));
  },
} satisfies ExportedHandler<Env>;
