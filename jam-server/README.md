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

## Security

Anyone who knows a room code can join it, as with any party-code app. Everything else
is enforced here, not in the app:

- **Identity**: each install keeps a random secret, and its key for a room is derived
  from that secret and the room code. Everyone else only sees an id derived from the key
  (`participantId`, different in every room). Knowing someone's id, the host's included,
  doesn't let you connect as them, so nobody can take over hosting, kick people or replace
  someone's connection.
- **Host powers**: only the current host can set the queue and anchor or kick and ban.
  Guests can only send commands, and those go to the host, which decides what to apply.
- **Taking over rooms**: "create" only works on a fresh code, or for the room's own host
  bringing back a room that expired while everyone was offline.
- **Abuse and cost**: connects are rate-limited per IP (60 a minute; also slows down code
  guessing). Each socket has a message budget and a queue budget (`FRAME_BUDGET`,
  `QUEUE_BUDGET`), and a client that floods is disconnected with `rate_limited`.
  Frames over 512 KB are refused, and names are cleaned of control characters.
- **What the app does with peer data**: queue items and suggestions from other devices
  are cleaned before use (`JamMediaItem.sanitized`): streaming tracks only, no URLs, only
  https covers. The host credits suggestions to the sender the server reports, not to
  whatever name the sender claims. The host's local files go out as an opaque id, never
  as a path.
- **Privacy**: participants never see each other's IP addresses, since everything goes
  through the room. The room keeps no history: the queue and bans are deleted 10 minutes
  after the last person leaves.

Known limits:

- **Bans**: a ban covers the identity (the same install gets the same identity in a
  room every time) and the device. The app sends a digest of stable device traits
  (model, OS version, locale, …) hashed with the room code. The server hashes that again
  with the network (IPv4 address or IPv6 /64), so the room only stores an opaque value
  that means nothing outside it. Someone who reinstalls and rejoins from the same device
  and network is refused. People who were already in the room aren't affected, so a
  friend on the same Wi-Fi with the same phone is never caught by someone else's ban.
  Ways around it: a different device or network, or a modified client. A new arrival
  with exactly the same phone, OS version and settings on the banned person's network
  would also be refused.
- **Room codes**: six characters, about a billion codes. That's fine against guessing
  at the rate limit, but a code someone sees on your screen lets them in.
- **Spending**: on the paid plan, a determined attacker with many IPs can still run up
  usage. Set a usage alert in the Cloudflare dashboard. The free plan just stops at its
  daily limit (00:00 UTC reset) instead of billing.

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
