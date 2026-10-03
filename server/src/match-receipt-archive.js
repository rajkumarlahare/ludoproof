import {
  canonicalJson,
  httpError,
  sha256Hex,
} from "./crypto.js";
import {
  fairnessSummary,
} from "./fairness.js";
import {
  RULESET,
} from "./game.js";

const META_KEY =
  "receipt:meta:v1";
const SEAL_KEY =
  "receipt:seal:v1";
const EVENT_PREFIX =
  "receipt:event:v1:";
const RECEIPT_PROTOCOL =
  "ludoproof-match-receipt-v1";
const RECEIPT_DIGEST_DOMAIN =
  "ludoproof:match-receipt:v1:";

export class MatchReceiptArchive {
  constructor(ctx) {
    this.ctx = ctx;
  }

  async fetch(request) {
    try {
      const url =
        new URL(
          request.url,
        );

      if (
        request.method ===
          "POST" &&
        url.pathname ===
          "/sync"
      ) {
        return await this.#sync(
          request,
        );
      }

      if (
        request.method ===
          "GET" &&
        url.pathname ===
          "/receipt"
      ) {
        return await this.#receipt();
      }

      return json(
        404,
        {
          error: "NOT_FOUND",
          message: "route not found",
        },
      );
    } catch (error) {
      return errorResponse(
        error,
      );
    }
  }

  async #sync(request) {
    const incoming =
      normalizeSnapshot(
        await readJson(
          request,
        ),
      );
    const existingMeta =
      await this.ctx.storage.get(
        META_KEY,
      );

    if (
      existingMeta &&
      canonicalJson(
        staticMetadata(
          existingMeta,
        ),
      ) !==
        canonicalJson(
          staticMetadata(
            incoming,
          ),
        )
    ) {
      throw httpError(
        409,
        "RECEIPT_METADATA_CONFLICT",
        "match receipt metadata conflicts with the archived match",
      );
    }

    for (
      const event of
        incoming.events
    ) {
      await this.#upsertEvent(
        event,
      );
    }

    const meta = {
      protocol:
        RECEIPT_PROTOCOL,
      matchId:
        incoming.matchId,
      rulesetId:
        incoming.rulesetId,
      matchMode:
        incoming.matchMode,
      createdAt:
        incoming.createdAt,
      status:
        incoming.status,
      updatedAt:
        incoming.updatedAt,
      finishedAt:
        incoming.finishedAt,
      finalRevision:
        incoming.finalRevision,
      randomEventCount:
        incoming.randomEventCount,
      winnerPlayerId:
        incoming.winnerPlayerId,
      players:
        incoming.players,
    };

    await this.ctx.storage.put(
      META_KEY,
      meta,
    );

    const receipt =
      await this.#buildReceipt(
        meta,
      );
    const seal =
      await this.#sealIfEligible(
        receipt,
      );

    return json(
      200,
      {
        ok: true,
        matchId:
          meta.matchId,
        archivedEvents:
          receipt.events.length,
        completeEventArchive:
          receipt.completeEventArchive,
        fairnessValid:
          receipt.fairness.valid,
        sealed:
          seal != null,
        receiptDigest:
          seal?.receiptDigest ??
          null,
      },
    );
  }

  async #upsertEvent(
    incoming,
  ) {
    const key =
      eventKey(
        incoming.eventIndex,
      );
    const existing =
      await this.ctx.storage.get(
        key,
      );

    if (!existing) {
      await this.ctx.storage.put(
        key,
        incoming,
      );
      return;
    }

    if (
      canonicalJson(
        immutableEventCore(
          existing,
        ),
      ) !==
      canonicalJson(
        immutableEventCore(
          incoming,
        ),
      )
    ) {
      throw httpError(
        409,
        "RECEIPT_EVENT_CONFLICT",
        "archived roll evidence cannot be rewritten",
      );
    }

    const existingMove =
      mutableMoveCore(
        existing,
      );
    const incomingMove =
      mutableMoveCore(
        incoming,
      );

    if (
      existing.moveTokenIndex !=
        null &&
      canonicalJson(
        existingMove,
      ) !==
        canonicalJson(
          incomingMove,
        )
    ) {
      throw httpError(
        409,
        "RECEIPT_MOVE_CONFLICT",
        "an archived move result cannot be rewritten",
      );
    }

    if (
      existing.moveTokenIndex ==
        null &&
      incoming.moveTokenIndex !=
        null
    ) {
      await this.ctx.storage.put(
        key,
        {
          ...existing,
          ...incomingMove,
        },
      );
    }
  }

  async #buildReceipt(meta) {
    const events =
      await this.#events();
    const fairness =
      fairnessSummary(
        events,
      );
    const completeEventArchive =
      isCompleteEventArchive(
        events,
        meta.randomEventCount,
      );

    return {
      protocol:
        RECEIPT_PROTOCOL,
      matchId:
        meta.matchId,
      rulesetId:
        meta.rulesetId,
      matchMode:
        meta.matchMode,
      createdAt:
        meta.createdAt,
      status:
        meta.status,
      updatedAt:
        meta.updatedAt,
      finishedAt:
        meta.finishedAt,
      finalRevision:
        meta.finalRevision,
      randomEventCount:
        meta.randomEventCount,
      winnerPlayerId:
        meta.winnerPlayerId,
      players:
        meta.players,
      completeEventArchive,
      fairness,
      events,
    };
  }

  async #sealIfEligible(
    receipt,
  ) {
    const eligible =
      receipt.status ===
        "FINISHED" &&
      receipt.completeEventArchive ===
        true &&
      receipt.fairness.valid ===
        true &&
      receipt.fairness.legacyEvents ===
        0;

    const existingSeal =
      await this.ctx.storage.get(
        SEAL_KEY,
      );

    if (!eligible) {
      return existingSeal ??
        null;
    }

    const receiptDigest =
      await digestReceipt(
        receipt,
      );

    if (existingSeal) {
      if (
        existingSeal.receiptDigest !==
          receiptDigest
      ) {
        throw httpError(
          409,
          "RECEIPT_SEAL_CONFLICT",
          "sealed match receipt no longer matches archived evidence",
        );
      }
      return existingSeal;
    }

    const seal = {
      protocol:
        RECEIPT_PROTOCOL,
      receiptDigest,
      sealedAt:
        Date.now(),
    };
    await this.ctx.storage.put(
      SEAL_KEY,
      seal,
    );
    return seal;
  }

  async #receipt() {
    const meta =
      await this.ctx.storage.get(
        META_KEY,
      );
    if (!meta) {
      throw httpError(
        404,
        "MATCH_RECEIPT_NOT_FOUND",
        "match receipt has not been archived",
      );
    }

    const receipt =
      await this.#buildReceipt(
        meta,
      );
    const seal =
      await this.ctx.storage.get(
        SEAL_KEY,
      );

    if (seal) {
      const expected =
        await digestReceipt(
          receipt,
        );
      if (
        seal.receiptDigest !==
          expected
      ) {
        throw httpError(
          500,
          "MATCH_RECEIPT_INTEGRITY_ERROR",
          "archived match receipt failed integrity verification",
        );
      }
    }

    return json(
      200,
      {
        ...receipt,
        sealed:
          seal != null,
        receiptDigest:
          seal?.receiptDigest ??
          null,
        sealedAt:
          seal?.sealedAt ??
          null,
      },
    );
  }

  async #events() {
    const rows =
      await this.ctx.storage.list({
        prefix:
          EVENT_PREFIX,
      });
    return [
      ...rows.values(),
    ].sort(
      (left, right) =>
        left.eventIndex -
        right.eventIndex,
    );
  }
}

