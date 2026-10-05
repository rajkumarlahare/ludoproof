import test from "node:test";
import assert from "node:assert/strict";

import {
  buildMatchReceiptSnapshot,
} from "../src/match-receipt-archive.js";
import {
  TEAM_ASSIGNMENTS,
  addPlayer,
  newMatch,
  startMatch,
} from "../src/game.js";

function teamState() {
  let state = newMatch({
    matchId: "LPTEAM2345",
    hostPlayerId: "p_red",
    hostDisplayName: "Red",
    now: 100,
    targetPlayerCount: 4,
    matchMode: "TEAM_UP",
  });
  for (const [index, color] of ["green", "yellow", "blue"].entries()) {
    state = addPlayer(state, {
      playerId: `p_${color}`,
      displayName: color,
      tokenAuthHash: `hash-${index}`,
      now: 101 + index,
    });
  }
  return startMatch(state, "p_red", 110);
}

test("Team Up receipt snapshot seals explicit team rules metadata", () => {
  const state = teamState();
  state.winnerTeamId = "A";
  const snapshot = buildMatchReceiptSnapshot(state);

  assert.equal(snapshot.matchMode, "TEAM_UP");
  assert.equal(snapshot.rulesetId, "ludoproof-team-v2");
  assert.deepEqual(snapshot.teamAssignments, TEAM_ASSIGNMENTS);
  assert.equal(snapshot.winnerTeamId, "A");
  assert.deepEqual(
    snapshot.players.map((player) => player.teamId),
    ["A", "B", "A", "B"],
  );
});

test("classic Online receipt shape does not gain Team Up fields", () => {
  const state = newMatch({
    matchId: "LPONLIN234",
    hostPlayerId: "p_host",
    hostDisplayName: "Host",
    now: 100,
    targetPlayerCount: 2,
    matchMode: "ONLINE",
  });
  state.status = "ACTIVE";
  state.turnSeat = 0;

  const snapshot = buildMatchReceiptSnapshot(state);
  assert.equal(snapshot.rulesetId, "ludoproof-standard-v2");
  assert.equal("teamAssignments" in snapshot, false);
  assert.equal("winnerTeamId" in snapshot, false);
  assert.equal("teamId" in snapshot.players[0], false);
});
