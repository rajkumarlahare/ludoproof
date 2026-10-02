import { createHash } from "node:crypto";
import { canonicalJson } from "./crypto.js";

export const FAIRNESS_PROTOCOL =
  "ludoproof-roll-chain-v1";

const FAIRNESS_DOMAIN =
  "ludoproof:fairness-receipt:v1:";

export function sealFairnessEvent(
  history,
  event,
) {
  const previousFairnessDigest =
    latestFairnessDigest(history);
  const core =
    fairnessCore(
      event,
      previousFairnessDigest,
    );
  const fairnessDigest =
    fairnessDigestFor(core);

  return {
    ...event,
    fairnessProtocol:
      FAIRNESS_PROTOCOL,
    previousFairnessDigest,
    fairnessDigest,
  };
}

export function fairnessSummary(history) {
  const events =
    Array.isArray(history)
      ? history
      : [];
  const sealed =
    events.filter(
      (event) =>
        typeof event?.fairnessDigest ===
          "string",
    );
  const legacyEvents =
    events.length -
    sealed.length;

  if (sealed.length === 0) {
    return {
      protocol:
        FAIRNESS_PROTOCOL,
      valid: true,
      headDigest: null,
      sealedEvents: 0,
      legacyEvents,
    };
  }

  let previous =
    sealed[0]
      .previousFairnessDigest ??
    null;

  for (const event of sealed) {
    if (
      event.fairnessProtocol !==
        FAIRNESS_PROTOCOL ||
      (
        event.previousFairnessDigest ??
        null
      ) !== previous
    ) {
      return {
        protocol:
          FAIRNESS_PROTOCOL,
        valid: false,
        headDigest: previous,
        sealedEvents:
          sealed.length,
        legacyEvents,
      };
    }

    const expected =
      fairnessDigestFor(
        fairnessCore(
          event,
          previous,
        ),
      );
    if (
      expected.toLowerCase() !==
        String(
          event.fairnessDigest,
        ).toLowerCase()
    ) {
      return {
        protocol:
          FAIRNESS_PROTOCOL,
        valid: false,
        headDigest: previous,
        sealedEvents:
          sealed.length,
        legacyEvents,
      };
    }

    previous =
      event.fairnessDigest;
  }

  return {
    protocol:
      FAIRNESS_PROTOCOL,
    valid: true,
    headDigest: previous,
    sealedEvents:
      sealed.length,
    legacyEvents,
  };
}

export function fairnessDigestFor(core) {
  return createHash("sha256")
    .update(
      FAIRNESS_DOMAIN +
        canonicalJson(core),
      "utf8",
    )
    .digest("hex");
}

function fairnessCore(
  event,
  previousFairnessDigest,
) {
  return {
    protocol:
      FAIRNESS_PROTOCOL,
    eventIndex:
      event.eventIndex,
    eventId:
      event.eventId ?? null,
    playerId:
      event.playerId,
    color:
      event.color ?? null,
    roundId:
      event.roundId ?? null,
    serverCommitment:
      event.serverCommitment ?? null,
    clientCommitment:
      event.clientCommitment ?? null,
    actorHash:
      event.actorHash ?? null,
    previousStateHash:
      event.previousStateHash ?? null,
    rulesetHash:
      event.rulesetHash ?? null,
    proofDigest:
      event.proofDigest ?? null,
    outcome:
      event.outcome ?? null,
    status:
      event.status ?? "RESOLVED",
    timeoutReason:
      event.timeoutReason ?? null,
    replacementRoundAllowed:
      event.replacementRoundAllowed ??
      null,
    previousFairnessDigest:
      previousFairnessDigest ?? null,
  };
}

function latestFairnessDigest(history) {
  if (!Array.isArray(history)) {
    return null;
  }
  for (
    let index =
      history.length - 1;
    index >= 0;
    index -= 1
  ) {
    const digest =
      history[index]
        ?.fairnessDigest;
    if (
      typeof digest === "string" &&
      /^[0-9a-f]{64}$/i.test(digest)
    ) {
      return digest.toLowerCase();
    }
  }
  return null;
}
