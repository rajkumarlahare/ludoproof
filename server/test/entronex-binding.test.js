import test from "node:test";
import assert from "node:assert/strict";

import {
  buildEntroNexConfig,
  expectedDigests,
  validateResolvedProof,
  validateRoundCommitment,
} from "../src/match-room.js";

const ALGORITHM =
  "entronex-v4-dual-commit-hkdf-sha256-context-bound";

function pending() {
  return {
    eventIndex: 7,
    eventId: "roll:7",
    roundId: "11111111-1111-4111-8111-111111111111",
    serverCommitment: "a".repeat(64),
    clientCommitment: "b".repeat(64),
    actorHash: "c".repeat(64),
    previousStateHash: "d".repeat(64),
    rulesetHash: "e".repeat(64),
  };
}

async function validFixture() {
  const value = pending();
  const config = buildEntroNexConfig("LPABCDEFGH", value);
  const digests = await expectedDigests(config);
  const commitment = {
    protocol: "v4",
    roundId: value.roundId,
    serverCommitment: value.serverCommitment,
    clientCommitment: value.clientCommitment,
    config,
    ...digests,
  };
  const proof = {
    algorithm: ALGORITHM,
    roundId: value.roundId,
    serverCommitment: value.serverCommitment,
    serverSeed: "f".repeat(64),
    clientCommitment: value.clientCommitment,
    clientSeed: "1".repeat(64),
    config,
    ...digests,
    transcriptDigest: "2".repeat(64),
    outcomeIndex: 5,
    outcome: 6,
    world: {},
    proofDigest: "3".repeat(64),
  };
  return { value, config, digests, commitment, proof };
}

test("commitment must echo the exact locked EntroNex config", async () => {
  const { value, config, digests, commitment } =
    await validFixture();

  assert.doesNotThrow(() =>
    validateRoundCommitment(
      commitment,
      value,
      digests,
      config,
    ),
  );

  const changed = structuredClone(commitment);
  changed.config.context.sessionId = "LPZZZZZZZZ";
  assert.throws(
    () =>
      validateRoundCommitment(
        changed,
        value,
        digests,
        config,
      ),
    (error) =>
      error.code === "ENTRONEX_COMMITMENT_INVALID",
  );
});

test("resolved proof is bound to exact match and event context", async () => {
  const { value, proof } = await validFixture();

  await assert.doesNotReject(() =>
    validateResolvedProof(
      proof,
      value,
      "LPABCDEFGH",
    ),
  );

  const changed = structuredClone(proof);
  changed.config.context.sessionId = "LPZZZZZZZZ";
  await assert.rejects(
    () =>
      validateResolvedProof(
        changed,
        value,
        "LPABCDEFGH",
      ),
    (error) =>
      error.code === "ENTRONEX_PROOF_CONTEXT_MISMATCH",
  );
});

test("resolved proof rejects algorithm, commitment, or digest substitution", async () => {
  const { value, proof } = await validFixture();

  for (const mutate of [
    (copy) => {
      copy.algorithm = "entronex-v4-wrong";
    },
    (copy) => {
      copy.serverCommitment = "9".repeat(64);
    },
    (copy) => {
      copy.contextDigest = "8".repeat(64);
    },
    (copy) => {
      copy.config.context.eventType = "OTHER_EVENT";
    },
  ]) {
    const changed = structuredClone(proof);
    mutate(changed);
    await assert.rejects(
      () =>
        validateResolvedProof(
          changed,
          value,
          "LPABCDEFGH",
        ),
      (error) =>
        error.code === "ENTRONEX_PROOF_CONTEXT_MISMATCH",
    );
  }
});
