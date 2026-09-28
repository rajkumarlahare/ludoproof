import { hasPinnedEntroNexTrust } from "./entronex-trust.js";
import {
  deterministicMatchId,
  hasSessionKey,
  httpError,
  requireClientRequestId,
  sha256Hex,
} from "./crypto.js";

export { MatchRoom } from "./match-room.js";
export { ApiGate } from "./api-gate.js";

const MAX_BODY_BYTES = 8 * 1024;
const RATE_WINDOW_MS = 60 * 1000;
const RATE_POLICIES = Object.freeze({
  create: 12,
  join: 30,
  state: 120,
  mutation: 90,
});

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (request.method === "GET" && url.pathname === "/") {
        return json(200, {
          ok: true,
          service: "ludoproof-game-api",
          production: false,
          game: "LudoProof",
          ruleset: "ludoproof-standard-v1",
          entronex: "v4-evaluation",
        });
      }

      if (request.method === "GET" && url.pathname === "/health") {
        const checks =
          configurationChecks(env);
        return json(200, {
          ok: true,
          service: "ludoproof-game-api",
          entronexConfigured:
            checks.entronexConfigured,
          checks,
        });
      }

      if (request.method === "GET" && url.pathname === "/ready") {
        const checks =
          configurationChecks(env);
        const configured =
          checks.entronexConfigured &&
          checks.matchStoreConfigured &&
          checks.rateGateConfigured;
        const entronexReachable =
          configured
            ? await probeEntroNex(env)
            : false;
        const ready =
          configured &&
          checks.sessionKeyConfigured &&
          entronexReachable;
        return json(ready ? 200 : 503, {
          ok: ready,
          ready,
          service: "ludoproof-game-api",
          ruleset: "ludoproof-standard-v1",
          entronex: "v4-evaluation",
          entronexReachable,
          checks,
          productionClaim: false,
        });
      }

      if (url.pathname === "/api/matches") {
        requireMethod(request, "POST");
        const body = await readJsonRequest(request);
        await enforceRateLimit(
          env,
          request,
          "create",
          RATE_POLICIES.create,
        );

        const clientRequestId =
          requireClientRequestId(
            body.clientRequestId,
          );

        for (let attempt = 0; attempt < 4; attempt += 1) {
          const matchId =
            await deterministicMatchId(
              clientRequestId,
              attempt,
            );
          const target = room(env, matchId);
          const response = await target.fetch(
            new Request("https://room/create", {
              method: "POST",
              headers: {
                "content-type": "application/json",
              },
              body: JSON.stringify({
                ...body,
                matchId,
              }),
            }),
          );
          if (response.status !== 409) {
            return withSecurityHeaders(response);
          }
        }

        throw httpError(
          503,
          "MATCH_ID_EXHAUSTED",
          "could not allocate a match ID",
        );
      }

      const matchRoute = url.pathname.match(
        /^\/api\/matches\/(LP[A-Z2-9]{8})(?:\/(.+))?$/,
      );
      if (!matchRoute) {
        return json(
          404,
          {
            error: "NOT_FOUND",
            message: "route not found",
          },
        );
      }

      const matchId = matchRoute[1];
      const action = matchRoute[2] ?? "state";
      const routeMap = {
        join: {
          path: "/join",
          method: "POST",
        },
        state: {
          path: "/state",
          method: "GET",
        },
        start: {
          path: "/start",
          method: "POST",
        },
        "roll/commit": {
          path: "/roll/commit",
          method: "POST",
        },
        "roll/reveal": {
          path: "/roll/reveal",
          method: "POST",
        },
        move: {
          path: "/move",
          method: "POST",
        },
      };

      const route = routeMap[action];
      if (!route) {
        return json(
          404,
          {
            error: "NOT_FOUND",
            message: "route not found",
          },
        );
      }

      requireMethod(
        request,
        route.method,
      );

      const target = room(env, matchId);
      const headers = new Headers();
      const authorization =
        request.headers.get("authorization");
      if (authorization) {
        headers.set(
          "authorization",
          authorization,
        );
      }
      headers.set(
        "content-type",
        "application/json",
      );

      let body;
      if (route.method !== "GET") {
        body =
          JSON.stringify(
            await readJsonRequest(request),
          );
      }

      const policyKey =
        action === "join"
          ? "join"
          : action === "state"
            ? "state"
            : "mutation";
      await enforceRateLimit(
        env,
        request,
        policyKey + ":" + matchId,
        RATE_POLICIES[policyKey],
      );

      const response =
        await target.fetch(
          new Request(
            "https://room" + route.path,
            {
              method: route.method,
              headers,
              body,
            },
          ),
        );

      return withSecurityHeaders(response);
    } catch (error) {
      return errorResponse(error);
    }
  },
};

