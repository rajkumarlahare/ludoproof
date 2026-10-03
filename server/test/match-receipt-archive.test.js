import test from "node:test";
import assert from "node:assert/strict";

import {
  MatchReceiptArchive,
  buildMatchReceiptSnapshot,
} from "../src/match-receipt-archive.js";
import {
  sealFairnessEvent,
} from "../src/fairness.js";

class FakeStorage {
  constructor() {
    this.values =
      new Map();
  }

  async get(key) {
    const value =
      this.values.get(
        key,
      );
    return value ===
      undefined
      ? undefined
      : structuredClone(
          value,
        );
  }

  async put(key, value) {
    this.values.set(
      key,
      structuredClone(
        value,
      ),
    );
  }

  async list({ prefix } = {}) {
    const rows =
      [...this.values.entries()]
        .filter(
          ([key]) =>
            prefix == null ||
            key.startsWith(
              prefix,
            ),
        )
        .sort(
          ([left], [right]) =>
            left.localeCompare(
              right,
            ),
        )
        .map(
          ([key, value]) => [
            key,
            structuredClone(
              value,
            ),
          ],
        );
    return new Map(
      rows,
    );
  }
}

function room() {
  const ctx = {
    storage:
      new FakeStorage(),
  };
  return {
    ctx,
    archive:
      new MatchReceiptArchive(
        ctx,
      ),
  };
}

function request(
  path,
  {
    method = "GET",
    body = null,
  } = {},
) {
  return new Request(
    "https://receipt" +
      path,
    {
      method,
      headers:
        body == null
          ? undefined
          : {
              "content-type":
                "application/json",
            },
      body:
        body == null
          ? undefined
          : JSON.stringify(
              body,
            ),
    },
  );
}

async function json(response) {
  return {
    response,
    body:
      await response.json(),
  };
}

function proofEvent(
  history,
  eventIndex,
  outcome,
) {
  return sealFairnessEvent(
    history,
    {
      eventIndex,
      eventId:
        "roll:" +
        eventIndex,
      playerId:
        eventIndex % 2 ===
          0
          ? "p_red"
          : "p_green",
      color:
        eventIndex % 2 ===
          0
          ? "RED"
          : "GREEN",
      roundId:
        "round-" +
        eventIndex,
      serverCommitment:
        String(
          eventIndex + 1,
        ).repeat(64)
          .slice(0, 64),
      clientCommitment:
        String(
          eventIndex + 3,
        ).repeat(64)
          .slice(0, 64),
      actorHash:
        "a".repeat(64),
      previousStateHash:
        "b".repeat(64),
      rulesetHash:
        "c".repeat(64),
      proofDigest:
        "d".repeat(64),
      outcome,
      moveTokenIndex:
        null,
      captures: 0,
      status:
        "RESOLVED",
      resolvedAt:
        1_000 +
        eventIndex,
    },
  );
}

function matchState(
  history,
  {
    status = "ACTIVE",
    randomEventIndex =
      history.length,
  } = {},
) {
  return {
    schemaVersion: 1,
    matchId:
      "LPABCDEFGH",
    status,
    createdAt: 100,
    updatedAt:
      2_000 +
      history.length,
    revision:
      10 +
      history.length,
    matchMode:
      "ONLINE",
    players: [
      {
        playerId:
          "p_red",
        displayName:
          "Secret Red Name",
        color: "RED",
        tokenAuthHash:
          "must-never-archive",
        tokens:
          [-1, -1, -1, -1],
      },
      {
        playerId:
          "p_green",
        displayName:
          "Secret Green Name",
        color: "GREEN",
        tokenAuthHash:
          "must-never-archive-2",
        tokens:
          [-1, -1, -1, -1],
      },
    ],
    randomEventIndex,
    winnerPlayerId:
      status ===
        "FINISHED"
        ? "p_red"
        : null,
    finishedAt:
      status ===
        "FINISHED"
        ? 3_000
        : null,
    history:
      structuredClone(
        history,
      ),
  };
}

