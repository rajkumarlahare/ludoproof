import test from "node:test";
import assert from "node:assert/strict";

import {
  LEGACY_RULESET,
  LEGACY_RULESET_KEY,
  LEGACY_TEAM_RULESET,
  ROLL_REVEAL_TIMEOUT_MS,
  RULESET,
  RULESET_KEY,
  TEAM_RULESET,
  TEAM_RULESET_KEY,
  addPlayer,
  applyMove,
  forfeitTimedOutRoll,
  newMatch,
  publicState,
  reserveRoll,
  rulesetForState,
  startMatch,
} from "../src/game.js";
import { rulesetHashForState } from "../src/match-room.js";

function waitingClassic(
  rulesetKey = RULESET_KEY,
) {
  return newMatch({
    matchId: "LPV2TEST01",
    hostPlayerId: "host",
    hostDisplayName: "Host",
    now: 1_000,
    targetPlayerCount: 2,
    rulesetKey,
  });
}

function activeClassic(
  rulesetKey = RULESET_KEY,
) {
  const waiting = addPlayer(waitingClassic(rulesetKey), {
    playerId: "guest",
    displayName: "Guest",
    tokenAuthHash: "guest-auth",
    now: 1_001,
  });
  return startMatch(waiting, "host", 1_002);
}

function resolvedMoveState({
  moverTokens,
  opponentTokens,
  outcome,
  tokenIndex = 0,
}) {
  const state = structuredClone(activeClassic());
  state.players[0].tokens = [...moverTokens];
  state.players[1].tokens = [...opponentTokens];
  state.pendingRoll = {
    status: "RESOLVED",
    seat: 0,
    playerId: "host",
    outcome,
    legalTokenIndexes: [tokenIndex],
  };
  return state;
}

test("production default stays v1 while explicit classic v2 is fully bindable", async () => {
  const legacy = newMatch({
    matchId: "LPLEGACY01",
    hostPlayerId: "host",
    hostDisplayName: "Host",
    now: 900,
    targetPlayerCount: 2,
  });
  assert.equal("rulesetKey" in legacy, false);
  assert.equal(rulesetForState(legacy), LEGACY_RULESET);
  assert.equal(publicState(legacy).rulesetId, "ludoproof-standard-v1");

  const legacyHash = await rulesetHashForState(legacy);
  assert.equal(
    legacyHash,
    "4bd777ac5ec430c0a70956dd83a6451f4f8f0e91848b09000791e912fe3886cc",
  );

  const current = waitingClassic();
  assert.equal(current.rulesetKey, RULESET_KEY);
  assert.equal(rulesetForState(current), RULESET);
  assert.equal(publicState(current).rulesetId, "ludoproof-standard-v2");
  assert.equal(
    await rulesetHashForState(current),
    "7ad14daf100d7e14eea9b0a6da3a68cbd85d35f647052add4433df2efd9eb862",
  );
  assert.notEqual(await rulesetHashForState(current), legacyHash);
});

test("production Team Up stays v1 while explicit team v2 is fully bindable", async () => {
  const legacy = newMatch({
    matchId: "LPTEAMV101",
    hostPlayerId: "host",
    hostDisplayName: "Host",
    now: 1_900,
    targetPlayerCount: 4,
    matchMode: "TEAM_UP",
  });
  assert.equal("rulesetKey" in legacy, false);
  assert.equal(rulesetForState(legacy), LEGACY_TEAM_RULESET);
  assert.equal(publicState(legacy).rulesetId, "ludoproof-team-v1");
  assert.equal(
    await rulesetHashForState(legacy),
    "75610e6d5908344586e1c0d22d5846bae5312c6e0274a4e613a8c5f8cd8bc426",
  );

  const current = newMatch({
    matchId: "LPV2TEAM1",
    hostPlayerId: "host",
    hostDisplayName: "Host",
    now: 2_000,
    targetPlayerCount: 4,
    matchMode: "TEAM_UP",
    rulesetKey: TEAM_RULESET_KEY,
  });
  assert.equal(current.rulesetKey, TEAM_RULESET_KEY);
  assert.equal(rulesetForState(current), TEAM_RULESET);
  assert.equal(publicState(current).rulesetId, "ludoproof-team-v2");
  assert.equal(
    await rulesetHashForState(current),
    "a10326d8a631381eba6bd775fe90decf1c3e5788f5862d19e4f23cf08fec57ce",
  );
});

