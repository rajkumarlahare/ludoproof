import teamWorker from "./index-team.js";
import { httpError, sha256Hex } from "./crypto.js";

const FRIEND_ID = /^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/;
const MAX_BODY_BYTES = 8 * 1024;
const RATE_WINDOW_MS = 60_000;

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (request.method === "GET" && url.pathname === "/ready") {
        const base = await teamWorker.fetch(request, env);
        let body;
        try {
          body = await base.clone().json();
        } catch {
          return base;
        }
        const friendConversationConfigured = Boolean(env?.LUDOPROOF_FRIEND_CONVERSATIONS);
        const friendRecentConfigured = Boolean(env?.LUDOPROOF_FRIEND_RECENT);
        const ready =
          base.ok &&
          body?.ready === true &&
          friendConversationConfigured &&
          friendRecentConfigured;
        return json(ready ? 200 : 503, {
          ...body,
          ok: ready,
          ready,
          checks: {
            ...(body?.checks ?? {}),
            friendConversationConfigured,
            friendRecentConfigured,
          },
        });
      }

      const socialAction =
        url.pathname === "/api/friends/messages/send"
          ? "messages/send"
          : url.pathname === "/api/friends/messages/list"
            ? "messages/list"
            : url.pathname === "/api/friends/recent"
              ? "recent"
              : null;

      if (!socialAction) return teamWorker.fetch(request, env);
      if (request.method !== "POST") {
        const error = httpError(405, "METHOD_NOT_ALLOWED", "method not allowed");
        error.allow = "POST";
        throw error;
      }

      const body = await readJson(request);
      await enforceRateLimit(
        env,
        request,
        socialAction,
        socialAction === "messages/send" ? 60 : 120,
      );
      const snapshot = await authorizedFriendSnapshot(env, request);
      const meId = normalizeFriendId(snapshot.friendId);

      if (socialAction === "recent") {
        const target = recentRoom(env, meId);
        const response = await target.fetch(
          new Request("https://recent/list", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ ownerFriendId: meId }),
          }),
        );
        return withSecurityHeaders(response);
      }

      const otherId = normalizeFriendId(body?.friendId);
      assertFriendship(snapshot, otherId);
      const target = conversation(env, meId, otherId);
      const upstreamBody =
        socialAction === "messages/send"
          ? {
              senderId: meId,
              receiverId: otherId,
              text: body?.text,
              clientRequestId: body?.clientRequestId,
            }
          : {
              meId,
              otherId,
            };
      const response = await target.fetch(
        new Request(
          socialAction === "messages/send"
            ? "https://conversation/send"
            : "https://conversation/list",
          {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(upstreamBody),
          },
        ),
      );
      return withSecurityHeaders(response);
    } catch (error) {
      return errorResponse(error);
    }
  },
};

async function authorizedFriendSnapshot(env, request) {
  if (!env?.LUDOPROOF_FRIENDS) {
    throw httpError(503, "FRIENDS_NOT_CONFIGURED", "friend directory is not configured");
  }
  const authorization = request.headers.get("authorization");
  if (!authorization) throw httpError(401, "FRIEND_AUTH_REQUIRED", "friend credential is required");
  const id = env.LUDOPROOF_FRIENDS.idFromName("GLOBAL:V1");
  const target = env.LUDOPROOF_FRIENDS.get(id);
  const response = await target.fetch(
    new Request("https://friends/snapshot", {
      method: "GET",
      headers: { authorization },
    }),
  );
  let body;
  try {
    body = await response.json();
  } catch {
    throw httpError(503, "FRIENDS_BAD_RESPONSE", "friend directory returned invalid data");
  }
  if (!response.ok) {
    throw httpError(
      response.status,
      body?.error ?? "FRIEND_AUTH_INVALID",
      body?.message ?? "friend credential is invalid",
    );
  }
  return body;
}

function assertFriendship(snapshot, otherId) {
  const friends = Array.isArray(snapshot?.friends) ? snapshot.friends : [];
  if (!friends.some((friend) => String(friend?.friendId ?? "").toUpperCase() === otherId)) {
    throw httpError(403, "MESSAGE_REQUIRES_FRIEND", "messages can only be exchanged with friends");
  }
}

function conversation(env, firstId, secondId) {
  if (!env?.LUDOPROOF_FRIEND_CONVERSATIONS) {
    throw httpError(503, "FRIEND_MESSAGES_NOT_CONFIGURED", "friend messaging is not configured");
  }
  const key = [firstId, secondId].sort().join(":");
  const id = env.LUDOPROOF_FRIEND_CONVERSATIONS.idFromName(key);
  return env.LUDOPROOF_FRIEND_CONVERSATIONS.get(id);
}

