export const COLORS = ["RED", "GREEN", "YELLOW", "BLUE"];

export const ROLL_REVEAL_TIMEOUT_MS = 4 * 60 * 1000;

export const RULESET_KEY = "CLASSIC_V1";
export const TEAM_RULESET_KEY = "TEAM_UP_V1";
export const TEAM_ASSIGNMENTS = Object.freeze(["A", "B", "A", "B"]);

export const RULESET = Object.freeze({
  id: "ludoproof-standard-v1",
  boardTrackCells: 52,
  homePosition: 57,
  tokensPerPlayer: 4,
  startOffsets: Object.freeze({
    RED: 0,
    GREEN: 13,
    YELLOW: 26,
    BLUE: 39,
  }),
  safeGlobalCells: Object.freeze([0, 8, 13, 21, 26, 34, 39, 47]),
  leaveYardRequiresSix: true,
  exactRollToHome: true,
  extraTurnOnSix: true,
  extraTurnOnCapture: true,
  threeConsecutiveSixesForfeit: true,
  captureOnSafeCell: false,
});

export const TEAM_RULESET = Object.freeze({
  ...RULESET,
  id: "ludoproof-team-v1",
  mode: "TEAM_UP",
  teams: Object.freeze(["A", "B"]),
  teamAssignments: TEAM_ASSIGNMENTS,
  friendlyCapture: false,
  partnerTurnHandoff: true,
  winCondition: "BOTH_PARTNERS_ALL_TOKENS_HOME",
});

export function isTeamUp(state) {
  return state?.matchMode === "TEAM_UP" || state?.teamMode === "TEAM_UP";
}

export function rulesetForState(state) {
  return isTeamUp(state) ? TEAM_RULESET : RULESET;
}

export function teamIdForSeat(state, seat) {
  if (!isTeamUp(state) || !Number.isInteger(seat) || seat < 0 || seat > 3) {
    return null;
  }
  return state.teamAssignments?.[seat] ?? TEAM_ASSIGNMENTS[seat] ?? null;
}

export function actingSeatForTurn(state) {
  const scheduledSeat = state?.turnSeat;
  if (!Number.isInteger(scheduledSeat)) return scheduledSeat;
  if (!isTeamUp(state)) return scheduledSeat;

  const scheduled = state.players?.[scheduledSeat];
  if (!scheduled || !playerFinished(scheduled)) return scheduledSeat;

  const partnerSeat = (scheduledSeat + 2) % 4;
  const partner = state.players?.[partnerSeat];
  if (partner && !playerFinished(partner)) return partnerSeat;
  return scheduledSeat;
}

export function newMatch({
  matchId,
  hostPlayerId,
  hostDisplayName,
  now,
  targetPlayerCount = null,
  matchMode = "ONLINE",
}) {
  const teamUp = matchMode === "TEAM_UP";
  return {
    schemaVersion: 1,
    matchId,
    status: "WAITING",
    createdAt: now,
    updatedAt: now,
    revision: 1,
    hostPlayerId,
    targetPlayerCount,
    matchMode,
    ...(teamUp
      ? {
          teamAssignments: [...TEAM_ASSIGNMENTS],
          winnerTeamId: null,
        }
      : {}),
    players: [
      {
        playerId: hostPlayerId,
        displayName: hostDisplayName,
        color: COLORS[0],
        ...(teamUp ? { teamId: TEAM_ASSIGNMENTS[0] } : {}),
        tokenAuthHash: null,
        tokens: [-1, -1, -1, -1],
      },
    ],
    turnSeat: null,
    randomEventIndex: 0,
    consecutiveSixes: [0],
    pendingRoll: null,
    winnerPlayerId: null,
    finishedAt: null,
  };
}

