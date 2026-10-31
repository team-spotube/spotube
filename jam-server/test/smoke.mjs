// End-to-end smoke test against a running server: `npm run dev`, then `npm test`.
// Point it elsewhere with JAM_URL=https://spotube-jam.<account>.workers.dev npm test
import assert from "node:assert/strict";

const BASE = process.env.JAM_URL ?? "http://127.0.0.1:8787";
const WS_BASE = BASE.replace(/^http/, "ws");
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const code = () => Array.from({ length: 6 }, () => ALPHABET[Math.floor(Math.random() * ALPHABET.length)]).join("");
const newKey = () => crypto.randomUUID().replaceAll("-", "");

/** [id] is only a label for errors; the server assigns the real id (welcome.you). */
function connect(room, id, mode, name = id, key = newKey(), dev = null) {
  const devParam = dev ? `&dev=${dev}` : "";
  const ws = new WebSocket(`${WS_BASE}/v1/rooms/${room}?key=${key}&name=${encodeURIComponent(name)}&mode=${mode}${devParam}`);
  const inbox = [];
  const waiters = [];
  ws.addEventListener("message", (e) => {
    const frame = e.data === "pong" ? { t: "keepalive" } : JSON.parse(e.data);
    const i = waiters.findIndex((w) => w.match(frame));
    if (i >= 0) waiters.splice(i, 1)[0].resolve(frame);
    else inbox.push(frame);
  });
  const closed = new Promise((resolve) => ws.addEventListener("close", (e) => resolve(e.code)));
  return {
    ws,
    key,
    closed,
    send: (frame) => ws.send(JSON.stringify(frame)),
    next(t, pred = () => true, timeoutMs = 5_000) {
      const match = (f) => f.t === t && pred(f);
      const i = inbox.findIndex(match);
      if (i >= 0) return Promise.resolve(inbox.splice(i, 1)[0]);
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error(`${id}: timed out waiting for ${t}`)), timeoutMs);
        waiters.push({ match, resolve: (f) => (clearTimeout(timer), resolve(f)) });
      });
    },
  };
}

const step = async (name, fn) => {
  await fn();
  console.log(`ok - ${name}`);
};

await step("health", async () => {
  const res = await (await fetch(`${BASE}/v1/health`)).json();
  assert.equal(res.ok, true);
  assert.equal(res.v, 1);
});

const room = code();
const host = connect(room, "host", "create", "Host");
let guest, hostId, guestId;

await step("host creates the room", async () => {
  const welcome = await host.next("welcome");
  hostId = welcome.you;
  assert.match(hostId, /^[A-Za-z0-9_-]{16}$/);
  assert.equal(welcome.host, hostId);
  host.send({ t: "queue", body: { items: [1, 2, 3] } });
});

await step("guest joins and gets the retained queue", async () => {
  // The host's queue frame is processed before the guest's join is.
  host.send({ t: "ping", id: 1, c: 0 });
  await host.next("pong");
  guest = connect(room, "guest", "join", "Guest\u202e\u0000  1");
  const welcome = await guest.next("welcome");
  guestId = welcome.you;
  assert.equal(welcome.host, hostId);
  assert.deepEqual(welcome.queue, { items: [1, 2, 3] });
  const me = welcome.participants.find((p) => p.id === guestId);
  assert.equal(me.name, "Guest 1", "control and formatting characters are stripped");
  await host.next("presence", (f) => f.participants.length === 2);
});

await step("clock ping", async () => {
  guest.send({ t: "ping", id: 7, c: Date.now() });
  const pong = await guest.next("pong");
  assert.equal(pong.id, 7);
  assert.equal(typeof pong.s, "number");
});

await step("keep-alives are answered", async () => {
  guest.ws.send("ping");
  await guest.next("keepalive");
});

await step("anchor fans out to guests", async () => {
  host.send({ t: "anchor", body: { positionMs: 1234 } });
  const anchor = await guest.next("anchor");
  assert.equal(anchor.body.positionMs, 1234);
});

await step("guest anchors are ignored", async () => {
  guest.send({ t: "anchor", body: { positionMs: 1 } });
  guest.send({ t: "ping", id: 8, c: 0 });
  await guest.next("pong", (f) => f.id === 8);
  await assert.rejects(host.next("anchor", () => true, 500));
});