export async function syncMatchReceipt(
  env,
  state,
) {
  if (
    !env?.LUDOPROOF_RECEIPTS ||
    !state ||
    ![
      "ACTIVE",
      "FINISHED",
    ].includes(
      state.status,
    )
  ) {
    return {
      configured:
        Boolean(
          env?.LUDOPROOF_RECEIPTS,
        ),
      synced: false,
    };
  }

  const snapshot =
    buildMatchReceiptSnapshot(
      state,
    );
  const id =
    env.LUDOPROOF_RECEIPTS
      .idFromName(
        snapshot.matchId,
      );
  const target =
    env.LUDOPROOF_RECEIPTS
      .get(id);
  const response =
    await target.fetch(
      new Request(
        "https://receipt/sync",
        {
          method: "POST",
          headers: {
            "content-type":
              "application/json",
          },
          body:
            JSON.stringify(
              snapshot,
            ),
        },
      ),
    );

  if (!response.ok) {
    let errorCode =
      "MATCH_RECEIPT_SYNC_FAILED";
    try {
      const value =
        await response.json();
      errorCode =
        value?.error ??
        errorCode;
    } catch {
      // Keep a stable local error code.
    }
    throw httpError(
      503,
      errorCode,
      "match receipt archive rejected the sync",
    );
  }

  return {
    configured: true,
    synced: true,
  };
}

