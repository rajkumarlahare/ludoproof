import test from "node:test";
import assert from "node:assert/strict";

import {
  MatchRoom,
  expectedDigests,
} from "../src/match-room.js";
import { sha256Hex } from "../src/crypto.js";
import {
  TEST_TRUST_ENV,
  attachCommitmentEvidence,
  attachProofEvidence,
} from "./entronex-test-trust.js";
import {
  clientCommitmentForSeedV4,
  resolveOutcomeV4,
  serverCommitmentForSeedV4,
} from "../src/vendor/entronex-v4/v4.js";

const ALGORITHM =
  "entronex-v4-dual-commit-hkdf-sha256-context-bound";
const BASE_URL = "https://entronex.example.test";
const API_TOKEN = "lp_test_entronex_token_1234567890";
const SESSION_KEY =
  "lp_test_session_hmac_key_1234567890abcdef";

class FakeStorage {
  constructor() {
    this.values = new Map();
    this.alarmAt = null;
    this.deleteAllCalls = 0;
  }

  async get(key) {
    const value = this.values.get(key);
    return value === undefined
      ? undefined
      : structuredClone(value);
  }

  async put(key, value) {
    this.values.set(key, structuredClone(value));
  }

  async setAlarm(timestamp) {
    this.alarmAt = timestamp;
  }

  async deleteAll() {
    this.values.clear();
    this.deleteAllCalls += 1;
  }
}

function makeContext() {
  return {
    storage: new FakeStorage(),
  };
}

function makeEnv() {
  return {
    ENTRONEX_BASE_URL: BASE_URL,
    ENTRONEX_API_TOKEN: API_TOKEN,
    LUDOPROOF_SESSION_HMAC_KEY:
      SESSION_KEY,
    ...TEST_TRUST_ENV,
  };
}

async function json(response) {
  const body = await response.json();
  return { response, body };
}

function roomRequest(path, {
  method = "GET",
  token = null,
  body = null,
} = {}) {
  const headers = new Headers();
  if (token) {
    headers.set("authorization", "Bearer " + token);
  }
  if (body !== null) {
    headers.set("content-type", "application/json");
  }

  return new Request("https://room" + path, {
    method,
    headers,
    body:
      body === null
        ? undefined
        : JSON.stringify(body),
  });
}

async function createMatch(
  room,
  matchId = "LPABCDEFGH",
  clientRequestId = crypto.randomUUID(),
) {
  const { response, body } = await json(
    await room.fetch(
      roomRequest("/create", {
        method: "POST",
        body: {
          matchId,
          displayName: "Alice",
          clientRequestId,
        },
      }),
    ),
  );
  assert.equal(response.status, 201);
  return body;
}

async function joinMatch(
  room,
  displayName,
  clientRequestId = crypto.randomUUID(),
) {
  const { response, body } = await json(
    await room.fetch(
      roomRequest("/join", {
        method: "POST",
        body: {
          displayName,
          clientRequestId,
        },
      }),
    ),
  );
  assert.equal(response.status, 201);
  return body;
}

async function startMatch(room, hostToken) {
  const { response, body } = await json(
    await room.fetch(
      roomRequest("/start", {
        method: "POST",
        token: hostToken,
        body: {},
      }),
    ),
  );
  assert.equal(response.status, 200);
  return body;
}

async function setupActiveMatch(playerCount = 2) {
  const ctx = makeContext();
  const env = makeEnv();
  const room = new MatchRoom(ctx, env);
  const host = await createMatch(room);

  const names = ["Bob", "Cara", "Dev"];
  for (let i = 1; i < playerCount; i += 1) {
    await joinMatch(room, names[i - 1]);
  }

  const started = await startMatch(
    room,
    host.playerToken,
  );

  return {
    ctx,
    env,
    room,
    host,
    started,
  };
}

