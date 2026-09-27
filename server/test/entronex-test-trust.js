import {
  createHash,
  generateKeyPairSync,
  sign as cryptoSign,
} from "node:crypto";

import { canonicalJson } from "../src/vendor/entronex-v4/canonical.js";

export const TEST_KEY_ID =
  "cf-v4-eval-sign-1";
export const TEST_TENANT_ID =
  "cloudflare_v4_eval";

const { privateKey, publicKey } =
  generateKeyPairSync("ed25519");

const PUBLIC_PEM =
  publicKey
    .export({
      type: "spki",
      format: "pem",
    })
    .toString();

export const TEST_FINGERPRINT =
  createHash("sha256")
    .update(
      publicKey.export({
        type: "spki",
        format: "der",
      }),
    )
    .digest("hex");

const TEST_TENANT_HASH =
  createHash("sha256")
    .update(
      "entronex:tenant:v1:" +
        TEST_TENANT_ID,
      "utf8",
    )
    .digest("hex");

export const TEST_TRUST_ENV =
  Object.freeze({
    ENTRONEX_SIGNING_KEY_ID:
      TEST_KEY_ID,
    ENTRONEX_TENANT_ID:
      TEST_TENANT_ID,
    ENTRONEX_SIGNING_KEY_FINGERPRINT:
      TEST_FINGERPRINT,
    ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64:
      Buffer.from(
        PUBLIC_PEM,
        "utf8",
      ).toString("base64"),
  });

export function attachCommitmentEvidence(
  round,
  sequence,
) {
  const audit =
    auditFor(
      sequence,
      round.roundId,
    );
  const attestationPayload = {
    type:
      "entronex-round-commitment-attestation-v1",
    tenantHash:
      TEST_TENANT_HASH,
    protocol:
      round.protocol,
    roundId:
      round.roundId,
    serverCommitment:
      round.serverCommitment,
    clientCommitment:
      round.clientCommitment,
    configDigest:
      round.configDigest,
    auditEntryHash:
      audit.entryHash,
    auditSequence:
      audit.sequence,
    issuedAtMs:
      audit.createdAt,
  };
  return {
    ...round,
    audit,
    attestationPayload,
    attestation:
      signPayload(
        attestationPayload,
      ),
  };
}

export function attachProofEvidence(
  proof,
  sequence,
) {
  const audit =
    auditFor(
      sequence,
      proof.roundId,
    );
  const attestationPayload = {
    type:
      "entronex-proof-attestation-v1",
    tenantHash:
      TEST_TENANT_HASH,
    roundId:
      proof.roundId,
    algorithm:
      proof.algorithm,
    proofDigest:
      proof.proofDigest,
    auditEntryHash:
      audit.entryHash,
    auditSequence:
      audit.sequence,
    issuedAtMs:
      audit.createdAt,
  };
  return {
    ...proof,
    audit,
    attestationPayload,
    attestation:
      signPayload(
        attestationPayload,
      ),
    attestationSignedAt:
      audit.createdAt,
  };
}

function auditFor(
  sequence,
  roundId,
) {
  const createdAt =
    1_700_000_000_000 +
    sequence;
  return {
    sequence,
    previousHash:
      "0".repeat(64),
    entryHash:
      createHash("sha256")
        .update(
          "test-audit:" +
            sequence +
            ":" +
            roundId,
          "utf8",
        )
        .digest("hex"),
    createdAt,
  };
}

function signPayload(payload) {
  return {
    algorithm:
      "Ed25519",
    keyId:
      TEST_KEY_ID,
    publicKeyFingerprint:
      TEST_FINGERPRINT,
    signature:
      cryptoSign(
        null,
        Buffer.from(
          canonicalJson(payload),
          "utf8",
        ),
        privateKey,
      ).toString("base64url"),
  };
}
