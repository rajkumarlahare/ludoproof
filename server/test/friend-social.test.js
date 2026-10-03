import test from "node:test";
import assert from "node:assert/strict";

import {
  FriendConversation,
  FriendRecentRoom,
} from "../src/friend-social.js";

class MemoryStorage {
  constructor() {
    this.values = new Map();
  }

  async get(key) {
    const value = this.values.get(key);
    return value == null ? value : structuredClone(value);
  }

  async put(key, value) {
    this.values.set(key, structuredClone(value));
  }
}

function jsonRequest(path, body) {
  return new Request(`https://unit.test${path}`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(body),
  });
}

const A = "LPF-ABCD-EFGH-JKLM";
const B = "LPF-MNPQ-RSTU-VWXY";
const C = "LPF-2345-6789-ABCD";
const REQUEST_1 = "00000000-0000-4000-8000-000000000001";
const REQUEST_2 = "00000000-0000-4000-8000-000000000002";

test("friend conversation is idempotent and marks incoming messages read", async () => {
  const room = new FriendConversation({
    storage: new MemoryStorage(),
  });

  const first = await room.fetch(
    jsonRequest("/send", {
      senderId: A,
      receiverId: B,
      text: "Ready for a match?",
      clientRequestId: REQUEST_1,
    }),
  );
  assert.equal(first.status, 201);
  const firstBody = await first.json();
  assert.equal(firstBody.replayed, false);
  assert.equal(firstBody.message.readAt, null);

  const replay = await room.fetch(
    jsonRequest("/send", {
      senderId: A,
      receiverId: B,
      text: "Ready for a match?",
      clientRequestId: REQUEST_1,
    }),
  );
  assert.equal(replay.status, 200);
  assert.equal((await replay.json()).replayed, true);

  const list = await room.fetch(
    jsonRequest("/list", {
      meId: B,
      otherId: A,
    }),
  );
  assert.equal(list.status, 200);
  const listBody = await list.json();
  assert.equal(listBody.messages.length, 1);
  assert.ok(Number.isSafeInteger(listBody.messages[0].readAt));
});

test("friend conversation rejects idempotency conflicts and pair drift", async () => {
  const room = new FriendConversation({
    storage: new MemoryStorage(),
  });

  await room.fetch(
    jsonRequest("/send", {
      senderId: A,
      receiverId: B,
      text: "First",
      clientRequestId: REQUEST_1,
    }),
  );

  const conflict = await room.fetch(
    jsonRequest("/send", {
      senderId: A,
      receiverId: B,
      text: "Changed",
      clientRequestId: REQUEST_1,
    }),
  );
  assert.equal(conflict.status, 409);
  assert.equal((await conflict.json()).error, "MESSAGE_IDEMPOTENCY_CONFLICT");

  const pairDrift = await room.fetch(
    jsonRequest("/list", {
      meId: A,
      otherId: C,
    }),
  );
  assert.equal(pairDrift.status, 409);
  assert.equal((await pairDrift.json()).error, "CONVERSATION_PAIR_CONFLICT");
});

test("friend conversation validates message size", async () => {
  const room = new FriendConversation({
    storage: new MemoryStorage(),
  });

  const empty = await room.fetch(
    jsonRequest("/send", {
      senderId: A,
      receiverId: B,
      text: "   ",
      clientRequestId: REQUEST_1,
    }),
  );
  assert.equal(empty.status, 400);
  assert.equal((await empty.json()).error, "INVALID_MESSAGE");

  const tooLong = await room.fetch(
    jsonRequest("/send", {
      senderId: A,
      receiverId: B,
      text: "x".repeat(241),
      clientRequestId: REQUEST_2,
    }),
  );
  assert.equal(tooLong.status, 400);
  assert.equal((await tooLong.json()).error, "INVALID_MESSAGE");
});

test("recent-player history deduplicates opponents and keeps newest first", async () => {
  const room = new FriendRecentRoom({
    storage: new MemoryStorage(),
  });

  const records = [
    {
      opponentFriendId: B,
      displayName: "Beta",
      matchId: "LPABCDEFGH",
      playedAt: 100,
    },
    {
      opponentFriendId: C,
      displayName: "Gamma",
      matchId: "LPBCDEFGH2",
      playedAt: 200,
    },
    {
      opponentFriendId: B,
      displayName: "Beta Updated",
      matchId: "LPCDEFGH23",
      playedAt: 300,
    },
  ];

  for (const record of records) {
    const response = await room.fetch(
      jsonRequest("/record", {
        ownerFriendId: A,
        ...record,
      }),
    );
    assert.equal(response.status, 200);
  }

  const list = await room.fetch(
    jsonRequest("/list", {
      ownerFriendId: A,
    }),
  );
  assert.equal(list.status, 200);
  const body = await list.json();
  assert.equal(body.recent.length, 2);
  assert.equal(body.recent[0].opponentFriendId, B);
  assert.equal(body.recent[0].displayName, "Beta Updated");
  assert.equal(body.recent[0].playedAt, 300);
  assert.equal(body.recent[1].opponentFriendId, C);
});
