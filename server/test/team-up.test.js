import test from "node:test";
import assert from "node:assert/strict";
import {
  TEAM_ASSIGNMENTS,
  actingSeatForTurn,
  addPlayer,
  applyMove,
  assertCanCommitRoll,
  authoritativeStateForRandomness,
  newMatch,
  publicState,
  startMatch,
} from "../src/game.js";

function fourPlayerTeamMatch() {
  let state = newMatch({
    matchId: "LPABCDEFGH".slice(0, 10),
    hostPlayerId: "p_red",
    hostDisplayName: "Red",
    now: 1,
    targetPlayerCount: 4,
    matchMode: "TEAM_UP",
  });
  state = addPlayer(state, {
    playerId: "p_green",
    displayName: "Green",
    tokenAuthHash: "g",
    now: 2,
  });
  state = addPlayer(state, {
    playerId: "p_yellow",
    displayName: "Yellow",
    tokenAuthHash: "y",
    now: 3,
  });
  state = addPlayer(state, {
    playerId: "p_blue",
    displayName: "Blue",
    tokenAuthHash: "b",
    now: 4,
  });
  return startMatch(state, "p_red", 5);
}

test("Team Up assigns opposite colors to the same team", () => {
  const state = fourPlayerTeamMatch();
  assert.deepEqual(state.teamAssignments, TEAM_ASSIGNMENTS);
  assert.deepEqual(
    state.players.map((player) => player.teamId),
    ["A", "B", "A", "B"],
  );
  const publicValue = publicState(state);
  assert.equal(publicValue.rulesetId, "ludoproof-team-v2");
  assert.equal(publicValue.actingSeat, 0);
});

test("Team Up never captures a friendly partner but still captures opponents", () => {
  const state = fourPlayerTeamMatch();
  state.players[0].tokens = [4, -1, -1, -1];
  // Global cell 5: GREEN relative 44, YELLOW relative 31.
  state.players[1].tokens = [44, -1, -1, -1];
  state.players[2].tokens = [31, -1, -1, -1];
  state.pendingRoll = {
    status: "RESOLVED",
    seat: 0,
    scheduledSeat: 0,
    playerId: "p_red",
    outcome: 1,
    legalTokenIndexes: [0],
  };

  const result = applyMove(state, {
    playerId: "p_red",
    tokenIndex: 0,
    now: 10,
  });

  assert.equal(result.captures, 1);
  assert.equal(result.state.players[1].tokens[0], -1);
  assert.equal(result.state.players[2].tokens[0], 31);
});

test("Team Up finishes only after both partners complete all tokens", () => {
  const incomplete = fourPlayerTeamMatch();
  incomplete.players[0].tokens = [56, 57, 57, 57];
  incomplete.players[2].tokens = [57, 57, 57, 56];
  incomplete.pendingRoll = {
    status: "RESOLVED",
    seat: 0,
    scheduledSeat: 0,
    playerId: "p_red",
    outcome: 1,
    legalTokenIndexes: [0],
  };
  const first = applyMove(incomplete, {
    playerId: "p_red",
    tokenIndex: 0,
    now: 20,
  });
  assert.equal(first.state.status, "ACTIVE");
  assert.equal(first.state.winnerTeamId, null);

  const winning = fourPlayerTeamMatch();
  winning.players[0].tokens = [56, 57, 57, 57];
  winning.players[2].tokens = [57, 57, 57, 57];
  winning.pendingRoll = {
    status: "RESOLVED",
    seat: 0,
    scheduledSeat: 0,
    playerId: "p_red",
    outcome: 1,
    legalTokenIndexes: [0],
  };
  const final = applyMove(winning, {
    playerId: "p_red",
    tokenIndex: 0,
    now: 21,
  });
  assert.equal(final.state.status, "FINISHED");
  assert.equal(final.state.winnerTeamId, "A");
  assert.equal(final.state.winnerPlayerId, null);
});

test("finished scheduled player hands the team turn to the opposite partner", () => {
  const state = fourPlayerTeamMatch();
  state.players[0].tokens = [57, 57, 57, 57];
  state.turnSeat = 0;

  assert.equal(actingSeatForTurn(state), 2);
  assert.equal(assertCanCommitRoll(state, "p_yellow"), 2);
  assert.throws(
    () => assertCanCommitRoll(state, "p_red"),
    (error) => error?.code === "NOT_YOUR_TURN",
  );
});

test("classic Online proof-state shape remains free of Team Up fields", () => {
  const online = newMatch({
    matchId: "LPQWERTY23",
    hostPlayerId: "p_host",
    hostDisplayName: "Host",
    now: 1,
    targetPlayerCount: 2,
    matchMode: "ONLINE",
  });
  const proofState = authoritativeStateForRandomness(online);
  assert.equal("matchMode" in proofState, false);
  assert.equal("teamAssignments" in proofState, false);
  assert.equal("actingSeat" in proofState, false);

  const team = fourPlayerTeamMatch();
  const teamProofState = authoritativeStateForRandomness(team);
  assert.equal(teamProofState.matchMode, "TEAM_UP");
  assert.deepEqual(teamProofState.teamAssignments, ["A", "B", "A", "B"]);
  assert.equal(teamProofState.teamRulesetId, "ludoproof-team-v2");
});
