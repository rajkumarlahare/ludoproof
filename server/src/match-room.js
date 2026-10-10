import { verifyProofV4 } from "./vendor/entronex-v4/v4.js";
import {
  hasPinnedEntroNexTrust,
  verifyCommitmentAttestation,
  verifyProofAttestation,
} from "./entronex-trust.js";
import {
  entronexFetch,
} from "./entronex-transport.js";
import {
  ROLL_REVEAL_TIMEOUT_MS,
  addPlayer,
  applyMove,
  attachHostAuth,
  attachRoundCommitment,
  authoritativeStateForRandomness,
  forfeitTimedOutRoll,
  newMatch,
  publicState,
  registerResolvedRoll,
  reserveRoll,
  rulesetForState,
  startMatch,
} from "./game.js";
import {
  bearerToken,
  canonicalJson,
  derivePlayerIdentity,
  httpError,
  normalizeDisplayName,
  requireClientRequestId,
  requireDigest,
  sha256Hex,
  verifyFriendRoomJoinToken,
  verifyLeaderboardProfileAssertion,
} from "./crypto.js";
import {
  fairnessSummary,
  sealFairnessEvent,
} from "./fairness.js";
import {
  normalizeLeaderboardProfileId,
} from "./leaderboard-core.js";
import {
  normalizeCharacterId,
} from "./ludo-paws-characters.js";

const STATE_KEY = "match-state";
const APP_ID = "ludoproof";
const ACTIVE_IDLE_TTL_MS =
  7 * 24 * 60 * 60 * 1000;
const FINISHED_IDLE_TTL_MS =
  24 * 60 * 60 * 1000;
const DEFAULT_ENTRONEX_TIMEOUT_MS =
  8_000;
const MAX_ENTRONEX_RESPONSE_BYTES =
  256 * 1024;
