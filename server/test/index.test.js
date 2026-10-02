import test from "node:test";
import assert from "node:assert/strict";

import worker from "../src/index.js";
import { TEST_TRUST_ENV } from "./entronex-test-trust.js";

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

test("ready is fail-closed until EntroNex trust, storage, and upstream health are ready", { concurrency: false }, async (t) => {
  const originalFetch =
    globalThis.fetch;
  globalThis.fetch =
    async (input) => {
      const url =
        new URL(
          typeof input === "string"
            ? input
            : input.url,
        );
      if (
        url.pathname ===
        "/health"
      ) {
        return Response.json({
          ok: true,
          service:
            "entronex-v4-eval",
          protocol: "v4",
        });
      }
      throw new Error(
        "unexpected fetch",
      );
    };
  t.after(() => {
    globalThis.fetch =
      originalFetch;
  });
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
  assert.deepEqual(
    missing.json.checks,
    {
      entronexBaseUrlConfigured:
        false,
      entronexTokenConfigured:
        false,
      entronexTrustConfigured:
        false,
      entronexServiceBindingConfigured:
        false,
      entronexConfigured: false,
      matchStoreConfigured:
        false,
      rateGateConfigured:
        false,
      leaderboardConfigured:
        false,
      matchmakerConfigured:
        false,
      friendsConfigured:
        false,
      sessionKeyConfigured:
        false,
    },
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
          ...TEST_TRUST_ENV,
          LUDOPROOF_SESSION_HMAC_KEY:
            "lp_test_session_hmac_key_1234567890abcdef",
          LUDOPROOF_MATCHES: {},
          LUDOPROOF_API_GATE: {},
          LUDOPROOF_LEADERBOARD: {},
          LUDOPROOF_MATCHMAKER: {},
          LUDOPROOF_FRIENDS: {},
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
    ready.json.checks
      .entronexConfigured,
    true,
  );
  assert.equal(
    ready.json.checks
      .entronexServiceBindingConfigured,
    false,
  );
  assert.equal(
    ready.json.checks
      .sessionKeyConfigured,
    true,
  );
  assert.equal(
    ready.json.checks
      .matchStoreConfigured,
    true,
  );
  assert.equal(
    ready.json.checks
      .rateGateConfigured,
    true,
  );
  assert.equal(
    ready.json.checks
      .leaderboardConfigured,
    true,
  );
  assert.equal(
    ready.json.checks
      .matchmakerConfigured,
    true,
  );
  assert.equal(
    ready.json.checks
      .friendsConfigured,
    true,
  );
  assert.equal(
    ready.json.productionClaim,
    false,
  );
});

test(
  "ready prefers the EntroNex Cloudflare service binding when present",
  { concurrency: false },
  async (t) => {
    const originalFetch =
      globalThis.fetch;
    globalThis.fetch =
      async () => {
        throw new Error(
          "public fetch should not be used",
        );
      };
    t.after(() => {
      globalThis.fetch =
        originalFetch;
    });

    let serviceCalls = 0;
    const result =
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
            ...TEST_TRUST_ENV,
            LUDOPROOF_SESSION_HMAC_KEY:
              "lp_test_session_hmac_key_1234567890abcdef",
            LUDOPROOF_MATCHES: {},
            LUDOPROOF_API_GATE: {},
          LUDOPROOF_LEADERBOARD: {},
          LUDOPROOF_MATCHMAKER: {},
          LUDOPROOF_FRIENDS: {},
            ENTRONEX_SERVICE: {
              async fetch(request) {
                serviceCalls += 1;
                assert.equal(
                  new URL(
                    request.url,
                  ).pathname,
                  "/health",
                );
                return Response.json({
                  ok: true,
                  service:
                    "entronex-v4-eval",
                  protocol: "v4",
                });
              },
            },
          },
        ),
      );

    assert.equal(
      result.response.status,
      200,
    );
    assert.equal(
      result.json.ready,
      true,
    );
    assert.equal(
      result.json.checks
        .entronexServiceBindingConfigured,
      true,
    );
    assert.equal(
      serviceCalls,
      1,
    );
  },
);

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


test(
  "ready fails closed when EntroNex health is unreachable",
  { concurrency: false },
  async (t) => {
    const originalFetch =
      globalThis.fetch;
    globalThis.fetch =
      async () => {
        throw new Error(
          "offline",
        );
      };
    t.after(() => {
      globalThis.fetch =
        originalFetch;
    });

    const result =
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
            ...TEST_TRUST_ENV,
            LUDOPROOF_SESSION_HMAC_KEY:
              "lp_test_session_hmac_key_1234567890abcdef",
            LUDOPROOF_MATCHES: {},
            LUDOPROOF_API_GATE: {},
          LUDOPROOF_LEADERBOARD: {},
          LUDOPROOF_MATCHMAKER: {},
          LUDOPROOF_FRIENDS: {},
          },
        ),
      );

    assert.equal(
      result.response.status,
      503,
    );
    assert.equal(
      result.json.ready,
      false,
    );
    assert.equal(
      result.json.entronexReachable,
      false,
    );
  },
);
