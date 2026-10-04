import test from "node:test";
import assert from "node:assert/strict";
import {
  LUDO_PAWS_CHARACTER_IDS,
  normalizeCharacterId,
  publicCharacterId,
} from "../src/ludo-paws-characters.js";
import {
  authoritativeStateForRandomness,
  newMatch,
  publicState,
} from "../src/game.js";

test("starter Ludo Paws character IDs are accepted and normalized", () => {
  assert.deepEqual(
    LUDO_PAWS_CHARACTER_IDS,
    ["duck", "squirrel", "hedgehog", "sheep"],
  );
  assert.equal(normalizeCharacterId(), "duck");
  assert.equal(normalizeCharacterId(null), "duck");
  assert.equal(normalizeCharacterId("   "), "duck");
  assert.equal(normalizeCharacterId("  SHEEP  "), "sheep");
});

test("unsupported remote character IDs are rejected at the request boundary", () => {
  assert.throws(
    () => normalizeCharacterId("dragon"),
    (error) =>
      error?.status === 400 &&
      error?.code === "INVALID_CHARACTER_ID",
  );
});

test("legacy persisted character values fall back without making rooms unreadable", () => {
  assert.equal(publicCharacterId(undefined), "duck");
  assert.equal(publicCharacterId("legacy_pet"), "duck");
  assert.equal(publicCharacterId("SQUIRREL"), "squirrel");
});

test("public player state exposes character identity but proof state ignores cosmetics", () => {
  const duck = newMatch({
    matchId: "LPPAWS1234",
    hostPlayerId: "p1",
    hostDisplayName: "Player",
    hostCharacterId: "duck",
    now: 1,
    targetPlayerCount: 2,
    matchMode: "ONLINE",
  });
  const sheep = structuredClone(duck);
  sheep.players[0].characterId = "sheep";

  assert.equal(publicState(duck).players[0].characterId, "duck");
  assert.equal(publicState(sheep).players[0].characterId, "sheep");
  assert.deepEqual(
    authoritativeStateForRandomness(duck),
    authoritativeStateForRandomness(sheep),
  );
  assert.equal(
    "characterId" in authoritativeStateForRandomness(duck).players[0],
    false,
  );
});
