import test from "node:test";
import assert from "node:assert/strict";

import {
  MatchmakerQueue,
} from "../src/matchmaker-queue.js";
import {
  MatchRoom,
} from "../src/match-room.js";

const SESSION_KEY =
  "lp_test_session_hmac_key_1234567890abcdef";

class FakeStorage {
  constructor() {
    this.values =
      new Map();
    this.alarmAt =
      null;
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

  async put(
    key,
    value,
  ) {
    this.values.set(
      key,
      structuredClone(
        value,
      ),
    );
  }

  async setAlarm(
    timestamp,
  ) {
    this.alarmAt =
      timestamp;
  }
}

class MatchNamespace {
  constructor(env) {
    this.env =
      env;
    this.rooms =
      new Map();
  }

  idFromName(name) {
    return name;
  }

  get(id) {
    if (
      !this.rooms.has(
        id,
      )
    ) {
      this.rooms.set(
        id,
        new MatchRoom(
          {
            storage:
              new FakeStorage(),
          },
          this.env,
        ),
      );
    }
    return this.rooms.get(
      id,
    );
  }
}

function makeQueue() {
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY:
      SESSION_KEY,
  };
  env.LUDOPROOF_MATCHES =
    new MatchNamespace(
      env,
    );

  return new MatchmakerQueue(
    {
      storage:
        new FakeStorage(),
    },
    env,
  );
}

function request(
  path,
  body,
) {
  return new Request(
    "https://matchmaker" +
      path,
    {
      method:
        "POST",
      headers: {
        "content-type":
          "application/json",
      },
      body:
        JSON.stringify(
          body,
        ),
    },
  );
}

async function read(
  response,
) {
  return {
    status:
      response.status,
    body:
      await response.json(),
  };
}

function player(
  suffix,
  playerCount,
) {
  return {
    displayName:
      "Player " +
      suffix,
    clientRequestId:
      "00000000-0000-4000-8000-" +
      String(
        suffix,
      ).padStart(
        12,
        "0",
      ),
    profileId:
      "10000000-0000-4000-8000-" +
      String(
        suffix,
      ).padStart(
        12,
        "0",
      ),
    playerCount,
  };
}

test(
  "2P public queue creates and auto-starts one authoritative match",
  async () => {
    const queue =
      makeQueue();
    const first =
      player(
        1,
        2,
      );
    const second =
      player(
        2,
        2,
      );

    const waiting =
      await read(
        await queue.fetch(
          request(
            "/search",
            first,
          ),
        ),
      );
    assert.equal(
      waiting.status,
      202,
    );
    assert.equal(
      waiting.body.status,
      "SEARCHING",
    );
    assert.equal(
      waiting.body.queuedPlayers,
      1,
    );

    const matchedSecond =
      await read(
        await queue.fetch(
          request(
            "/search",
            second,
          ),
        ),
      );
    assert.equal(
      matchedSecond.status,
      200,
    );
    assert.equal(
      matchedSecond.body.status,
      "MATCHED",
    );
    assert.equal(
      matchedSecond.body.state
        .status,
      "ACTIVE",
    );
    assert.equal(
      matchedSecond.body.state
        .players.length,
      2,
    );

    const matchedFirst =
      await read(
        await queue.fetch(
          request(
            "/status",
            {
              clientRequestId:
                first.clientRequestId,
              playerCount:
                2,
            },
          ),
        ),
      );

    assert.equal(
      matchedFirst.body.status,
      "MATCHED",
    );
    assert.equal(
      matchedFirst.body.matchId,
      matchedSecond.body.matchId,
    );
    assert.notEqual(
      matchedFirst.body.playerToken,
      matchedSecond.body.playerToken,
    );
    assert.equal(
      matchedFirst.body.state
        .status,
      "ACTIVE",
    );
  },
);

test(
  "4P queue waits for all four real players before starting",
  async () => {
    const queue =
      makeQueue();

    for (
      let index = 1;
      index <= 3;
      index += 1
    ) {
      const result =
        await read(
          await queue.fetch(
            request(
              "/search",
              player(
                index,
                4,
              ),
            ),
          ),
        );
      assert.equal(
        result.body.status,
        "SEARCHING",
      );
      assert.equal(
        result.body.queuedPlayers,
        index,
      );
    }

    const fourth =
      await read(
        await queue.fetch(
          request(
            "/search",
            player(
              4,
              4,
            ),
          ),
        ),
      );

    assert.equal(
      fourth.body.status,
      "MATCHED",
    );
    assert.equal(
      fourth.body.state
        .status,
      "ACTIVE",
    );
    assert.equal(
      fourth.body.state
        .players.length,
      4,
    );
  },
);

test(
  "cancel removes a queued player but never discards an assigned match",
  async () => {
    const queue =
      makeQueue();
    const first =
      player(
        1,
        2,
      );
    const second =
      player(
        2,
        2,
      );

    await queue.fetch(
      request(
        "/search",
        first,
      ),
    );

    const cancelled =
      await read(
        await queue.fetch(
          request(
            "/cancel",
            {
              clientRequestId:
                first.clientRequestId,
              playerCount:
                2,
            },
          ),
        ),
      );
    assert.equal(
      cancelled.body.status,
      "CANCELLED",
    );
    assert.equal(
      cancelled.body.cancelled,
      true,
    );

    await queue.fetch(
      request(
        "/search",
        first,
      ),
    );
    await queue.fetch(
      request(
        "/search",
        second,
      ),
    );

    const raced =
      await read(
        await queue.fetch(
          request(
            "/cancel",
            {
              clientRequestId:
                first.clientRequestId,
              playerCount:
                2,
            },
          ),
        ),
      );

    assert.equal(
      raced.body.status,
      "MATCHED",
    );
    assert.equal(
      raced.body.cancelled,
      false,
    );
    assert.match(
      raced.body.matchId,
      /^LP[A-Z2-9]{8}$/,
    );
  },
);