export function buildMatchReceiptSnapshot(
  state,
) {
  if (
    !state ||
    !/^LP[A-Z2-9]{8}$/.test(
      String(
        state.matchId ??
          "",
      ),
    )
  ) {
    throw new TypeError(
      "match receipt requires a valid match state",
    );
  }

  const history =
    Array.isArray(
      state.history,
    )
      ? state.history
      : [];
  const selectedHistory =
    state.status ===
      "FINISHED"
      ? history
      : history.slice(-2);

  return normalizeSnapshot({
    matchId:
      state.matchId,
    rulesetId:
      RULESET.id,
    matchMode:
      state.matchMode ??
      "ONLINE",
    createdAt:
      state.createdAt,
    status:
      state.status,
    updatedAt:
      state.updatedAt,
    finishedAt:
      state.finishedAt ??
      null,
    finalRevision:
      state.revision,
    randomEventCount:
      state.randomEventIndex,
    winnerPlayerId:
      state.winnerPlayerId ??
      null,
    players:
      state.players.map(
        (player, seat) => ({
          playerId:
            player.playerId,
          seat,
          color:
            player.color,
          forfeited:
            Number.isSafeInteger(
              player.forfeitedAt,
            ),
          forfeitedAt:
            Number.isSafeInteger(
              player.forfeitedAt,
            )
              ? player.forfeitedAt
              : null,
        }),
      ),
    events:
      selectedHistory.map(
        normalizeEvent,
      ),
  });
}

function normalizeSnapshot(
  value,
) {
  const matchId =
    String(
      value?.matchId ??
        "",
    )
      .trim()
      .toUpperCase();
  if (
    !/^LP[A-Z2-9]{8}$/.test(
      matchId,
    )
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      "match receipt contains an invalid match ID",
    );
  }

  const status =
    String(
      value?.status ??
        "",
    );
  if (
    status !== "ACTIVE" &&
    status !== "FINISHED"
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      "match receipt status is invalid",
    );
  }

  const players =
    Array.isArray(
      value?.players,
    )
      ? value.players.map(
          normalizePlayer,
        )
      : [];
  if (
    players.length < 1 ||
    players.length > 4
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      "match receipt players are invalid",
    );
  }

  const events =
    Array.isArray(
      value?.events,
    )
      ? value.events.map(
          normalizeEvent,
        )
      : [];

  return {
    protocol:
      RECEIPT_PROTOCOL,
    matchId,
    rulesetId:
      String(
        value?.rulesetId ??
          RULESET.id,
      ),
    matchMode:
      String(
        value?.matchMode ??
          "ONLINE",
      ),
    createdAt:
      safeInteger(
        value?.createdAt,
        "createdAt",
      ),
    status,
    updatedAt:
      safeInteger(
        value?.updatedAt,
        "updatedAt",
      ),
    finishedAt:
      nullableSafeInteger(
        value?.finishedAt,
        "finishedAt",
      ),
    finalRevision:
      nonNegativeInteger(
        value?.finalRevision,
        "finalRevision",
      ),
    randomEventCount:
      nonNegativeInteger(
        value?.randomEventCount,
        "randomEventCount",
      ),
    winnerPlayerId:
      nullableString(
        value?.winnerPlayerId,
      ),
    players,
    events,
  };
}

