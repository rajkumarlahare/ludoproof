import {
  MatchRoom as SocialMatchRoom,
} from "./match-room-social.js";

const STATE_KEY = "match-state";
const ACTIVE_IDLE_TTL_MS = 7 * 24 * 60 * 60 * 1000;

// A resolved roll must not let a modified/disconnected client hold the room
// indefinitely. Strategy stays client-controlled inside this window; once it
// expires the move is forfeited and the authoritative turn advances.
export const MOVE_SELECTION_TIMEOUT_MS = 45 * 1000;

export class MatchRoom extends SocialMatchRoom {
  async fetch(request) {
    await this.#enqueue(
      () => this.#expireResolvedMoveIfNeeded(),
    );

    const response = await super.fetch(request);

    await this.#enqueue(
      () => this.#scheduleResolvedMoveDeadline(),
    );
    return response;
  }

  async alarm() {
    const expired = await this.#enqueue(
      () => this.#expireResolvedMoveIfNeeded(),
    );
    if (expired) return;

    await super.alarm();

    await this.#enqueue(
      () => this.#scheduleResolvedMoveDeadline(),
    );
  }

  #enqueue(work) {
    const run = this.mutationTail.then(work, work);
    this.mutationTail = run.catch(() => {});
    return run;
  }

  async #expireResolvedMoveIfNeeded() {
    const state = await this.ctx.storage.get(STATE_KEY);
    const pending = state?.pendingRoll;
    if (
      !state ||
      state.status !== "ACTIVE" ||
      pending?.status !== "RESOLVED"
    ) {
      return false;
    }

    const deadline = resolvedMoveDeadline(pending);
    const now = Date.now();
    if (deadline == null || now < deadline) {
      return false;
    }

    const next = structuredClone(state);
    const seat = next.pendingRoll.seat;
    if (
      Array.isArray(next.consecutiveSixes) &&
      Number.isInteger(seat) &&
      seat >= 0 &&
      seat < next.consecutiveSixes.length
    ) {
      next.consecutiveSixes[seat] = 0;
    }

    const history = Array.isArray(next.history)
      ? [...next.history]
      : [];
    const historyIndex = history.findLastIndex(
      (event) =>
        event?.eventIndex === next.pendingRoll.eventIndex &&
        event?.playerId === next.pendingRoll.playerId,
    );
    if (historyIndex >= 0) {
      history[historyIndex] = {
        ...history[historyIndex],
        moveTimedOutAt: now,
        moveTimeoutReason: "MOVE_SELECTION_TIMEOUT",
      };
      next.history = history;
    }

    next.pendingRoll = null;
    next.turnSeat = nextActiveSeat(
      next,
      Number.isInteger(next.turnSeat)
        ? next.turnSeat
        : seat,
    );
    next.updatedAt = now;
    next.revision = Number(next.revision ?? 0) + 1;

    await this.ctx.storage.put(STATE_KEY, next);
    if (typeof this.ctx.storage.setAlarm === "function") {
      await this.ctx.storage.setAlarm(now + ACTIVE_IDLE_TTL_MS);
    }
    broadcastStateChanged(this.ctx, next);
    return true;
  }

  async #scheduleResolvedMoveDeadline() {
    if (typeof this.ctx.storage.setAlarm !== "function") {
      return false;
    }

    const state = await this.ctx.storage.get(STATE_KEY);
    const pending = state?.pendingRoll;
    if (
      !state ||
      state.status !== "ACTIVE" ||
      pending?.status !== "RESOLVED"
    ) {
      return false;
    }

    const deadline = resolvedMoveDeadline(pending);
    if (deadline == null) {
      return false;
    }
    if (deadline <= Date.now()) {
      return this.#expireResolvedMoveIfNeeded();
    }

    await this.ctx.storage.setAlarm(deadline);
    return true;
  }
}

function resolvedMoveDeadline(pending) {
  const resolvedAt = pending?.resolvedAt;
  if (!Number.isSafeInteger(resolvedAt) || resolvedAt <= 0) {
    return null;
  }
  return resolvedAt + MOVE_SELECTION_TIMEOUT_MS;
}

function nextActiveSeat(state, currentSeat) {
  const count = Array.isArray(state?.players)
    ? state.players.length
    : 0;
  if (count < 1) return currentSeat;

  const start = Number.isInteger(currentSeat)
    ? currentSeat
    : -1;
  for (let offset = 1; offset <= count; offset += 1) {
    const candidate = ((start + offset) % count + count) % count;
    const player = state.players[candidate];
    if (!Number.isSafeInteger(player?.forfeitedAt)) {
      return candidate;
    }
  }
  return start >= 0 && start < count ? start : 0;
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
