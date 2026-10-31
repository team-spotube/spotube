import { CLIENT_KEY, DEVICE_DIGEST, PROTOCOL_VERSION, ROOM_CODE, cleanName, deviceMark, participantId } from "./protocol";

export { JamRoom } from "./room";

const JSON_HEADERS = { "content-type": "application/json" };

/**
 * Routes:
 * - `GET /v1/health` → `{ ok, v, now }` (used by the app's "Test connection")
 * - `GET /v1/rooms/{CODE}?key=&name=&mode=create|join[&dev=]` (WebSocket) → the room's Durable Object
 *
 * Durable Objects aren't reachable from the internet, only through this Worker, so the
 * room trusts the `id` and `name` this Worker hands it.
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
    const key = url.searchParams.get("key") ?? "";
    const name = cleanName(url.searchParams.get("name") ?? "");
    const mode = url.searchParams.get("mode");
    const dev = url.searchParams.get("dev");
    if (
      !ROOM_CODE.test(code) ||
      !CLIENT_KEY.test(key) ||
      !name ||
      (mode !== "create" && mode !== "join") ||
      (dev !== null && !DEVICE_DIGEST.test(dev))
    ) {
      return new Response(JSON.stringify({ error: "bad_request" }), { status: 400, headers: JSON_HEADERS });
    }

    const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
    const { success } = await env.CONNECT_LIMITER.limit({ key: ip });
    if (!success) return new Response("Too many connections", { status: 429 });

    const forwarded = new URL(request.url);
    forwarded.search = new URLSearchParams({
      id: await participantId(code, key),
      name,
      mode,
      mark: (await deviceMark(code, dev, ip)) ?? "",
    }).toString();
    const room = env.JAM_ROOM.get(env.JAM_ROOM.idFromName(code));
    return room.fetch(new Request(forwarded, request));
  },
} satisfies ExportedHandler<Env>;