function recentRoom(env, friendId) {
  if (!env?.LUDOPROOF_FRIEND_RECENT) {
    throw httpError(503, "FRIEND_RECENT_NOT_CONFIGURED", "recent-player history is not configured");
  }
  const id = env.LUDOPROOF_FRIEND_RECENT.idFromName(friendId);
  return env.LUDOPROOF_FRIEND_RECENT.get(id);
}

function normalizeFriendId(value) {
  const normalized = String(value ?? "").trim().toUpperCase();
  if (!FRIEND_ID.test(normalized)) throw httpError(400, "INVALID_FRIEND_ID", "invalid Friend ID");
  return normalized;
}

async function enforceRateLimit(env, request, action, limit) {
  if (!env?.LUDOPROOF_API_GATE) {
    throw httpError(503, "RATE_LIMITER_NOT_CONFIGURED", "API rate limiter is not configured");
  }
  const ip = request.headers.get("cf-connecting-ip") ?? "unknown";
  const key = await sha256Hex("ludoproof:api-rate:v1:friends-social:" + action + ":" + ip);
  const id = env.LUDOPROOF_API_GATE.idFromName(key);
  const target = env.LUDOPROOF_API_GATE.get(id);
  let response;
  try {
    response = await target.fetch(
      new Request("https://gate/check", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ limit, periodMs: RATE_WINDOW_MS }),
      }),
    );
  } catch {
    throw httpError(503, "RATE_LIMITER_UNAVAILABLE", "API rate limiter is unavailable");
  }
  let result;
  try {
    result = await response.json();
  } catch {
    throw httpError(503, "RATE_LIMITER_BAD_RESPONSE", "API rate limiter returned invalid data");
  }
  if (response.status === 429 || result?.allowed === false) {
    const error = httpError(429, "RATE_LIMITED", "too many requests");
    error.retryAfter = response.headers.get("retry-after") ?? "60";
    throw error;
  }
  if (!response.ok || result?.allowed !== true) {
    throw httpError(503, "RATE_LIMITER_UNAVAILABLE", "API rate limiter rejected the check");
  }
}

async function readJson(request) {
  const type = request.headers.get("content-type") ?? "";
  if (!type.toLowerCase().startsWith("application/json")) {
    throw httpError(415, "UNSUPPORTED_MEDIA_TYPE", "content-type must be application/json");
  }
  const text = await request.text();
  if (new TextEncoder().encode(text).byteLength > MAX_BODY_BYTES) {
    throw httpError(413, "REQUEST_TOO_LARGE", "request body is too large");
  }
  try {
    const value = text.length === 0 ? {} : JSON.parse(text);
    if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("object required");
    return value;
  } catch {
    throw httpError(400, "INVALID_JSON", "request body must be a JSON object");
  }
}

function withSecurityHeaders(response) {
  const headers = new Headers(response.headers);
  for (const [name, value] of securityHeaders()) headers.set(name, value);
  headers.set("cache-control", "no-store");
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers,
  });
}

function securityHeaders() {
  return new Headers({
    "content-security-policy": "default-src 'none'; frame-ancestors 'none'; base-uri 'none'",
    "permissions-policy": "camera=(), microphone=(), geolocation=()",
    "referrer-policy": "no-referrer",
    "strict-transport-security": "max-age=31536000; includeSubDomains",
    "x-content-type-options": "nosniff",
    "x-frame-options": "DENY",
    "x-robots-tag": "noindex, nofollow",
  });
}

function errorResponse(error) {
  const status = Number.isInteger(error?.status) && error.status >= 400 && error.status <= 599 ? error.status : 500;
  const extra = new Headers();
  if (error?.allow) extra.set("allow", error.allow);
  if (error?.retryAfter) extra.set("retry-after", String(error.retryAfter));
  return json(
    status,
    {
      error: error?.code ?? "INTERNAL_ERROR",
      message: status === 500 && !error?.code ? "internal server error" : String(error?.message ?? "internal server error"),
    },
    extra,
  );
}

function json(status, body, extraHeaders = null) {
  const headers = securityHeaders();
  headers.set("content-type", "application/json; charset=utf-8");
  headers.set("cache-control", "no-store");
  if (extraHeaders) {
    for (const [name, value] of extraHeaders) headers.set(name, value);
  }
  return new Response(JSON.stringify(body), { status, headers });
}
