import test from "node:test";
import assert from "node:assert/strict";

import worker from "../src/index.js";

async function body(response) {
  return {
    response,
    json: await response.json(),
  };
}

test("public responses include hardened security headers", async () => {
  const response =
    await worker.fetch(
      new Request(
        "https://ludoproof.example/",
      ),
      {},
    );

  assert.equal(response.status, 200);
  assert.equal(
    response.headers.get(
      "x-content-type-options",
    ),
    "nosniff",
  );
  assert.equal(
    response.headers.get(
      "x-frame-options",
    ),
    "DENY",
  );
  assert.equal(
    response.headers.get(
      "referrer-policy",
    ),
    "no-referrer",
  );
  assert.match(
    response.headers.get(
      "strict-transport-security",
    ) ?? "",
    /max-age=31536000/,
  );
});

test("ready is fail-closed until EntroNex secret and match storage exist", async () => {
  const missing =
    await body(
      await worker.fetch(
        new Request(
          "https://ludoproof.example/ready",
        ),
        {},
      ),
    );

  assert.equal(
    missing.response.status,
    503,
  );
  assert.equal(
    missing.json.ready,
    false,
  );

  const ready =
    await body(
      await worker.fetch(
        new Request(
          "https://ludoproof.example/ready",
        ),
        {
          ENTRONEX_BASE_URL:
            "https://entronex.example.test",
          ENTRONEX_API_TOKEN:
            "lp_test_entronex_token_1234567890",
          LUDOPROOF_MATCHES: {},
        },
      ),
    );

  assert.equal(
    ready.response.status,
    200,
  );
  assert.equal(
    ready.json.ready,
    true,
  );
  assert.equal(
    ready.json.productionClaim,
    false,
  );
});

test("match creation rejects the wrong method", async () => {
  const result =
    await body(
      await worker.fetch(
        new Request(
          "https://ludoproof.example/api/matches",
        ),
        {},
      ),
    );

  assert.equal(
    result.response.status,
    405,
  );
  assert.equal(
    result.response.headers.get("allow"),
    "POST",
  );
  assert.equal(
    result.json.error,
    "METHOD_NOT_ALLOWED",
  );
});

test("POST endpoints require application/json", async () => {
  const result =
    await body(
      await worker.fetch(
        new Request(
          "https://ludoproof.example/api/matches",
          {
            method: "POST",
            headers: {
              "content-type":
                "text/plain",
            },
            body: "{}",
          },
        ),
        {},
      ),
    );

  assert.equal(
    result.response.status,
    415,
  );
  assert.equal(
    result.json.error,
    "UNSUPPORTED_MEDIA_TYPE",
  );
});

test("oversized JSON bodies fail before allocating match storage", async () => {
  const oversized =
    JSON.stringify({
      displayName:
        "x".repeat(9 * 1024),
    });

  const result =
    await body(
      await worker.fetch(
        new Request(
          "https://ludoproof.example/api/matches",
          {
            method: "POST",
            headers: {
              "content-type":
                "application/json",
            },
            body: oversized,
          },
        ),
        {},
      ),
    );

  assert.equal(
    result.response.status,
    413,
  );
  assert.equal(
    result.json.error,
    "REQUEST_TOO_LARGE",
  );
});
