import test from "node:test";
import assert from "node:assert/strict";

import worker from "../src/index-hardening.js";
import {
  TEST_TRUST_ENV,
} from "./entronex-test-trust.js";

function gateBinding() {
  return {
    idFromName() {
      return "gate";
    },
    get() {
      return {
        async fetch() {
          return Response.json({
            allowed: true,
          });
        },
      };
    },
  };
}

function receiptBinding(
  receiptBody = {
    protocol:
      "ludoproof-match-receipt-v1",
    matchId:
      "LPABCDEFGH",
    sealed: true,
    receiptDigest:
      "a".repeat(64),
  },
) {
  return {
    idFromName(matchId) {
      return matchId;
    },
    get(id) {
      return {
        async fetch(request) {
          assert.equal(
            id,
            "LPABCDEFGH",
          );
          assert.equal(
            new URL(
              request.url,
            ).pathname,
            "/receipt",
          );
          return Response.json(
            receiptBody,
          );
        },
      };
    },
  };
}

function readyEnv(
  entronexService,
) {
  return {
    ENTRONEX_BASE_URL:
      "https://entronex.example.test",
    ENTRONEX_API_TOKEN:
      "lp_test_entronex_token_1234567890",
    ...TEST_TRUST_ENV,
    LUDOPROOF_SESSION_HMAC_KEY:
      "lp_test_session_hmac_key_1234567890abcdef",
    LUDOPROOF_MATCHES: {},
    LUDOPROOF_API_GATE:
      gateBinding(),
    LUDOPROOF_LEADERBOARD: {},
    LUDOPROOF_MATCHMAKER: {},
    LUDOPROOF_FRIENDS: {},
    LUDOPROOF_RECEIPTS:
      receiptBinding(),
    ENTRONEX_SERVICE:
      entronexService,
  };
}

async function body(response) {
  return {
    response,
    json:
      await response.json(),
  };
}

test(
  "ready caches and coalesces EntroNex reachability probes per environment",
  async () => {
    let serviceCalls = 0;
    const env =
      readyEnv({
        async fetch() {
          serviceCalls += 1;
          return Response.json({
            ok: true,
            protocol: "v4",
          });
        },
      });
    const request =
      () =>
        new Request(
          "https://ludoproof.example/ready",
        );

    const [first, second] =
      await Promise.all([
        body(
          await worker.fetch(
            request(),
            env,
          ),
        ),
        body(
          await worker.fetch(
            request(),
            env,
          ),
        ),
      ]);

    assert.equal(
      first.response.status,
      200,
    );
    assert.equal(
      second.response.status,
      200,
    );
    assert.equal(
      first.json.ready,
      true,
    );
    assert.equal(
      first.json.checks
        .receiptArchiveConfigured,
      true,
    );
    assert.equal(
      serviceCalls,
      1,
    );

    const third =
      await body(
        await worker.fetch(
          request(),
          env,
        ),
      );
    assert.equal(
      third.response.status,
      200,
    );
    assert.equal(
      serviceCalls,
      1,
    );
  },
);

test(
  "production readiness fails closed when receipt archive binding is missing",
  async () => {
    const env =
      readyEnv({
        async fetch() {
          return Response.json({
            ok: true,
            protocol: "v4",
          });
        },
      });
    delete env.LUDOPROOF_RECEIPTS;

    const result =
      await body(
        await worker.fetch(
          new Request(
            "https://ludoproof.example/ready",
          ),
          env,
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
      result.json.checks
        .receiptArchiveConfigured,
      false,
    );
  },
);

test(
  "public match receipt route is rate limited and returns hardened proof evidence",
  async () => {
    const env = {
      LUDOPROOF_API_GATE:
        gateBinding(),
      LUDOPROOF_RECEIPTS:
        receiptBinding(),
    };
    const result =
      await body(
        await worker.fetch(
          new Request(
            "https://ludoproof.example/api/matches/LPABCDEFGH/receipt",
            {
              headers: {
                "cf-connecting-ip":
                  "203.0.113.10",
              },
            },
          ),
          env,
        ),
      );

    assert.equal(
      result.response.status,
      200,
    );
    assert.equal(
      result.json.sealed,
      true,
    );
    assert.equal(
      result.response.headers.get(
        "x-frame-options",
      ),
      "DENY",
    );
    assert.equal(
      result.response.headers.get(
        "cache-control",
      ),
      "no-store",
    );
  },
);

test(
  "public receipt endpoint rejects mutations",
  async () => {
    const result =
      await body(
        await worker.fetch(
          new Request(
            "https://ludoproof.example/api/matches/LPABCDEFGH/receipt",
            {
              method: "POST",
            },
          ),
          {
            LUDOPROOF_API_GATE:
              gateBinding(),
            LUDOPROOF_RECEIPTS:
              receiptBinding(),
          },
        ),
      );

    assert.equal(
      result.response.status,
      405,
    );
    assert.equal(
      result.response.headers.get(
        "allow",
      ),
      "GET",
    );
    assert.equal(
      result.json.error,
      "METHOD_NOT_ALLOWED",
    );
  },
);
