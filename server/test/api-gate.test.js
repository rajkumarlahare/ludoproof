import test from "node:test";
import assert from "node:assert/strict";

import { ApiGate } from "../src/api-gate.js";

class FakeStorage {
  constructor() {
    this.values = new Map();
    this.alarmAt = null;
    this.deleteAllCalls = 0;
  }

  async get(key) {
    return this.values.get(key);
  }

  async put(key, value) {
    this.values.set(
      key,
      structuredClone(value),
    );
  }

  async setAlarm(timestamp) {
    this.alarmAt = timestamp;
  }

  async deleteAll() {
    this.values.clear();
    this.deleteAllCalls += 1;
  }
}

function request(limit = 2, periodMs = 60_000) {
  return new Request(
    "https://gate/check",
    {
      method: "POST",
      headers: {
        "content-type":
          "application/json",
      },
      body: JSON.stringify({
        limit,
        periodMs,
      }),
    },
  );
}

test("rate gate allows requests through the configured limit and then returns 429", async () => {
  const storage = new FakeStorage();
  const gate =
    new ApiGate({ storage });

  const first =
    await gate.fetch(
      request(2),
    );
  const second =
    await gate.fetch(
      request(2),
    );
  const third =
    await gate.fetch(
      request(2),
    );

  assert.equal(first.status, 200);
  assert.equal(second.status, 200);
  assert.equal(third.status, 429);

  const thirdBody =
    await third.json();
  assert.equal(
    thirdBody.allowed,
    false,
  );
  assert.equal(
    thirdBody.remaining,
    0,
  );
  assert.ok(
    Number(
      third.headers.get(
        "retry-after",
      ),
    ) >= 1,
  );
  assert.ok(
    storage.alarmAt > Date.now(),
  );
});

test("rate gate validates policy bounds", async () => {
  const storage = new FakeStorage();
  const gate =
    new ApiGate({ storage });

  const response =
    await gate.fetch(
      request(0),
    );

  assert.equal(
    response.status,
    400,
  );
  const body =
    await response.json();
  assert.equal(
    body.error,
    "INVALID_RATE_POLICY",
  );
});

test("rate gate alarm clears the old bucket", async () => {
  const storage = new FakeStorage();
  const gate =
    new ApiGate({ storage });

  await gate.fetch(
    request(1),
  );
  assert.ok(
    await storage.get("bucket"),
  );

  await gate.alarm();

  assert.equal(
    await storage.get("bucket"),
    undefined,
  );
  assert.equal(
    storage.deleteAllCalls,
    1,
  );
});
