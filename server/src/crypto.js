const HEX_32 = /^[0-9a-f]{64}$/i;
const REQUEST_ID =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const MATCH_ALPHABET =
  "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

export async function sha256Hex(text) {
  const bytes = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("");
}

export function canonicalJson(value) {
  return serialize(value);
}

function serialize(value) {
  if (value === null) return "null";
  if (typeof value === "string") return JSON.stringify(value);
  if (typeof value === "boolean") return value ? "true" : "false";

  if (typeof value === "number") {
    if (!Number.isSafeInteger(value)) {
      throw new TypeError("canonical JSON accepts safe integers only");
    }
    return Object.is(value, -0) ? "0" : String(value);
  }

  if (Array.isArray(value)) {
    return "[" + value.map(serialize).join(",") + "]";
  }

  if (value && typeof value === "object") {
    const keys = Object.keys(value).sort();
    return (
      "{" +
      keys
        .map((key) => {
          const child = value[key];
          if (
            child === undefined ||
            typeof child === "function" ||
            typeof child === "symbol" ||
            typeof child === "bigint"
          ) {
            throw new TypeError("unsupported canonical JSON value");
          }
          return JSON.stringify(key) + ":" + serialize(child);
        })
        .join(",") +
      "}"
    );
  }

  throw new TypeError("unsupported canonical JSON value");
}

export async function deterministicMatchId(
  clientRequestId,
  attempt = 0,
) {
  const requestId =
    requireClientRequestId(
      clientRequestId,
    );
  if (
    !Number.isInteger(attempt) ||
    attempt < 0 ||
    attempt > 16
  ) {
    throw new RangeError(
      "match ID attempt is invalid",
    );
  }
  const bytes =
    await sha256Bytes(
      "ludoproof:match-id:v1:" +
        requestId +
        ":" +
        attempt,
    );
  let value = "LP";
  for (let index = 0; index < 8; index += 1) {
    value +=
      MATCH_ALPHABET[
        bytes[index] & 31
      ];
  }
  return value;
}

export async function derivePlayerIdentity(
  env,
  matchId,
  clientRequestId,
) {
  const requestId =
    requireClientRequestId(
      clientRequestId,
    );
  const sessionKey =
    requireSessionKey(env);
  const playerDigest =
    await sha256Hex(
      "ludoproof:player-id:v1:" +
        matchId +
        ":" +
        requestId,
    );
  const tokenBytes =
    await hmacSha256(
      sessionKey,
      "ludoproof:player-token:v2:" +
        matchId +
        ":" +
        requestId,
    );

  return {
    playerId:
      "p_" +
      playerDigest.slice(0, 32),
    playerToken:
      "lp_" +
      base64Url(tokenBytes),
  };
}

export function requireClientRequestId(
  value,
) {
  if (
    typeof value !== "string" ||
    !REQUEST_ID.test(value)
  ) {
    throw httpError(
      400,
      "INVALID_CLIENT_REQUEST_ID",
      "clientRequestId must be a UUID v4",
    );
  }
  return value.toLowerCase();
}

export function hasSessionKey(env) {
  return (
    typeof env?.LUDOPROOF_SESSION_HMAC_KEY ===
      "string" &&
    env.LUDOPROOF_SESSION_HMAC_KEY.length >=
      32 &&
    env.LUDOPROOF_SESSION_HMAC_KEY.length <=
      512
  );
}

export function randomToken() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  const body = base64Url(bytes);
  return "lp_" + body;
}

export function randomMatchId() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = new Uint8Array(8);
  crypto.getRandomValues(bytes);
  let value = "LP";
  for (const byte of bytes) {
    value += alphabet[byte % alphabet.length];
  }
  return value;
}

export function requireDigest(value, name) {
  if (typeof value !== "string" || !HEX_32.test(value)) {
    throw httpError(400, "INVALID_" + name.toUpperCase(), name + " must be SHA-256 hex");
  }
  return value.toLowerCase();
}

export function normalizeDisplayName(value) {
  if (typeof value !== "string") {
    throw httpError(400, "INVALID_DISPLAY_NAME", "displayName is required");
  }
  const name = value.trim().replace(/\s+/g, " ");
  if (name.length < 2 || name.length > 24) {
    throw httpError(400, "INVALID_DISPLAY_NAME", "displayName must contain 2 to 24 characters");
  }
  return name;
}

export function bearerToken(request) {
  const header = request.headers.get("authorization") || "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) throw httpError(401, "AUTH_REQUIRED", "player bearer token is required");
  return match[1];
}

export function httpError(status, code, message) {
  const error = new Error(message);
  error.status = status;
  error.code = code;
  return error;
}

async function sha256Bytes(text) {
  const bytes =
    new TextEncoder().encode(text);
  const digest =
    await crypto.subtle.digest(
      "SHA-256",
      bytes,
    );
  return new Uint8Array(digest);
}

async function hmacSha256(
  secret,
  message,
) {
  const key =
    await crypto.subtle.importKey(
      "raw",
      new TextEncoder()
        .encode(secret),
      {
        name: "HMAC",
        hash: "SHA-256",
      },
      false,
      ["sign"],
    );
  const signature =
    await crypto.subtle.sign(
      "HMAC",
      key,
      new TextEncoder()
        .encode(message),
    );
  return new Uint8Array(
    signature,
  );
}

function requireSessionKey(env) {
  if (!hasSessionKey(env)) {
    throw httpError(
      503,
      "SESSION_KEY_NOT_CONFIGURED",
      "LudoProof session-token HMAC key is not configured",
    );
  }
  return env.LUDOPROOF_SESSION_HMAC_KEY;
}

function base64Url(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/g, "");
}
