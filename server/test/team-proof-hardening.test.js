import test from "node:test";
import assert from "node:assert/strict";
import {
  newMatch,
} from "../src/game.js";
import {
  rulesetHashForState,
} from "../src/match-room.js";
import {
  MatchRoom as TeamMatchRoom,
} from "../src/match-room-team.js";

const LEGACY_CLASSIC_RULESET_HASH =
  "4bd777ac5ec430c0a70956dd83a6451f4f8f0e91848b09000791e912fe3886cc";
const LEGACY_TEAM_RULESET_HASH =
  "75610e6d5908344586e1c0d22d5846bae5312c6e0274a4e613a8c5f8cd8bc426";
const CLASSIC_V2_RULESET_HASH =
  "7ad14daf100d7e14eea9b0a6da3a68cbd85d35f647052add4433df2efd9eb862";
const TEAM_V2_RULESET_HASH =
  "a10326d8a631381eba6bd775fe90decf1c3e5788f5862d19e4f23cf08fec57ce";

class TestStorage {
  constructor() {
    this.values = new Map();
    this.putCount = 0;
    this.alarm = null;
  }

  async get(key) {
    const value = this.values.get(key);
    return value == null ? value : structuredClone(value);
  }

  async put(key, value) {
    this.putCount += 1;
    this.values.set(key, structuredClone(value));
  }

  async setAlarm(timestamp) {
    this.alarm = timestamp;
  }
}

function roomHarness() {
  const storage = new TestStorage();
  const ctx = {
    storage,
    getWebSockets() {
      return [];
    },
  };
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY: "t".repeat(32),
  };
  return {
    storage,
    room: new TeamMatchRoom(ctx, env),
  };
}

function createRequest({
  matchMode = "TEAM_UP",
  targetPlayerCount = 4,
} = {}) {
  return new Request("https://match/create", {
    method: "POST",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify({
      displayName: "Red",
      characterId: "duck",
      matchId: "LPABCDEFGH",
      clientRequestId: "00000000-0000-4000-8000-000000000001",
      targetPlayerCount,
      matchMode,
    }),
  });
}

test("legacy classic and team ruleset digests remain frozen", async () => {
  const classic = newMatch({
    matchId: "LPQWERTY23",
    hostPlayerId: "p_host",
    hostDisplayName: "Host",
    now: 1,
    targetPlayerCount: 2,
    matchMode: "ONLINE",
  });
  delete classic.rulesetKey;

  const team = newMatch({
    matchId: "LPABCDEFGH",
    hostPlayerId: "p_host",
    hostDisplayName: "Host",
    now: 1,
    targetPlayerCount: 4,
    matchMode: "TEAM_UP",
  });
  delete team.rulesetKey;

  assert.equal(
    await rulesetHashForState(classic),
    LEGACY_CLASSIC_RULESET_HASH,
  );
  assert.equal(
    await rulesetHashForState(team),
    LEGACY_TEAM_RULESET_HASH,
  );
});

test("new matches bind the frozen v2 classic and team ruleset digests", async () => {
  const classic = newMatch({
    matchId: "LPQWERTY23",
    hostPlayerId: "p_host",
    hostDisplayName: "Host",
    now: 1,
    targetPlayerCount: 2,
    matchMode: "ONLINE",
  });
  const team = newMatch({
    matchId: "LPABCDEFGH",
    hostPlayerId: "p_host",
    hostDisplayName: "Host",
    now: 1,
    targetPlayerCount: 4,
    matchMode: "TEAM_UP",
  });

  assert.equal(
    await rulesetHashForState(classic),
    CLASSIC_V2_RULESET_HASH,
  );
  assert.equal(
    await rulesetHashForState(team),
    TEAM_V2_RULESET_HASH,
  );
  assert.notEqual(TEAM_V2_RULESET_HASH, CLASSIC_V2_RULESET_HASH);
});

test("Team Up create persists the final team state exactly once", async () => {
  const { room, storage } = roomHarness();
  const response = await room.fetch(createRequest());
  const payload = await response.json();
  const stored = await storage.get("match-state");

  assert.equal(response.status, 201);
  assert.equal(storage.putCount, 1);
  assert.equal(stored.matchMode, "TEAM_UP");
  assert.equal(stored.rulesetKey, "TEAM_UP_V2");
  assert.deepEqual(stored.teamAssignments, ["A", "B", "A", "B"]);
  assert.equal(stored.players[0].teamId, "A");
  assert.equal(payload.state.matchMode, "TEAM_UP");
  assert.equal(payload.state.rulesetId, "ludoproof-team-v2");
});

test("Team Up create rejects any player count other than four before persistence", async () => {
  const { room, storage } = roomHarness();
  const response = await room.fetch(
    createRequest({ targetPlayerCount: 2 }),
  );
  const payload = await response.json();

  assert.equal(response.status, 400);
  assert.equal(payload.error, "TEAM_UP_REQUIRES_FOUR_PLAYERS");
  assert.equal(storage.putCount, 0);
  assert.equal(await storage.get("match-state"), undefined);
});
