import {
  RULESET,
  addPlayer,
  applyMove,
  attachHostAuth,
  attachRoundCommitment,
  authoritativeStateForRandomness,
  newMatch,
  publicState,
  registerResolvedRoll,
  reserveRoll,
  startMatch,
} from "./game.js";
import {
  bearerToken,
  canonicalJson,
  httpError,
  normalizeDisplayName,
  randomToken,
  requireDigest,
  sha256Hex,
} from "./crypto.js";

const STATE_KEY = "match-state";
const APP_ID = "ludoproof";
const WORLD = Object.freeze({
  cellsPerOutcome: 16,
  timelineTicks: 512,
  epochCount: 8,
  probeCount: 3,
});

export class MatchRoom {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
    this.mutationTail = Promise.resolve();
  }

  async fetch(request) {
    try {
      const url = new URL(request.url);
      const body =
        request.method === "POST"
          ? await readJson(request)
          : null;

      if (request.method === "POST" && url.pathname === "/create") {
        return this.#mutate(() => this.#create(body));
      }
      if (request.method === "POST" && url.pathname === "/join") {
        return this.#mutate(() => this.#join(body));
      }
      if (request.method === "GET" && url.pathname === "/state") {
        return this.#state(request);
      }
      if (request.method === "POST" && url.pathname === "/start") {
        return this.#mutate(() => this.#start(request));
      }
      if (request.method === "POST" && url.pathname === "/roll/commit") {
        return this.#mutate(() => this.#commitRoll(request, body));
      }
      if (request.method === "POST" && url.pathname === "/roll/reveal") {
        return this.#mutate(() => this.#revealRoll(request, body));
      }
      if (request.method === "POST" && url.pathname === "/move") {
        return this.#mutate(() => this.#move(request, body));
      }

      return json(404, { error: "NOT_FOUND", message: "route not found" });
    } catch (error) {
      return errorResponse(error);
    }
  }

  #mutate(work) {
    const run = this.mutationTail.then(work, work);
    this.mutationTail = run.catch(() => {});
    return run;
  }

  async #create(body) {
    const existing = await this.ctx.storage.get(STATE_KEY);
    if (existing) {
      throw httpError(409, "MATCH_EXISTS", "match already exists");
    }

    const displayName = normalizeDisplayName(body?.displayName);
    const matchId = String(body?.matchId ?? "");
    if (!/^LP[A-Z2-9]{8}$/.test(matchId)) {
      throw httpError(400, "INVALID_MATCH_ID", "invalid match ID");
    }

    const playerId = crypto.randomUUID();
    const playerToken = randomToken();
    const tokenAuthHash = await sha256Hex(
      "ludoproof:player-token:v1:" + playerToken,
    );
    const now = Date.now();

    let state = newMatch({
      matchId,
      hostPlayerId: playerId,
      hostDisplayName: displayName,
      now,
    });
    state = attachHostAuth(state, tokenAuthHash, now);
    state.history = [];
    await this.ctx.storage.put(STATE_KEY, state);

    return json(201, {
      matchId,
      playerId,
      playerToken,
      state: publicStateWithHistory(state),
    });
  }

  async #join(body) {
    const state = await this.#requireState();
    const displayName = normalizeDisplayName(body?.displayName);
    const playerId = crypto.randomUUID();
    const playerToken = randomToken();
    const tokenAuthHash = await sha256Hex(
      "ludoproof:player-token:v1:" + playerToken,
    );

    const next = addPlayer(state, {
      playerId,
      displayName,
      tokenAuthHash,
      now: Date.now(),
    });
    next.history = state.history ?? [];
    await this.ctx.storage.put(STATE_KEY, next);

    return json(201, {
      matchId: next.matchId,
      playerId,
      playerToken,
      state: publicStateWithHistory(next),
    });
  }

  async #state(request) {
    const state = await this.#requireState();
    const player = await this.#authorize(state, request);
    return json(200, {
      playerId: player.playerId,
      state: publicStateWithHistory(state),
    });
  }

  async #start(request) {
    const state = await this.#requireState();
    const player = await this.#authorize(state, request);
    const next = startMatch(state, player.playerId, Date.now());
    next.history = state.history ?? [];
    await this.ctx.storage.put(STATE_KEY, next);
    return json(200, { state: publicStateWithHistory(next) });
  }

  async #commitRoll(request, body) {
    requireEntroNex(this.env);
    const clientCommitment = requireDigest(
      body?.clientCommitment,
      "clientCommitment",
    );

    let state = await this.#requireState();
    const player = await this.#authorize(state, request);

    if (state.pendingRoll) {
      const pending = state.pendingRoll;
      if (
        pending.playerId !== player.playerId ||
        pending.clientCommitment !== clientCommitment
      ) {
        throw httpError(
          409,
          "ROLL_ALREADY_PENDING",
          "this turn already has a different pending roll",
        );
      }
      if (pending.status === "COMMITTED" || pending.status === "RESOLVED") {
        return json(200, {
          replayed: true,
          round: commitmentSummary(pending),
          state: publicStateWithHistory(state),
        });
      }
      if (pending.status !== "CREATING") {
        throw httpError(409, "ROLL_BUSY", "roll is currently resolving");
      }
    } else {
      const eventIndex = state.randomEventIndex;
      const actorHash = await sha256Hex(
        "ludoproof:actor:v1:" + player.playerId,
      );
      const snapshot = {
        ...authoritativeStateForRandomness(state),
        history: historyForHash(state.history ?? []),
      };
      const previousStateHash = await sha256Hex(
        "entronex:v4:game-state:" + canonicalJson(snapshot),
      );
      const rulesetHash = await sha256Hex(
        "entronex:v4:game-ruleset:" + canonicalJson(RULESET),
      );

      state = reserveRoll(state, {
        playerId: player.playerId,
        clientCommitment,
        eventIndex,
        actorHash,
        previousStateHash,
        rulesetHash,
        now: Date.now(),
      });
      state.history = state.history ?? [];
      await this.ctx.storage.put(STATE_KEY, state);
    }

    const pending = state.pendingRoll;
    const config = buildEntroNexConfig(state.matchId, pending);
    const expected = await expectedDigests(config);

    let remote;
    try {
      remote = await entronexRequest(
        this.env,
        "/v4/rounds",
        {
          method: "POST",
          body: {
            clientCommitment,
            ...config,
          },
        },
      );
    } catch (error) {
      throw error;
    }

    validateRoundCommitment(remote, pending, expected);

    let latest = await this.#requireState();
    if (
      !latest.pendingRoll ||
      latest.pendingRoll.clientCommitment !== clientCommitment ||
      latest.pendingRoll.eventIndex !== pending.eventIndex
    ) {
      throw httpError(409, "ROLL_STATE_CONFLICT", "roll state changed during commitment");
    }

    if (latest.pendingRoll.status === "CREATING") {
      latest = attachRoundCommitment(latest, {
        roundId: remote.roundId,
        serverCommitment: remote.serverCommitment,
        now: Date.now(),
      });
      latest.history = state.history ?? [];
      await this.ctx.storage.put(STATE_KEY, latest);
    }

    return json(201, {
      replayed: Boolean(remote.replayed),
      round: commitmentSummary(latest.pendingRoll),
      attestation: remote.attestation ?? null,
      attestationPayload: remote.attestationPayload ?? null,
      state: publicStateWithHistory(latest),
    });
  }

  async #revealRoll(request, body) {
    requireEntroNex(this.env);
    const clientSeed = requireDigest(body?.clientSeed, "clientSeed");

    let state = await this.#requireState();
    const player = await this.#authorize(state, request);
    const actualCommitment = await sha256Hex(
      "entronex:v4:client-commit:" + clientSeed,
    );
    const pending = state.pendingRoll;

    if (!pending) {
      return this.#replayHistoricalReveal(
        state,
        player,
        actualCommitment,
      );
    }
    if (pending.playerId !== player.playerId) {
      throw httpError(403, "NOT_ROLL_OWNER", "only the rolling player can reveal");
    }
    if (actualCommitment !== pending.clientCommitment) {
      throw httpError(
        400,
        "CLIENT_COMMITMENT_MISMATCH",
        "revealed seed does not match the committed seed",
      );
    }
    if (pending.status === "RESOLVED") {
      const proof = await this.#loadArchivedProof(pending.roundId);
      validateResolvedProof(proof, pending);
      return json(200, {
        replayed: true,
        outcome: proof.outcome,
        proofDigest: proof.proofDigest,
        roundId: proof.roundId,
        legalTokenIndexes: pending.legalTokenIndexes,
        awaitingMove: true,
        proof,
        state: publicStateWithHistory(state),
      });
    }
    if (!["COMMITTED", "RESOLVING"].includes(pending.status)) {
      throw httpError(409, "ROLL_NOT_COMMITTED", "server commitment is not ready");
    }

    if (pending.status === "COMMITTED") {
      state.pendingRoll.status = "RESOLVING";
      state.updatedAt = Date.now();
      state.revision += 1;
      await this.ctx.storage.put(STATE_KEY, state);
    }

    let proof;
    try {
      proof = await entronexRequest(
        this.env,
        "/v4/rounds/" +
          encodeURIComponent(pending.roundId) +
          "/resolve",
        {
          method: "POST",
          body: { clientSeed },
        },
      );
    } catch (error) {
      const latest = await this.#requireState();
      if (
        latest.pendingRoll?.roundId === pending.roundId &&
        latest.pendingRoll.status === "RESOLVING"
      ) {
        latest.pendingRoll.status = "COMMITTED";
        latest.updatedAt = Date.now();
        latest.revision += 1;
        await this.ctx.storage.put(STATE_KEY, latest);
      }
      throw error;
    }

    validateResolvedProof(proof, pending);

    const hostedVerification = await entronexRequest(
      this.env,
      "/v4/verify",
      {
        method: "POST",
        body: proof,
        authenticated: false,
      },
    );
    if (hostedVerification.valid !== true) {
      throw httpError(
        502,
        "ENTRONEX_PROOF_INVALID",
        "EntroNex mathematical proof verification failed",
      );
    }

    let latest = await this.#requireState();
    if (
      !latest.pendingRoll ||
      latest.pendingRoll.roundId !== pending.roundId
    ) {
      throw httpError(409, "ROLL_STATE_CONFLICT", "roll state changed during resolution");
    }

    const result = registerResolvedRoll(latest, {
      outcome: proof.outcome,
      proofDigest: proof.proofDigest,
      now: Date.now(),
    });
    latest = result.state;
    latest.history = [
      ...(state.history ?? []),
      {
        eventIndex: pending.eventIndex,
        eventId: pending.eventId,
        playerId: pending.playerId,
        color: state.players[pending.seat].color,
        roundId: pending.roundId,
        clientCommitment: pending.clientCommitment,
        previousStateHash: pending.previousStateHash,
        rulesetHash: pending.rulesetHash,
        proofDigest: proof.proofDigest,
        outcome: proof.outcome,
        moveTokenIndex: null,
        captures: 0,
        resolvedAt: Date.now(),
      },
    ].slice(-200);
    await this.ctx.storage.put(STATE_KEY, latest);

    return json(200, {
      replayed: Boolean(proof.replayed),
      outcome: proof.outcome,
      proofDigest: proof.proofDigest,
      roundId: proof.roundId,
      legalTokenIndexes: result.legalTokenIndexes,
      awaitingMove: result.awaitingMove,
      forfeitedThirdSix: result.forfeitedThirdSix,
      proof,
      state: publicStateWithHistory(latest),
    });
  }

  async #move(request, body) {
    let state = await this.#requireState();
    const player = await this.#authorize(state, request);
    const tokenIndex = Number(body?.tokenIndex);

    if (!Number.isInteger(tokenIndex) || tokenIndex < 0 || tokenIndex > 3) {
      throw httpError(400, "INVALID_TOKEN_INDEX", "tokenIndex must be 0 through 3");
    }

    const pending = state.pendingRoll;
    const eventIndex = pending?.eventIndex;
    const moved = applyMove(state, {
      playerId: player.playerId,
      tokenIndex,
      now: Date.now(),
    });
    state = moved.state;

    const history = [...(state.history ?? [])];
    const entryIndex = history.findLastIndex(
      (event) =>
        event.eventIndex === eventIndex &&
        event.playerId === player.playerId,
    );
    if (entryIndex >= 0) {
      history[entryIndex] = {
        ...history[entryIndex],
        moveTokenIndex: tokenIndex,
        captures: moved.captures,
      };
    }
    state.history = history;
    await this.ctx.storage.put(STATE_KEY, state);

    return json(200, {
      captures: moved.captures,
      extraTurn: moved.extraTurn,
      winnerPlayerId: moved.winnerPlayerId,
      state: publicStateWithHistory(state),
    });
  }

  async #replayHistoricalReveal(
    state,
    player,
    clientCommitment,
  ) {
    const event = [...(state.history ?? [])]
      .reverse()
      .find(
        (candidate) =>
          candidate.playerId === player.playerId &&
          candidate.clientCommitment === clientCommitment,
      );

    if (!event) {
      throw httpError(
        409,
        "NO_PENDING_ROLL",
        "there is no matching committed or resolved roll",
      );
    }

    const proof = await this.#loadArchivedProof(event.roundId);
    if (
      proof.roundId !== event.roundId ||
      proof.clientCommitment !== clientCommitment ||
      proof.proofDigest !== event.proofDigest ||
      proof.outcome !== event.outcome
    ) {
      throw httpError(
        502,
        "ENTRONEX_ARCHIVE_MISMATCH",
        "archived proof does not match the recorded game event",
      );
    }

    return json(200, {
      replayed: true,
      outcome: proof.outcome,
      proofDigest: proof.proofDigest,
      roundId: proof.roundId,
      legalTokenIndexes: [],
      awaitingMove: false,
      proof,
      state: publicStateWithHistory(state),
    });
  }

  async #loadArchivedProof(roundId) {
    const proof = await entronexRequest(
      this.env,
      "/v4/rounds/" +
        encodeURIComponent(roundId) +
        "/proof",
      {
        method: "GET",
        body: null,
      },
    );
    const hostedVerification = await entronexRequest(
      this.env,
      "/v4/verify",
      {
        method: "POST",
        body: proof,
        authenticated: false,
      },
    );
    if (hostedVerification.valid !== true) {
      throw httpError(
        502,
        "ENTRONEX_PROOF_INVALID",
        "archived EntroNex proof verification failed",
      );
    }
    return proof;
  }

  async #authorize(state, request) {
    const token = bearerToken(request);
    const digest = await sha256Hex(
      "ludoproof:player-token:v1:" + token,
    );
    const player = state.players.find(
      (candidate) => candidate.tokenAuthHash === digest,
    );
    if (!player) {
      throw httpError(403, "AUTH_INVALID", "player token is invalid for this match");
    }
    return player;
  }

  async #requireState() {
    const state = await this.ctx.storage.get(STATE_KEY);
    if (!state) throw httpError(404, "MATCH_NOT_FOUND", "match does not exist");
    if (!Array.isArray(state.history)) state.history = [];
    return state;
  }
}

