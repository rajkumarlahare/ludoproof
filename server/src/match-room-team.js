import {
  MatchRoom as SafeMatchRoom,
} from "./match-room-safe.js";
import {
  isTeamUp,
  publicState,
  teamIdForSeat,
} from "./game.js";
import {
  bearerToken,
  sha256Hex,
} from "./crypto.js";
import {
  fairnessSummary,
} from "./fairness.js";

const STATE_KEY = "match-state";
const PLAYER_TOKEN_HASH_DOMAIN = "ludoproof:player-token:v1:";
const FINISHED_IDLE_TTL_MS = 24 * 60 * 60 * 1000;

/**
 * Team-aware compatibility layer.
 *
 * TEAM_UP creation is handled directly by the base authoritative MatchRoom so
 * the final team state is constructed and persisted once. This layer remains
 * only for Team Up lifecycle behavior that is not yet part of the base room,
 * such as team forfeiture.
 */
export class MatchRoom extends SafeMatchRoom {
  async fetch(request) {
    const url = new URL(request.url);

    if (request.method === "POST" && url.pathname === "/start") {
      const teamForfeit = await this.#teamForfeit(request);
      if (teamForfeit != null) return teamForfeit;
    }

    return super.fetch(request);
  }

  async #teamForfeit(request) {
    let body;
    try {
      body = await request.clone().json();
    } catch {
      return null;
    }
    if (body?.forfeit !== true) return null;

    const state = await this.ctx.storage.get(STATE_KEY);
    if (!state || !isTeamUp(state)) return null;

    let token;
    try {
      token = bearerToken(request);
    } catch {
      return null;
    }
    const tokenHash = await sha256Hex(PLAYER_TOKEN_HASH_DOMAIN + token);
    const seat = state.players.findIndex(
      (player) => player?.tokenAuthHash === tokenHash,
    );
    if (seat < 0) {
      return json(403, {
        error: "INVALID_PLAYER_TOKEN",
        message: "player token is not valid for this match",
      });
    }

    if (state.status === "FINISHED") {
      return json(200, {
        ok: true,
        replayed: true,
        winnerTeamId: state.winnerTeamId ?? null,
        state: decoratedState(state),
      });
    }
    if (state.status !== "ACTIVE") {
      return json(409, {
        error: "INVALID_MATCH_STATUS",
        message: "only an active team match can be forfeited",
      });
    }

    const now = Date.now();
    const next = structuredClone(state);
    const actor = next.players[seat];
    actor.forfeitedAt = now;
    actor.forfeitReason = "PLAYER_EXIT";
    actor.tokens = [-1, -1, -1, -1];
    if (Array.isArray(next.consecutiveSixes)) {
      next.consecutiveSixes[seat] = 0;
    }
    if (next.pendingRoll?.seat === seat) {
      if (
        next.pendingRoll.status === "CREATING" &&
        next.randomEventIndex === next.pendingRoll.eventIndex
      ) {
        next.randomEventIndex += 1;
      }
      next.pendingRoll = null;
    }

    const losingTeam = teamIdForSeat(next, seat);
    const winnerTeamId = losingTeam === "A" ? "B" : "A";
    next.status = "FINISHED";
    next.winnerPlayerId = null;
    next.winnerTeamId = winnerTeamId;
    next.finishedAt = now;
    next.updatedAt = now;
    next.revision = Number(next.revision ?? 0) + 1;
    // Individual leaderboard semantics cannot represent two co-winners safely.
    next.leaderboardRecordedAt = now;

    await this.ctx.storage.put(STATE_KEY, next);
    if (typeof this.ctx.storage.setAlarm === "function") {
      await this.ctx.storage.setAlarm(now + FINISHED_IDLE_TTL_MS);
    }
    broadcastStateChanged(this.ctx, next);

    return json(200, {
      ok: true,
      replayed: false,
      forfeitedPlayerId: actor.playerId,
      winnerPlayerId: null,
      winnerTeamId,
      state: decoratedState(next),
    });
  }
}

function decoratedState(state) {
  const history = (state.history ?? []).slice(-100);
  const base = publicState(state);
  return {
    ...base,
    players: base.players.map((player, seat) => ({
      ...player,
      forfeited: Number.isSafeInteger(state.players?.[seat]?.forfeitedAt),
      forfeitedAt: Number.isSafeInteger(state.players?.[seat]?.forfeitedAt)
        ? state.players[seat].forfeitedAt
        : null,
    })),
    history,
    fairness: fairnessSummary(history),
  };
}

function broadcastStateChanged(ctx, state) {
  if (typeof ctx.getWebSockets !== "function") return;
  const payload = JSON.stringify({
    type: "STATE_CHANGED",
    matchId: state.matchId,
    revision: state.revision,
    status: state.status,
    playerCount: state.players?.length ?? 0,
  });
  for (const socket of ctx.getWebSockets()) {
    try {
      socket.send(payload);
    } catch {
      try {
        socket.close(1011, "state sync failed");
      } catch {
        // Socket is already gone.
      }
    }
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