function installEntroNexMock(t, {
  outcomes = [6],
  clientSeeds = [
    "1".repeat(64),
    "3".repeat(64),
  ],
} = {}) {
  const originalFetch = globalThis.fetch;
  const rounds = new Map();
  let createCalls = 0;
  let resolveCalls = 0;
  let outcomeCursor = 0;

  globalThis.fetch = async (input, init = {}) => {
    const url =
      new URL(
        typeof input === "string"
          ? input
          : input.url,
      );
    const method =
      String(init.method ?? "GET").toUpperCase();
    const parsedBody =
      init.body == null
        ? null
        : JSON.parse(String(init.body));

    if (
      method === "POST" &&
      url.pathname === "/v4/rounds"
    ) {
      createCalls += 1;
      const config = {
        outcomes: parsedBody.outcomes,
        context: parsedBody.context,
        world: parsedBody.world,
      };
      const digests =
        await expectedDigests(config);
      const roundId =
        "00000000-0000-4000-8000-" +
        String(createCalls).padStart(12, "0");
      const desiredOutcome =
        outcomes[
          Math.min(
            createCalls - 1,
            outcomes.length - 1,
          )
        ];
      const hintedClientSeed =
        clientSeeds[
          createCalls - 1
        ];
      const serverSeed =
        chooseServerSeed({
          roundId,
          config,
          digests,
          clientCommitment:
            parsedBody.clientCommitment,
          clientSeed:
            hintedClientSeed,
          desiredOutcome,
          fallbackIndex:
            createCalls,
        });
      const serverCommitment =
        serverCommitmentForSeedV4(
          serverSeed,
        );
      const round =
        attachCommitmentEvidence(
          {
            protocol: "v4",
            roundId,
            serverCommitment,
            clientCommitment:
              parsedBody.clientCommitment,
            config,
            ...digests,
            replayed: false,
          },
          createCalls * 2 - 1,
        );
      rounds.set(roundId, {
        round,
        serverSeed,
        proof: null,
        revealedSeed: null,
      });
      return Response.json(
        round,
        { status: 201 },
      );
    }

    const resolveMatch =
      url.pathname.match(
        /^\/v4\/rounds\/([^/]+)\/resolve$/,
      );
    if (
      method === "POST" &&
      resolveMatch
    ) {
      resolveCalls += 1;
      const roundId =
        decodeURIComponent(
          resolveMatch[1],
        );
      const record =
        rounds.get(roundId);
      assert.ok(
        record,
        "mock round must exist",
      );

      if (record.proof) {
        if (
          record.revealedSeed !==
          parsedBody.clientSeed
        ) {
          return Response.json(
            {
              error:
                "ROUND_ALREADY_RESOLVED",
              message:
                "round resolved with another reveal",
            },
            { status: 409 },
          );
        }
        return Response.json({
          ...record.proof,
          replayed: true,
        });
      }

      outcomeCursor += 1;

      const proof =
        attachProofEvidence(
          resolveOutcomeV4({
            roundId,
            serverSeed:
              record.serverSeed,
            serverCommitment:
              record.round.serverCommitment,
            clientSeed:
              parsedBody.clientSeed,
            clientCommitment:
              record.round.clientCommitment,
            contextDigest:
              record.round.contextDigest,
            eventBindingDigest:
              record.round
                .eventBindingDigest,
            config:
              record.round.config,
            configDigest:
              record.round.configDigest,
          }),
          createCalls * 2,
        );

      record.revealedSeed =
        parsedBody.clientSeed;
      record.proof = proof;
      return Response.json(proof);
    }

    const proofMatch =
      url.pathname.match(
        /^\/v4\/rounds\/([^/]+)\/proof$/,
      );
    if (
      method === "GET" &&
      proofMatch
    ) {
      const roundId =
        decodeURIComponent(
          proofMatch[1],
        );
      const record =
        rounds.get(roundId);
      if (!record?.proof) {
        return Response.json(
          {
            error: "proof_not_found",
          },
          { status: 404 },
        );
      }
      return Response.json({
        ...record.proof,
        archived: true,
      });
    }

    throw new Error(
      "unexpected EntroNex request: " +
        method +
        " " +
        url.pathname,
    );
  };

  t.after(() => {
    globalThis.fetch = originalFetch;
  });

  return {
    createCalls: () => createCalls,
    resolveCalls: () => resolveCalls,
  };
}

function chooseServerSeed({
  roundId,
  config,
  digests,
  clientCommitment,
  clientSeed,
  desiredOutcome,
  fallbackIndex,
}) {
  if (
    typeof clientSeed !== "string" ||
    clientCommitmentForSeedV4(
      clientSeed,
    ) !== clientCommitment
  ) {
    return String(fallbackIndex)
      .padStart(64, "0");
  }

  for (
    let candidate = 1;
    candidate <= 10_000;
    candidate += 1
  ) {
    const serverSeed =
      candidate
        .toString(16)
        .padStart(64, "0");
    const proof =
      resolveOutcomeV4({
        roundId,
        serverSeed,
        serverCommitment:
          serverCommitmentForSeedV4(
            serverSeed,
          ),
        clientSeed,
        clientCommitment,
        contextDigest:
          digests.contextDigest,
        eventBindingDigest:
          digests.eventBindingDigest,
        config,
        configDigest:
          digests.configDigest,
      });
    if (
      proof.outcome ===
      desiredOutcome
    ) {
      return serverSeed;
    }
  }

  throw new Error(
    "could not find deterministic test server seed",
  );
}

