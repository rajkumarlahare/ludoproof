import {
  createHash,
  createPublicKey,
  verify as cryptoVerify,
} from "node:crypto";

import { canonicalJson } from "./vendor/entronex-v4/canonical.js";

const FINGERPRINT_RE = /^[0-9a-f]{64}$/i;
const KEY_ID_RE = /^[A-Za-z0-9._-]{3,100}$/;
const TENANT_ID_RE = /^[A-Za-z0-9._:-]{1,128}$/;

export function hasPinnedEntroNexTrust(env) {
  return Boolean(
    typeof env?.ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64 === "string" &&
      env.ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64.length >= 40 &&
      typeof env?.ENTRONEX_SIGNING_KEY_FINGERPRINT === "string" &&
      FINGERPRINT_RE.test(env.ENTRONEX_SIGNING_KEY_FINGERPRINT) &&
      typeof env?.ENTRONEX_SIGNING_KEY_ID === "string" &&
      KEY_ID_RE.test(env.ENTRONEX_SIGNING_KEY_ID) &&
      typeof env?.ENTRONEX_TENANT_ID === "string" &&
      TENANT_ID_RE.test(env.ENTRONEX_TENANT_ID)
  );
}

export function verifyCommitmentAttestation(round, env) {
  const key = pinnedKey(env);
  const payload = round?.attestationPayload;
  const attestation = round?.attestation;
  const audit = round?.audit;

  if (
    !payload ||
    payload.type !== "entronex-round-commitment-attestation-v1" ||
    payload.tenantHash !== tenantHash(env.ENTRONEX_TENANT_ID) ||
    payload.protocol !== round?.protocol ||
    payload.roundId !== round?.roundId ||
    payload.serverCommitment !== (round?.serverCommitment ?? round?.commitment ?? null) ||
    payload.clientCommitment !== (round?.clientCommitment ?? null) ||
    payload.configDigest !== (round?.configDigest ?? null) ||
    payload.auditEntryHash !== (audit?.entryHash ?? null) ||
    payload.auditSequence !== (audit?.sequence ?? null) ||
    payload.issuedAtMs !== (audit?.createdAt ?? null)
  ) {
    throw trustError(
      502,
      "ENTRONEX_COMMITMENT_ATTESTATION_INVALID",
      "EntroNex commitment attestation payload is not bound to the returned round",
    );
  }

  verifySignedPayload(payload, attestation, key, "commitment");
}

export function verifyProofAttestation(proof, env) {
  const key = pinnedKey(env);
  const payload = proof?.attestationPayload;
  const attestation = proof?.attestation;
  const audit = proof?.audit;

  if (
    !payload ||
    payload.type !== "entronex-proof-attestation-v1" ||
    payload.tenantHash !== tenantHash(env.ENTRONEX_TENANT_ID) ||
    payload.roundId !== proof?.roundId ||
    payload.algorithm !== proof?.algorithm ||
    payload.proofDigest !== proof?.proofDigest ||
    payload.auditEntryHash !== (audit?.entryHash ?? null) ||
    payload.auditSequence !== (audit?.sequence ?? null) ||
    payload.issuedAtMs !== (audit?.createdAt ?? null)
  ) {
    throw trustError(
      502,
      "ENTRONEX_PROOF_ATTESTATION_INVALID",
      "EntroNex proof attestation payload is not bound to the returned proof",
    );
  }

  verifySignedPayload(payload, attestation, key, "proof");
}

function pinnedKey(env) {
  if (!hasPinnedEntroNexTrust(env)) {
    throw trustError(
      503,
      "ENTRONEX_TRUST_NOT_CONFIGURED",
      "EntroNex signing public key, fingerprint, key ID, and tenant ID must be pinned",
    );
  }

  let pem;
  try {
    pem = Buffer.from(
      env.ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64,
      "base64",
    ).toString("utf8");
  } catch {
    throw trustError(
      503,
      "ENTRONEX_TRUST_NOT_CONFIGURED",
      "EntroNex signing public key is not valid base64",
    );
  }

  if (pem.length < 50 || pem.length > 8192) {
    throw trustError(
      503,
      "ENTRONEX_TRUST_NOT_CONFIGURED",
      "EntroNex signing public key is invalid",
    );
  }

  let publicKey;
  let fingerprint;
  try {
    publicKey = createPublicKey(pem);
    if (publicKey.asymmetricKeyType !== "ed25519") {
      throw new Error("wrong key type");
    }
    fingerprint = createHash("sha256")
      .update(
        publicKey.export({
          type: "spki",
          format: "der",
        }),
      )
      .digest("hex");
  } catch {
    throw trustError(
      503,
      "ENTRONEX_TRUST_NOT_CONFIGURED",
      "EntroNex pinned signing key must be Ed25519",
    );
  }

  if (
    fingerprint !==
    env.ENTRONEX_SIGNING_KEY_FINGERPRINT.toLowerCase()
  ) {
    throw trustError(
      503,
      "ENTRONEX_TRUST_FINGERPRINT_MISMATCH",
      "EntroNex signing public key fingerprint does not match the pinned fingerprint",
    );
  }

  return {
    publicKey,
    fingerprint,
    keyId: env.ENTRONEX_SIGNING_KEY_ID,
  };
}

function verifySignedPayload(payload, attestation, key, label) {
  if (
    !attestation ||
    attestation.algorithm !== "Ed25519" ||
    attestation.keyId !== key.keyId ||
    attestation.publicKeyFingerprint !== key.fingerprint ||
    typeof attestation.signature !== "string" ||
    attestation.signature.length < 40
  ) {
    throw trustError(
      502,
      "ENTRONEX_ATTESTATION_KEY_MISMATCH",
      "EntroNex " + label + " attestation does not match the pinned signing identity",
    );
  }

  let valid = false;
  try {
    valid = cryptoVerify(
      null,
      Buffer.from(
        canonicalJson(payload),
        "utf8",
      ),
      key.publicKey,
      Buffer.from(
        attestation.signature,
        "base64url",
      ),
    );
  } catch {
    valid = false;
  }

  if (!valid) {
    throw trustError(
      502,
      "ENTRONEX_ATTESTATION_SIGNATURE_INVALID",
      "EntroNex " + label + " attestation signature verification failed",
    );
  }
}

function tenantHash(tenantId) {
  return createHash("sha256")
    .update(
      "entronex:tenant:v1:" + tenantId,
      "utf8",
    )
    .digest("hex");
}

function trustError(status, code, message) {
  const error = new Error(message);
  error.status = status;
  error.code = code;
  return error;
}
