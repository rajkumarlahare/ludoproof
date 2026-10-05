import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const catalogPath = path.join(
  root,
  "android/app/src/main/assets/ludo_paws/catalog.json",
);

const fail = (message) => {
  throw new Error(`Ludo Paws character-domain gate: ${message}`);
};

const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
const idPattern = /^[a-z][a-z0-9_]{1,31}$/;
const validPersonalities = new Set([
  "CHEERFUL",
  "MISCHIEVOUS",
  "SHY",
  "GENTLE",
  "BOLD",
  "PLAYFUL",
  "CURIOUS",
  "CALM",
  "PROUD",
  "NERVOUS",
  "BRAVE",
  "SASSY",
]);

const expectedStarter = new Map([
  [
    "dog",
    {
      displayName: "Dog",
      species: "DOG",
      personality: "PLAYFUL",
      voiceSetId: "dog_default",
      animationSetId: "dog_default",
      fallbackDrawable: "lp_3d_pawn_placeholder",
    },
  ],
  [
    "goat",
    {
      displayName: "Goat",
      species: "GOAT",
      personality: "CURIOUS",
      voiceSetId: "goat_default",
      animationSetId: "goat_default",
      fallbackDrawable: "lp_3d_pawn_placeholder",
    },
  ],
  [
    "duck",
    {
      displayName: "Duck",
      species: "DUCK",
      personality: "CHEERFUL",
      voiceSetId: "duck_default",
      animationSetId: "duck_default",
      fallbackDrawable: "lp_3d_pawn_placeholder",
    },
  ],
  [
    "cat",
    {
      displayName: "Cat",
      species: "CAT",
      personality: "SASSY",
      voiceSetId: "cat_default",
      animationSetId: "cat_default",
      fallbackDrawable: "lp_3d_pawn_placeholder",
    },
  ],
]);

const characters = catalog.characters ?? [];
const packs = catalog.packs ?? [];
const voiceSets = catalog.voiceSets ?? [];
const animationSets = catalog.animationSets ?? [];

if (!Array.isArray(characters) || characters.length === 0) {
  fail("characters must be a non-empty array");
}
if (!Array.isArray(packs) || packs.length === 0) {
  fail("packs must be a non-empty array");
}
if (!Array.isArray(voiceSets) || voiceSets.length === 0) {
  fail("voiceSets must be a non-empty array");
}
if (!Array.isArray(animationSets) || animationSets.length === 0) {
  fail("animationSets must be a non-empty array");
}

const uniqueMap = (items, label) => {
  const result = new Map();
  for (const item of items) {
    if (!idPattern.test(item?.id ?? "")) {
      fail(`${label} has invalid id: ${item?.id}`);
    }
    if (result.has(item.id)) {
      fail(`${label} has duplicate id: ${item.id}`);
    }
    result.set(item.id, item);
  }
  return result;
};

const charactersById = uniqueMap(characters, "characters");
const packsById = uniqueMap(packs, "packs");
const voiceSetsById = uniqueMap(voiceSets, "voiceSets");
const animationSetsById = uniqueMap(animationSets, "animationSets");

if ([...packsById.values()].filter((pack) => pack.starter === true).length !== 1) {
  fail("exactly one starter pack is required");
}

const starter = packsById.get("starter_paws");
if (!starter || starter.starter !== true) {
  fail("starter_paws must exist and be the starter pack");
}
if (!Array.isArray(starter.characterIds) || starter.characterIds.length !== 4) {
  fail("starter_paws must contain exactly four character ids");
}
if (new Set(starter.characterIds).size !== starter.characterIds.length) {
  fail("starter_paws cannot contain duplicate character ids");
}

const expectedStarterIds = [...expectedStarter.keys()];
if (JSON.stringify(starter.characterIds) !== JSON.stringify(expectedStarterIds)) {
  fail(`starter_paws order must be ${expectedStarterIds.join(", ")}`);
}

for (const [id, expected] of expectedStarter) {
  const character = charactersById.get(id);
  if (!character) fail(`missing starter character: ${id}`);

  for (const [key, expectedValue] of Object.entries(expected)) {
    if (character[key] !== expectedValue) {
      fail(`${id}.${key} must be ${expectedValue}`);
    }
  }

  if (!validPersonalities.has(character.personality)) {
    fail(`${id} has unsupported personality ${character.personality}`);
  }

  const voiceSet = voiceSetsById.get(character.voiceSetId);
  if (!voiceSet) {
    fail(`${id} references missing voice set ${character.voiceSetId}`);
  }
  if (voiceSet.species !== character.species) {
    fail(`${id} voice set species must match character species`);
  }

  if (!animationSetsById.has(character.animationSetId)) {
    fail(`${id} references missing animation set ${character.animationSetId}`);
  }
}

for (const pack of packsById.values()) {
  if (!Array.isArray(pack.characterIds) || pack.characterIds.length === 0) {
    fail(`${pack.id} must contain at least one character`);
  }
  if (new Set(pack.characterIds).size !== pack.characterIds.length) {
    fail(`${pack.id} has duplicate character ids`);
  }
  for (const characterId of pack.characterIds) {
    if (!charactersById.has(characterId)) {
      fail(`${pack.id} references missing character ${characterId}`);
    }
  }
}

const selectionStorePath = path.join(
  root,
  "android/app/src/main/java/com/ludoproof/game/feature/characters/data/local/CharacterSelectionStore.kt",
);
const selectionStore = fs.readFileSync(selectionStorePath, "utf8");
if (!selectionStore.includes('"ludo_paws_character_selection_v1"')) {
  fail("character selection must use the dedicated versioned persistence namespace");
}
if (!selectionStore.includes("LudoPawsCharacterCatalog") || !selectionStore.includes("defaultSelection")) {
  fail("character selection store must validate and safely fall back through the catalog");
}

console.log(
  "Ludo Paws character-domain gate passed: canonical 3D animal metadata, pack references, voice/animation links and selection persistence are valid.",
);