const WORLD = Object.freeze({
  cellsPerOutcome: 16,
  timelineTicks: 512,
  epochCount: 8,
  probeCount: 3,
});
const QUICK_CHAT_EMOJIS = new Set([
  "👍", "😂", "😮", "😭", "😠", "🥳",
  "😅", "😛", "😎", "🤔", "😘", "🤭",
  "🤦", "😈", "🔥", "💪", "🤡", "👑",
  "😵‍💫", "😞", "🤣", "😆", "😤", "😡",
]);
const QUICK_CHAT_COOLDOWN_MS = 700;

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
        return await this.#mutate(() => this.#create(request, body));
      }
      if (request.method === "POST" && url.pathname === "/join") {
        return await this.#mutate(() => this.#join(request, body));
      }
      if (
        request.method === "POST" &&
        url.pathname === "/friend-host/assert"
      ) {
        return await this.#mutate(
          () => this.#assertFriendHost(request, body),
        );
      }
      if (request.method === "GET" && url.pathname === "/state") {
        return await this.#mutate(() => this.#state(request));
      }
      if (request.method === "GET" && url.pathname === "/events") {
        return await this.#mutate(() => this.#events(request));
      }
      if (request.method === "POST" && url.pathname === "/start") {
        return await this.#mutate(() => this.#start(request));
      }
      if (request.method === "POST" && url.pathname === "/roll/commit") {
        return await this.#mutate(() => this.#commitRoll(request, body));
      }
      if (request.method === "POST" && url.pathname === "/roll/reveal") {
        return await this.#mutate(() => this.#revealRoll(request, body));
      }
      if (request.method === "POST" && url.pathname === "/move") {
        return await this.#mutate(() => this.#move(request, body));
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

  async alarm() {
    let state =
      await this.ctx.storage.get(STATE_KEY);
    if (!state) return;

    const now = Date.now();
    const expired =
      await this.#expirePendingIfNeeded(
        state,
        now,
      );
    state = expired.state;

    if (expired.changed) {
      await this.#persist(state);
      return;
    }

    const expiresAt =
      Number(state.updatedAt ?? state.createdAt ?? 0) +
      this.#idleTtl(state);

    if (now < expiresAt) {
      await this.#setAlarm(
        this.#nextAlarmAt(state, expiresAt),
      );
      return;
    }

    if (
      typeof this.ctx.storage.deleteAll === "function"
    ) {
      await this.ctx.storage.deleteAll();
    }
  }

  async #persist(state) {
    await this.ctx.storage.put(
      STATE_KEY,
      state,
    );
    const idleExpiresAt =
      Number(state.updatedAt ?? state.createdAt ?? Date.now()) +
      this.#idleTtl(state);
    await this.#setAlarm(
      this.#nextAlarmAt(
        state,
        idleExpiresAt,
      ),
    );
    this.#broadcastStateChanged(
      state,
    );
  }

  #broadcastStateChanged(
    state,
  ) {
    if (
      typeof this.ctx
        .getWebSockets !==
      "function"
    ) {
      return;
    }

    const payload =
      JSON.stringify({
        type:
          "STATE_CHANGED",
        matchId:
          state.matchId,
        revision:
          state.revision,
        status:
          state.status,
        playerCount:
          state.players
            ?.length ??
          0,
      });

    for (
      const socket of
        this.ctx.getWebSockets()
    ) {
      try {
        socket.send(
          payload,
        );
      } catch {
        try {
          socket.close(
            1011,
            "state sync failed",
          );
        } catch {
          // Socket is already gone.
        }
      }
    }
  }

  #nextAlarmAt(state, idleExpiresAt) {
    const deadline =
      state.pendingRoll?.revealDeadlineAt;
    return Number.isSafeInteger(deadline)
      ? Math.min(idleExpiresAt, deadline)
      : idleExpiresAt;
  }

  #idleTtl(state) {
    return state.status === "FINISHED"
      ? FINISHED_IDLE_TTL_MS
      : ACTIVE_IDLE_TTL_MS;
  }

  async #setAlarm(timestamp) {
    if (
      typeof this.ctx.storage.setAlarm === "function"
    ) {
      await this.ctx.storage.setAlarm(timestamp);
    }
  }

  async #create(
    request,
    body,
  ) {
    const displayName =
      normalizeDisplayName(
        body?.displayName,
      );
    const characterId =
      normalizeCharacterId(
        body?.characterId,
      );
    const matchId =
      String(
        body?.matchId ?? "",
      );
    const clientRequestId =
      requireClientRequestId(
        body?.clientRequestId,
      );
    const targetPlayerCount =
      optionalTargetPlayerCount(
        body?.targetPlayerCount,
      );
    const matchMode =
      normalizeMatchMode(
        body?.matchMode,
        targetPlayerCount,
      );
    const hostFriend =
      matchMode ===
        "FRIENDS"
        ? await this.#friendIdentity(
            request,
          )
        : null;

    if (
      !/^LP[A-Z2-9]{8}$/.test(
        matchId,
      )
    ) {
      throw httpError(
        400,
        "INVALID_MATCH_ID",
        "invalid match ID",
      );
    }

    const profileId =
      await this.#resolveLeaderboardProfile(
        request,
        {
          matchId,
          clientRequestId,
          matchMode,
        },
      );

    const identity =
      await derivePlayerIdentity(
        this.env,
        matchId,
        clientRequestId,
      );
    const existing =
      await this.ctx.storage.get(
        STATE_KEY,
      );

    if (existing) {
      if (
        existing.createRequestId ===
          clientRequestId &&
        existing.hostPlayerId ===
          identity.playerId &&
        (
          existing.matchMode !==
            "FRIENDS" ||
          existing.hostFriendId ===
            hostFriend?.friendId
        )
      ) {
        return json(200, {
          replayed: true,
          matchId,
          playerId:
            identity.playerId,
          playerToken:
            identity.playerToken,
          state:
            publicStateWithHistory(
              existing,
            ),
        });
      }
      throw httpError(
        409,
        "MATCH_EXISTS",
        "match already exists",
      );
    }

    const tokenAuthHash =
      await sha256Hex(
        "ludoproof:player-token:v1:" +
          identity.playerToken,
      );
    const now = Date.now();

    let state = newMatch({
      matchId,
      hostPlayerId:
        identity.playerId,
      hostDisplayName:
        displayName,
      hostCharacterId:
        characterId,
      now,
      targetPlayerCount,
      matchMode,
    });
    state = attachHostAuth(
      state,
      tokenAuthHash,
      now,
    );
    state.createRequestId =
      clientRequestId;
    state.players[0]
      .joinRequestId =
      clientRequestId;
    state.players[0]
      .profileId =
      matchMode ===
        "FRIENDS"
        ? null
        : profileId;
    if (
      hostFriend
    ) {
      state.hostFriendId =
        hostFriend.friendId;
      state.players[0]
        .friendId =
        hostFriend.friendId;
    }
    state.history = [];
    await this.#persist(state);

    return json(201, {
      replayed: false,
      matchId,
      playerId:
        identity.playerId,
      playerToken:
        identity.playerToken,
      state:
        publicStateWithHistory(
          state,
        ),
    });
  }

  async #join(
    request,
    body,
  ) {
    const state =
      await this.#requireState();
    const displayName =
      normalizeDisplayName(
        body?.displayName,
      );
    const characterId =
      normalizeCharacterId(
        body?.characterId,
      );
    const clientRequestId =
      requireClientRequestId(
        body?.clientRequestId,
      );
    const profileId =
      await this.#resolveLeaderboardProfile(
        request,
        {
          matchId:
            state.matchId,
          clientRequestId,
          matchMode:
            state.matchMode,
        },
      );
    let friendJoin =
      null;
    if (
      state.matchMode ===
        "FRIENDS"
    ) {
      const token =
        request.headers.get(
          "x-ludoproof-friend-join-token",
        );
      if (
        typeof token !==
          "string" ||
        token.length ===
          0
      ) {
        throw httpError(
          401,
          "FRIEND_JOIN_TOKEN_REQUIRED",
          "accepted friend invite credential is required",
        );
      }

      friendJoin =
        await verifyFriendRoomJoinToken(
          this.env,
          token,
        );
      if (
        friendJoin.matchId !==
          state.matchId ||
        friendJoin.hostFriendId !==
          state.hostFriendId
      ) {
        throw httpError(
          403,
          "FRIEND_JOIN_TOKEN_MISMATCH",
          "friend-room join credential is not valid for this room",
        );
      }

      await this.#assertFriendInviteActive(
        friendJoin,
      );

      const existingInvitePlayer =
        state.players.find(
          (candidate) =>
            candidate.friendInviteId ===
              friendJoin.inviteId &&
            candidate.friendId ===
              friendJoin.friendId,
        );
      if (
        existingInvitePlayer
      ) {
        const replayIdentity =
          await derivePlayerIdentity(
            this.env,
            state.matchId,
            existingInvitePlayer
              .joinRequestId,
          );
        return json(200, {
          replayed: true,
          matchId:
            state.matchId,
          playerId:
            replayIdentity.playerId,
          playerToken:
            replayIdentity.playerToken,
          state:
            publicStateWithHistory(
              state,
            ),
        });
      }

      if (
        state.players.some(
          (candidate) =>
            candidate.friendId ===
              friendJoin.friendId,
        )
      ) {
        throw httpError(
          409,
          "FRIEND_ALREADY_JOINED",
          "this friend is already seated in the room",
        );
      }
      if (
        state.players.some(
          (candidate) =>
            candidate.friendInviteId ===
              friendJoin.inviteId,
        )
      ) {
        throw httpError(
          409,
          "FRIEND_INVITE_ALREADY_USED",
          "this friend invite already filled a room seat",
        );
      }
    }

    const identity =
      await derivePlayerIdentity(
        this.env,
        state.matchId,
        clientRequestId,
      );

    const existingPlayer =
      state.players.find(
        (candidate) =>
          candidate.playerId ===
            identity.playerId &&
          candidate.joinRequestId ===
            clientRequestId,
      );
    if (existingPlayer) {
      return json(200, {
        replayed: true,
        matchId:
          state.matchId,
        playerId:
          identity.playerId,
        playerToken:
          identity.playerToken,
        state:
          publicStateWithHistory(
            state,
          ),
      });
    }

    if (
      profileId != null &&
      state.players.some(
        (candidate) =>
          candidate.profileId ===
            profileId,
      )
    ) {
      throw httpError(
        409,
        "PROFILE_ALREADY_JOINED",
        "this leaderboard profile is already in the match",
      );
    }

    const tokenAuthHash =
      await sha256Hex(
        "ludoproof:player-token:v1:" +
          identity.playerToken,
      );

    const next = addPlayer(
      state,
      {
        playerId:
          identity.playerId,
        displayName,
        characterId,
        tokenAuthHash,
        now: Date.now(),
      },
    );
    next.players[
      next.players.length - 1
    ].joinRequestId =
      clientRequestId;
    next.players[
      next.players.length - 1
    ].profileId =
      profileId;
    if (
      friendJoin
    ) {
      next.players[
        next.players.length - 1
      ].friendId =
        friendJoin.friendId;
      next.players[
        next.players.length - 1
      ].friendInviteId =
        friendJoin.inviteId;
    }
    next.history =
      state.history ?? [];
    await this.#persist(next);

    return json(201, {
      replayed: false,
      matchId:
        next.matchId,
      playerId:
        identity.playerId,
      playerToken:
        identity.playerToken,
      state:
        publicStateWithHistory(
          next,
        ),
    });
  }

  async #state(request) {
    let state = await this.#requireState();
    const player = await this.#authorize(state, request);
    state =
      await this.#recordFinishedMatchIfNeeded(
        state,
      );
    return json(200, {
      playerId: player.playerId,
      state: publicStateWithHistory(state),
    });
  }

  async #start(request) {
    const state =
      await this.#requireState();
    const player =
      await this.#authorize(
        state,
        request,
      );

    if (
      state.status === "ACTIVE" &&
      state.hostPlayerId ===
        player.playerId
    ) {
      return json(200, {
        replayed: true,
        state:
          publicStateWithHistory(
            state,
          ),
      });
    }

    const next =
      startMatch(
        state,
        player.playerId,
        Date.now(),
      );
    next.history =
      state.history ?? [];
    await this.#persist(next);
    return json(200, {
      replayed: false,
      state:
        publicStateWithHistory(
          next,
        ),
    });
  }

  async #commitRoll(request, body) {
    requireEntroNex(this.env);
    const clientCommitment = requireDigest(
      body?.clientCommitment,
      "clientCommitment",
    );

    let state = await this.#requireState();
    const player = await this.#authorize(state, request);

    if (
      !state.pendingRoll &&
      (state.history ?? []).some(
        (event) =>
          event.clientCommitment ===
          clientCommitment,
      )
    ) {
      throw httpError(
        409,
        "CLIENT_COMMITMENT_REUSED",
        "client commitment was already consumed by an earlier roll",
      );
    }

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
      const rulesetHash =
        await rulesetHashForState(state);

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
      await this.#persist(state);
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

    validateRoundCommitment(remote, pending, expected, config);
    verifyCommitmentAttestation(remote, this.env);

    let latest = await this.#requireState();
    if (
      !latest.pendingRoll ||
      latest.pendingRoll.clientCommitment !== clientCommitment ||
      latest.pendingRoll.eventIndex !== pending.eventIndex
    ) {
      throw httpError(409, "ROLL_STATE_CONFLICT", "roll state changed during commitment");
    }

    if (latest.pendingRoll.status === "CREATING") {
      const committedAt = Date.now();
      latest = attachRoundCommitment(latest, {
        roundId: remote.roundId,
        serverCommitment: remote.serverCommitment,
        revealDeadlineAt:
          committedAt +
          ROLL_REVEAL_TIMEOUT_MS,
        now: committedAt,
      });
      latest.history = state.history ?? [];
      await this.#persist(latest);
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
      await validateResolvedProof(proof, pending, state.matchId);
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
      await this.#persist(state);
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
      let latest = await this.#requireState();
      if (
        error?.code === "ROUND_EXPIRED" &&
        latest.pendingRoll?.roundId ===
          pending.roundId
      ) {
        const expired =
          await this.#expirePendingIfNeeded(
            latest,
            Math.max(
              Date.now(),
              Number(
                latest.pendingRoll
                  .revealDeadlineAt ?? 0,
              ),
            ),
            "ENTRONEX_ROUND_EXPIRED",
          );
        latest = expired.state;
        if (expired.changed) {
          await this.#persist(latest);
        }
      } else if (
        latest.pendingRoll?.roundId === pending.roundId &&
        latest.pendingRoll.status === "RESOLVING"
      ) {
        latest.pendingRoll.status = "COMMITTED";
        latest.updatedAt = Date.now();
        latest.revision += 1;
        await this.#persist(latest);
      }
      throw error;
    }

    await validateResolvedProof(proof, pending, state.matchId);

    assertLocalProofValid(proof);
    verifyProofAttestation(proof, this.env);

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
    const fairnessEvent =
      sealFairnessEvent(
        state.history ?? [],
        {
          eventIndex: pending.eventIndex,
          eventId: pending.eventId,
          playerId: pending.playerId,
          color: state.players[pending.seat].color,
          roundId: pending.roundId,
          serverCommitment: pending.serverCommitment,
          clientCommitment: pending.clientCommitment,
          actorHash: pending.actorHash,
          previousStateHash: pending.previousStateHash,
          rulesetHash: pending.rulesetHash,
          proofDigest: proof.proofDigest,
          outcome: proof.outcome,
          moveTokenIndex: null,
          captures: 0,
          status: "RESOLVED",
          resolvedAt: Date.now(),
        },
      );
    latest.history = [
      ...(state.history ?? []),
      fairnessEvent,
    ].slice(-200);
    await this.#persist(latest);

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
    let state =
      await this.#requireState();
    const player =
      await this.#authorize(
        state,
        request,
      );
    const tokenIndex =
      Number(body?.tokenIndex);
    const eventIndex =
      Number(body?.eventIndex);

    if (
      !Number.isInteger(tokenIndex) ||
      tokenIndex < 0 ||
      tokenIndex > 3
    ) {
      throw httpError(
        400,
        "INVALID_TOKEN_INDEX",
        "tokenIndex must be 0 through 3",
      );
    }
    if (
      !Number.isSafeInteger(
        eventIndex,
      ) ||
      eventIndex < 0
    ) {
      throw httpError(
        400,
        "INVALID_EVENT_INDEX",
        "eventIndex must be a non-negative safe integer",
      );
    }

    const previous =
      [...(state.history ?? [])]
        .reverse()
        .find(
          (event) =>
            event.eventIndex ===
              eventIndex &&
            event.playerId ===
              player.playerId &&
            event.moveTokenIndex !=
              null,
        );

    if (previous) {
      if (
        previous.moveTokenIndex !==
        tokenIndex
      ) {
        throw httpError(
          409,
          "MOVE_ALREADY_APPLIED",
          "this verified roll was already applied to another token",
        );
      }
      state =
        await this.#recordFinishedMatchIfNeeded(
          state,
        );
      return json(200, {
        replayed: true,
        captures:
          previous.captures ?? 0,
        extraTurn:
          Boolean(
            previous.extraTurn,
          ),
        winnerPlayerId:
          previous.winnerPlayerId ??
          null,
        state:
          publicStateWithHistory(
            state,
          ),
      });
    }

    const pending =
      state.pendingRoll;
    if (!pending) {
      throw httpError(
        409,
        "NO_RESOLVED_ROLL",
        "a verified roll is required before moving",
      );
    }
    if (
      pending.eventIndex !==
      eventIndex
    ) {
      throw httpError(
        409,
        "EVENT_INDEX_DRIFT",
        "move does not match the currently resolved roll",
      );
    }

    const moved =
      applyMove(
        state,
        {
          playerId:
            player.playerId,
          tokenIndex,
          now: Date.now(),
        },
      );
    state = moved.state;

    const history = [
      ...(state.history ?? []),
    ];
    const entryIndex =
      history.findLastIndex(
        (event) =>
          event.eventIndex ===
            eventIndex &&
          event.playerId ===
            player.playerId,
      );
    if (entryIndex < 0) {
      throw httpError(
        500,
        "MOVE_HISTORY_MISSING",
        "verified roll history is missing",
      );
    }

    history[entryIndex] = {
      ...history[entryIndex],
      moveTokenIndex:
        tokenIndex,
      captures:
        moved.captures,
      extraTurn:
        moved.extraTurn,
      winnerPlayerId:
        moved.winnerPlayerId,
      movedAt:
        Date.now(),
    };
    state.history = history;
    await this.#persist(state);
    state =
      await this.#recordFinishedMatchIfNeeded(
        state,
      );

    return json(200, {
      replayed: false,
      captures: moved.captures,
      extraTurn: moved.extraTurn,
      winnerPlayerId:
        moved.winnerPlayerId,
      state:
        publicStateWithHistory(
          state,
        ),
    });
  }

  async #recordFinishedMatchIfNeeded(
    state,
  ) {
    if (
      state.status !== "FINISHED" ||
      state.leaderboardRecordedAt != null
    ) {
      return state;
    }

    if (
      state.matchMode ===
      "FRIENDS"
    ) {
      const next =
        structuredClone(
          state,
        );
      next.leaderboardRecordedAt =
        Date.now();
      await this.#persist(
        next,
      );
      return next;
    }

    const players =
      state.players
        .map((player) => ({
          profileId:
            normalizeLeaderboardProfileId(
              player.profileId,
            ),
          displayName:
            player.displayName,
          won:
            player.playerId ===
              state.winnerPlayerId,
        }))
        .filter(
          (player) =>
            player.profileId != null,
        );

    if (players.length === 0) {
      const next =
        structuredClone(state);
      next.leaderboardRecordedAt =
        Date.now();
      await this.#persist(next);
      return next;
    }

    if (!this.env.LUDOPROOF_LEADERBOARD) {
      return state;
    }

    try {
      const id =
        this.env.LUDOPROOF_LEADERBOARD
          .idFromName("global");
      const target =
        this.env.LUDOPROOF_LEADERBOARD
          .get(id);
      const response =
        await target.fetch(
          new Request(
            "https://leaderboard/record",
            {
              method: "POST",
              headers: {
                "content-type":
                  "application/json",
              },
              body: JSON.stringify({
                matchId:
                  state.matchId,
                players,
              }),
            },
          ),
        );

      if (!response.ok) {
        return state;
      }

      const next =
        structuredClone(state);
      next.leaderboardRecordedAt =
        Date.now();
      await this.#persist(next);
      return next;
    } catch {
      return state;
    }
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
    const actorHash =
      event.actorHash ??
      (await sha256Hex(
        "ludoproof:actor:v1:" + event.playerId,
      ));
    await validateResolvedProof(
      proof,
      {
        ...event,
        actorHash,
        clientCommitment,
      },
      state.matchId,
    );
    if (
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
    assertLocalProofValid(proof);
    verifyProofAttestation(proof, this.env);
    return proof;
  }

  async #events(
    request,
  ) {
    const upgrade =
      request.headers
        .get(
          "upgrade",
        );
    if (
      upgrade
        ?.toLowerCase() !==
      "websocket"
    ) {
      throw httpError(
        426,
        "WEBSOCKET_REQUIRED",
        "realtime events require a WebSocket upgrade",
      );
    }

    const state =
      await this.#requireState();
    const player =
      await this.#authorize(
        state,
        request,
      );

    if (
      typeof WebSocketPair ===
        "undefined" ||
      typeof this.ctx
        .acceptWebSocket !==
        "function"
    ) {
      throw httpError(
        503,
        "REALTIME_UNAVAILABLE",
        "realtime events are unavailable",
      );
    }

    const [
      client,
      server,
    ] =
      Object.values(
        new WebSocketPair(),
      );

    this.ctx.acceptWebSocket(
      server,
      [
        "player:" +
          player.playerId,
      ],
    );

    server.send(
      JSON.stringify({
        type:
          "SYNC",
        matchId:
          state.matchId,
        revision:
          state.revision,
        status:
          state.status,
        playerCount:
          state.players
            .length,
      }),
    );

    return new Response(
      null,
      {
        status:
          101,
        webSocket:
          client,
      },
    );
  }

  async webSocketMessage(
    socket,
    message,
  ) {
    if (typeof message !== "string") return;

    if (message === "ping") {
      try {
        socket.send(JSON.stringify({ type: "PONG", at: Date.now() }));
      } catch {
        // Hibernated socket may already be closing.
      }
      return;
    }

    if (message.length > 512) return;
    let payload;
    try {
      payload = JSON.parse(message);
    } catch {
      return;
    }
    if (payload?.type !== "QUICK_CHAT_SEND") return;

    await this.#mutate(() => this.#handleQuickChat(socket, payload));
  }

  async #handleQuickChat(socket, payload) {
    const emoji = payload?.emoji;
    if (typeof emoji !== "string" || !QUICK_CHAT_EMOJIS.has(emoji)) return;

    // Player identity comes only from the authenticated WebSocket's server-side tag.
    const tags =
      typeof this.ctx.getTags === "function"
        ? this.ctx.getTags(socket)
        : [];
    const playerTag =
      Array.isArray(tags)
        ? tags.find((tag) => typeof tag === "string" && tag.startsWith("player:"))
        : null;
    if (!playerTag) return;
    const playerId = playerTag.slice("player:".length);
    if (!playerId) return;

    const state = await this.ctx.storage.get(STATE_KEY);
    if (!state || state.status !== "ACTIVE" || !Array.isArray(state.players)) return;
    const player = state.players.find((candidate) => candidate?.playerId === playerId);
    if (!player || Number.isSafeInteger(player.forfeitedAt)) return;

    const now = Date.now();
    const cooldownKey = "quick-chat:last:" + playerId;
    const lastSentAt = await this.ctx.storage.get(cooldownKey);
    if (
      Number.isSafeInteger(lastSentAt) &&
      now - lastSentAt < QUICK_CHAT_COOLDOWN_MS
    ) {
      return;
    }
    await this.ctx.storage.put(cooldownKey, now);

    const outgoing = JSON.stringify({
      type: "QUICK_CHAT",
      matchId: state.matchId,
      playerId: player.playerId,
      displayName: player.displayName,
      emoji,
      sentAt: now,
    });
    if (typeof this.ctx.getWebSockets !== "function") return;
    for (const target of this.ctx.getWebSockets()) {
      // Sender already presents the reaction locally after the WebSocket accepts the send.
      if (target === socket) continue;
      try {
        target.send(outgoing);
      } catch {
        try {
          target.close(1011, "quick chat delivery failed");
        } catch {
          // Socket is already closing.
        }
      }
    }
  }

  async webSocketClose(
    socket,
    code,
    reason,
  ) {
    try {
      socket.close(
        code,
        reason,
      );
    } catch {
      // Socket is already closed.
    }
  }

  async webSocketError(
    socket,
  ) {
    try {
      socket.close(
        1011,
        "realtime socket error",
      );
    } catch {
      // Socket is already closed.
    }
  }

  async #resolveLeaderboardProfile(
    request,
    {
      matchId,
      clientRequestId,
      matchMode,
    },
  ) {
    if (
      matchMode ===
        "FRIENDS"
    ) {
      return null;
    }

    const assertion =
      request.headers.get(
        "x-ludoproof-profile-assertion",
      );
    if (
      assertion
    ) {
      const claims =
        await verifyLeaderboardProfileAssertion(
          this.env,
          assertion,
        );
      if (
        claims.matchId !==
          matchId ||
        claims.clientRequestId !==
          clientRequestId
      ) {
        throw httpError(
          403,
          "PROFILE_ASSERTION_MISMATCH",
          "leaderboard profile assertion is not valid for this match request",
        );
      }
      return claims.profileId;
    }

    const authorization =
      request.headers.get(
        "authorization",
      );
    if (
      authorization == null ||
      authorization.trim() ===
        ""
    ) {
      return null;
    }
    if (
      !/^Bearer\s+lpp_[A-Za-z0-9_-]{32,}$/i
        .test(
          authorization,
        )
    ) {
      throw httpError(
        401,
        "PROFILE_AUTH_INVALID",
        "leaderboard profile credential is invalid",
      );
    }
    if (
      !this.env
        .LUDOPROOF_LEADERBOARD
    ) {
      throw httpError(
        503,
        "LEADERBOARD_NOT_CONFIGURED",
        "leaderboard storage is not configured",
      );
    }

    const id =
      this.env
        .LUDOPROOF_LEADERBOARD
        .idFromName(
          "global",
        );
    const target =
      this.env
        .LUDOPROOF_LEADERBOARD
        .get(id);
    const response =
      await target.fetch(
        new Request(
          "https://leaderboard/profile/identity",
          {
            method:
              "GET",
            headers: {
              authorization,
            },
          },
        ),
      );

    let value;
    try {
      value =
        await response.json();
    } catch {
      throw httpError(
        503,
        "PROFILE_AUTH_BAD_RESPONSE",
        "leaderboard profile authorization returned invalid data",
      );
    }

    const profileId =
      normalizeLeaderboardProfileId(
        value?.profileId,
      );
    if (
      !response.ok ||
      value?.ok !== true ||
      profileId == null
    ) {
      throw httpError(
        response.status ===
          401
          ? 401
          : 403,
        value?.error ??
          "PROFILE_AUTH_INVALID",
        value?.message ??
          "leaderboard profile credential is invalid",
      );
    }

    return profileId;
  }

  async #assertFriendHost(
    request,
    body,
  ) {
    const state =
      await this.#requireState();
    const player =
      await this.#authorize(
        state,
        request,
      );
    const hostFriendId =
      String(
        body?.hostFriendId ??
          "",
      )
        .trim()
        .toUpperCase();

    if (
      !/^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/
        .test(
          hostFriendId,
        )
    ) {
      throw httpError(
        400,
        "INVALID_FRIEND_ID",
        "invalid host Friend ID",
      );
    }
    if (
      state.matchMode !==
        "FRIENDS"
    ) {
      throw httpError(
        409,
        "NOT_FRIEND_ROOM",
        "room is not a friends private room",
      );
    }
    if (
      state.status !==
        "WAITING"
    ) {
      throw httpError(
        409,
        "ROOM_NOT_WAITING",
        "room is no longer accepting invites",
      );
    }
    if (
      player.playerId !==
        state.hostPlayerId
    ) {
      throw httpError(
        403,
        "HOST_ONLY",
        "only the private room host can invite friends",
      );
    }
    if (
      state.hostFriendId !==
        hostFriendId ||
      player.friendId !==
        hostFriendId
    ) {
      throw httpError(
        403,
        "FRIEND_HOST_MISMATCH",
        "room host credential is not bound to this Friend ID",
      );
    }

    return json(
      200,
      {
        ok: true,
        matchId:
          state.matchId,
      },
    );
  }

  async #friendIdentity(
    request,
  ) {
    const authorization =
      request.headers.get(
        "authorization",
      );
    if (
      typeof authorization !==
        "string" ||
      !/^Bearer\s+lf_[A-Za-z0-9_-]{32,}$/i
        .test(
          authorization,
        )
    ) {
      throw httpError(
        401,
        "FRIEND_AUTH_REQUIRED",
        "friend credential is required to create a private room",
      );
    }
    if (
      !this.env
        .LUDOPROOF_FRIENDS
    ) {
      throw httpError(
        503,
        "FRIENDS_NOT_CONFIGURED",
        "friend directory is not configured",
      );
    }

    const id =
      this.env
        .LUDOPROOF_FRIENDS
        .idFromName(
          "GLOBAL:V1",
        );
    const target =
      this.env
        .LUDOPROOF_FRIENDS
        .get(id);
    const response =
      await target.fetch(
        new Request(
          "https://friends/identity",
          {
            method:
              "GET",
            headers: {
              authorization,
            },
          },
        ),
      );

    let value;
    try {
      value =
        await response.json();
    } catch {
      throw httpError(
        503,
        "FRIEND_AUTH_BAD_RESPONSE",
        "friend directory returned invalid authorization data",
      );
    }

    if (
      !response.ok ||
      value?.ok !== true ||
      !/^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/
        .test(
          String(
            value?.friendId ??
              "",
          ),
        )
    ) {
      throw httpError(
        response.status ===
          401
          ? 401
          : 403,
        value?.error ??
          "FRIEND_AUTH_INVALID",
        value?.message ??
          "friend credential is invalid",
      );
    }

    return {
      friendId:
        String(
          value.friendId,
        ),
      displayName:
        String(
          value.displayName ??
            "",
        ),
    };
  }

  async #assertFriendInviteActive(
    claims,
  ) {
    if (
      !this.env
        .LUDOPROOF_FRIENDS
    ) {
      throw httpError(
        503,
        "FRIENDS_NOT_CONFIGURED",
        "friend directory is not configured",
      );
    }

    const id =
      this.env
        .LUDOPROOF_FRIENDS
        .idFromName(
          "GLOBAL:V1",
        );
    const target =
      this.env
        .LUDOPROOF_FRIENDS
        .get(id);
    const response =
      await target.fetch(
        new Request(
          "https://friends/invite/join-check",
          {
            method:
              "POST",
            headers: {
              "content-type":
                "application/json",
            },
            body:
              JSON.stringify(
                claims,
              ),
          },
        ),
      );

    let value;
    try {
      value =
        await response.json();
    } catch {
      throw httpError(
        503,
        "FRIEND_INVITE_BAD_RESPONSE",
        "friend directory returned invalid invite authorization data",
      );
    }

    if (
      !response.ok ||
      value?.ok !== true
    ) {
      throw httpError(
        response.status,
        value?.error ??
          "FRIEND_INVITE_INVALID",
        value?.message ??
          "friend invite is not authorized for this room",
      );
    }
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
    let state = await this.ctx.storage.get(STATE_KEY);
    if (!state) throw httpError(404, "MATCH_NOT_FOUND", "match does not exist");
    if (!Array.isArray(state.history)) state.history = [];

    const expired =
      await this.#expirePendingIfNeeded(
        state,
        Date.now(),
      );
    state = expired.state;
    if (expired.changed) {
      await this.#persist(state);
    }
    return state;
  }

  async #expirePendingIfNeeded(
    state,
    now,
    reason = "REVEAL_TIMEOUT",
  ) {
    const pending = state.pendingRoll;
    if (
      !pending ||
      !["CREATING", "COMMITTED", "RESOLVING"]
        .includes(pending.status) ||
      !Number.isSafeInteger(
        pending.revealDeadlineAt,
      ) ||
      now < pending.revealDeadlineAt
    ) {
      return {
        state,
        changed: false,
      };
    }

    const result = forfeitTimedOutRoll(
      state,
      {
        now,
        reason,
      },
    );
    const timedOut =
      result.timedOutRoll;
    const player =
      state.players[timedOut.seat];

    const fairnessEvent =
      sealFairnessEvent(
        state.history ?? [],
        {
          eventIndex: timedOut.eventIndex,
          eventId: timedOut.eventId,
          playerId: timedOut.playerId,
          color: player?.color ?? null,
          roundId: timedOut.roundId,
          serverCommitment:
            timedOut.serverCommitment,
          clientCommitment:
            timedOut.clientCommitment,
          actorHash: timedOut.actorHash,
          previousStateHash:
            timedOut.previousStateHash,
          rulesetHash:
            timedOut.rulesetHash,
          proofDigest: null,
          outcome: null,
          moveTokenIndex: null,
          captures: 0,
          status: "TIMED_OUT",
          timeoutReason:
            timedOut.timeoutReason,
          timedOutAt:
            timedOut.timedOutAt,
          replacementRoundAllowed: false,
        },
      );
    result.state.history = [
      ...(state.history ?? []),
      fairnessEvent,
    ].slice(-200);

    return {
      state: result.state,
      changed: true,
    };
  }
}

