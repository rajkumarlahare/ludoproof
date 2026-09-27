import { canonicalJson, httpError, sha256Hex } from "./crypto.js";

export async function verifyEntroNexAttestation({
  env,
  payload,
  attestation,
  expected,
  keyring,
}) {
  if (
    !payload ||
    !attestation ||
    attestation.algorithm !== "Ed25519" ||
    typeof attestation.keyId !== "string" ||
    typeof attestation.publicKeyFingerprint !== "string" ||
    typeof attestation.signature !== "string"
  ) {
    throw httpError(
      502,
      "ENTRONEX_ATTESTATION_MISSING",
      "EntroNex attestation is missing or malformed",
    );
  }

  const pinnedFingerprint =
    String(
      env.ENTRONEX_SIGNING_KEY_FINGERPRINT ?? "",
    )
      .trim()
      .toLowerCase();
  if (
    !/^[0-9a-f]{64}$/.test(pinnedFingerprint)
  ) {
    throw httpError(
      503,
      "ENTRONEX_SIGNING_KEY_NOT_CONFIGURED",
      "EntroNex signing key fingerprint is not configured",
    );
  }

  if (
    attestation.publicKeyFingerprint.toLowerCase() !==
    pinnedFingerprint
  ) {
    throw httpError(
      502,
      "ENTRONEX_ATTESTATION_KEY_MISMATCH",
      "EntroNex attestation was signed by an unpinned key",
    );
  }

  const entry =
    keyring.keys?.find(
      (candidate) =>
        candidate?.algorithm === "Ed25519" &&
        candidate?.keyId ===
          attestation.keyId &&
        String(
          candidate?.fingerprint ?? "",
        ).toLowerCase() ===
          pinnedFingerprint &&
        candidate?.status !== "revoked",
    );
  if (!entry) {
    throw httpError(
      502,
      "ENTRONEX_SIGNING_KEY_NOT_FOUND",
      "Pinned EntroNex signing key is not present in the public keyring",
    );
  }

  const actualFingerprint =
    await fingerprintPublicKeyPem(
      entry.publicKeyPem,
    );
  if (
    actualFingerprint !== pinnedFingerprint
  ) {
    throw httpError(
      502,
      "ENTRONEX_PUBLIC_KEY_FINGERPRINT_MISMATCH",
      "EntroNex public key fingerprint does not match the pinned value",
    );
  }

  validatePayload(
    payload,
    expected,
  );

  const publicKey =
    await importEd25519PublicKey(
      entry.publicKeyPem,
    );
  const signature =
    decodeBase64Url(
      attestation.signature,
    );
  const valid =
    await crypto.subtle.verify(
      "Ed25519",
      publicKey,
      signature,
      new TextEncoder().encode(
        canonicalJson(payload),
      ),
    );

  if (!valid) {
    throw httpError(
      502,
      "ENTRONEX_ATTESTATION_INVALID",
      "EntroNex attestation signature verification failed",
    );
  }

  return true;
}

export function validatePayload(
  payload,
  expected,
) {
  if (
    payload.type !== expected.type ||
    payload.roundId !== expected.roundId ||
    (
      "protocol" in expected &&
      payload.protocol !==
        expected.protocol
    ) ||
    (
      "serverCommitment" in expected &&
      payload.serverCommitment !==
        expected.serverCommitment
    ) ||
    (
      "clientCommitment" in expected &&
      payload.clientCommitment !==
        expected.clientCommitment
    ) ||
    (
      "configDigest" in expected &&
      payload.configDigest !==
        expected.configDigest
    ) ||
    (
      "algorithm" in expected &&
      payload.algorithm !==
        expected.algorithm
    ) ||
    (
      "proofDigest" in expected &&
      payload.proofDigest !==
        expected.proofDigest
    )
  ) {
    throw httpError(
      502,
      "ENTRONEX_ATTESTATION_PAYLOAD_MISMATCH",
      "EntroNex attestation payload does not match the verified object",
    );
  }
}

export async function fingerprintPublicKeyPem(
  publicKeyPem,
) {
  const der =
    pemToDer(
      publicKeyPem,
    );
  return sha256HexBytes(der);
}

async function importEd25519PublicKey(
  publicKeyPem,
) {
  const der =
    pemToDer(
      publicKeyPem,
    );
  try {
    return await crypto.subtle.importKey(
      "spki",
      der,
      {
        name: "Ed25519",
      },
      false,
      ["verify"],
    );
  } catch {
    throw httpError(
      502,
      "ENTRONEX_PUBLIC_KEY_INVALID",
      "EntroNex public key could not be imported",
    );
  }
}

function pemToDer(publicKeyPem) {
  if (
    typeof publicKeyPem !== "string"
  ) {
    throw httpError(
      502,
      "ENTRONEX_PUBLIC_KEY_INVALID",
      "EntroNex public key is missing",
    );
  }

  const body =
    publicKeyPem
      .replace(
        "-----BEGIN PUBLIC KEY-----",
        "",
      )
      .replace(
        "-----END PUBLIC KEY-----",
        "",
      )
      .replace(/\s+/g, "");

  if (!body) {
    throw httpError(
      502,
      "ENTRONEX_PUBLIC_KEY_INVALID",
      "EntroNex public key is malformed",
    );
  }

  try {
    return decodeBase64(body);
  } catch {
    throw httpError(
      502,
      "ENTRONEX_PUBLIC_KEY_INVALID",
      "EntroNex public key is malformed",
    );
  }
}

function decodeBase64Url(value) {
  const normalized =
    value
      .replaceAll("-", "+")
      .replaceAll("_", "/");
  const padded =
    normalized +
    "=".repeat(
      (4 -
        (normalized.length % 4)) %
        4,
    );
  return decodeBase64(padded);
}

function decodeBase64(value) {
  const binary =
    atob(value);
  const bytes =
    new Uint8Array(
      binary.length,
    );
  for (
    let index = 0;
    index < binary.length;
    index += 1
  ) {
    bytes[index] =
      binary.charCodeAt(index);
  }
  return bytes;
}

async function sha256HexBytes(bytes) {
  const digest =
    await crypto.subtle.digest(
      "SHA-256",
      bytes,
    );
  return [
    ...new Uint8Array(digest),
  ]
    .map((value) =>
      value
        .toString(16)
        .padStart(2, "0"),
    )
    .join("");
}
