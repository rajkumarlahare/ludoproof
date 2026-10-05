import test from "node:test";
import assert from "node:assert/strict";
import {
  MatchRoom,
  MOVE_SELECTION_TIMEOUT_MS,
} from "../src/match-room-lifecycle.js";

class FakeStorage {
  constructor(state) {
    this.values = new Map([["match-state", structuredClone(state)]]);
    this.alarmAt = null;
  }

  async get(key) {
    const value = this.values.get(key);
    return value == null ? value : structuredClone(value);
  }

  async put(key, value) {
    this.values.set(key, structuredClone(value));
  }

  async setAlarm(timestamp) {
    this.alarmAt = timestamp;
  }

  async deleteAll() {
    this.values.clear();
  }
}

function stateWithResolvedMove(resolvedAt) {
  return {
    schemaVersion: 1,
    matchId: "LPABCDEFGH",
    status: "ACTIVE",
    createdAt: resolvedAt - 5_000,
    updatedAt: resolvedAt,
    revision: 7,
    hostPlayerId: "p1",
    targetPlayerCount: 2,
    matchMode: "ONLINE",
    players: [
      {
        playerId: "p1",
        displayName: "One",
        characterId: "duck",
        color: "RED",
        tokenAuthHash: "a".repeat(64),
        tokens: [0, -1, -1, -1],
      },
      {
        playerId: "p2",
        displayName: "Two",
        characterId: "dog",
        color: "GREEN",
        tokenAuthHash: "b".repeat(64),
        tokens: [-1, -1, -1, -1],
      },
    ],
    turnSeat: 0,
    randomEventIndex: 1,
    consecutiveSixes: [1, 0],
    pendingRoll: {
      status: "RESOLVED",
      seat: 0,
      playerId: "p1",
      eventIndex: 0,
      eventId: "roll:0",
      roundId: "round-0",
      clientCommitment: "c".repeat(64),
      serverCommitment: "d".repeat(64),
      proofDigest: "e".repeat(64),
      outcome: 6,
      legalTokenIndexes: [0, 1],
      resolvedAt,
      revealDeadlineAt: resolvedAt + 4 * 60 * 1000,
    },
    winnerPlayerId: null,
    finishedAt: null,
    history: [
      {
        eventIndex: 0,
        playerId: "p1",
        status: "RESOLVED",
        outcome: 6,
        fairnessDigest: "f".repeat(64),
      },
    ],
  };
}

function makeRoom(state) {
  const storage = new FakeStorage(state);
  const ctx = {
    storage,
    getWebSockets() {
      return [];
    },
  };
  return {
    storage,
    room: new MatchRoom(ctx, {}),
  };
}

test("resolved roll schedules the authoritative move-selection deadline", async () => {
  const resolvedAt = Date.now();
  const { room, storage } = makeRoom(stateWithResolvedMove(resolvedAt));

  await room.alarm();

  assert.equal(
    storage.alarmAt,
    resolvedAt + MOVE_SELECTION_TIMEOUT_MS,
  );
  const stored = await storage.get("match-state");
  assert.equal(stored.pendingRoll.status, "RESOLVED");
  assert.equal(stored.turnSeat, 0);
});

test("expired resolved roll forfeits the move and advances the turn", async () => {
  const resolvedAt = Date.now() - MOVE_SELECTION_TIMEOUT_MS - 5_000;
  const { room, storage } = makeRoom(stateWithResolvedMove(resolvedAt));

  await room.alarm();

  const stored = await storage.get("match-state");
  assert.equal(stored.status, "ACTIVE");
  assert.equal(stored.pendingRoll, null);
  assert.equal(stored.turnSeat, 1);
  assert.deepEqual(stored.consecutiveSixes, [0, 0]);
  assert.equal(stored.revision, 8);
  assert.equal(
    stored.history[0].moveTimeoutReason,
    "MOVE_SELECTION_TIMEOUT",
  );
  assert.ok(Number.isSafeInteger(stored.history[0].moveTimedOutAt));
});

test("move timeout skips players that already forfeited", async () => {
  const resolvedAt = Date.now() - MOVE_SELECTION_TIMEOUT_MS - 5_000;
  const state = stateWithResolvedMove(resolvedAt);
  state.targetPlayerCount = 4;
  state.players.push(
    {
      playerId: "p3",
      displayName: "Three",
      characterId: "goat",
      color: "YELLOW",
      tokenAuthHash: "c".repeat(64),
      tokens: [-1, -1, -1, -1],
    },
    {
      playerId: "p4",
      displayName: "Four",
      characterId: "cat",
      color: "BLUE",
      tokenAuthHash: "d".repeat(64),
      tokens: [-1, -1, -1, -1],
    },
  );
  state.players[1].forfeitedAt = Date.now() - 10_000;
  state.consecutiveSixes = [1, 0, 0, 0];

  const { room, storage } = makeRoom(state);
  await room.alarm();

  const stored = await storage.get("match-state");
  assert.equal(stored.turnSeat, 2);
});