test(
  "player bearer auth rejects an invalid session token",
  { concurrency: false },
  async () => {
    const ctx = makeContext();
    const env = makeEnv();
    const room = new MatchRoom(ctx, env);
    await createMatch(room);

    const { response, body } = await json(
      await room.fetch(
        roomRequest("/state", {
          token: "lp_wrong_token",
        }),
      ),
    );

    assert.equal(response.status, 403);
    assert.equal(body.error, "AUTH_INVALID");
  },
);

for (const playerCount of [2, 3, 4]) {
  test(
    `host can start an authoritative ${playerCount}-player match`,
    { concurrency: false },
    async () => {
      const {
        started,
      } =
        await setupActiveMatch(
          playerCount,
        );

      assert.equal(
        started.state.status,
        "ACTIVE",
      );
      assert.equal(
        started.state.players.length,
        playerCount,
      );
      assert.equal(
        started.state.turnSeat,
        0,
      );
      assert.equal(
        started.state.randomEventIndex,
        0,
      );
    },
  );
}

test(
  "commit/reveal is idempotent across reconnect and consumes eventIndex at commitment",
  { concurrency: false },
  async (t) => {
    const mock =
      installEntroNexMock(t, {
        outcomes: [6, 2],
      });
    const {
      ctx,
      env,
      room,
      host,
    } =
      await setupActiveMatch(2);

    const clientSeed =
      "1".repeat(64);
    const clientCommitment =
      await sha256Hex(
        "entronex:v4:client-commit:" +
          clientSeed,
      );

    const firstCommit = await json(
      await room.fetch(
        roomRequest(
          "/roll/commit",
          {
            method: "POST",
            token:
              host.playerToken,
            body: {
              clientCommitment,
            },
          },
        ),
      ),
    );
    assert.equal(
      firstCommit.response.status,
      201,
    );
    assert.equal(
      firstCommit.body.round
        .eventIndex,
      0,
    );
    assert.equal(
      mock.createCalls(),
      1,
    );

    const duplicateCommit =
      await json(
        await room.fetch(
          roomRequest(
            "/roll/commit",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {
                clientCommitment,
              },
            },
          ),
        ),
      );
    assert.equal(
      duplicateCommit.response.status,
      200,
    );
    assert.equal(
      duplicateCommit.body.replayed,
      true,
    );
    assert.equal(
      mock.createCalls(),
      1,
      "same logical roll must not create another EntroNex round",
    );

    const otherSeed =
      "2".repeat(64);
    const otherCommitment =
      await sha256Hex(
        "entronex:v4:client-commit:" +
          otherSeed,
      );
    const conflictingCommit =
      await json(
        await room.fetch(
          roomRequest(
            "/roll/commit",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {
                clientCommitment:
                  otherCommitment,
              },
            },
          ),
        ),
      );
    assert.equal(
      conflictingCommit.response.status,
      409,
    );
    assert.equal(
      conflictingCommit.body.error,
      "ROLL_ALREADY_PENDING",
    );
    assert.equal(
      mock.createCalls(),
      1,
    );

    const revealed = await json(
      await room.fetch(
        roomRequest(
          "/roll/reveal",
          {
            method: "POST",
            token:
              host.playerToken,
            body: {
              clientSeed,
            },
          },
        ),
      ),
    );
    assert.equal(
      revealed.response.status,
      200,
    );
    assert.equal(
      revealed.body.outcome,
      6,
    );
    assert.deepEqual(
      revealed.body
        .legalTokenIndexes,
      [0, 1, 2, 3],
    );
    assert.equal(
      mock.resolveCalls(),
      1,
    );

    const reconnectedRoom =
      new MatchRoom(ctx, env);
    const replayedReveal =
      await json(
        await reconnectedRoom.fetch(
          roomRequest(
            "/roll/reveal",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {
                clientSeed,
              },
            },
          ),
        ),
      );
    assert.equal(
      replayedReveal.response.status,
      200,
    );
    assert.equal(
      replayedReveal.body.replayed,
      true,
    );
    assert.equal(
      replayedReveal.body.outcome,
      6,
    );

    const changedReveal =
      await json(
        await reconnectedRoom.fetch(
          roomRequest(
            "/roll/reveal",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {
                clientSeed:
                  otherSeed,
              },
            },
          ),
        ),
      );
    assert.equal(
      changedReveal.response.status,
      400,
    );
    assert.equal(
      changedReveal.body.error,
      "CLIENT_COMMITMENT_MISMATCH",
    );

    const moved = await json(
      await reconnectedRoom.fetch(
        roomRequest(
          "/move",
          {
            method: "POST",
            token:
              host.playerToken,
            body: {
              tokenIndex: 0,
            },
          },
        ),
      ),
    );
    assert.equal(
      moved.response.status,
      200,
    );
    assert.equal(
      moved.body.extraTurn,
      true,
    );

    const nextSeed =
      "3".repeat(64);
    const nextCommitment =
      await sha256Hex(
        "entronex:v4:client-commit:" +
          nextSeed,
      );
    const nextCommit = await json(
      await reconnectedRoom.fetch(
        roomRequest(
          "/roll/commit",
          {
            method: "POST",
            token:
              host.playerToken,
            body: {
              clientCommitment:
                nextCommitment,
            },
          },
        ),
      ),
    );

    assert.equal(
      nextCommit.response.status,
      201,
    );
    assert.equal(
      nextCommit.body.round
        .eventIndex,
      1,
    );
    assert.equal(
      mock.createCalls(),
      2,
    );
  },
);