export async function rulesetHashForState(state) {
  return sha256Hex(
    "entronex:v4:game-ruleset:" +
      canonicalJson(
        rulesetForState(state),
      ),
  );
}

export function buildEntroNexConfig(matchId, pending) {
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

export async function expectedDigests(config) {
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

export function validateRoundCommitment(
  round,
  pending,
  expected,
  config,
) {
  const exactConfig =
    round?.config &&
    canonicalJson(round.config) === canonicalJson(config);

  if (
    !round ||
    round.protocol !== "v4" ||
    typeof round.roundId !== "string" ||
    round.roundId.length < 1 ||
    round.clientCommitment !== pending.clientCommitment ||
    round.configDigest !== expected.configDigest ||
    round.contextDigest !== expected.contextDigest ||
    round.eventBindingDigest !== expected.eventBindingDigest ||
    !exactConfig ||
    !/^[0-9a-f]{64}$/i.test(round.serverCommitment ?? "")
  ) {
    throw httpError(
      502,
      "ENTRONEX_COMMITMENT_INVALID",
      "EntroNex returned a commitment that does not match the locked game event",
    );
  }
}

export async function validateResolvedProof(
  proof,
  pending,
  matchId,
) {
  const config = buildEntroNexConfig(matchId, pending);
  const expected = await expectedDigests(config);
  const exactConfig =
    proof?.config &&
    canonicalJson(proof.config) === canonicalJson(config);

  if (
    !proof ||
    proof.algorithm !==
      "entronex-v4-dual-commit-hkdf-sha256-context-bound" ||
    proof.roundId !== pending.roundId ||
    proof.serverCommitment !== pending.serverCommitment ||
    proof.clientCommitment !== pending.clientCommitment ||
    proof.configDigest !== expected.configDigest ||
    proof.contextDigest !== expected.contextDigest ||
    proof.eventBindingDigest !== expected.eventBindingDigest ||
    !exactConfig ||
    proof.config?.context?.applicationId !== APP_ID ||
    proof.config.context.sessionId !== matchId ||
    proof.config.context.eventType !== "DICE_ROLL" ||
    proof.config.context.eventIndex !== pending.eventIndex ||
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
    revealDeadlineAt:
      pending.revealDeadlineAt,
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
    fairnessProtocol:
      event.fairnessProtocol ?? null,
    previousFairnessDigest:
      event.previousFairnessDigest ?? null,
    fairnessDigest:
      event.fairnessDigest ?? null,
  }));
}

