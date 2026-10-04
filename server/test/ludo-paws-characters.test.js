import test from "node:test";
import assert from "node:assert/strict";
import {
  CHARACTER_ID_SCHEMA_VERSION,
  CHARACTER_ID_WIRE_FIELD,
  LUDO_PAWS_CHARACTER_IDS,
  normalizeCharacterId,
  publicCharacterId,
} from "../src/ludo-paws-characters.js";
import {
  addPlayer,
  authoritativeStateForRandomness,
  newMatch,
  publicState,
} from "../src/game.js";

test("character identity wire contract stays backward compatible", () => {
  assert.equal(CHARACTER_ID_WIRE_FIELD, "characterId");
  assert.equal(CHARACTER_ID_SCHEMA_VERSION, 1);
});

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

test("legacy create and join calls that omit character identity still resolve to duck", () => {
  const created = newMatch({
    matchId: "LPPAWS1234",
    hostPlayerId: "p1",
    hostDisplayName: "Host",
    now: 1,
    targetPlayerCount: 2,
    matchMode: "ONLINE",
  });
  const joined = addPlayer(created, {
    playerId: "p2",
    displayName: "Guest",
    tokenAuthHash: "hash",
    now: 2,
  });

  assert.deepEqual(
    publicState(joined).players.map((player) => player.characterId),
    ["duck", "duck"],
  );
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
    CHARACTER_ID_WIRE_FIELD in authoritativeStateForRandomness(duck).players[0],
    false,
  );
});