test(
  "move endpoint rejects movement without a resolved verified roll",
  { concurrency: false },
  async () => {
    const {
      room,
      host,
    } =
      await setupActiveMatch(2);

    const { response, body } = await json(
      await room.fetch(
        roomRequest("/move", {
          method: "POST",
          token: host.playerToken,
          body: {
            tokenIndex: 0,
            eventIndex: 0,
          },
        }),
      ),
    );

    assert.equal(response.status, 409);
    assert.equal(
      body.error,
      "NO_RESOLVED_ROLL",
    );
  },
);


test(
  "inactive matches are removed by the Durable Object alarm",
  { concurrency: false },
  async () => {
    const ctx = makeContext();
    const env = makeEnv();
    const room = new MatchRoom(ctx, env);
    await createMatch(room);

    assert.ok(
      ctx.storage.alarmAt > Date.now(),
      "creating a match should schedule expiry",
    );

    const state =
      await ctx.storage.get("match-state");
    state.updatedAt =
      Date.now() -
      8 * 24 * 60 * 60 * 1000;
    await ctx.storage.put(
      "match-state",
      state,
    );

    await room.alarm();

    assert.equal(
      await ctx.storage.get("match-state"),
      undefined,
    );
    assert.equal(
      ctx.storage.deleteAllCalls,
      1,
    );
  },
);


test(
  "pending committed roll timeout is sealed and passes the turn without replacement",
  { concurrency: false },
  async (t) => {
    installEntroNexMock(t, {
      outcomes: [6],
    });
    const {
      ctx,
      room,
      host,
    } =
      await setupActiveMatch(2);

    const clientSeed = "7".repeat(64);
    const clientCommitment =
      await sha256Hex(
        "entronex:v4:client-commit:" +
          clientSeed,
      );

    const committed = await json(
      await room.fetch(
        roomRequest(
          "/roll/commit",
          {
            method: "POST",
            token: host.playerToken,
            body: { clientCommitment },
          },
        ),
      ),
    );
    assert.equal(
      committed.response.status,
      201,
    );

    const stored =
      await ctx.storage.get(
        "match-state",
      );
    stored.pendingRoll.revealDeadlineAt =
      Date.now() - 1;
    await ctx.storage.put(
      "match-state",
      stored,
    );

    await room.alarm();

    const after =
      await ctx.storage.get(
        "match-state",
      );
    assert.equal(
      after.pendingRoll,
      null,
    );
    assert.equal(
      after.turnSeat,
      1,
    );
    assert.equal(
      after.randomEventIndex,
      1,
    );
    assert.equal(
      after.history.at(-1).status,
      "TIMED_OUT",
    );
    assert.equal(
      after.history.at(-1)
        .replacementRoundAllowed,
      false,
    );
    assert.equal(
      after.history.at(-1)
        .clientCommitment,
      clientCommitment,
    );
  },
);