async function sync(
  archive,
  state,
) {
  return json(
    await archive.fetch(
      request(
        "/sync",
        {
          method: "POST",
          body:
            buildMatchReceiptSnapshot(
              state,
            ),
        },
      ),
    ),
  );
}

test(
  "receipt archive accumulates events and seals a complete finished match",
  async () => {
    const {
      archive,
    } =
      room();
    const history = [];

    history.push(
      proofEvent(
        history,
        0,
        6,
      ),
    );
    const first =
      await sync(
        archive,
        matchState(
          history,
        ),
      );
    assert.equal(
      first.response.status,
      200,
    );
    assert.equal(
      first.body.sealed,
      false,
    );

    history[0] = {
      ...history[0],
      moveTokenIndex: 0,
      captures: 1,
      extraTurn: true,
      movedAt: 1_100,
    };
    history.push(
      proofEvent(
        history,
        1,
        3,
      ),
    );

    await sync(
      archive,
      matchState(
        history,
      ),
    );

    history[1] = {
      ...history[1],
      moveTokenIndex: 2,
      captures: 0,
      extraTurn: false,
      winnerPlayerId:
        "p_red",
      movedAt: 1_200,
    };

    const finished =
      await sync(
        archive,
        matchState(
          history,
          {
            status:
              "FINISHED",
          },
        ),
      );
    assert.equal(
      finished.response.status,
      200,
    );
    assert.equal(
      finished.body.completeEventArchive,
      true,
    );
    assert.equal(
      finished.body.fairnessValid,
      true,
    );
    assert.equal(
      finished.body.sealed,
      true,
    );
    assert.match(
      finished.body.receiptDigest,
      /^[0-9a-f]{64}$/,
    );

    const receipt =
      await json(
        await archive.fetch(
          request(
            "/receipt",
          ),
        ),
      );
    assert.equal(
      receipt.response.status,
      200,
    );
    assert.equal(
      receipt.body.sealed,
      true,
    );
    assert.equal(
      receipt.body.events.length,
      2,
    );
    assert.equal(
      receipt.body.events[0]
        .moveTokenIndex,
      0,
    );

    const serialized =
      JSON.stringify(
        receipt.body,
      );
    assert.equal(
      serialized.includes(
        "Secret Red Name",
      ),
      false,
    );
    assert.equal(
      serialized.includes(
        "tokenAuthHash",
      ),
      false,
    );
    assert.equal(
      serialized.includes(
        "must-never-archive",
      ),
      false,
    );
  },
);

test(
  "archived roll proof evidence cannot be rewritten",
  async () => {
    const {
      archive,
    } =
      room();
    const history = [];
    history.push(
      proofEvent(
        history,
        0,
        4,
      ),
    );

    await sync(
      archive,
      matchState(
        history,
      ),
    );

    const conflicting =
      structuredClone(
        history,
      );
    conflicting[0]
      .serverCommitment =
      "f".repeat(64);

    const result =
      await sync(
        archive,
        matchState(
          conflicting,
        ),
      );
    assert.equal(
      result.response.status,
      409,
    );
    assert.equal(
      result.body.error,
      "RECEIPT_EVENT_CONFLICT",
    );
  },
);

test(
  "finished receipt remains unsealed when historical event coverage is incomplete",
  async () => {
    const {
      archive,
    } =
      room();
    const history = [];
    history.push(
      proofEvent(
        history,
        1,
        2,
      ),
    );

    const result =
      await sync(
        archive,
        matchState(
          history,
          {
            status:
              "FINISHED",
            randomEventIndex: 2,
          },
        ),
      );

    assert.equal(
      result.response.status,
      200,
    );
    assert.equal(
      result.body.completeEventArchive,
      false,
    );
    assert.equal(
      result.body.sealed,
      false,
    );
    assert.equal(
      result.body.receiptDigest,
      null,
    );
  },
);