export function addPlayer(state, { playerId, displayName, tokenAuthHash, now }) {
  requireStatus(state, "WAITING");
  const playerLimit = Number.isInteger(state.targetPlayerCount)
    ? state.targetPlayerCount
    : 4;
  if (state.players.length >= playerLimit) {
    throw gameError("MATCH_FULL", "match already has all required players");
  }
  if (state.players.some((player) => player.playerId === playerId)) {
    throw gameError("PLAYER_EXISTS", "player already joined");
  }

  const next = clone(state);
  const seat = next.players.length;
  next.players.push({
    playerId,
    displayName,
    color: COLORS[seat],
    ...(isTeamUp(next) ? { teamId: teamIdForSeat(next, seat) } : {}),
    tokenAuthHash,
    tokens: [-1, -1, -1, -1],
  });
  next.consecutiveSixes.push(0);
  touch(next, now);
  return next;
}

export function attachHostAuth(state, tokenAuthHash, now) {
  const next = clone(state);
  next.players[0].tokenAuthHash = tokenAuthHash;
  touch(next, now);
  return next;
}

export function startMatch(state, playerId, now) {
  requireStatus(state, "WAITING");
  if (state.hostPlayerId !== playerId) {
    throw gameError("HOST_ONLY", "only the host can start the match");
  }
  if (state.players.length < 2) {
    throw gameError("NEED_MORE_PLAYERS", "at least two players are required");
  }
  if (
    Number.isInteger(state.targetPlayerCount) &&
    state.players.length !== state.targetPlayerCount
  ) {
    throw gameError(
      "WAITING_FOR_PLAYERS",
      "all invited seats must be filled before the match can start",
    );
  }
  if (
    isTeamUp(state) &&
    (state.targetPlayerCount !== 4 || state.players.length !== 4)
  ) {
    throw gameError(
      "TEAM_UP_REQUIRES_FOUR_PLAYERS",
      "team up requires exactly four players",
    );
  }

  const next = clone(state);
  next.status = "ACTIVE";
  next.turnSeat = 0;
  next.pendingRoll = null;
  touch(next, now);
  return next;
}

export function assertCanCommitRoll(state, playerId) {
  requireStatus(state, "ACTIVE");
  const seat = seatForPlayer(state, playerId);
  const actingSeat = actingSeatForTurn(state);
  if (actingSeat !== seat) {
    throw gameError("NOT_YOUR_TURN", "it is not this player's turn");
  }
  if (state.pendingRoll) {
    throw gameError("ROLL_ALREADY_PENDING", "the current turn already has a roll");
  }
  return seat;
}

export function authoritativeStateForRandomness(state) {
  const base = {
    schemaVersion: state.schemaVersion,
    matchId: state.matchId,
    status: state.status,
    players: state.players.map((player) => ({
      playerId: player.playerId,
      color: player.color,
      tokens: [...player.tokens],
    })),
    turnSeat: state.turnSeat,
    randomEventIndex: state.randomEventIndex,
    consecutiveSixes: [...state.consecutiveSixes],
    winnerPlayerId: state.winnerPlayerId,
  };

  // Preserve the exact historical ONLINE proof-state shape. Team semantics are
  // appended only for TEAM_UP so existing CLASSIC_V1 proofs remain compatible.
  if (!isTeamUp(state)) return base;

  return {
    ...base,
    matchMode: "TEAM_UP",
    teamRulesetId: TEAM_RULESET.id,
    teamAssignments: [...(state.teamAssignments ?? TEAM_ASSIGNMENTS)],
    actingSeat: actingSeatForTurn(state),
    winnerTeamId: state.winnerTeamId ?? null,
  };
}

export function reserveRoll(state, {
  playerId,
  clientCommitment,
  eventIndex,
  actorHash,
  previousStateHash,
  rulesetHash,
  now,
}) {
  const seat = assertCanCommitRoll(state, playerId);
  if (eventIndex !== state.randomEventIndex) {
    throw gameError("EVENT_INDEX_DRIFT", "random event index drifted");
  }

  const next = clone(state);
  next.pendingRoll = {
    status: "CREATING",
    seat,
    ...(isTeamUp(state) ? { scheduledSeat: state.turnSeat } : {}),
    playerId,
    clientCommitment,
    eventIndex,
    eventId: `roll:${eventIndex}`,
    actorHash,
    previousStateHash,
    rulesetHash,
    roundId: null,
    serverCommitment: null,
    proofDigest: null,
    outcome: null,
    legalTokenIndexes: null,
    createdAt: now,
    resolvedAt: null,
    committedAt: null,
    revealDeadlineAt: now + ROLL_REVEAL_TIMEOUT_MS,
  };
  touch(next, now);
  return next;
}

