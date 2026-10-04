import test from "node:test";
import assert from "node:assert/strict";
import {
  addPlayer,
  applyMove,
  attachHostAuth,
  newMatch,
  startMatch,
} from "../src/game.js";

function activeMatch() {
  let state = newMatch({
    matchId: "HOMEEXTRA",
    hostPlayerId: "p1",
    hostDisplayName: "One",
    now: 1,
  });
  state = attachHostAuth(state, "a".repeat(64), 2);
  state = addPlayer(state, {
    playerId: "p2",
    displayName: "Two",
    tokenAuthHash: "b".repeat(64),
    now: 3,
  });
  return startMatch(state, "p1", 4);
}

test("moving one token exactly home grants another turn", () => {
  const state = activeMatch();
  state.players[0].tokens = [56, 0, -1, -1];
  state.pendingRoll = {
    status: "RESOLVED",
    seat: 0,
    playerId: "p1",
    outcome: 1,
    legalTokenIndexes: [0],
  };

  const moved = applyMove(state, {
    playerId: "p1",
    tokenIndex: 0,
    now: 5,
  });

  assert.equal(moved.state.players[0].tokens[0], 57);
  assert.equal(moved.extraTurn, true);
  assert.equal(moved.state.turnSeat, 0);
  assert.equal(moved.state.status, "ACTIVE");
});
