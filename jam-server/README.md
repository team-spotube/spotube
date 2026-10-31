# Spotube Jam server

The room server behind Group Jam: a Cloudflare Worker with one
[Durable Object](https://developers.cloudflare.com/durable-objects/) per room code.

Each room relays a few small JSON frames between the host and its guests. Nobody streams
audio through it (every device fetches its own), so a room costs a few KB per minute.

Rooms are billed per incoming message (20 count as one request) and per storage write,
so steady playback sends almost nothing:

- The host sends a playback anchor only when playback changes (play/pause, seek, skip,
  speed), when its player drifts from its last anchor, and once a minute as a safety net.
  Guests extrapolate anchors with the shared clock in between.
- Anchors are kept in memory, not storage. A room that hibernated wakes for a join, and
  the host answers the join with a fresh anchor. Only queue changes and membership
  bookkeeping are written.
- Keep-alives (`"ping"` → `"pong"`) are answered by the runtime without waking the room.
  Clock pings go out on join and every 10 minutes.

A four-person room sends roughly 200 messages an hour (about 10 billed requests).

## What a room does

- **Membership**: tracks who is connected, who hosts, and who is banned. If the host
  drops, it keeps hosting for 30 s (so it can reconnect, e.g. after switching networks), then the longest-connected guest
  takes over.
- **Retained state**: keeps the host's latest queue (and, while awake, its playback
  anchor) so late joiners start in sync. An empty room keeps them for 10 minutes, then deletes itself.
- **Routing**: guest commands (play/pause, skip, seek, suggestions) go to the host.
  Kicks and bans are enforced by the server.
- **Clock**: answers pings with its own time. Every device syncs playback to this one
  clock (see `JamClock` in the app).

The wire protocol is in [`src/protocol.ts`](src/protocol.ts). The app side is
`composeApp/src/commonMain/kotlin/org/spotube/spotube/core/jam/`.

## Develop

```sh
npm install
npm run dev          # http://127.0.0.1:8787
npm test             # smoke test against the dev server (another terminal)
npm run typecheck
```

The app's end-to-end tests run against it too:

```sh
JAM_TEST_URL=http://127.0.0.1:8787 ./gradlew :composeApp:jvmTest --tests 'org.spotube.spotube.core.jam.*'
```

## Deploy

```sh
CLOUDFLARE_API_TOKEN=... CLOUDFLARE_ACCOUNT_ID=... npx wrangler deploy
```

The token needs the "Edit Cloudflare Workers" permissions. Durable Objects on the
SQLite backend work on the free plan. `.github/workflows/jam-server-deploy.yml` deploys on
pushes to `master` that touch this directory (secrets `CLOUDFLARE_API_TOKEN` and
`CLOUDFLARE_ACCOUNT_ID`; optional variable `JAM_SERVER_URL` runs the smoke test after).

## Self-host

Deploy to your own Cloudflare account as above, then enter the Worker's URL in
Spotube → Settings → Jam → Server URL.

## Limits

- 32 participants per room, 512 KB per frame.
- 60 room connections per minute per IP (`CONNECT_LIMITER` in `wrangler.jsonc`).
