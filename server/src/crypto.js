const HEX_32 = /^[0-9a-f]{64}$/i;

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

function base64Url(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary)
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/g, "");
}
