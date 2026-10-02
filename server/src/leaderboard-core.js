export const LEADERBOARD_FINISH_POINTS = 20;
export const LEADERBOARD_WIN_BONUS = 30;

const PROFILE_ID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function normalizeLeaderboardProfileId(value) {
  const normalized =
    typeof value === "string"
      ? value.trim().toLowerCase()
      : "";
  return PROFILE_ID_PATTERN.test(normalized)
    ? normalized
    : null;
}

export function leaderboardPointsForResult(won) {
  return (
    LEADERBOARD_FINISH_POINTS +
    (won ? LEADERBOARD_WIN_BONUS : 0)
  );
}
