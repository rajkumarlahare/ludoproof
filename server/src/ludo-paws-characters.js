import { httpError } from "./crypto.js";

export const DEFAULT_CHARACTER_ID = "duck";

export const LUDO_PAWS_CHARACTER_IDS = Object.freeze([
  "duck",
  "squirrel",
  "hedgehog",
  "sheep",
]);

const CHARACTER_IDS = new Set(LUDO_PAWS_CHARACTER_IDS);

/**
 * Normalizes presentation-only Ludo Paws identity coming from an Android client.
 * Missing values intentionally fall back to duck so old installed clients and
 * already-created rooms remain compatible during the rollout.
 */
export function normalizeCharacterId(value) {
  if (value == null || String(value).trim() === "") {
    return DEFAULT_CHARACTER_ID;
  }

  const characterId = String(value).trim().toLowerCase();
  if (!CHARACTER_IDS.has(characterId)) {
    throw httpError(
      400,
      "INVALID_CHARACTER_ID",
      "selected Ludo Paws character is not supported",
    );
  }
  return characterId;
}

/**
 * Reads legacy persisted state safely without trusting arbitrary stored values.
 * Unlike request normalization this never throws, because old state must remain
 * readable across a deployment.
 */
export function publicCharacterId(value) {
  const characterId =
    typeof value === "string"
      ? value.trim().toLowerCase()
      : "";
  return CHARACTER_IDS.has(characterId)
    ? characterId
    : DEFAULT_CHARACTER_ID;
}
