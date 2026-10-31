// End-to-end smoke test against a running server: `npm run dev`, then `npm test`.
// Point it elsewhere with JAM_URL=https://spotube-jam.<account>.workers.dev npm test
import assert from "node:assert/strict";

const BASE = process.env.JAM_URL ?? "http://127.0.0.1:8787";
const WS_BASE = BASE.replace(/^http/, "ws");
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const code = () => Array.from({ length: 6 }, () => ALPHABET[Math.floor(Math.random() * ALPHABET.length)]).join("");

function connect(room, id, mode, name = id) {
  const ws = new WebSocket(`${WS_BASE}/v1/rooms/${room}?id=${id}&name=${encodeURIComponent(name)}&mode=${mode}`);
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
const host = connect(room, "host-0001", "create", "Host");
let guest;

await step("host creates the room", async () => {
  const welcome = await host.next("welcome");
  assert.equal(welcome.host, "host-0001");
  host.send({ t: "queue", body: { items: [1, 2, 3] } });
});

await step("guest joins and gets the retained queue", async () => {
  // The host's queue frame is processed before the guest's join is.
  host.send({ t: "ping", id: 1, c: 0 });
  await host.next("pong");
  guest = connect(room, "guest-0001", "join", "Guest");
  const welcome = await guest.next("welcome");
  assert.equal(welcome.host, "host-0001");
  assert.deepEqual(welcome.queue, { items: [1, 2, 3] });
  assert.ok(welcome.participants.some((p) => p.id === "guest-0001"));
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
  assert.equal(cmd.from, "guest-0001");
});

await step("a taken code can't be created again", async () => {
  const intruder = connect(room, "intruder-1", "create");
  const error = await intruder.next("error");
  assert.equal(error.code, "room_taken");
  assert.equal(await intruder.closed, 4001);
});

await step("unknown rooms are not found", async () => {
  const lost = connect(code(), "lost-0001", "join");
  assert.equal((await lost.next("error")).code, "room_not_found");
});

await step("ban closes the socket and blocks rejoining", async () => {
  host.send({ t: "kick", target: "guest-0001", ban: true });
  assert.equal((await guest.next("error")).code, "banned");
  assert.equal(await guest.closed, 4003);
  const again = connect(room, "guest-0001", "join");
  assert.equal((await again.next("error")).code, "banned");
});

await step("host hands over after the grace period", async () => {
  const heir = connect(room, "heir-0001", "join", "Heir");
  await heir.next("welcome");
  host.ws.close();
  const presence = await heir.next("presence", (f) => f.host === "heir-0001", 45_000);
  assert.deepEqual(presence.participants.map((p) => p.id), ["heir-0001"]);
  heir.ws.close();
});

console.log("all smoke tests passed");
process.exit(0);
