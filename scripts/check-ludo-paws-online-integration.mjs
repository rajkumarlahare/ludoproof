import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (source, token, message) => {
  if (!source.includes(token)) throw new Error(message);
};

const characters = read("server/src/ludo-paws-characters.js");
const game = read("server/src/game.js");
const room = read("server/src/match-room.js");
const publicQueue = read("server/src/matchmaker-queue.js");
const teamQueue = read("server/src/team-matchmaker-queue.js");
const models = read(
  "android/app/src/main/java/com/ludoproof/game/game/domain/model/GameModels.kt",
);
const gameApi = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/data/remote/GameApi.kt",
);
const teamApi = read(
  "android/app/src/main/java/com/ludoproof/game/feature/team/data/remote/TeamMatchmakingApi.kt",
);
const runtime = read(
  "android/app/src/main/java/com/ludoproof/game/feature/characters/data/local/LudoPawsCharacterRuntime.kt",
);
const policy = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/domain/OnlineLudoPawsCharacterPolicy.kt",
);
const renderer = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/actions/OnlineResponseRenderer.kt",
);
const presentation = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/OnlineLudoPawsPresentation.kt",
);

for (const id of ["duck", "squirrel", "hedgehog", "sheep"]) {
  requireText(characters, `"${id}"`, `Server character allowlist is missing ${id}.`);
}
requireText(characters, "INVALID_CHARACTER_ID", "Invalid remote character IDs must be rejected.");
requireText(characters, "DEFAULT_CHARACTER_ID", "Legacy clients need a deterministic character fallback.");

requireText(game, "hostCharacterId", "Match creation must persist the host character.");
requireText(game, "characterId: publicCharacterId", "Public player state must expose character identity.");
const proofStart = game.indexOf("export function authoritativeStateForRandomness");
const proofEnd = game.indexOf("export function reserveRoll", proofStart);
if (proofStart < 0 || proofEnd <= proofStart) {
  throw new Error("Could not locate the authoritative proof-state boundary.");
}
const proofState = game.slice(proofStart, proofEnd);
if (proofState.includes("characterId")) {
  throw new Error("Character identity must never enter authoritative randomness state.");
}

requireText(room, "normalizeCharacterId", "MatchRoom must validate incoming character identity.");
requireText(room, "hostCharacterId", "MatchRoom must bind host character identity.");
requireText(publicQueue, "characterId", "Public matchmaking must preserve character identity across queue/status/replay.");
requireText(teamQueue, "characterId", "Team Up matchmaking must preserve character identity across queue/status/replay.");

requireText(runtime, "selectedCharacterId", "Android needs a process-safe selected character mirror.");
requireText(gameApi, ".put(\"characterId\", characterId)", "Online/Friends requests must send selected character identity.");
requireText(teamApi, ".put(\"characterId\", characterId)", "Team Up search must send selected character identity.");
requireText(models, "val characterId: String? = null", "Remote player snapshots must parse character identity.");
requireText(policy, "LudoPawsCharacterCatalog", "Remote character policy must validate IDs against the local catalog.");
requireText(policy, "DEFAULT_CHARACTER_ID", "Remote character policy must support legacy rooms.");
requireText(renderer, "OnlineLudoPawsPresentation.render", "Remote response rendering must drive Ludo Paws presentation.");
for (const marker of [
  "LudoPawsReactiveBoardView",
  "LudoPawsPlayerCardView",
  "characterIdsBySeat",
  "GameSoundFeedback",
  "LudoPawsHaptics",
  "consumedFeedbackKeys",
]) {
  requireText(presentation, marker, `Remote Ludo Paws presentation is missing ${marker}.`);
}

console.log(
  "Ludo Paws online integration gate passed: character identity sync, remote rendering, feedback dedupe and proof-state isolation are wired.",
);
