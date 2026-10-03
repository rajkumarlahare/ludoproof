import {
  MatchRoom as BaseMatchRoom,
} from "./match-room.js";
import {
  bearerToken,
  httpError,
  sha256Hex,
} from "./crypto.js";
import {
  publicState,
} from "./game.js";
import {
  fairnessSummary,
} from "./fairness.js";

const STATE_KEY =
  "match-state";
const ACTIVE_IDLE_TTL_MS =
  7 * 24 * 60 * 60 * 1000;
const FINISHED_IDLE_TTL_MS =
  24 * 60 * 60 * 1000;

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

    if (
      shouldNormalizeBefore(
        request.method,
        url.pathname,
      )
    ) {
      await this.#enqueue(
        () =>
          this.#normalizeTurn(),
      );
    }

    if (
      shouldBlockForfeitedMutation(
        request.method,
        url.pathname,
      )
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
      shouldNormalizeAfter(
        request.method,
        url.pathname,
      )
    ) {
      await this.#enqueue(
        () =>
          this.#normalizeTurn(),
      );
    }

    return this.#decorateResponse(
      response,
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
        Number.isSafeInteger(
          actor.player
            .forfeitedAt,
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
              decoratedState(
                state,
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
      next.consecutiveSixes[
        actor.seat
      ] = 0;

      const pending =
        next.pendingRoll;
      if (
        pending?.seat ===
        actor.seat
      ) {
        if (
          pending.status ===
            "CREATING" &&
          next.randomEventIndex ===
            pending.eventIndex
        ) {
          next.randomEventIndex +=
            1;
        }
        next.pendingRoll =
          null;
      }

      const activeSeats =
        activeSeatIndexes(
          next,
        );
      if (
        activeSeats.length <=
        1
      ) {
        const winnerSeat =
          activeSeats[0] ??
          null;
        next.status =
          "FINISHED";
        next.winnerPlayerId =
          winnerSeat == null
            ? null
            : next.players[
                winnerSeat
              ].playerId;
        next.turnSeat =
          winnerSeat;
        next.pendingRoll =
          null;
        next.finishedAt =
          now;
      } else if (
        next.turnSeat ===
          actor.seat ||
        isForfeitedSeat(
          next,
          next.turnSeat,
        )
      ) {
        next.turnSeat =
          nextActiveSeat(
            next,
            actor.seat,
          );
      }

      touch(
        next,
        now,
      );
      await this.#persist(
        next,
      );

      if (
        next.status ===
        "FINISHED"
      ) {
        await super.fetch(
          new Request(
            "https://room/state",
            {
              method: "GET",
              headers: {
                authorization:
                  request.headers.get(
                    "authorization",
                  ) ?? "",
              },
            },
          ),
        );
      }

      const latest =
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
            latest.winnerPlayerId ??
            null,
          state:
            decoratedState(
              latest,
            ),
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
      !Number.isSafeInteger(
        actor.player
          .forfeitedAt,
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
      const winnerSeat =
        activeSeats[0] ??
        null;
      next.status =
        "FINISHED";
      next.winnerPlayerId =
        winnerSeat == null
          ? null
          : next.players[
              winnerSeat
            ].playerId;
      next.turnSeat =
        winnerSeat;
      next.pendingRoll =
        null;
      next.finishedAt =
        now;
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

  async #decorateResponse(
    response,
  ) {
    if (
      response.status ===
        101 ||
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

    let payload;
    try {
      payload =
        await response
          .clone()
          .json();
    } catch {
      return response;
    }

    if (
      !payload ||
      typeof payload !==
        "object" ||
      payload.state ==
        null
    ) {
      return response;
    }

    const state =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    if (
      !state
    ) {
      return response;
    }

    payload.state =
      decoratedState(
        state,
      );

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

function shouldNormalizeBefore(
  method,
  path,
) {
  if (
    path ===
      "/state"
  ) {
    return true;
  }
  return shouldNormalizeAfter(
    method,
    path,
  );
}

function shouldNormalizeAfter(
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

function shouldBlockForfeitedMutation(
  method,
  path,
) {
  return shouldNormalizeAfter(
    method,
    path,
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
    Number.isSafeInteger(
      state.players[
        seat
      ]?.forfeitedAt,
    )
  );
}

function nextActiveSeat(
  state,
  fromSeat,
) {
  const playerCount =
    state.players.length;
  if (
    playerCount <
    1
  ) {
    return null;
  }

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

function decoratedState(
  state,
) {
  const history =
    (
      state.history ??
      []
    ).slice(
      -100,
    );
  const base =
    publicState(
      state,
    );

  return {
    ...base,
    players:
      base.players.map(
        (
          player,
          seat,
        ) => ({
          ...player,
          forfeited:
            Number.isSafeInteger(
              state.players[
                seat
              ]?.forfeitedAt,
            ),
          forfeitedAt:
            Number.isSafeInteger(
              state.players[
                seat
              ]?.forfeitedAt,
            )
              ? state.players[
                  seat
                ].forfeitedAt
              : null,
        }),
      ),
    history,
    fairness:
      fairnessSummary(
        history,
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
