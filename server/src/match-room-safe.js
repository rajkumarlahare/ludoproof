import {
  MatchRoom as ForfeitMatchRoom,
} from "./match-room-forfeit.js";
import {
  bearerToken,
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
const PLAYER_TOKEN_HASH_DOMAIN =
  "ludoproof:player-token:v1:";
const DIGEST =
  /^[0-9a-f]{64}$/i;

/**
 * Production safety layer around the legacy forfeit wrapper.
 *
 * Two invariants live here until forfeit handling is folded into the base room:
 * 1. A pending reveal deadline must remain the next alarm even when a different
 *    player forfeits or turn normalization persists the room.
 * 2. If a resolved reveal response was lost on the network, retrying the same
 *    commit is a read-only replay so the existing client can proceed to the
 *    server's historical reveal replay path instead of getting stuck on
 *    CLIENT_COMMITMENT_REUSED.
 */
export class MatchRoom extends ForfeitMatchRoom {
  async fetch(request) {
    const replay =
      await this.#resolvedCommitReplay(
        request,
      );

    if (
      replay != null
    ) {
      await this.#preservePendingRevealAlarm();
      return replay;
    }

    const response =
      await super.fetch(
        request,
      );

    await this.#preservePendingRevealAlarm();
    return response;
  }

  async #preservePendingRevealAlarm() {
    if (
      typeof this.ctx.storage
        .setAlarm !==
      "function"
    ) {
      return;
    }

    const state =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    const deadline =
      state?.pendingRoll
        ?.revealDeadlineAt;

    if (
      Number.isSafeInteger(
        deadline,
      ) &&
      deadline >
        0
    ) {
      await this.ctx.storage
        .setAlarm(
          deadline,
        );
    }
  }

  async #resolvedCommitReplay(
    request,
  ) {
    const url =
      new URL(
        request.url,
      );
    if (
      request.method !==
        "POST" ||
      url.pathname !==
        "/roll/commit"
    ) {
      return null;
    }

    let body;
    try {
      body =
        await request
          .clone()
          .json();
    } catch {
      return null;
    }

    const clientCommitment =
      String(
        body?.clientCommitment ??
          "",
      )
        .trim()
        .toLowerCase();
    if (
      !DIGEST.test(
        clientCommitment,
      )
    ) {
      return null;
    }

    const state =
      await this.ctx.storage.get(
        STATE_KEY,
      );
    if (
      !state ||
      state.status !==
        "ACTIVE" ||
      state.pendingRoll !=
        null ||
      !Array.isArray(
        state.players,
      ) ||
      !Array.isArray(
        state.history,
      )
    ) {
      return null;
    }

    let token;
    try {
      token =
        bearerToken(
          request,
        );
    } catch {
      return null;
    }

    const tokenHash =
      await sha256Hex(
        PLAYER_TOKEN_HASH_DOMAIN +
          token,
      );
    const player =
      state.players.find(
        (candidate) =>
          candidate
            ?.tokenAuthHash ===
          tokenHash,
      );

    if (
      !player ||
      Number.isSafeInteger(
        player.forfeitedAt,
      )
    ) {
      return null;
    }

    const event =
      [...state.history]
        .reverse()
        .find(
          (candidate) =>
            candidate
              ?.playerId ===
              player.playerId &&
            String(
              candidate
                ?.clientCommitment ??
                "",
            )
              .toLowerCase() ===
              clientCommitment &&
            candidate?.status ===
              "RESOLVED" &&
            typeof candidate
              ?.roundId ===
              "string" &&
            candidate.roundId
              .length >
              0 &&
            DIGEST.test(
              candidate
                ?.serverCommitment ??
                "",
            ) &&
            DIGEST.test(
              candidate
                ?.proofDigest ??
                "",
            ) &&
            Number.isInteger(
              candidate
                ?.outcome,
            ) &&
            candidate.outcome >=
              1 &&
            candidate.outcome <=
              6,
        );

    if (
      !event
    ) {
      return null;
    }

    return json(
      200,
      {
        replayed: true,
        historical: true,
        round: {
          eventIndex:
            event.eventIndex,
          eventId:
            event.eventId ??
            null,
          roundId:
            event.roundId,
          serverCommitment:
            event.serverCommitment,
          clientCommitment:
            event.clientCommitment,
          previousStateHash:
            event.previousStateHash ??
            null,
          rulesetHash:
            event.rulesetHash ??
            null,
          revealDeadlineAt:
            null,
        },
        state:
          decoratedState(
            state,
          ),
      },
    );
  }
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
              state.players?.[
                seat
              ]?.forfeitedAt,
            ),
          forfeitedAt:
            Number.isSafeInteger(
              state.players?.[
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
