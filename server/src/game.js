import * as legacy from "./game-v1-core.js";
import {
  LEGACY_RULESET,
  LEGACY_RULESET_KEY,
  LEGACY_TEAM_RULESET,
  LEGACY_TEAM_RULESET_KEY,
  ROLL_REVEAL_TIMEOUT_MS,
  RULESET,
  RULESET_KEY,
  TEAM_ASSIGNMENTS,
  TEAM_RULESET,
  TEAM_RULESET_KEY,
  rulesetForKey,
  rulesetKeyForState,
} from "./rulesets.js";

// Keep the battle-tested v1 engine implementation intact. This compatibility
// layer selects the immutable ruleset contract for each persisted match while
// new matches opt into v2. Missing rulesetKey intentionally means legacy v1.
export * from "./game-v1-core.js";

export {
  LEGACY_RULESET,
  LEGACY_RULESET_KEY,
  LEGACY_TEAM_RULESET,
  LEGACY_TEAM_RULESET_KEY,
  ROLL_REVEAL_TIMEOUT_MS,
  RULESET,
  RULESET_KEY,
  TEAM_ASSIGNMENTS,
  TEAM_RULESET,
  TEAM_RULESET_KEY,
};

export function isTeamUp(state) {
  return legacy.isTeamUp(state);
}

export function rulesetForState(state) {
  const teamUp = isTeamUp(state);
  const key = rulesetKeyForState(state, { teamUp });
  const allowed = teamUp
    ? new Set([LEGACY_TEAM_RULESET_KEY, TEAM_RULESET_KEY])
    : new Set([LEGACY_RULESET_KEY, RULESET_KEY]);

  if (!allowed.has(key)) {
    throw legacy.gameError(
      "UNSUPPORTED_RULESET",
      `ruleset ${String(key)} is not valid for this match mode`,
    );
  }

  try {
    return rulesetForKey(key);
  } catch {
    throw legacy.gameError(
      "UNSUPPORTED_RULESET",
      `unsupported ruleset ${String(key)}`,
    );
  }
}

export function newMatch(options) {
  const state = legacy.newMatch(options);
  return {
    ...state,
    rulesetKey: isTeamUp(state) ? TEAM_RULESET_KEY : RULESET_KEY,
  };
}

export function authoritativeStateForRandomness(state) {
  const material = legacy.authoritativeStateForRandomness(state);
  if (!isTeamUp(state)) {
    return material;
  }
  return {
    ...material,
    teamRulesetId: rulesetForState(state).id,
  };
}

export function publicState(state) {
  return {
    ...legacy.publicState(state),
    rulesetId: rulesetForState(state).id,
  };
}