export function attachRoundCommitment(state, {
  roundId,
  serverCommitment,
  revealDeadlineAt,
  now,
}) {
  if (!state.pendingRoll || state.pendingRoll.status !== "CREATING") {
    throw gameError("ROLL_STATE_CONFLICT", "roll is no longer waiting for a server commitment");
  }

  const next = clone(state);
  next.pendingRoll.status = "COMMITTED";
  next.pendingRoll.roundId = roundId;
  next.pendingRoll.serverCommitment = serverCommitment;
  next.pendingRoll.committedAt = now;
  next.pendingRoll.revealDeadlineAt = Number.isSafeInteger(revealDeadlineAt)
    ? revealDeadlineAt
    : now + ROLL_REVEAL_TIMEOUT_MS;
  next.randomEventIndex += 1;
  touch(next, now);
  return next;
}

export function forfeitTimedOutRoll(state, {
  now,
  reason = "REVEAL_TIMEOUT",
}) {
  requireStatus(state, "ACTIVE");
  const pending = state.pendingRoll;
  if (!pending) {
    throw gameError("NO_PENDING_ROLL", "there is no pending roll to forfeit");
  }
  if (!["CREATING", "COMMITTED", "RESOLVING"].includes(pending.status)) {
    throw gameError("ROLL_STATE_CONFLICT", "only an unresolved roll can time out");
  }
  if (!Number.isSafeInteger(pending.revealDeadlineAt) || now < pending.revealDeadlineAt) {
    throw gameError("ROLL_NOT_TIMED_OUT", "pending roll has not reached its reveal deadline");
  }

  const next = clone(state);
  const timedOut = structuredClone(next.pendingRoll);

  if (
    timedOut.status === "CREATING" &&
    next.randomEventIndex === timedOut.eventIndex
  ) {
    next.randomEventIndex += 1;
  }

  next.consecutiveSixes[timedOut.seat] = 0;
  next.pendingRoll = null;
  advanceTurn(next);
  touch(next, now);

  return {
    state: next,
    timedOutRoll: {
      ...timedOut,
      status: "TIMED_OUT",
      timeoutReason: reason,
      timedOutAt: now,
      replacementRoundAllowed: false,
    },
  };
}

export function legalTokenIndexes(state, seat, roll) {
  if (!Number.isInteger(roll) || roll < 1 || roll > 6) {
    throw gameError("INVALID_ROLL", "roll must be between 1 and 6");
  }
  const player = state.players[seat];
  if (!player) throw gameError("INVALID_SEAT", "player seat is invalid");

  const legal = [];
  player.tokens.forEach((position, index) => {
    if (position === 57) return;
    if (position === -1) {
      if (roll === 6) legal.push(index);
      return;
    }
    if (position >= 0 && position <= 56 && position + roll <= 57) {
      legal.push(index);
    }
  });
  return legal;
}