function normalizePlayer(
  value,
) {
  const seat =
    nonNegativeInteger(
      value?.seat,
      "seat",
    );
  if (seat > 3) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      "match receipt seat is invalid",
    );
  }

  return {
    playerId:
      requiredString(
        value?.playerId,
        "playerId",
      ),
    seat,
    color:
      requiredString(
        value?.color,
        "color",
      ),
    forfeited:
      value?.forfeited ===
        true,
    forfeitedAt:
      nullableSafeInteger(
        value?.forfeitedAt,
        "forfeitedAt",
      ),
  };
}

function normalizeEvent(
  value,
) {
  return {
    eventIndex:
      nonNegativeInteger(
        value?.eventIndex,
        "eventIndex",
      ),
    eventId:
      nullableString(
        value?.eventId,
      ),
    playerId:
      requiredString(
        value?.playerId,
        "playerId",
      ),
    color:
      nullableString(
        value?.color,
      ),
    roundId:
      nullableString(
        value?.roundId,
      ),
    serverCommitment:
      nullableString(
        value?.serverCommitment,
      ),
    clientCommitment:
      nullableString(
        value?.clientCommitment,
      ),
    actorHash:
      nullableString(
        value?.actorHash,
      ),
    previousStateHash:
      nullableString(
        value?.previousStateHash,
      ),
    rulesetHash:
      nullableString(
        value?.rulesetHash,
      ),
    proofDigest:
      nullableString(
        value?.proofDigest,
      ),
    outcome:
      nullableSmallInteger(
        value?.outcome,
        1,
        6,
        "outcome",
      ),
    status:
      String(
        value?.status ??
          "RESOLVED",
      ),
    timeoutReason:
      nullableString(
        value?.timeoutReason,
      ),
    replacementRoundAllowed:
      value?.replacementRoundAllowed ==
        null
        ? null
        : value.replacementRoundAllowed ===
          true,
    fairnessProtocol:
      nullableString(
        value?.fairnessProtocol,
      ),
    previousFairnessDigest:
      nullableString(
        value?.previousFairnessDigest,
      ),
    fairnessDigest:
      nullableString(
        value?.fairnessDigest,
      ),
    resolvedAt:
      nullableSafeInteger(
        value?.resolvedAt,
        "resolvedAt",
      ),
    timedOutAt:
      nullableSafeInteger(
        value?.timedOutAt,
        "timedOutAt",
      ),
    moveTokenIndex:
      nullableSmallInteger(
        value?.moveTokenIndex,
        0,
        3,
        "moveTokenIndex",
      ),
    captures:
      nonNegativeInteger(
        value?.captures ??
          0,
        "captures",
      ),
    extraTurn:
      value?.extraTurn ===
        true,
    winnerPlayerId:
      nullableString(
        value?.winnerPlayerId,
      ),
    movedAt:
      nullableSafeInteger(
        value?.movedAt,
        "movedAt",
      ),
  };
}

function immutableEventCore(
  event,
) {
  const {
    moveTokenIndex,
    captures,
    extraTurn,
    winnerPlayerId,
    movedAt,
    ...immutable
  } = event;
  return immutable;
}

