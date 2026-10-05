import test from "node:test";
import assert from "node:assert/strict";

import {
  RULESET_V3,
  RULESET_V3_KEY,
  addPlayer,
  applyMove,
  attachRoundCommitment,
  newMatch,
  publicState,
  registerResolvedRoll,
  reserveRoll,
  rulesetForState,
  startMatch,
} from "../src/game.js";

function activeV3() {
  let state = newMatch({
    matchId: "LPV3TEST01",
    hostPlayerId: "host",
    hostDisplayName: "Host",
    now: 1_000,
    targetPlayerCount: 2,
    rulesetKey: RULESET_V3_KEY,
  });
  state = addPlayer(state, {
    playerId: "guest",
    displayName: "Guest",
    tokenAuthHash: "guest-auth",
    now: 1_001,
  });
  return startMatch(state, "host", 1_002);
}

function committedRoll(state, outcomeEventIndex = state.randomEventIndex) {
  const reserved = reserveRoll(state, {
    playerId: state.players[state.turnSeat].playerId,
    clientCommitment: `commit-${outcomeEventIndex}`,
    eventIndex: outcomeEventIndex,
    actorHash: `actor-${outcomeEventIndex}`,
    previousStateHash: `state-${outcomeEventIndex}`,
    rulesetHash: `rules-${outcomeEventIndex}`,
    now: 2_000 + outcomeEventIndex,
  });
  return attachRoundCommitment(reserved, {
    roundId: `round-${outcomeEventIndex}`,
    serverCommitment: `server-${outcomeEventIndex}`,
    revealDeadlineAt: 99_999,
    now: 2_100 + outcomeEventIndex,
  });
}

test("v3 is explicit and does not change the production v1 default", () => {
  const legacy = newMatch({
    matchId: "LPV3LEG001",
    hostPlayerId: "host",
    hostDisplayName: "Host",
    now: 900,
    targetPlayerCount: 2,
  });
  assert.equal("rulesetKey" in legacy, false);
  assert.equal(publicState(legacy).rulesetId, "ludoproof-standard-v1");

  const v3 = activeV3();
  assert.equal(v3.rulesetKey, RULESET_V3_KEY);
  assert.equal(rulesetForState(v3), RULESET_V3);
  assert.equal(publicState(v3).rulesetId, "ludoproof-standard-v3");
  assert.deepEqual(v3.openingRollConsumed, [false, false]);
});

test("v3 turns the first all-yard roll into six without counting it toward the six streak", () => {
  const committed = committedRoll(activeV3());
  const resolved = registerResolvedRoll(committed, {
    outcome: 2,
    proofDigest: "proof-0",
    now: 2_200,
  });

  assert.equal(resolved.randomOutcome, 2);
  assert.equal(resolved.effectiveOutcome, 6);
  assert.equal(resolved.openingRollApplied, true);
  assert.deepEqual(resolved.legalTokenIndexes, [0, 1, 2, 3]);
  assert.equal(resolved.state.pendingRoll.outcome, 6);
  assert.equal(resolved.state.pendingRoll.randomOutcome, 2);
  assert.equal(resolved.state.consecutiveSixes[0], 0);
  assert.deepEqual(resolved.state.openingRollConsumed, [true, false]);
});

test("v3 opening guarantee is consumed once and later rolls keep the verified outcome", () => {
  const first = registerResolvedRoll(committedRoll(activeV3()), {
    outcome: 1,
    proofDigest: "proof-0",
    now: 2_200,
  });
  const moved = applyMove(first.state, {
    playerId: "host",
    tokenIndex: 0,
    now: 2_300,
  });

  // Six grants another turn. The next verified roll is no longer transformed.
  const second = registerResolvedRoll(committedRoll(moved.state), {
    outcome: 4,
    proofDigest: "proof-1",
    now: 2_400,
  });

  assert.equal(second.openingRollApplied, false);
  assert.equal(second.randomOutcome, 4);
  assert.equal(second.effectiveOutcome, 4);
  assert.equal(second.state.pendingRoll.outcome, 4);
});