export function registerResolvedRoll(state, {
  outcome,
  proofDigest,
  now,
}) {
  if (!state.pendingRoll || !["COMMITTED", "RESOLVING"].includes(state.pendingRoll.status)) {
    throw gameError("ROLL_STATE_CONFLICT", "roll is not ready to resolve");
  }
  const seat = state.pendingRoll.seat;
  if (isTeamUp(state)) {
    if (
      state.pendingRoll.scheduledSeat !== state.turnSeat ||
      seat !== actingSeatForTurn(state)
    ) {
      throw gameError("TURN_STATE_CONFLICT", "team turn changed before roll resolution");
    }
  } else if (seat !== state.turnSeat) {
    throw gameError("TURN_STATE_CONFLICT", "turn changed before roll resolution");
  }

  const next = clone(state);
  if (outcome === 6) {
    next.consecutiveSixes[seat] = (next.consecutiveSixes[seat] ?? 0) + 1;
  } else {
    next.consecutiveSixes[seat] = 0;
  }

  const ruleset = rulesetForState(next);
  const thirdSix =
    ruleset.threeConsecutiveSixesForfeit &&
    outcome === 6 &&
    next.consecutiveSixes[seat] >= 3;

  if (thirdSix) {
    next.consecutiveSixes[seat] = 0;
    next.pendingRoll = null;
    advanceTurn(next);
    touch(next, now);
    return {
      state: next,
      awaitingMove: false,
      forfeitedThirdSix: true,
      legalTokenIndexes: [],
    };
  }

  const legal = legalTokenIndexes(next, seat, outcome);
  if (legal.length === 0) {
    next.pendingRoll = null;
    if (!(ruleset.extraTurnOnSix && outcome === 6)) {
      advanceTurn(next);
    }
    touch(next, now);
    return {
      state: next,
      awaitingMove: false,
      forfeitedThirdSix: false,
      legalTokenIndexes: [],
    };
  }

  next.pendingRoll.status = "RESOLVED";
  next.pendingRoll.outcome = outcome;
  next.pendingRoll.proofDigest = proofDigest;
  next.pendingRoll.legalTokenIndexes = legal;
  next.pendingRoll.resolvedAt = now;
  touch(next, now);

  return {
    state: next,
    awaitingMove: true,
    forfeitedThirdSix: false,
    legalTokenIndexes: legal,
  };
}

export function applyMove(state, { playerId, tokenIndex, now }) {
  requireStatus(state, "ACTIVE");
  const pending = state.pendingRoll;
  if (!pending || pending.status !== "RESOLVED") {
    throw gameError("NO_RESOLVED_ROLL", "a verified roll is required before moving");
  }
  const seat = seatForPlayer(state, playerId);
  const expectedSeat = actingSeatForTurn(state);
  if (
    seat !== expectedSeat ||
    pending.seat !== seat ||
    (isTeamUp(state) && pending.scheduledSeat !== state.turnSeat)
  ) {
    throw gameError("NOT_YOUR_TURN", "it is not this player's move");
  }
  if (!pending.legalTokenIndexes.includes(tokenIndex)) {
    throw gameError("ILLEGAL_TOKEN_MOVE", "selected token cannot use this roll");
  }

  const next = clone(state);
  const player = next.players[seat];
  const roll = pending.outcome;
  const current = player.tokens[tokenIndex];
  const destination = current === -1 ? 0 : current + roll;
  player.tokens[tokenIndex] = destination;

  const captures = captureOpponents(next, seat, destination);
  const ruleset = rulesetForState(next);
  next.pendingRoll = null;

  if (isTeamUp(next)) {
    const teamId = teamIdForSeat(next, seat);
    if (teamFinished(next, teamId)) {
      next.status = "FINISHED";
      next.winnerPlayerId = null;
      next.winnerTeamId = teamId;
      next.finishedAt = now;
      // The legacy leaderboard is individual-winner based. Mark this result as
      // intentionally handled so it can never record one teammate as the sole
      // winner. Team ranking can be added later with a dedicated ledger.
      next.leaderboardRecordedAt = now;
      touch(next, now);
      return {
        state: next,
        captures,
        extraTurn: false,
        winnerPlayerId: null,
        winnerTeamId: teamId,
      };
    }
  } else if (playerFinished(player)) {
    next.status = "FINISHED";
    next.winnerPlayerId = player.playerId;
    next.finishedAt = now;
    touch(next, now);
    return {
      state: next,
      captures,
      extraTurn: false,
      winnerPlayerId: player.playerId,
      winnerTeamId: null,
    };
  }

  const extraTurn =
    (ruleset.extraTurnOnSix && roll === 6) ||
    (ruleset.extraTurnOnCapture && captures > 0);

  if (!extraTurn) {
    advanceTurn(next);
  }

  touch(next, now);
  return {
    state: next,
    captures,
    extraTurn,
    winnerPlayerId: null,
    winnerTeamId: next.winnerTeamId ?? null,
  };
}