function mutableMoveCore(
  event,
) {
  return {
    moveTokenIndex:
      event.moveTokenIndex ??
      null,
    captures:
      event.captures ??
      0,
    extraTurn:
      event.extraTurn ===
      true,
    winnerPlayerId:
      event.winnerPlayerId ??
      null,
    movedAt:
      event.movedAt ??
      null,
  };
}

function staticMetadata(
  meta,
) {
  return {
    protocol:
      RECEIPT_PROTOCOL,
    matchId:
      meta.matchId,
    rulesetId:
      meta.rulesetId,
    matchMode:
      meta.matchMode,
    createdAt:
      meta.createdAt,
    players:
      meta.players.map(
        (player) => ({
          playerId:
            player.playerId,
          seat:
            player.seat,
          color:
            player.color,
        }),
      ),
  };
}

function isCompleteEventArchive(
  events,
  randomEventCount,
) {
  if (
    events.length !==
      randomEventCount
  ) {
    return false;
  }
  return events.every(
    (event, index) =>
      event.eventIndex ===
      index,
  );
}

async function digestReceipt(
  receipt,
) {
  return sha256Hex(
    RECEIPT_DIGEST_DOMAIN +
      canonicalJson(
        receipt,
      ),
  );
}

function eventKey(
  eventIndex,
) {
  return (
    EVENT_PREFIX +
    String(
      eventIndex,
    ).padStart(
      12,
      "0",
    )
  );
}

async function readJson(
  request,
) {
  const type =
    request.headers.get(
      "content-type",
    ) ?? "";
  if (
    !type
      .toLowerCase()
      .startsWith(
        "application/json",
      )
  ) {
    throw httpError(
      415,
      "UNSUPPORTED_MEDIA_TYPE",
      "content-type must be application/json",
    );
  }

  try {
    const body =
      await request.json();
    if (
      !body ||
      typeof body !==
        "object" ||
      Array.isArray(
        body,
      )
    ) {
      throw new Error(
        "object required",
      );
    }
    return body;
  } catch (error) {
    if (
      error?.code ===
      "UNSUPPORTED_MEDIA_TYPE"
    ) {
      throw error;
    }
    throw httpError(
      400,
      "INVALID_JSON",
      "request body must be a JSON object",
    );
  }
}

function safeInteger(
  value,
  name,
) {
  const number =
    Number(
      value,
    );
  if (
    !Number.isSafeInteger(
      number,
    )
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      `${name} must be a safe integer`,
    );
  }
  return number;
}

function nonNegativeInteger(
  value,
  name,
) {
  const number =
    safeInteger(
      value,
      name,
    );
  if (number < 0) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      `${name} must be non-negative`,
    );
  }
  return number;
}

function nullableSafeInteger(
  value,
  name,
) {
  return value ==
    null
    ? null
    : safeInteger(
        value,
        name,
      );
}

function nullableSmallInteger(
  value,
  min,
  max,
  name,
) {
  if (value == null) {
    return null;
  }
  const number =
    safeInteger(
      value,
      name,
    );
  if (
    number < min ||
    number > max
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      `${name} is outside the allowed range`,
    );
  }
  return number;
}

function requiredString(
  value,
  name,
) {
  const text =
    String(
      value ??
        "",
    );
  if (
    text.length < 1 ||
    text.length > 256
  ) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      `${name} is invalid`,
    );
  }
  return text;
}

function nullableString(
  value,
) {
  if (value == null) {
    return null;
  }
  const text =
    String(
      value,
    );
  if (text.length > 512) {
    throw httpError(
      400,
      "INVALID_MATCH_RECEIPT",
      "match receipt string is too long",
    );
  }
  return text;
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
    candidate >= 400 &&
    candidate <= 599
      ? candidate
      : 500;

  return json(
    status,
    {
      error:
        error?.code ??
        "INTERNAL_ERROR",
      message:
        status >= 500 &&
        !error?.code
          ? "internal server error"
          : String(
              error?.message ??
                "internal server error",
            ),
    },
  );
}
