import test from "node:test";
import assert from "node:assert/strict";
import {
  LEADERBOARD_FINISH_POINTS,
  LEADERBOARD_WIN_BONUS,
  leaderboardPointsForResult,
  normalizeLeaderboardProfileId,
} from "../src/leaderboard-core.js";

test("leaderboard scoring matches app progression", () => {
  assert.equal(
    leaderboardPointsForResult(false),
    LEADERBOARD_FINISH_POINTS,
  );
  assert.equal(
    leaderboardPointsForResult(true),
    LEADERBOARD_FINISH_POINTS +
      LEADERBOARD_WIN_BONUS,
  );
  assert.equal(
    leaderboardPointsForResult(false),
    20,
  );
  assert.equal(
    leaderboardPointsForResult(true),
    50,
  );
});

test("leaderboard profile IDs accept UUID v4 only", () => {
  const id =
    "550e8400-e29b-41d4-a716-446655440000";
  assert.equal(
    normalizeLeaderboardProfileId(
      id.toUpperCase(),
    ),
    id,
  );
  assert.equal(
    normalizeLeaderboardProfileId(
      "not-a-profile-id",
    ),
    null,
  );
  assert.equal(
    normalizeLeaderboardProfileId(
      "550e8400-e29b-11d4-a716-446655440000",
    ),
    null,
  );
});
