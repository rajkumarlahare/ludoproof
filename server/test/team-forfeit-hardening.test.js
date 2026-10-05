import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import {
  MatchRoom,
} from "../src/match-room-team.js";

const PLAYER_TOKEN_HASH_DOMAIN = "ludoproof:player-token:v1:";

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
}

function tokenHash(token) {
  return createHash("sha256")
    .update(PLAYER_TOKEN_HASH_DOMAIN + token, "utf8")
    .digest("hex");
}

function teamState({ pendingRoll = null } = {}) {
  const tokens = ["token-red", "token-green", "token-yellow", "token-blue"];
  return {
    tokens,
    state: {
      schemaVersion: 1,
      matchId: "LPABCDEFGH",
      status: "ACTIVE",
      createdAt: 1,
      updatedAt: 2,
      revision: 9,
      hostPlayerId: "p_red",
      targetPlayerCount: 4,
      matchMode: "TEAM_UP",
      teamAssignments: ["A", "B", "A", "B"],
      players: [
        {
          playerId: "p_red",
          displayName: "Red",
          characterId: "duck",
          color: "RED",
          teamId: "A",
          tokenAuthHash: tokenHash(tokens[0]),
          tokens: [0, -1, -1, -1],
        },
        {
          playerId: "p_green",
          displayName: "Green",
          characterId: "dog",
          color: "GREEN",
          teamId: "B",
          tokenAuthHash: tokenHash(tokens[1]),
          tokens: [-1, -1, -1, -1],
        },
        {
          playerId: "p_yellow",
          displayName: "Yellow",
          characterId: "goat",
          color: "YELLOW",
          teamId: "A",
          tokenAuthHash: tokenHash(tokens[2]),
          tokens: [-1, -1, -1, -1],
        },
        {
          playerId: "p_blue",
          displayName: "Blue",
          characterId: "cat",
          color: "BLUE",
          teamId: "B",
          tokenAuthHash: tokenHash(tokens[3]),
          tokens: [-1, -1, -1, -1],
        },
      ],
      turnSeat: 0,
      randomEventIndex: pendingRoll == null ? 0 : 1,
      consecutiveSixes: [0, 0, 0, 0],
      pendingRoll,
      winnerPlayerId: null,
      winnerTeamId: null,
      finishedAt: null,
      history: [],
    },
  };
}

function committedPendingRoll() {
  return {
    status: "COMMITTED",
    seat: 0,
    scheduledSeat: 0,
    playerId: "p_red",
    eventIndex: 0,
    eventId: "roll:0",
    roundId: "round-0",
    clientCommitment: "a".repeat(64),
    serverCommitment: "b".repeat(64),
    actorHash: "c".repeat(64),
    previousStateHash: "d".repeat(64),
    rulesetHash: "e".repeat(64),
    proofDigest: null,
    outcome: null,
    legalTokenIndexes: null,
    revealDeadlineAt: Date.now() + 60_000,
  };
}

function requestForfeit(token) {
  return new Request("https://room/start", {
    method: "POST",
    headers: {
      authorization: "Bearer " + token,
      "content-type": "application/json",
    },
    body: JSON.stringify({ forfeit: true }),
  });
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

test("Team Up forfeit waits for the shared mutation queue", async () => {
  const { state, tokens } = teamState();
  const { room, storage } = makeRoom(state);

  let release;
  room.mutationTail = new Promise((resolve) => {
    release = resolve;
  });

  const pendingResponse = room.fetch(requestForfeit(tokens[1]));
  await new Promise((resolve) => setTimeout(resolve, 0));

  const beforeRelease = await storage.get("match-state");
  assert.equal(beforeRelease.status, "ACTIVE");

  release();
  const response = await pendingResponse;
  assert.equal(response.status, 200);

  const afterRelease = await storage.get("match-state");
  assert.equal(afterRelease.status, "FINISHED");
  assert.equal(afterRelease.winnerTeamId, "A");
});

test("Team Up forfeit always clears another player's pending roll", async () => {
  const { state, tokens } = teamState({
    pendingRoll: committedPendingRoll(),
  });
  const { room, storage } = makeRoom(state);

  const response = await room.fetch(requestForfeit(tokens[1]));
  const body = await response.json();
  assert.equal(response.status, 200, JSON.stringify(body));
  assert.equal(body.state.status, "FINISHED");
  assert.equal(body.state.pendingRoll, null);
  assert.equal(body.winnerTeamId, "A");

  const stored = await storage.get("match-state");
  assert.equal(stored.pendingRoll, null);
  assert.equal(stored.status, "FINISHED");
  assert.equal(stored.history.length, 1);
  assert.equal(stored.history[0].eventIndex, 0);
  assert.equal(stored.history[0].status, "FORFEITED");
  assert.equal(stored.randomEventIndex, 1);
});