export function publicState(state) {
  const teamUp = isTeamUp(state);
  const ruleset = rulesetForState(state);
  return {
    schemaVersion: state.schemaVersion,
    matchId: state.matchId,
    status: state.status,
    createdAt: state.createdAt,
    updatedAt: state.updatedAt,
    revision: state.revision,
    hostPlayerId: state.hostPlayerId,
    targetPlayerCount: Number.isInteger(state.targetPlayerCount)
      ? state.targetPlayerCount
      : null,
    matchMode: teamUp ? "TEAM_UP" : (state.matchMode ?? "ONLINE"),
    ...(teamUp
      ? {
          teamAssignments: [...(state.teamAssignments ?? TEAM_ASSIGNMENTS)],
          actingSeat: actingSeatForTurn(state),
          winnerTeamId: state.winnerTeamId ?? null,
        }
      : {}),
    players: state.players.map((player, seat) => ({
      playerId: player.playerId,
      displayName: player.displayName,
      color: player.color,
      seat,
      ...(teamUp ? { teamId: teamIdForSeat(state, seat) } : {}),
      tokens: [...player.tokens],
    })),
    turnSeat: state.turnSeat,
    randomEventIndex: state.randomEventIndex,
    consecutiveSixes: [...state.consecutiveSixes],
    pendingRoll: state.pendingRoll
      ? {
          status: state.pendingRoll.status,
          seat: state.pendingRoll.seat,
          ...(teamUp ? { scheduledSeat: state.pendingRoll.scheduledSeat } : {}),
          eventIndex: state.pendingRoll.eventIndex,
          eventId: state.pendingRoll.eventId,
          roundId: state.pendingRoll.roundId,
          serverCommitment: state.pendingRoll.serverCommitment,
          proofDigest: state.pendingRoll.proofDigest,
          outcome: state.pendingRoll.outcome,
          legalTokenIndexes: state.pendingRoll.legalTokenIndexes,
          clientCommitment: state.pendingRoll.clientCommitment,
          revealDeadlineAt: state.pendingRoll.revealDeadlineAt,
        }
      : null,
    winnerPlayerId: state.winnerPlayerId,
    rulesetId: ruleset.id,
  };
}

export function globalCellFor(color, relativePosition) {
  if (relativePosition < 0 || relativePosition > 51) return null;
  return (RULESET.startOffsets[color] + relativePosition) % RULESET.boardTrackCells;
}

function captureOpponents(state, movingSeat, destination) {
  if (destination < 0 || destination > 51) return 0;
  const mover = state.players[movingSeat];
  const cell = globalCellFor(mover.color, destination);
  if (RULESET.safeGlobalCells.includes(cell)) return 0;

  const movingTeam = teamIdForSeat(state, movingSeat);
  let captures = 0;
  state.players.forEach((opponent, seat) => {
    if (seat === movingSeat) return;
    if (isTeamUp(state) && teamIdForSeat(state, seat) === movingTeam) return;
    opponent.tokens = opponent.tokens.map((position) => {
      const opponentCell = globalCellFor(opponent.color, position);
      if (opponentCell === cell) {
        captures += 1;
        return -1;
      }
      return position;
    });
  });
  return captures;
}

function teamFinished(state, teamId) {
  if (!teamId) return false;
  const members = state.players.filter(
    (_, seat) => teamIdForSeat(state, seat) === teamId,
  );
  return members.length === 2 && members.every(playerFinished);
}

function playerFinished(player) {
  return Array.isArray(player?.tokens) &&
    player.tokens.length === 4 &&
    player.tokens.every((position) => position === RULESET.homePosition);
}

function advanceTurn(state) {
  if (state.players.length < 1) return;
  state.turnSeat = (state.turnSeat + 1) % state.players.length;
}

function seatForPlayer(state, playerId) {
  const seat = state.players.findIndex((player) => player.playerId === playerId);
  if (seat < 0) throw gameError("PLAYER_NOT_FOUND", "player is not part of this match");
  return seat;
}

function requireStatus(state, expected) {
  if (state.status !== expected) {
    throw gameError("INVALID_MATCH_STATUS", `match must be ${expected}`);
  }
}

function touch(state, now) {
  state.updatedAt = now;
  state.revision += 1;
}

function clone(value) {
  return structuredClone(value);
}

export function gameError(code, message, status = 409) {
  const error = new Error(message);
  error.code = code;
  error.status = status;
  return error;
}