function buildEntroNexConfig(matchId, pending) {
  return {
    outcomes: [1, 2, 3, 4, 5, 6],
    context: {
      applicationId: APP_ID,
      sessionId: matchId,
      eventId: pending.eventId,
      eventType: "DICE_ROLL",
      eventIndex: pending.eventIndex,
      subjectHash: pending.actorHash,
      previousStateHash: pending.previousStateHash,
      metadataDigest: pending.rulesetHash,
    },
    world: WORLD,
  };
}

async function expectedDigests(config) {
  const contextDigest = await sha256Hex(
    "entronex:v4:context:" + canonicalJson(config.context),
  );
  const binding = {
    applicationId: config.context.applicationId,
    sessionId: config.context.sessionId,
    eventType: config.context.eventType,
    eventIndex: config.context.eventIndex,
  };
  const eventBindingDigest = await sha256Hex(
    "entronex:v4:event-binding:" + canonicalJson(binding),
  );
  const configDigest = await sha256Hex(
    "entronex:v4:config:" + canonicalJson(config),
  );
  return { contextDigest, eventBindingDigest, configDigest };
}

function validateRoundCommitment(round, pending, expected) {
  if (
    !round ||
    typeof round.roundId !== "string" ||
    round.clientCommitment !== pending.clientCommitment ||
    round.configDigest !== expected.configDigest ||
    round.contextDigest !== expected.contextDigest ||
    round.eventBindingDigest !== expected.eventBindingDigest ||
    !/^[0-9a-f]{64}$/i.test(round.serverCommitment ?? "")
  ) {
    throw httpError(
      502,
      "ENTRONEX_COMMITMENT_INVALID",
      "EntroNex returned a commitment that does not match the locked game event",
    );
  }
}