async function enforceRateLimit(
  env,
  request,
  scope,
  limit,
) {
  if (!env.LUDOPROOF_API_GATE) {
    throw httpError(
      503,
      "RATE_LIMITER_NOT_CONFIGURED",
      "API rate limiter is not configured",
    );
  }

  const clientIp =
    request.headers.get(
      "cf-connecting-ip",
    ) ?? "unknown";
  const rateKey =
    await sha256Hex(
      "ludoproof:api-rate:v1:" +
        scope +
        ":" +
        clientIp,
    );
  const id =
    env.LUDOPROOF_API_GATE
      .idFromName(rateKey);
  const target =
    env.LUDOPROOF_API_GATE.get(id);

  let response;
  try {
    response =
      await target.fetch(
        new Request(
          "https://gate/check",
          {
            method: "POST",
            headers: {
              "content-type":
                "application/json",
            },
            body: JSON.stringify({
              limit,
              periodMs:
                RATE_WINDOW_MS,
            }),
          },
        ),
      );
  } catch {
    throw httpError(
      503,
      "RATE_LIMITER_UNAVAILABLE",
      "API rate limiter is unavailable",
    );
  }

  let result;
  try {
    result =
      await response.json();
  } catch {
    throw httpError(
      503,
      "RATE_LIMITER_BAD_RESPONSE",
      "API rate limiter returned invalid data",
    );
  }

  if (
    response.status === 429 ||
    result.allowed === false
  ) {
    const error =
      httpError(
        429,
        "RATE_LIMITED",
        "too many requests",
      );
    error.retryAfter =
      response.headers.get(
        "retry-after",
      ) ?? "60";
    throw error;
  }

  if (
    !response.ok ||
    result.allowed !== true
  ) {
    throw httpError(
      503,
      "RATE_LIMITER_UNAVAILABLE",
      "API rate limiter rejected the check",
    );
  }
}

function room(env, matchId) {
  if (!env.LUDOPROOF_MATCHES) {
    throw httpError(
      503,
      "MATCH_STORE_NOT_CONFIGURED",
      "match storage is not configured",
    );
  }
  const id =
    env.LUDOPROOF_MATCHES.idFromName(matchId);
  return env.LUDOPROOF_MATCHES.get(id);
}

async function probeEntroNex(env) {
  const baseUrl =
    String(
      env.ENTRONEX_BASE_URL,
    ).replace(/\/$/, "");

  try {
    const response =
      await fetch(
        baseUrl + "/health",
        {
          method: "GET",
          headers: {
            accept:
              "application/json",
          },
          signal:
            AbortSignal.timeout(
              3_000,
            ),
        },
      );
    if (!response.ok) {
      return false;
    }

    const length =
      Number(
        response.headers.get(
          "content-length",
        ),
      );
    if (
      Number.isFinite(length) &&
      length > 16 * 1024
    ) {
      return false;
    }

    const text =
      await response.text();
    if (
      new TextEncoder()
        .encode(text)
        .byteLength >
      16 * 1024
    ) {
      return false;
    }
    const body =
      JSON.parse(text);
    return (
      body?.ok === true &&
      (
        body?.protocol ===
          "v4" ||
        body?.service ===
          "entronex-v4-eval"
      )
    );
  } catch {
    return false;
  }
}

function configurationChecks(env) {
  const entronexBaseUrlConfigured =
    typeof env.ENTRONEX_BASE_URL === "string" &&
    env.ENTRONEX_BASE_URL.startsWith("https://");
  const entronexTokenConfigured =
    typeof env.ENTRONEX_API_TOKEN === "string" &&
    env.ENTRONEX_API_TOKEN.length >= 20;
  const entronexTrustConfigured =
    hasPinnedEntroNexTrust(env);

  return {
    entronexBaseUrlConfigured,
    entronexTokenConfigured,
    entronexTrustConfigured,
    entronexConfigured:
      entronexBaseUrlConfigured &&
      entronexTokenConfigured &&
      entronexTrustConfigured,
    matchStoreConfigured:
      Boolean(env.LUDOPROOF_MATCHES),
    rateGateConfigured:
      Boolean(env.LUDOPROOF_API_GATE),
    sessionKeyConfigured:
      hasSessionKey(env),
  };
}

