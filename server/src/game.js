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
// layer can bind an explicit v2 contract to a match without changing the
// production default. Missing rulesetKey intentionally means immutable v1 so
// an accidental backend deploy cannot strand older installed Android clients.
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
  const explicitRulesetKey = options?.rulesetKey;
  if (explicitRulesetKey == null) {
    return state;
  }

  const versionedState = {
    ...state,
    rulesetKey: explicitRulesetKey,
  };
  rulesetForState(versionedState);
  return versionedState;
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