function validateResolvedProof(proof, pending) {
  if (
    !proof ||
    proof.roundId !== pending.roundId ||
    proof.clientCommitment !== pending.clientCommitment ||
    proof.config?.context?.eventIndex !== pending.eventIndex ||
    proof.config?.context?.sessionId == null ||
    proof.config.context.eventId !== pending.eventId ||
    proof.config.context.subjectHash !== pending.actorHash ||
    proof.config.context.previousStateHash !== pending.previousStateHash ||
    proof.config.context.metadataDigest !== pending.rulesetHash ||
    !Number.isInteger(proof.outcome) ||
    proof.outcome < 1 ||
    proof.outcome > 6 ||
    !/^[0-9a-f]{64}$/i.test(proof.proofDigest ?? "")
  ) {
    throw httpError(
      502,
      "ENTRONEX_PROOF_CONTEXT_MISMATCH",
      "resolved proof does not match the locked Ludo event",
    );
  }
}

function commitmentSummary(pending) {
  return {
    eventIndex: pending.eventIndex,
    eventId: pending.eventId,
    roundId: pending.roundId,
    serverCommitment: pending.serverCommitment,
    clientCommitment: pending.clientCommitment,
    previousStateHash: pending.previousStateHash,
    rulesetHash: pending.rulesetHash,
  };
}

