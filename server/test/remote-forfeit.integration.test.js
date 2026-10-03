import test from "node:test";
import assert from "node:assert/strict";

import {
  MatchRoom,
} from "../src/match-room-forfeit.js";

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

  async put(key, value) {
    this.values.set(
      key,
      structuredClone(
        value,
      ),
    );
  }

  async setAlarm(timestamp) {
    this.alarmAt =
      timestamp;
  }

  async deleteAll() {
    this.values.clear();
  }
}

function makeRoom() {
  const ctx = {
    storage:
      new FakeStorage(),
  };
  const env = {
    LUDOPROOF_SESSION_HMAC_KEY:
      SESSION_KEY,
  };
  return {
    ctx,
    room:
      new MatchRoom(
        ctx,
        env,
      ),
  };
}

function request(
  path,
  {
    method = "GET",
    token = null,
    body = null,
  } = {},
) {
  const headers =
    new Headers();
  if (
    token
  ) {
    headers.set(
      "authorization",
      "Bearer " +
        token,
    );
  }
  if (
    body !==
    null
  ) {
    headers.set(
      "content-type",
      "application/json",
    );
  }

  return new Request(
    "https://room" +
      path,
    {
      method,
      headers,
      body:
        body ===
        null
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

async function createStartedMatch(
  playerCount,
) {
  const {
    ctx,
    room,
  } =
    makeRoom();
  const matchId =
    "LPABCDEFGH";

  const created =
    await json(
      await room.fetch(
        request(
          "/create",
          {
            method:
              "POST",
            body: {
              matchId,
              displayName:
                "Host",
              clientRequestId:
                crypto.randomUUID(),
            },
          },
        ),
      ),
    );
  assert.equal(
    created.response.status,
    201,
    JSON.stringify(
      created.body,
    ),
  );

  const players = [
    {
      playerId:
        created.body.playerId,
      playerToken:
        created.body.playerToken,
    },
  ];

  for (
    let index = 1;
    index <
      playerCount;
    index += 1
  ) {
    const joined =
      await json(
        await room.fetch(
          request(
            "/join",
            {
              method:
                "POST",
              body: {
                displayName:
                  "Player " +
                    (
                      index +
                        1
                    ),
                clientRequestId:
                  crypto.randomUUID(),
              },
            },
          ),
        ),
      );
    assert.equal(
      joined.response.status,
      201,
      JSON.stringify(
        joined.body,
      ),
    );
    players.push({
      playerId:
        joined.body.playerId,
      playerToken:
        joined.body.playerToken,
    });
  }

  const started =
    await json(
      await room.fetch(
        request(
          "/start",
          {
            method:
              "POST",
            token:
              players[0]
                .playerToken,
            body: {},
          },
        ),
      ),
    );
  assert.equal(
    started.response.status,
    200,
    JSON.stringify(
      started.body,
    ),
  );
  assert.equal(
    started.body.state.status,
    "ACTIVE",
  );

  return {
    ctx,
    room,
    players,
  };
}

async function forfeit(
  room,
  playerToken,
) {
  return json(
    await room.fetch(
      request(
        "/start",
        {
          method:
            "POST",
          token:
            playerToken,
          body: {
            forfeit:
              true,
          },
        },
      ),
    ),
  );
}

test(
  "two-player exit is an authoritative loss and awards the opponent",
  async () => {
    const {
      room,
      players,
    } =
      await createStartedMatch(
        2,
      );

    const result =
      await forfeit(
        room,
        players[1]
          .playerToken,
      );

    assert.equal(
      result.response.status,
      200,
      JSON.stringify(
        result.body,
      ),
    );
    assert.equal(
      result.body.state.status,
      "FINISHED",
    );
    assert.equal(
      result.body.winnerPlayerId,
      players[0]
        .playerId,
    );
    assert.equal(
      result.body.state.players[1]
        .forfeited,
      true,
    );

    const replay =
      await forfeit(
        room,
        players[1]
          .playerToken,
      );
    assert.equal(
      replay.response.status,
      200,
    );
    assert.equal(
      replay.body.replayed,
      true,
    );
  },
);

test(
  "four-player exits keep the match active until one player remains",
  async () => {
    const {
      room,
      players,
    } =
      await createStartedMatch(
        4,
      );

    const first =
      await forfeit(
        room,
        players[1]
          .playerToken,
      );
    assert.equal(
      first.response.status,
      200,
    );
    assert.equal(
      first.body.state.status,
      "ACTIVE",
    );

    const second =
      await forfeit(
        room,
        players[2]
          .playerToken,
      );
    assert.equal(
      second.response.status,
      200,
    );
    assert.equal(
      second.body.state.status,
      "ACTIVE",
    );

    const third =
      await forfeit(
        room,
        players[3]
          .playerToken,
      );
    assert.equal(
      third.response.status,
      200,
    );
    assert.equal(
      third.body.state.status,
      "FINISHED",
    );
    assert.equal(
      third.body.winnerPlayerId,
      players[0]
        .playerId,
    );
  },
);

test(
  "state normalization skips a forfeited turn seat",
  async () => {
    const {
      ctx,
      room,
      players,
    } =
      await createStartedMatch(
        4,
      );

    await forfeit(
      room,
      players[1]
        .playerToken,
    );

    const stored =
      await ctx.storage.get(
        "match-state",
      );
    stored.turnSeat =
      1;
    await ctx.storage.put(
      "match-state",
      stored,
    );

    const state =
      await json(
        await room.fetch(
          request(
            "/state",
            {
              token:
                players[0]
                  .playerToken,
            },
          ),
        ),
      );

    assert.equal(
      state.response.status,
      200,
      JSON.stringify(
        state.body,
      ),
    );
    assert.equal(
      state.body.state.turnSeat,
      2,
    );
  },
);

test(
  "a forfeited player cannot mutate the match again",
  async () => {
    const {
      room,
      players,
    } =
      await createStartedMatch(
        4,
      );

    await forfeit(
      room,
      players[1]
        .playerToken,
    );

    const mutation =
      await json(
        await room.fetch(
          request(
            "/roll/commit",
            {
              method:
                "POST",
              token:
                players[1]
                  .playerToken,
              body: {
                clientCommitment:
                  "a".repeat(
                    64,
                  ),
              },
            },
          ),
        ),
      );

    assert.equal(
      mutation.response.status,
      409,
    );
    assert.equal(
      mutation.body.error,
      "PLAYER_FORFEITED",
    );
  },
);