test(
  "create retry with the same request ID returns the same host session",
  { concurrency: false },
  async () => {
    const ctx = makeContext();
    const env = makeEnv();
    const room = new MatchRoom(
      ctx,
      env,
    );
    const requestId =
      "11111111-1111-4111-8111-111111111111";

    const first =
      await json(
        await room.fetch(
          roomRequest(
            "/create",
            {
              method: "POST",
              body: {
                matchId:
                  "LPABCDEFGH",
                displayName:
                  "Alice",
                clientRequestId:
                  requestId,
              },
            },
          ),
        ),
      );
    const retry =
      await json(
        await room.fetch(
          roomRequest(
            "/create",
            {
              method: "POST",
              body: {
                matchId:
                  "LPABCDEFGH",
                displayName:
                  "Alice",
                clientRequestId:
                  requestId,
              },
            },
          ),
        ),
      );

    assert.equal(
      first.response.status,
      201,
    );
    assert.equal(
      retry.response.status,
      200,
    );
    assert.equal(
      retry.body.replayed,
      true,
    );
    assert.equal(
      retry.body.playerId,
      first.body.playerId,
    );
    assert.equal(
      retry.body.playerToken,
      first.body.playerToken,
    );
    assert.equal(
      retry.body.state.players.length,
      1,
    );
  },
);

test(
  "join retry with the same request ID does not create a duplicate player",
  { concurrency: false },
  async () => {
    const ctx = makeContext();
    const env = makeEnv();
    const room = new MatchRoom(
      ctx,
      env,
    );
    await createMatch(room);

    const requestId =
      "22222222-2222-4222-8222-222222222222";
    const first =
      await json(
        await room.fetch(
          roomRequest(
            "/join",
            {
              method: "POST",
              body: {
                displayName:
                  "Bob",
                clientRequestId:
                  requestId,
              },
            },
          ),
        ),
      );
    const retry =
      await json(
        await room.fetch(
          roomRequest(
            "/join",
            {
              method: "POST",
              body: {
                displayName:
                  "Bob",
                clientRequestId:
                  requestId,
              },
            },
          ),
        ),
      );

    assert.equal(
      first.response.status,
      201,
    );
    assert.equal(
      retry.response.status,
      200,
    );
    assert.equal(
      retry.body.replayed,
      true,
    );
    assert.equal(
      retry.body.playerId,
      first.body.playerId,
    );
    assert.equal(
      retry.body.playerToken,
      first.body.playerToken,
    );
    assert.equal(
      retry.body.state.players.length,
      2,
    );
  },
);

test(
  "host start retry is replay-safe",
  { concurrency: false },
  async () => {
    const {
      room,
      host,
    } =
      await setupActiveMatch(2);

    const retry =
      await json(
        await room.fetch(
          roomRequest(
            "/start",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {},
            },
          ),
        ),
      );

    assert.equal(
      retry.response.status,
      200,
    );
    assert.equal(
      retry.body.replayed,
      true,
    );
    assert.equal(
      retry.body.state.status,
      "ACTIVE",
    );
  },
);

test(
  "move retry is idempotent for the same event and rejects token substitution",
  { concurrency: false },
  async (t) => {
    installEntroNexMock(t, {
      outcomes: [6],
      clientSeeds: [
        "1".repeat(64),
      ],
    });
    const {
      room,
      host,
    } =
      await setupActiveMatch(2);

    const clientSeed =
      "1".repeat(64);
    const clientCommitment =
      await sha256Hex(
        "entronex:v4:client-commit:" +
          clientSeed,
      );

    await room.fetch(
      roomRequest(
        "/roll/commit",
        {
          method: "POST",
          token:
            host.playerToken,
          body: {
            clientCommitment,
          },
        },
      ),
    );
    const reveal =
      await json(
        await room.fetch(
          roomRequest(
            "/roll/reveal",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {
                clientSeed,
              },
            },
          ),
        ),
      );
    assert.equal(
      reveal.response.status,
      200,
    );

    const moveBody = {
      tokenIndex: 0,
      eventIndex: 0,
    };
    const first =
      await json(
        await room.fetch(
          roomRequest(
            "/move",
            {
              method: "POST",
              token:
                host.playerToken,
              body: moveBody,
            },
          ),
        ),
      );
    const retry =
      await json(
        await room.fetch(
          roomRequest(
            "/move",
            {
              method: "POST",
              token:
                host.playerToken,
              body: moveBody,
            },
          ),
        ),
      );

    assert.equal(
      first.response.status,
      200,
    );
    assert.equal(
      first.body.replayed,
      false,
    );
    assert.equal(
      retry.response.status,
      200,
    );
    assert.equal(
      retry.body.replayed,
      true,
    );

    const changed =
      await json(
        await room.fetch(
          roomRequest(
            "/move",
            {
              method: "POST",
              token:
                host.playerToken,
              body: {
                tokenIndex: 1,
                eventIndex: 0,
              },
            },
          ),
        ),
      );
    assert.equal(
      changed.response.status,
      409,
    );
    assert.equal(
      changed.body.error,
      "MOVE_ALREADY_APPLIED",
    );
  },
);