test("v2 cryptographically declares the previously implicit gameplay policies", () => {
  assert.equal(RULESET_KEY, "CLASSIC_V2");
  assert.equal(RULESET.extraTurnOnHome, true);
  assert.equal(RULESET.rollRevealTimeoutMs, ROLL_REVEAL_TIMEOUT_MS);
  assert.equal(RULESET.rollTimeoutPolicy, "FORFEIT_ROLL_AND_ADVANCE_TURN");
  assert.equal(RULESET.replacementRoundAfterTimeout, false);
  assert.equal(RULESET.resetConsecutiveSixesOnTimeout, true);
  assert.equal(RULESET.ownTokenStacking, "ALLOWED");
  assert.equal(RULESET.opponentStackCapture, "CAPTURE_ALL_ON_UNSAFE_CELL");
  assert.equal(RULESET.startingPlayerPolicy, "HOST_SEAT_ZERO");
  assert.equal(RULESET.turnOrderPolicy, "SEQUENTIAL_SEAT_ORDER");
  assert.equal(LEGACY_RULESET_KEY, "CLASSIC_V1");
});

test("v2 host starts and timeout policy matches the declared contract", () => {
  const active = activeClassic();
  assert.equal(active.turnSeat, 0);

  active.consecutiveSixes[0] = 2;
  const reserved = reserveRoll(active, {
    playerId: "host",
    clientCommitment: "client-commitment",
    eventIndex: 0,
    actorHash: "actor-hash",
    previousStateHash: "state-hash",
    rulesetHash: "ruleset-hash",
    now: 3_000,
  });
  assert.equal(
    reserved.pendingRoll.revealDeadlineAt,
    3_000 + RULESET.rollRevealTimeoutMs,
  );

  const timedOut = forfeitTimedOutRoll(reserved, {
    now: reserved.pendingRoll.revealDeadlineAt,
  });
  assert.equal(timedOut.state.turnSeat, 1);
  assert.equal(timedOut.state.consecutiveSixes[0], 0);
  assert.equal(timedOut.timedOutRoll.replacementRoundAllowed, false);
});

test("v2 extra-turn, stacking, and stack-capture declarations match engine behavior", () => {
  const home = applyMove(
    resolvedMoveState({
      moverTokens: [56, -1, -1, -1],
      opponentTokens: [-1, -1, -1, -1],
      outcome: 1,
    }),
    { playerId: "host", tokenIndex: 0, now: 4_000 },
  );
  assert.equal(home.state.players[0].tokens[0], 57);
  assert.equal(home.state.turnSeat, 0);

  const stacked = applyMove(
    resolvedMoveState({
      moverTokens: [0, 1, -1, -1],
      opponentTokens: [-1, -1, -1, -1],
      outcome: 1,
    }),
    { playerId: "host", tokenIndex: 0, now: 4_100 },
  );
  assert.deepEqual(stacked.state.players[0].tokens.slice(0, 2), [1, 1]);

  // GREEN relative position 40 maps to global cell 1, the same unsafe cell
  // reached by RED moving from relative 0 to 1.
  const capture = applyMove(
    resolvedMoveState({
      moverTokens: [0, -1, -1, -1],
      opponentTokens: [40, 40, -1, -1],
      outcome: 1,
    }),
    { playerId: "host", tokenIndex: 0, now: 4_200 },
  );
  assert.equal(capture.captures, 2);
  assert.deepEqual(capture.state.players[1].tokens.slice(0, 2), [-1, -1]);
  assert.equal(capture.state.turnSeat, 0);
});
