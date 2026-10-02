import test from "node:test";
import assert from "node:assert/strict";
import {
  FAIRNESS_PROTOCOL,
  fairnessSummary,
  sealFairnessEvent,
} from "../src/fairness.js";

function event(index, outcome) {
  return {
    eventIndex: index,
    eventId: "roll:" + index,
    playerId: "player-" + (index % 2),
    color:
      index % 2 === 0
        ? "RED"
        : "GREEN",
    roundId: "round-" + index,
    serverCommitment:
      "a".repeat(64),
    clientCommitment:
      "b".repeat(64),
    actorHash:
      "c".repeat(64),
    previousStateHash:
      "d".repeat(64),
    rulesetHash:
      "e".repeat(64),
    proofDigest:
      "f".repeat(64),
    outcome,
    status: "RESOLVED",
  };
}

test(
  "fairness receipts form a tamper-evident chain",
  () => {
    const history = [];
    history.push(
      sealFairnessEvent(
        history,
        event(0, 2),
      ),
    );
    history.push(
      sealFairnessEvent(
        history,
        event(1, 6),
      ),
    );
    history.push(
      sealFairnessEvent(
        history,
        event(2, 4),
      ),
    );

    const summary =
      fairnessSummary(history);
    assert.equal(
      summary.protocol,
      FAIRNESS_PROTOCOL,
    );
    assert.equal(
      summary.valid,
      true,
    );
    assert.equal(
      summary.sealedEvents,
      3,
    );
    assert.equal(
      summary.headDigest,
      history[2].fairnessDigest,
    );

    const tampered =
      structuredClone(history);
    tampered[1].outcome = 1;
    assert.equal(
      fairnessSummary(tampered)
        .valid,
      false,
    );
  },
);

test(
  "retained history window verifies from its previous digest anchor",
  () => {
    const history = [];
    for (
      let index = 0;
      index < 5;
      index += 1
    ) {
      history.push(
        sealFairnessEvent(
          history,
          event(
            index,
            (index % 6) + 1,
          ),
        ),
      );
    }

    const summary =
      fairnessSummary(
        history.slice(2),
      );
    assert.equal(
      summary.valid,
      true,
    );
    assert.equal(
      summary.sealedEvents,
      3,
    );
    assert.equal(
      summary.headDigest,
      history[4].fairnessDigest,
    );
  },
);
