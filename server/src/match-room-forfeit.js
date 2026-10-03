import {
  MatchRoom as BaseMatchRoom,
} from "./match-room.js";
import {
  bearerToken,
  httpError,
  sha256Hex,
} from "./crypto.js";

const STATE_KEY =
  "match-state";
const ACTIVE_IDLE_TTL_MS =
  7 * 24 * 60 * 60 * 1000;
const FINISHED_IDLE_TTL_MS =
  24 * 60 * 60 * 1000;
const PLAYER_TOKEN_HASH_DOMAIN =
  "ludoproof:player-token:v1:";

export class MatchRoom extends BaseMatchRoom {
  async fetch(request) {
    const url =
      new URL(
        request.url,
      );

    if (
      request.method ===
        "POST" &&
      url.pathname ===
        "/start" &&
      await isForfeitRequest(
        request,
      )
    ) {
      return this.#enqueue(
        () =>
          this.#forfeit(
            request,
          ),
      );
    }

    const guarded =
      shouldGuard(
        request.method,
        url.pathname,
      );

    if (
      guarded ||
      url.pathname ===
        "/state"
    ) {
      await this.#enqueue(
        () =>
          this.#normalizeTurn(),
      );
    }

    if (
      guarded
    ) {
      const blocked =
        await this.#enqueue(
          () =>
            this.#forfeitedMutationResponse(
              request,
            ),
        );
      if (
        blocked
      ) {
        return blocked;
      }
    }

    const response =
      await super.fetch(
        request,
      );

    if (
      !guarded ||
      !response.ok
    ) {
      return response;
    }

    const changed =
      await this.#enqueue(
        () =>
          this.#normalizeTurn(),
      );
    if (
      !changed
    ) {
      return response;
    }

    const latest =
      await this.#readPublicState(
        request,
      );
    return replaceResponseState(
      response,
      latest,
    );
  }

  #enqueue(work) {
    const run =
      this.mutationTail.then(
        work,
        work,
      );
    this.mutationTail =
      run.catch(
        () => {},
      );
    return run;
  }

  async #forfeit(request) {
    try {
      const state =
        await this.ctx.storage.get(
          STATE_KEY,
        );
      if (
        !state
      ) {
        throw httpError(
          404,
          "MATCH_NOT_FOUND",
          "match does not exist",
        );
      }

      const actor =
        await authenticatedPlayer(
          request,
          state,
        );
      if (
        isForfeitedPlayer(
          actor.player,
        )
      ) {
        return json(
          200,
          {
            ok: true,
            replayed: true,
            forfeitedPlayerId:
              actor.player
                .playerId,
            winnerPlayerId:
              state.winnerPlayerId ??
              null,
            state:
              await this.#readPublicState(
                request,
              ),
          },
        );
      }

      if (
        state.status !==
        "ACTIVE"
      ) {
        throw httpError(
          409,
          "INVALID_MATCH_STATUS",
          "only an active match can be forfeited",
        );
      }

      const now =
        Date.now();
      const next =
        structuredClone(
          state,
        );
      const player =
        next.players[
          actor.seat
        ];

      player.forfeitedAt =
        now;
      player.forfeitReason =
        "PLAYER_EXIT";
      player.tokens =
        [-1, -1, -1, -1];
      if (
        Array.isArray(
          next.consecutiveSixes,
        )
      ) {
        next.consecutiveSixes[
          actor.seat
        ] = 0;
      }

      clearPendingRollForSeat(
        next,
        actor.seat,
      );
      finishOrAdvanceAfterForfeit(
        next,
        actor.seat,
        now,
      );
      touch(
        next,
        now,
      );
      await this.#persist(
        next,
      );

      const latestState =
        await this.#readPublicState(
          request,
        );
      const latestStored =
        await this.ctx.storage.get(
          STATE_KEY,
        ) ??
        next;

      return json(
        200,
        {
          ok: true,
          replayed: false,
          forfeitedPlayerId:
            player.playerId,
          winnerPlayerId:
            latestStored
              .winnerPlayerId ??
            null,
          state:
            latestState,
        },
      );
    } catch (error) {
      return errorResponse(
        error,
      );
    }
  }

  async #forfeitedMutationResponse(
    request,
  ) {
    const state =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    if (
      !state
    ) {
      return null;
    }

    let actor;
    try {
      actor =
        await authenticatedPlayer(
          request,
          state,
        );
    } catch {
      return null;
    }

    if (
      !isForfeitedPlayer(
        actor.player,
      )
    ) {
      return null;
    }

    return json(
      409,
      {
        error:
          "PLAYER_FORFEITED",
        message:
          "forfeited players cannot mutate this match",
      },
    );
  }

  async #normalizeTurn() {
    const state =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    if (
      !state ||
      state.status !==
        "ACTIVE"
    ) {
      return false;
    }

    const activeSeats =
      activeSeatIndexes(
        state,
      );
    const now =
      Date.now();

    if (
      activeSeats.length <=
      1
    ) {
      const next =
        structuredClone(
          state,
        );
      finishWithRemainingPlayer(
        next,
        activeSeats,
        now,
      );
      touch(
        next,
        now,
      );
      await this.#persist(
        next,
      );
      return true;
    }

    if (
      !Number.isInteger(
        state.turnSeat,
      ) ||
      isForfeitedSeat(
        state,
        state.turnSeat,
      )
    ) {
      const next =
        structuredClone(
          state,
        );
      next.turnSeat =
        nextActiveSeat(
          next,
          Number.isInteger(
            state.turnSeat,
          )
            ? state.turnSeat
            : -1,
        );
      touch(
        next,
        now,
      );
      await this.#persist(
        next,
      );
      return true;
    }

    return false;
  }

  async #readPublicState(
    request,
  ) {
    const response =
      await super.fetch(
        new Request(
          "https://room/state",
          {
            method:
              "GET",
            headers: {
              authorization:
                request.headers.get(
                  "authorization",
                ) ?? "",
            },
          },
        ),
      );
    const body =
      await response.json();
    if (
      !response.ok
    ) {
      throw httpError(
        response.status,
        body?.error ??
          "STATE_READ_FAILED",
        body?.message ??
          "match state could not be read",
      );
    }

    const stored =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    return decorateForfeitFields(
      body.state,
      stored,
    );
  }

  async #persist(state) {
    await this.ctx.storage.put(
      STATE_KEY,
      state,
    );

    if (
      typeof this.ctx.storage
        .setAlarm ===
      "function"
    ) {
      const ttl =
        state.status ===
          "FINISHED"
          ? FINISHED_IDLE_TTL_MS
          : ACTIVE_IDLE_TTL_MS;
      await this.ctx.storage
        .setAlarm(
          Number(
            state.updatedAt ??
              Date.now(),
          ) +
            ttl,
        );
    }

    broadcastStateChanged(
      this.ctx,
      state,
    );
  }
}

