import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (source, token, message) => {
  if (!source.includes(token)) throw new Error(message);
};

const characters = read("server/src/ludo-paws-characters.js");
const gameCorePath = fs.existsSync("server/src/game-v1-core.js")
  ? "server/src/game-v1-core.js"
  : "server/src/game.js";
const game = read(gameCorePath);
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
const verificationRenderer = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/actions/OnlineVerificationRenderer.kt",
);
const verifiedDicePolicy = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/state/OnlineVerifiedDicePresentationPolicy.kt",
);
const verifiedDiceTests = read(
  "android/app/src/test/java/com/ludoproof/game/OnlineVerifiedDicePresentationPolicyTest.kt",
);
const rollActionPolicy = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/state/OnlineRollActionPolicy.kt",
);
const rollActionTests = read(
  "android/app/src/test/java/com/ludoproof/game/OnlineRollActionPolicyTest.kt",
);
const matchActions = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/actions/OnlineMatchActions.kt",
);
const presentation = read(
  "android/app/src/main/java/com/ludoproof/game/feature/online/presentation/OnlineLudoPawsPresentation.kt",
);

for (const id of ["dog", "goat", "duck", "cat"]) {
  requireText(characters, `"${id}"`, `Server canonical 3D character roster is missing ${id}.`);
}
for (const alias of [
  '["squirrel", "dog"]',
  '["hedgehog", "goat"]',
  '["sheep", "cat"]',
]) {
  requireText(
    characters,
    alias,
    `Server compatibility bridge is missing legacy alias ${alias}.`,
  );
}
requireText(characters, 'DEFAULT_CHARACTER_ID = "dog"', "Legacy clients need Dog as the deterministic canonical fallback.");
requireText(characters, "INVALID_CHARACTER_ID", "Invalid remote character IDs must be rejected.");

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
requireText(policy, "LudoPawsCharacterIdentityContract", "Remote character policy must use the centralized compatibility contract.");
requireText(renderer, "OnlineLudoPawsPresentation.render", "Remote response rendering must drive Ludo Paws presentation.");

for (const marker of [
  "pending.status != \"RESOLVED\"",
  "val latest = state.history.lastOrNull()",
  "pending.proofDigest",
  "latest.proofDigest",
  "eventKey = \"${state.matchId}:$identity\"",
]) {
  requireText(
    verifiedDicePolicy,
    marker,
    `Fail-closed online verified-dice policy is missing ${marker}.`,
  );
}
for (const marker of [
  "OnlineVerifiedDicePresentationPolicy.resolve(state)",
  "presentedDiceEventKey",
  "animate = shouldAnimate",
  "Waiting for the complete proof",
  "onlineRollActionDecision(state)",
]) {
  requireText(
    verificationRenderer,
    marker,
    `Remote verified-dice/recovery renderer is missing ${marker}.`,
  );
}
for (const marker of [
  "committedPendingNeverBorrowsPreviousVerifiedHistory",
  "resolvingPendingNeverBorrowsPreviousVerifiedHistory",
  "resolvedPendingWithoutProofFailsClosedInsteadOfUsingHistory",
  "eventKeyIsStableForRefreshAndChangesForNextEventEvenWithSameOutcome",
]) {
  requireText(
    verifiedDiceTests,
    marker,
    `Remote verified-dice regression coverage is missing ${marker}.`,
  );
}

for (const marker of [
  "NEW_ROLL",
  "RESUME_COMMIT_THEN_REVEAL",
  "RESUME_REVEAL_ONLY",
  "WAIT_FOR_RECOVERY",
  "MOVE_REQUIRED",
  "localClientCommitment == remoteClientCommitment",
]) {
  requireText(
    rollActionPolicy,
    marker,
    `Online roll recovery policy is missing ${marker}.`,
  );
}
for (const marker of [
  "creatingRoundWithMatchingSecretResumesCommitThenReveal",
  "committedRoundWithMatchingSecretResumesRevealOnly",
  "resolvingRoundWithMatchingSecretRetriesRevealOnly",
  "lockedRoundWithoutLocalSecretWaitsInsteadOfStartingAnotherRoll",
  "commitmentMismatchFailsClosed",
  "resolvedRoundRequiresMoveNotAnotherRoll",
]) {
  requireText(
    rollActionTests,
    marker,
    `Online roll recovery regression coverage is missing ${marker}.`,
  );
}
for (const marker of [
  "val decision = onlineRollActionDecision(state)",
  "OnlineRollActionKind.RESUME_COMMIT_THEN_REVEAL",
  "OnlineRollActionKind.RESUME_REVEAL_ONLY",
  "api.commitRoll",
  "api.revealRoll",
  "pendingRollStore.save",
]) {
  requireText(
    matchActions,
    marker,
    `Online roll action flow is missing ${marker}.`,
  );
}
for (const marker of [
  "pending == null || pending.status == \"RESOLVED\"",
  "remoteCommitment != null",
  "remoteCommitment != secret.clientCommitment",
]) {
  requireText(
    renderer,
    marker,
    `Pending reveal-secret reconciliation is missing ${marker}.`,
  );
}

for (const marker of [
  "LudoPawsReactiveBoardView",
  "LudoPawsPlayerCardView",
  "characterIdsBySeat",
  "GameSoundFeedback",
  "LudoPawsHaptics",
  "LudoPawsFeedbackLedger",
  "feedbackLedger.once",
]) {
  requireText(presentation, marker, `Remote Ludo Paws presentation is missing ${marker}.`);
}

console.log(
  "Ludo Paws online integration gate passed: canonical Dog/Goat/Duck/Cat identity, legacy alias compatibility, remote 3D presentation, exactly-once feedback, fail-closed verified-dice presentation, reconnect-safe commit/reveal recovery and proof-state isolation are wired.",
);