function historyForHash(history) {
  return history.map((event) => ({
    eventIndex: event.eventIndex,
    playerId: event.playerId,
    clientCommitment: event.clientCommitment,
    proofDigest: event.proofDigest,
    outcome: event.outcome,
    moveTokenIndex: event.moveTokenIndex,
    captures: event.captures,
  }));
}

function publicStateWithHistory(state) {
  return {
    ...publicState(state),
    history: (state.history ?? []).slice(-100),
  };
}

async function entronexRequest(env, path, {
  method,
  body,
  authenticated = true,
}) {
  const baseUrl = String(env.ENTRONEX_BASE_URL ?? "").replace(/\/$/, "");
  const headers = {
    accept: "application/json",
    "content-type": "application/json",
  };

  if (authenticated) {
    headers.authorization = "Bearer " + env.ENTRONEX_API_TOKEN;
  }

  let response;
  try {
    response = await fetch(baseUrl + path, {
      method,
      headers,
      body: body == null ? undefined : JSON.stringify(body),
    });
  } catch {
    throw httpError(502, "ENTRONEX_UNAVAILABLE", "EntroNex request failed");
  }

  const text = await response.text();
  let parsed = {};
  try {
    parsed = text ? JSON.parse(text) : {};
  } catch {
    throw httpError(502, "ENTRONEX_BAD_RESPONSE", "EntroNex returned non-JSON data");
  }

  if (!response.ok) {
    const error = httpError(
      response.status >= 500 ? 502 : response.status,
      parsed.error ?? "ENTRONEX_ERROR",
      parsed.message ?? "EntroNex rejected the request",
    );
    error.remoteStatus = response.status;
    throw error;
  }

  return parsed;
}

function requireEntroNex(env) {
  if (
    typeof env.ENTRONEX_BASE_URL !== "string" ||
    !env.ENTRONEX_BASE_URL.startsWith("https://") ||
    typeof env.ENTRONEX_API_TOKEN !== "string" ||
    env.ENTRONEX_API_TOKEN.length < 20
  ) {
    throw httpError(
      503,
      "ENTRONEX_NOT_CONFIGURED",
      "game backend is not configured with EntroNex",
    );
  }
}

async function readJson(request) {
  try {
    const value = await request.json();
    if (!value || typeof value !== "object" || Array.isArray(value)) {
      throw new Error("object required");
    }
    return value;
  } catch {
    throw httpError(400, "INVALID_JSON", "request body must be a JSON object");
  }
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}

function errorResponse(error) {
  const status =
    Number.isInteger(error?.status) && error.status >= 400
      ? error.status
      : Number.isInteger(error?.status) && error.status < 400
        ? 500
        : (error?.status ?? 500);
  return json(
    status >= 400 && status <= 599 ? status : 500,
    {
      error: error?.code ?? "INTERNAL_ERROR",
      message:
        status >= 500 && !error?.code
          ? "internal server error"
          : String(error?.message ?? "internal server error"),
    },
  );
}
