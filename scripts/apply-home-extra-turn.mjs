import fs from "node:fs";

function replaceOnce(path, before, after) {
  const source = fs.readFileSync(path, "utf8");
  if (!source.includes(before)) {
    throw new Error(`Expected source block not found in ${path}`);
  }
  const updated = source.replace(before, after);
  fs.writeFileSync(path, updated);
}

replaceOnce(
  "server/src/game.js",
  `  const extraTurn =\n    (ruleset.extraTurnOnSix && roll === 6) ||\n    (ruleset.extraTurnOnCapture && captures > 0);`,
  `  const reachedHome = destination === ruleset.homePosition;\n  const extraTurn =\n    reachedHome ||\n    (ruleset.extraTurnOnSix && roll === 6) ||\n    (ruleset.extraTurnOnCapture && captures > 0);`,
);

replaceOnce(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/domain/engine/OfflineGameEngine.kt",
  `        val extraTurn =\n            outcome ==\n                6 ||\n                captures >\n                0`,
  `        val extraTurn =\n            LudoExtraTurnPolicy\n                .grantsExtraTurn(\n                    roll = outcome,\n                    captures = captures,\n                    destination = destination,\n                )`,
);

fs.writeFileSync(
  "android/app/src/main/java/com/ludoproof/game/game/domain/model/LudoExtraTurnPolicy.kt",
  `package com.ludoproof.game\n\n/**\n * Classic turn-retention rules shared by local gameplay.\n *\n * A player keeps the turn after rolling six, capturing an opponent, or moving\n * one token exactly into the finished center position.\n */\nobject LudoExtraTurnPolicy {\n    fun grantsExtraTurn(\n        roll: Int,\n        captures: Int,\n        destination: Int,\n    ): Boolean =\n        roll == 6 ||\n            captures > 0 ||\n            destination == LudoPathEncoding.HOME_POSITION\n}\n`,
);

fs.writeFileSync(
  "android/app/src/test/java/com/ludoproof/game/LudoExtraTurnPolicyTest.kt",
  `package com.ludoproof.game\n\nimport org.junit.Assert.assertFalse\nimport org.junit.Assert.assertTrue\nimport org.junit.Test\n\nclass LudoExtraTurnPolicyTest {\n    @Test\n    fun reachingHomeGrantsExtraTurn() {\n        assertTrue(\n            LudoExtraTurnPolicy.grantsExtraTurn(\n                roll = 1,\n                captures = 0,\n                destination = LudoPathEncoding.HOME_POSITION,\n            ),\n        )\n    }\n\n    @Test\n    fun existingSixAndCaptureRulesStayUnchanged() {\n        assertTrue(\n            LudoExtraTurnPolicy.grantsExtraTurn(\n                roll = 6,\n                captures = 0,\n                destination = 10,\n            ),\n        )\n        assertTrue(\n            LudoExtraTurnPolicy.grantsExtraTurn(\n                roll = 2,\n                captures = 1,\n                destination = 10,\n            ),\n        )\n        assertFalse(\n            LudoExtraTurnPolicy.grantsExtraTurn(\n                roll = 2,\n                captures = 0,\n                destination = 10,\n            ),\n        )\n    }\n}\n`,
);

fs.writeFileSync(
  "server/test/home-extra-turn.test.js",
  `import test from "node:test";\nimport assert from "node:assert/strict";\nimport {\n  addPlayer,\n  applyMove,\n  attachHostAuth,\n  newMatch,\n  startMatch,\n} from "../src/game.js";\n\nfunction activeMatch() {\n  let state = newMatch({\n    matchId: "HOMEEXTRA",\n    hostPlayerId: "p1",\n    hostDisplayName: "One",\n    now: 1,\n  });\n  state = attachHostAuth(state, "a".repeat(64), 2);\n  state = addPlayer(state, {\n    playerId: "p2",\n    displayName: "Two",\n    tokenAuthHash: "b".repeat(64),\n    now: 3,\n  });\n  return startMatch(state, "p1", 4);\n}\n\ntest("moving one token exactly home grants another turn", () => {\n  const state = activeMatch();\n  state.players[0].tokens = [56, 0, -1, -1];\n  state.pendingRoll = {\n    status: "RESOLVED",\n    seat: 0,\n    playerId: "p1",\n    outcome: 1,\n    legalTokenIndexes: [0],\n  };\n\n  const moved = applyMove(state, {\n    playerId: "p1",\n    tokenIndex: 0,\n    now: 5,\n  });\n\n  assert.equal(moved.state.players[0].tokens[0], 57);\n  assert.equal(moved.extraTurn, true);\n  assert.equal(moved.state.turnSeat, 0);\n  assert.equal(moved.state.status, "ACTIVE");\n});\n`,
);

console.log("Applied extra-turn-on-home behavior and regression tests.");
