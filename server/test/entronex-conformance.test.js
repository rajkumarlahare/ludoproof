import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  resolveOutcomeV4,
  verifyProofV4,
} from "../src/vendor/entronex-v4/v4.js";

const corpus =
  JSON.parse(
    await readFile(
      new URL(
        "./fixtures/entronex-v4-candidate.json",
        import.meta.url,
      ),
      "utf8",
    ),
  );

test(
  "vendored verifier conforms to the pinned EntroNex v4 candidate vector",
  () => {
    const vector =
      corpus.vectors[0];
    const proof =
      resolveOutcomeV4(
        vector.input,
      );

    assert.equal(
      proof.algorithm,
      corpus.protocol,
    );
    assert.equal(
      proof.transcriptDigest,
      vector.expected
        .transcriptDigest,
    );
    assert.equal(
      proof.outcomeIndex,
      vector.expected.outcomeIndex,
    );
    assert.equal(
      proof.outcome,
      vector.expected.outcome,
    );
    assert.equal(
      proof.world.width,
      vector.expected[
        "world.width"
      ],
    );
    assert.equal(
      proof.world.height,
      vector.expected[
        "world.height"
      ],
    );
    assert.equal(
      proof.world.sampleTick,
      vector.expected[
        "world.sampleTick"
      ],
    );
    assert.equal(
      proof.world.layoutEpoch,
      vector.expected[
        "world.layoutEpoch"
      ],
    );
    assert.equal(
      proof.world.selectedProbe,
      vector.expected[
        "world.selectedProbe"
      ],
    );
    assert.equal(
      proof.world.sampleIndex,
      vector.expected[
        "world.sampleIndex"
      ],
    );
    assert.equal(
      proof.world.motionProfile,
      vector.expected[
        "world.motionProfile"
      ],
    );
    assert.equal(
      proof.world.witness.swapped,
      vector.expected[
        "world.witness.swapped"
      ],
    );
    assert.equal(
      proof.world.witness.sourceIndex,
      vector.expected[
        "world.witness.sourceIndex"
      ],
    );
    assert.equal(
      proof.world.witness.targetIndex,
      vector.expected[
        "world.witness.targetIndex"
      ],
    );
    assert.equal(
      proof.world.fieldDigest,
      vector.expected[
        "world.fieldDigest"
      ],
    );
    assert.equal(
      proof.world.worldDigest,
      vector.expected[
        "world.worldDigest"
      ],
    );
    assert.equal(
      proof.proofDigest,
      vector.expected.proofDigest,
    );
    assert.equal(
      verifyProofV4(proof).valid,
      true,
    );

    const tampered =
      structuredClone(proof);
    tampered.outcome =
      tampered.outcome === 1
        ? 2
        : 1;
    assert.equal(
      verifyProofV4(tampered).valid,
      false,
    );
  },
);
