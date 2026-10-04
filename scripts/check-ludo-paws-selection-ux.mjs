import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (relative) => {
  const absolute = path.join(root, relative);
  if (!fs.existsSync(absolute)) {
    throw new Error(`Ludo Paws selection UX gate: missing ${relative}`);
  }
  return fs.readFileSync(absolute, "utf8");
};

const requireText = (source, needle, label) => {
  if (!source.includes(needle)) {
    throw new Error(`Ludo Paws selection UX gate: ${label} is missing ${needle}`);
  }
};

const policyPath =
  "android/app/src/main/java/com/ludoproof/game/feature/characters/domain/selection/StarterPawsAssignmentPolicy.kt";
const storePath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/data/local/OfflineCharacterSetupStore.kt";
const uiPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/setup/OfflineCharacterSelectionUi.kt";
const activityPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/OfflineGameActivity.kt";
const screenPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/setup/OfflineSetupScreenUi.kt";
const selectionPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/setup/OfflineSetupSelectionUi.kt";
const testPath =
  "android/app/src/test/java/com/ludoproof/game/feature/characters/domain/selection/StarterPawsAssignmentPolicyTest.kt";

const policy = read(policyPath);
const store = read(storePath);
const ui = read(uiPath);
const activity = read(activityPath);
const screen = read(screenPath);
const selection = read(selectionPath);
const tests = read(testPath);

for (const required of [
  "playerCount in MIN_PLAYERS..MAX_PLAYERS",
  "computerMode",
  "ownerSlot",
  "current[ownerSlot] = previousCharacterId",
  "LudoPawsCharacterCatalog.STARTER_PACK_ID",
]) {
  requireText(policy, required, "assignment policy");
}

for (const required of [
  "ludo_paws_offline_character_setup_v1",
  "saveSetup(",
  "saveActive(",
  "loadActive()",
  "StarterPawsAssignmentPolicy.normalize",
  "mode.isLocal",
]) {
  requireText(store, required, "offline character persistence");
}

for (const required of [
  '"CHOOSE YOUR PAW"',
  "charactersForPack(",
  "fallbackDrawableName",
  "selectCharacterForActiveSlot(",
  "rebuildCharacterSlotButtons()",
  "characterSelectionStore.select(",
]) {
  requireText(ui, required, "character selection UI");
}

for (const required of [
  "CharacterSelectionStore(this)",
  "OfflineCharacterSetupStore(this)",
  "initializeCharacterSetup()",
  "selectedCharacterIds",
  "characterSlotsRow",
]) {
  requireText(activity, required, "offline activity integration");
}

requireText(screen, "characterPanel()", "offline setup screen");
requireText(screen, "persistActiveCharacterSetup()", "offline setup start gate");
requireText(selection, "updateSelectedPlayerCount(", "player count integration");
requireText(selection, "refreshCharacterSelectionUi()", "setup refresh integration");

for (const required of [
  "computer mode keeps human preference first and assigns unique cpus",
  "selecting an animal owned by another local player swaps slots",
  "invalid character request repairs to safe starter assignment",
  "only two to four local players are accepted",
]) {
  requireText(tests, required, "Phase 4 unit tests");
}

// Phase 4 must remain cosmetic. The local session still starts only with
// player count and preferred color; character assignment must not enter the
// authoritative OfflineGameEngine start contract or proof binding.
const session = read(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/domain/session/LocalMatchSession.kt",
);
if (/character(Id|Ids|Selection)/.test(session)) {
  throw new Error(
    "Ludo Paws selection UX gate: character metadata leaked into LocalMatchSession gameplay contract",
  );
}

console.log(
  "Ludo Paws selection UX gate passed: starter assignment, local persistence, setup UI and gameplay isolation are intact.",
);
