import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (source, token, message) => {
  if (!source.includes(token)) throw new Error(message);
};

const serverContract = read("server/src/ludo-paws-characters.js");
const game = read("server/src/game.js");
const room = read("server/src/match-room.js");
const publicQueue = read("server/src/matchmaker-queue.js");
const teamQueue = read("server/src/team-matchmaker-queue.js");
const serverTests = read("server/test/ludo-paws-characters.test.js");
const androidContract = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/domain/LudoPawsCharacterIdentityContract.kt",
);
const androidPolicy = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/domain/OnlineLudoPawsCharacterPolicy.kt",
);
const gameApi = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/data/remote/GameApi.kt",
);
const models = read(
  "android/app/src/main/java/com/ludoproof/game/game/domain/model/GameModels.kt",
);

requireText(serverContract, 'CHARACTER_ID_WIRE_FIELD = "characterId"', "Server character wire field changed unexpectedly.");
requireText(serverContract, "CHARACTER_ID_SCHEMA_VERSION = 1", "Server character schema version must stay explicit.");
requireText(serverContract, "DEFAULT_CHARACTER_ID", "Server must retain deterministic legacy fallback.");
requireText(serverContract, "INVALID_CHARACTER_ID", "Unsupported new-client character IDs must be rejected.");

requireText(androidContract, 'WIRE_FIELD = "characterId"', "Android character wire field must match server.");
requireText(androidContract, "SCHEMA_VERSION = 1", "Android character schema version must match server.");
requireText(androidContract, "resolveRemote", "Android must have one fallback resolver for remote character IDs.");
requireText(androidPolicy, "LudoPawsCharacterIdentityContract", "Online rendering must consume the centralized character contract.");
requireText(models, "val characterId: String? = null", "Old server states must keep characterId optional on Android.");
requireText(gameApi, '.put("characterId", characterId)', "New Android create/join/search calls must send cosmetic identity.");

for (const source of [room, publicQueue, teamQueue]) {
  requireText(source, "characterId", "Every remote room/matchmaking path must preserve character identity.");
}

const proofStart = game.indexOf("export function authoritativeStateForRandomness");
const proofEnd = game.indexOf("export function reserveRoll", proofStart);
if (proofStart < 0 || proofEnd <= proofStart) {
  throw new Error("Could not locate authoritative randomness state.");
}
const proofState = game.slice(proofStart, proofEnd);
if (proofState.includes("characterId") || proofState.includes("characterSchema")) {
  throw new Error("Cosmetic character metadata must stay outside authoritative randomness/proof state.");
}

for (const marker of [
  "legacy create and join calls that omit character identity still resolve to duck",
  "legacy persisted character values fall back without making rooms unreadable",
  "proof state ignores cosmetics",
]) {
  requireText(serverTests, marker, `Phase 13 compatibility coverage is missing: ${marker}`);
}

console.log(
  "Ludo Paws Phase 13 character-schema gate passed: wire compatibility, legacy fallback, request validation and proof isolation are locked.",
);
