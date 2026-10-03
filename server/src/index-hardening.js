import baseWorker from "./index.js";
import {
  httpError,
  sha256Hex,
} from "./crypto.js";

const READY_OK_TTL_MS =
  10_000;
const READY_FAIL_TTL_MS =
  2_000;
const RECEIPT_RATE_LIMIT =
  60;
const RATE_WINDOW_MS =
  60_000;
const readyCache =
  new WeakMap();

export default {
  async fetch(request, env) {
    try {
      const url =
        new URL(
          request.url,
        );

      if (
        request.method ===
          "GET" &&
        url.pathname ===
          "/ready"
      ) {
        return await readinessResponse(
          request,
          env,
        );
      }

      const receiptRoute =
        url.pathname.match(
          /^\/api\/matches\/(LP[A-Z2-9]{8})\/receipt$/,
        );
      if (receiptRoute) {
        if (
          request.method !==
            "GET"
        ) {
          const error =
            httpError(
              405,
              "METHOD_NOT_ALLOWED",
              "method not allowed",
            );
          error.allow =
            "GET";
          throw error;
        }

        await enforceReceiptRateLimit(
          env,
          request,
          receiptRoute[1],
        );

        const target =
          receiptArchive(
            env,
            receiptRoute[1],
          );
        const response =
          await target.fetch(
            new Request(
              "https://receipt/receipt",
              {
                method: "GET",
              },
            ),
          );
        return withSecurityHeaders(
          response,
        );
      }

      return await baseWorker.fetch(
        request,
        env,
      );
    } catch (error) {
      return errorResponse(
        error,
      );
    }
  },
};

async function readinessResponse(
  request,
  env,
) {
  const base =
    await cachedBaseReadiness(
      request,
      env,
    );
  let body;
  try {
    body =
      JSON.parse(
        base.body,
      );
  } catch {
    return json(
      503,
      {
        ok: false,
        ready: false,
        error:
          "READINESS_BAD_RESPONSE",
      },
    );
  }

  const proofArchiveConfigured =
    Boolean(
      env?.LUDOPROOF_RECEIPTS,
    );
  const ready =
    base.status >= 200 &&
    base.status < 300 &&
    body?.ready === true &&
    proofArchiveConfigured;

  return json(
    ready
      ? 200
      : 503,
    {
      ...body,
      ok: ready,
      ready,
      checks: {
        ...(body?.checks ?? {}),
        receiptArchiveConfigured:
          proofArchiveConfigured,
      },
    },
  );
}

async function cachedBaseReadiness(
  request,
  env,
) {
  if (
    !env ||
    typeof env !==
      "object"
  ) {
    return snapshotResponse(
      await baseWorker.fetch(
        request,
        env,
      ),
    );
  }

  const now =
    Date.now();
  const cached =
    readyCache.get(
      env,
    );

  if (
    cached?.snapshot &&
    cached.expiresAt >
      now
  ) {
    return cached.snapshot;
  }
  if (cached?.inFlight) {
    return cached.inFlight;
  }

  const inFlight =
    (async () => {
      const response =
        await baseWorker.fetch(
          request,
          env,
        );
      return snapshotResponse(
        response,
      );
    })();

  readyCache.set(
    env,
    {
      inFlight,
      snapshot: null,
      expiresAt: 0,
    },
  );

  try {
    const snapshot =
      await inFlight;
    const healthy =
      snapshot.status >= 200 &&
      snapshot.status < 300;
    readyCache.set(
      env,
      {
        inFlight: null,
        snapshot,
        expiresAt:
          Date.now() +
          (
            healthy
              ? READY_OK_TTL_MS
              : READY_FAIL_TTL_MS
          ),
      },
    );
    return snapshot;
  } catch (error) {
    readyCache.delete(
      env,
    );
    throw error;
  }
}

async function snapshotResponse(
  response,
) {
  return {
    status:
      response.status,
    body:
      await response.text(),
  };
}

function receiptArchive(
  env,
  matchId,
) {
  if (
    !env?.LUDOPROOF_RECEIPTS
  ) {
    throw httpError(
      503,
      "MATCH_RECEIPT_ARCHIVE_NOT_CONFIGURED",
      "match receipt archive is not configured",
    );
  }

  const id =
    env.LUDOPROOF_RECEIPTS
      .idFromName(
        matchId,
      );
  return env.LUDOPROOF_RECEIPTS
    .get(id);
}

async function enforceReceiptRateLimit(
  env,
  request,
  matchId,
) {
  if (
    !env?.LUDOPROOF_API_GATE
  ) {
    throw httpError(
      503,
      "RATE_LIMITER_NOT_CONFIGURED",
      "API rate limiter is not configured",
    );
  }

  const clientIp =
    request.headers.get(
      "cf-connecting-ip",
    ) ??
    "unknown";
  const rateKey =
    await sha256Hex(
      "ludoproof:api-rate:v1:receipt:" +
        matchId +
        ":" +
        clientIp,
    );
  const id =
    env.LUDOPROOF_API_GATE
      .idFromName(
        rateKey,
      );
  const target =
    env.LUDOPROOF_API_GATE
      .get(id);
  const response =
    await target.fetch(
      new Request(
        "https://gate/check",
        {
          method: "POST",
          headers: {
            "content-type":
              "application/json",
          },
          body:
            JSON.stringify({
              limit:
                RECEIPT_RATE_LIMIT,
              periodMs:
                RATE_WINDOW_MS,
            }),
        },
      ),
    );

  let value;
  try {
    value =
      await response.json();
  } catch {
    throw httpError(
      503,
      "RATE_LIMITER_BAD_RESPONSE",
      "API rate limiter returned invalid data",
    );
  }

  if (
    response.status ===
      429 ||
    value?.allowed ===
      false
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
      ) ??
      "60";
    throw error;
  }

  if (
    !response.ok ||
    value?.allowed !==
      true
  ) {
    throw httpError(
      503,
      "RATE_LIMITER_UNAVAILABLE",
      "API rate limiter rejected the check",
    );
  }
}

function withSecurityHeaders(
  response,
) {
  const headers =
    new Headers(
      response.headers,
    );
  for (
    const [name, value] of
      securityHeaders()
  ) {
    headers.set(
      name,
      value,
    );
  }
  headers.set(
    "cache-control",
    "no-store",
  );

  return new Response(
    response.body,
    {
      status:
        response.status,
      statusText:
        response.statusText,
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

function json(
  status,
  body,
  extraHeaders = null,
) {
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
    for (
      const [name, value] of
        extraHeaders
    ) {
      headers.set(
        name,
        value,
      );
    }
  }

  return new Response(
    JSON.stringify(
      body,
    ),
    {
      status,
      headers,
    },
  );
}

function errorResponse(
  error,
) {
  const status =
    Number.isInteger(
      error?.status,
    ) &&
    error.status >= 400 &&
    error.status <= 599
      ? error.status
      : 500;
  const extraHeaders = [];

  if (error?.allow) {
    extraHeaders.push([
      "allow",
      error.allow,
    ]);
  }
  if (error?.retryAfter) {
    extraHeaders.push([
      "retry-after",
      String(
        error.retryAfter,
      ),
    ]);
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