async function isForfeitRequest(
  request,
) {
  try {
    const body =
      await request
        .clone()
        .json();
    return body?.forfeit ===
      true;
  } catch {
    return false;
  }
}

function shouldGuard(
  method,
  path,
) {
  return (
    method ===
      "POST" &&
    [
      "/start",
      "/roll/commit",
      "/roll/reveal",
      "/move",
    ].includes(
      path,
    )
  );
}

async function authenticatedPlayer(
  request,
  state,
) {
  const token =
    bearerToken(
      request,
    );
  const tokenHash =
    await sha256Hex(
      PLAYER_TOKEN_HASH_DOMAIN +
        token,
    );
  const seat =
    state.players.findIndex(
      (player) =>
        player.tokenAuthHash ===
        tokenHash,
    );

  if (
    seat <
    0
  ) {
    throw httpError(
      403,
      "INVALID_PLAYER_TOKEN",
      "player token is not valid for this match",
    );
  }

  return {
    seat,
    player:
      state.players[
        seat
      ],
  };
}

function clearPendingRollForSeat(
  state,
  seat,
) {
  const pending =
    state.pendingRoll;
  if (
    pending?.seat !==
    seat
  ) {
    return;
  }

  if (
    pending.status ===
      "CREATING" &&
    state.randomEventIndex ===
      pending.eventIndex
  ) {
    state.randomEventIndex +=
      1;
  }
  state.pendingRoll =
    null;
}