await step("guest commands reach the host", async () => {
  guest.send({ t: "cmd", body: { type: "skipNext" } });
  const cmd = await host.next("cmd");
  assert.equal(cmd.from, guestId);
});

await step("nobody can take over the host's identity", async () => {
  // The host's id is public; using it as a key yields a different id.
  const impostor = connect(room, "impostor", "join", "Host", hostId.padEnd(22, "x"));
  const welcome = await impostor.next("welcome");
  assert.notEqual(welcome.you, hostId);
  assert.equal(welcome.host, hostId);
  impostor.send({ t: "kick", target: guestId, ban: true });
  impostor.send({ t: "anchor", body: { positionMs: 666 } });
  impostor.send({ t: "ping", id: 9, c: 0 });
  await impostor.next("pong");
  await assert.rejects(guest.next("anchor", () => true, 500), "an impostor's anchor isn't relayed");
  impostor.ws.close();
  await host.next("presence", (f) => f.participants.length === 2);
});

await step("a taken code can't be created again", async () => {
  const intruder = connect(room, "intruder", "create");
  const error = await intruder.next("error");
  assert.equal(error.code, "room_taken");
  assert.equal(await intruder.closed, 4001);
});

await step("unknown rooms are not found", async () => {
  const lost = connect(code(), "lost", "join");
  assert.equal((await lost.next("error")).code, "room_not_found");
});

await step("ban closes the socket and blocks rejoining", async () => {
  host.send({ t: "kick", target: guestId, ban: true });
  assert.equal((await guest.next("error")).code, "banned");
  assert.equal(await guest.closed, 4003);
  const again = connect(room, "guest", "join", "Guest", guest.key);
  assert.equal((await again.next("error")).code, "banned");
});

await step("a ban covers the device, not just the identity", async () => {
  const party = code();
  const phone = newKey(); // The same device digest: same model, OS, locale…
  const owner = connect(party, "owner", "create", "Owner", newKey(), newKey());
  await owner.next("welcome");
  // A friend with an identical phone, already in the room before the ban.
  const friend = connect(party, "friend", "join", "Friend", newKey(), phone);
  const friendId = (await friend.next("welcome")).you;
  const troll = connect(party, "troll", "join", "Troll", newKey(), phone);
  const trollId = (await troll.next("welcome")).you;

  owner.send({ t: "kick", target: trollId, ban: true });
  assert.equal((await troll.next("error")).code, "banned");

  // Back with a fresh identity (reinstall) from the same device and network: refused.
  const again = connect(party, "troll-again", "join", "Troll", newKey(), phone);
  assert.equal((await again.next("error")).code, "banned");
  // The friend can still reconnect, and someone on another device can join.
  friend.ws.close();
  const friendBack = connect(party, "friend", "join", "Friend", friend.key, phone);
  assert.equal((await friendBack.next("welcome")).you, friendId);
  const stranger = connect(party, "stranger", "join", "Stranger", newKey(), newKey());
  await stranger.next("welcome");
  for (const c of [owner, friendBack, stranger]) c.ws.close();
});

await step("a client that floods is cut off", async () => {
  const flooder = connect(code(), "flooder", "create");
  await flooder.next("welcome");
  for (let i = 0; i < 100; i++) flooder.send({ t: "ping", id: i, c: 0 });
  assert.equal(await flooder.closed, 4029);
});

await step("host hands over after the grace period", async () => {
  const heir = connect(room, "heir", "join", "Heir");
  const heirId = (await heir.next("welcome")).you;
  host.ws.close();
  const presence = await heir.next("presence", (f) => f.host === heirId, 45_000);
  assert.deepEqual(presence.participants.map((p) => p.id), [heirId]);
  heir.ws.close();
});

await step("an empty room can't be taken over, but its host can bring it back", async () => {
  const other = code();
  const owner = connect(other, "owner", "create");
  await owner.next("welcome");
  owner.ws.close();
  await owner.closed;
  const squatter = connect(other, "squatter", "create");
  assert.equal((await squatter.next("error")).code, "room_taken");
  const back = connect(other, "owner", "create", "Owner", owner.key);
  const welcome = await back.next("welcome");
  assert.equal(welcome.host, welcome.you);
  back.ws.close();
});

console.log("all smoke tests passed");
process.exit(0);