function hasEntroNexConfig(env) {
  return (
    typeof env.ENTRONEX_BASE_URL === "string" &&
    env.ENTRONEX_BASE_URL.startsWith("https://") &&
    typeof env.ENTRONEX_API_TOKEN === "string" &&
    env.ENTRONEX_API_TOKEN.length >= 20 &&
    hasPinnedEntroNexTrust(env)
  );
}

function requireMethod(request, expected) {
  if (request.method !== expected) {
    const error = httpError(
      405,
      "METHOD_NOT_ALLOWED",
      "method not allowed",
    );
    error.allow = expected;
    throw error;
  }
}

async function readJsonRequest(request) {
  const contentType =
    request.headers.get("content-type") ?? "";
  if (
    !contentType
      .toLowerCase()
      .startsWith("application/json")
  ) {
    throw httpError(
      415,
      "UNSUPPORTED_MEDIA_TYPE",
      "content-type must be application/json",
    );
  }

  const declaredLength =
    Number(
      request.headers.get("content-length"),
    );
  if (
    Number.isFinite(declaredLength) &&
    declaredLength > MAX_BODY_BYTES
  ) {
    throw httpError(
      413,
      "REQUEST_TOO_LARGE",
      "request body is too large",
    );
  }

  const text = await request.text();
  const actualLength =
    new TextEncoder()
      .encode(text)
      .byteLength;
  if (actualLength > MAX_BODY_BYTES) {
    throw httpError(
      413,
      "REQUEST_TOO_LARGE",
      "request body is too large",
    );
  }

  let value;
  try {
    value =
      text.length === 0
        ? {}
        : JSON.parse(text);
  } catch {
    throw httpError(
      400,
      "INVALID_JSON",
      "request body must be valid JSON",
    );
  }

  if (
    !value ||
    typeof value !== "object" ||
    Array.isArray(value)
  ) {
    throw httpError(
      400,
      "INVALID_JSON",
      "request body must be a JSON object",
    );
  }

  return value;
}

function json(status, body, extraHeaders = null) {
  const headers =
    securityHeaders();
  headers.set(
    "content-type",
    "application/json; charset=utf-8",
  );
  headers.set(
    "cache-control",
    "no-store",
  );

  if (extraHeaders) {
    for (const [name, value] of extraHeaders) {
      headers.set(name, value);
    }
  }

  return new Response(
    JSON.stringify(body),
    {
      status,
      headers,
    },
  );
}

function withSecurityHeaders(response) {
  const headers =
    new Headers(response.headers);
  const hardened =
    securityHeaders();

  for (const [name, value] of hardened) {
    headers.set(name, value);
  }
  headers.set(
    "cache-control",
    "no-store",
  );

  return new Response(
    response.body,
    {
      status: response.status,
      statusText: response.statusText,
      headers,
    },
  );
}

function securityHeaders() {
  return new Headers({
    "content-security-policy":
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'",
    "permissions-policy":
      "camera=(), microphone=(), geolocation=()",
    "referrer-policy":
      "no-referrer",
    "strict-transport-security":
      "max-age=31536000; includeSubDomains",
    "x-content-type-options":
      "nosniff",
    "x-frame-options":
      "DENY",
    "x-robots-tag":
      "noindex, nofollow",
  });
}

function errorResponse(error) {
  const status =
    Number.isInteger(error?.status) &&
    error.status >= 400 &&
    error.status <= 599
      ? error.status
      : 500;

  const extraHeaders = [];
  if (error?.allow) {
    extraHeaders.push(
      ["allow", error.allow],
    );
  }
  if (error?.retryAfter) {
    extraHeaders.push(
      [
        "retry-after",
        String(error.retryAfter),
      ],
    );
  }

  return json(
    status,
    {
      error:
        error?.code ??
        "INTERNAL_ERROR",
      message:
        status === 500 &&
        !error?.code
          ? "internal server error"
          : String(
              error?.message ??
                "internal server error",
            ),
    },
    extraHeaders,
  );
}
