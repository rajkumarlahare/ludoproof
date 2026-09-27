export const COLORS = ["RED", "GREEN", "YELLOW", "BLUE"];

export const ROLL_REVEAL_TIMEOUT_MS = 4 * 60 * 1000;

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

export function newMatch({ matchId, hostPlayerId, hostDisplayName, now }) {
  return {
    schemaVersion: 1,
    matchId,
    status: "WAITING",
    createdAt: now,
    updatedAt: now,
    revision: 1,
    hostPlayerId,
    players: [
      {
        playerId: hostPlayerId,
        displayName: hostDisplayName,
        color: COLORS[0],
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
  if (state.players.length >= 4) throw gameError("MATCH_FULL", "match already has four players");
  if (state.players.some((player) => player.playerId === playerId)) {
    throw gameError("PLAYER_EXISTS", "player already joined");
  }

  const next = clone(state);
  next.players.push({
    playerId,
    displayName,
    color: COLORS[next.players.length],
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
  if (state.turnSeat !== seat) {
    throw gameError("NOT_YOUR_TURN", "it is not this player's turn");
  }
  if (state.pendingRoll) {
    throw gameError("ROLL_ALREADY_PENDING", "the current turn already has a roll");
  }
  return seat;
}

export function authoritativeStateForRandomness(state) {
  return {
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
  next.pendingRoll.revealDeadlineAt =
    Number.isSafeInteger(revealDeadlineAt)
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
  if (
    !Number.isSafeInteger(pending.revealDeadlineAt) ||
    now < pending.revealDeadlineAt
  ) {
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
  if (seat !== state.turnSeat) {
    throw gameError("TURN_STATE_CONFLICT", "turn changed before roll resolution");
  }

  const next = clone(state);
  if (outcome === 6) {
    next.consecutiveSixes[seat] = (next.consecutiveSixes[seat] ?? 0) + 1;
  } else {
    next.consecutiveSixes[seat] = 0;
  }

  const thirdSix =
    RULESET.threeConsecutiveSixesForfeit &&
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
    if (!(RULESET.extraTurnOnSix && outcome === 6)) {
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
  if (seat !== state.turnSeat || pending.seat !== seat) {
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
  const playerFinished = player.tokens.every((position) => position === 57);

  next.pendingRoll = null;

  if (playerFinished) {
    next.status = "FINISHED";
    next.winnerPlayerId = player.playerId;
    next.finishedAt = now;
    touch(next, now);
    return {
      state: next,
      captures,
      extraTurn: false,
      winnerPlayerId: player.playerId,
    };
  }

  const extraTurn =
    (RULESET.extraTurnOnSix && roll === 6) ||
    (RULESET.extraTurnOnCapture && captures > 0);

  if (!extraTurn) {
    advanceTurn(next);
  }

  touch(next, now);
  return {
    state: next,
    captures,
    extraTurn,
    winnerPlayerId: null,
  };
}

export function publicState(state) {
  return {
    schemaVersion: state.schemaVersion,
    matchId: state.matchId,
    status: state.status,
    createdAt: state.createdAt,
    updatedAt: state.updatedAt,
    revision: state.revision,
    hostPlayerId: state.hostPlayerId,
    players: state.players.map((player, seat) => ({
      playerId: player.playerId,
      displayName: player.displayName,
      color: player.color,
      seat,
      tokens: [...player.tokens],
    })),
    turnSeat: state.turnSeat,
    randomEventIndex: state.randomEventIndex,
    consecutiveSixes: [...state.consecutiveSixes],
    pendingRoll: state.pendingRoll
      ? {
          status: state.pendingRoll.status,
          seat: state.pendingRoll.seat,
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
    rulesetId: RULESET.id,
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

  let captures = 0;
  state.players.forEach((opponent, seat) => {
    if (seat === movingSeat) return;
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