function finishOrAdvanceAfterForfeit(
  state,
  forfeitedSeat,
  now,
) {
  const activeSeats =
    activeSeatIndexes(
      state,
    );
  if (
    activeSeats.length <=
    1
  ) {
    finishWithRemainingPlayer(
      state,
      activeSeats,
      now,
    );
    return;
  }

  if (
    state.turnSeat ===
      forfeitedSeat ||
    isForfeitedSeat(
      state,
      state.turnSeat,
    )
  ) {
    state.turnSeat =
      nextActiveSeat(
        state,
        forfeitedSeat,
      );
  }
}

function finishWithRemainingPlayer(
  state,
  activeSeats,
  now,
) {
  const winnerSeat =
    activeSeats[0] ??
    null;
  state.status =
    "FINISHED";
  state.winnerPlayerId =
    winnerSeat == null
      ? null
      : state.players[
          winnerSeat
        ].playerId;
  state.turnSeat =
    winnerSeat;
  state.pendingRoll =
    null;
  state.finishedAt =
    now;
}

function activeSeatIndexes(
  state,
) {
  const result = [];
  for (
    let seat = 0;
    seat <
      state.players.length;
    seat += 1
  ) {
    if (
      !isForfeitedSeat(
        state,
        seat,
      )
    ) {
      result.push(
        seat,
      );
    }
  }
  return result;
}

function isForfeitedSeat(
  state,
  seat,
) {
  return (
    Number.isInteger(
      seat,
    ) &&
    seat >=
      0 &&
    seat <
      state.players.length &&
    isForfeitedPlayer(
      state.players[
        seat
      ],
    )
  );
}

function isForfeitedPlayer(
  player,
) {
  return Number.isSafeInteger(
    player?.forfeitedAt,
  );
}

function nextActiveSeat(
  state,
  fromSeat,
) {
  const playerCount =
    state.players.length;
  for (
    let offset = 1;
    offset <=
      playerCount;
    offset += 1
  ) {
    const seat =
      (
        fromSeat +
        offset +
        playerCount
      ) %
      playerCount;
    if (
      !isForfeitedSeat(
        state,
        seat,
      )
    ) {
      return seat;
    }
  }
  return null;
}

function decorateForfeitFields(
  publicState,
  stored,
) {
  if (
    !publicState ||
    !stored ||
    !Array.isArray(
      publicState.players,
    )
  ) {
    return publicState;
  }

  return {
    ...publicState,
    players:
      publicState.players.map(
        (
          player,
          seat,
        ) => ({
          ...player,
          forfeited:
            isForfeitedPlayer(
              stored.players?.[
                seat
              ],
            ),
          forfeitedAt:
            isForfeitedPlayer(
              stored.players?.[
                seat
              ],
            )
              ? stored.players[
                  seat
                ].forfeitedAt
              : null,
        }),
      ),
  };
}

function touch(
  state,
  now,
) {
  state.updatedAt =
    now;
  state.revision =
    Number(
      state.revision ??
        0,
    ) +
    1;
}

function broadcastStateChanged(
  ctx,
  state,
) {
  if (
    typeof ctx
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
      ctx.getWebSockets()
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

async function replaceResponseState(
  response,
  state,
) {
  if (
    !response.headers
      .get(
        "content-type",
      )
      ?.includes(
        "application/json",
      )
  ) {
    return response;
  }

  try {
    const payload =
      await response
        .clone()
        .json();
    if (
      !payload ||
      typeof payload !==
        "object" ||
      payload.state ==
        null
    ) {
      return response;
    }
    payload.state =
      state;
    return new Response(
      JSON.stringify(
        payload,
      ),
      {
        status:
          response.status,
        headers:
          response.headers,
      },
    );
  } catch {
    return response;
  }
}

function json(
  status,
  body,
) {
  return new Response(
    JSON.stringify(
      body,
    ),
    {
      status,
      headers: {
        "content-type":
          "application/json; charset=utf-8",
        "cache-control":
          "no-store",
        "x-content-type-options":
          "nosniff",
      },
    },
  );
}

function errorResponse(
  error,
) {
  const candidate =
    Number(
      error?.status ??
        500,
    );
  const status =
    Number.isInteger(
      candidate,
    ) &&
    candidate >=
      400 &&
    candidate <=
      599
      ? candidate
      : 500;

  return json(
    status,
    {
      error:
        error?.code ??
        "INTERNAL_ERROR",
      message:
        status >=
          500 &&
        !error?.code
          ? "internal server error"
          : String(
              error?.message ??
                "internal server error",
            ),
    },
  );
}