function publicStateWithHistory(state) {
  const history =
    (state.history ?? []).slice(-100);
  return {
    ...publicState(state),
    history,
    fairness:
      fairnessSummary(history),
  };
}

export async function entronexRequest(env, path, {
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
    response = await entronexFetch(
      env,
      baseUrl + path,
      {
        method,
        headers,
        signal: AbortSignal.timeout(
          entronexTimeoutMs(env),
        ),
        body:
          body == null
            ? undefined
            : JSON.stringify(body),
      },
    );
  } catch (error) {
    if (
      error?.name === "TimeoutError" ||
      error?.name === "AbortError"
    ) {
      throw httpError(
        504,
        "ENTRONEX_TIMEOUT",
        "EntroNex request timed out",
      );
    }
    throw httpError(502, "ENTRONEX_UNAVAILABLE", "EntroNex request failed");
  }

  const text =
    await readTextLimited(
      response,
      MAX_ENTRONEX_RESPONSE_BYTES,
    );
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

function entronexTimeoutMs(env) {
  const value =
    Number(
      env?.ENTRONEX_REQUEST_TIMEOUT_MS,
    );
  return Number.isSafeInteger(value) &&
    value >= 100 &&
    value <= 30_000
    ? value
    : DEFAULT_ENTRONEX_TIMEOUT_MS;
}

async function readTextLimited(
  response,
  maxBytes,
) {
  const declared =
    Number(
      response.headers.get(
        "content-length",
      ),
    );
  if (
    Number.isFinite(declared) &&
    declared > maxBytes
  ) {
    throw httpError(
      502,
      "ENTRONEX_RESPONSE_TOO_LARGE",
      "EntroNex response exceeded the maximum allowed size",
    );
  }

  if (!response.body) {
    return "";
  }

  const reader =
    response.body.getReader();
  const chunks = [];
  let total = 0;

  try {
    while (true) {
      const { done, value } =
        await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > maxBytes) {
        await reader.cancel();
        throw httpError(
          502,
          "ENTRONEX_RESPONSE_TOO_LARGE",
          "EntroNex response exceeded the maximum allowed size",
        );
      }
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  const bytes =
    new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return new TextDecoder()
    .decode(bytes);
}

function assertLocalProofValid(proof) {
  const verification =
    verifyProofV4(proof);
  if (verification.valid !== true) {
    throw httpError(
      502,
      "ENTRONEX_PROOF_INVALID",
      "EntroNex proof failed local mathematical verification",
    );
  }
}

function requireEntroNex(env) {
  if (
    typeof env.ENTRONEX_BASE_URL !== "string" ||
    !env.ENTRONEX_BASE_URL.startsWith("https://") ||
    typeof env.ENTRONEX_API_TOKEN !== "string" ||
    env.ENTRONEX_API_TOKEN.length < 20 ||
    !hasPinnedEntroNexTrust(env)
  ) {
    throw httpError(
      503,
      "ENTRONEX_NOT_CONFIGURED",
      "game backend is not configured with EntroNex",
    );
  }
}

function optionalTargetPlayerCount(
  value,
) {
  if (
    value == null
  ) {
    return null;
  }

  const count =
    Number(
      value,
    );
  if (
    !Number.isInteger(
      count,
    ) ||
    count < 2 ||
    count > 4
  ) {
    throw httpError(
      400,
      "INVALID_TARGET_PLAYER_COUNT",
      "targetPlayerCount must be 2, 3, or 4",
    );
  }
  return count;
}

function normalizeMatchMode(
  value,
  targetPlayerCount,
) {
  const normalized =
    String(
      value ??
        "ONLINE",
    )
      .trim()
      .toUpperCase();

  if (
    normalized !==
      "ONLINE" &&
    normalized !==
      "FRIENDS" &&
    normalized !==
      "TEAM_UP"
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_MODE",
      "matchMode must be ONLINE, FRIENDS, or TEAM_UP",
    );
  }

  if (
    normalized ===
      "FRIENDS" &&
    !Number.isInteger(
      targetPlayerCount,
    )
  ) {
    throw httpError(
      400,
      "FRIEND_ROOM_PLAYER_COUNT_REQUIRED",
      "friend rooms require targetPlayerCount",
    );
  }

  if (
    normalized ===
      "TEAM_UP" &&
    targetPlayerCount !== 4
  ) {
    throw httpError(
      400,
      "TEAM_UP_REQUIRES_FOUR_PLAYERS",
      "team up requires exactly four players",
    );
  }

  return normalized;
}

function optionalLeaderboardProfileId(
  value,
) {
  if (
    value == null ||
    String(value).trim() === ""
  ) {
    return null;
  }

  const normalized =
    normalizeLeaderboardProfileId(
      value,
    );
  if (!normalized) {
    throw httpError(
      400,
      "INVALID_PROFILE_ID",
      "leaderboard profile ID must be a UUID v4",
    );
  }
  return normalized;
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
