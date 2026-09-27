import test from "node:test";
import assert from "node:assert/strict";

import {
  verifyCommitmentAttestation,
  verifyProofAttestation,
} from "../src/entronex-trust.js";
import {
  TEST_TRUST_ENV,
  attachCommitmentEvidence,
  attachProofEvidence,
} from "./entronex-test-trust.js";

function commitmentFixture() {
  return attachCommitmentEvidence(
    {
      protocol: "v4",
      roundId:
        "11111111-1111-4111-8111-111111111111",
      serverCommitment:
        "a".repeat(64),
      clientCommitment:
        "b".repeat(64),
      configDigest:
        "c".repeat(64),
    },
    1,
  );
}

function proofFixture() {
  return attachProofEvidence(
    {
      algorithm:
        "entronex-v4-dual-commit-hkdf-sha256-context-bound",
      roundId:
        "11111111-1111-4111-8111-111111111111",
      proofDigest:
        "d".repeat(64),
    },
    2,
  );
}

test(
  "pinned Ed25519 commitment and proof attestations verify",
  () => {
    assert.doesNotThrow(() =>
      verifyCommitmentAttestation(
        commitmentFixture(),
        TEST_TRUST_ENV,
      ),
    );
    assert.doesNotThrow(() =>
      verifyProofAttestation(
        proofFixture(),
        TEST_TRUST_ENV,
      ),
    );
  },
);

test(
  "attestation verification rejects a mismatched pinned fingerprint",
  () => {
    assert.throws(
      () =>
        verifyCommitmentAttestation(
          commitmentFixture(),
          {
            ...TEST_TRUST_ENV,
            ENTRONEX_SIGNING_KEY_FINGERPRINT:
              "0".repeat(64),
          },
        ),
      (error) =>
        error.code ===
        "ENTRONEX_TRUST_FINGERPRINT_MISMATCH",
    );
  },
);

test(
  "commitment attestation rejects payload tampering",
  () => {
    const round =
      commitmentFixture();
    round.attestationPayload = {
      ...round.attestationPayload,
      serverCommitment:
        "9".repeat(64),
    };

    assert.throws(
      () =>
        verifyCommitmentAttestation(
          round,
          TEST_TRUST_ENV,
        ),
      (error) =>
        error.code ===
        "ENTRONEX_COMMITMENT_ATTESTATION_INVALID",
    );
  },
);

test(
  "proof attestation rejects proof digest tampering",
  () => {
    const proof =
      proofFixture();
    proof.proofDigest =
      "8".repeat(64);

    assert.throws(
      () =>
        verifyProofAttestation(
          proof,
          TEST_TRUST_ENV,
        ),
      (error) =>
        error.code ===
        "ENTRONEX_PROOF_ATTESTATION_INVALID",
    );
  },
);
