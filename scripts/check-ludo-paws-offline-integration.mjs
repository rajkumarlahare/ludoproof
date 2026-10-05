import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");

const requireText = (path, needles) => {
  const text = read(path);
  for (const needle of needles) {
    if (!text.includes(needle)) {
      throw new Error(`${path} is missing required Phase 11 marker: ${needle}`);
    }
  }
};

const forbidText = (path, needles) => {
  const text = read(path);
  for (const needle of needles) {
    if (text.includes(needle)) {
      throw new Error(`${path} contains obsolete Phase 11 pattern: ${needle}`);
    }
  }
};

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/feedback/OfflineLudoPawsFeedback.kt",
  [
    "OfflineFeedbackAction",
    "OfflineFeedbackSound",
    "LudoPawsReactionEngine.derive",
    "GameSoundFeedback.six",
    "GameSoundFeedback.homeLane",
    "GameSoundFeedback.thirdSix",
    "LudoPawsHaptics.reaction",
    "tokenMovementCommitted",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/actions/OfflineGameActions.kt",
  [
    "GameSoundFeedback.roll",
    "OfflineLudoPawsFeedbackDispatcher.committed",
    "OfflineFeedbackAction.ROLL",
    "OfflineFeedbackAction.MOVE",
    "previous = latest",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt",
  [
    "val previous =",
    "session.snapshot()",
    "OfflineLudoPawsFeedbackDispatcher.committed",
    "OfflineFeedbackAction.MOVE",
  ],
);

forbidText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/actions/OfflineGameActions.kt",
  ["GameSoundFeedback.move("],
);
forbidText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt",
  ["GameSoundFeedback.move("],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/state/OfflineCharacterPresentationState.kt",
  [
    "OfflineCharacterAssignmentPolicy",
    "resolveOfflineCharacterIds",
    "characterSelectionStore",
    "offlineCharacterSetupStore.saveActive",
    "activeCharacterMatchId",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayControlsUi.kt",
  [
    "resolveOfflineCharacterIds",
    "characterIdsBySeat =",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflinePlayerRailUi.kt",
  ["activeCharacterIdsBySeat"],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/OfflineGameActivity.kt",
  [
    "activeCharacterMatchId",
    "activeCharacterIdsBySeat",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/feature/offline/presentation/feedback/OfflineLudoPawsFeedbackPolicyTest.kt",
  [
    "normal committed move uses move sound",
    "capture replaces generic move sound",
    "victory wins over home feedback",
    "third six uses dedicated soft penalty sound and keeps haptic reaction",
    "home lane and exact home miss have distinct sounds",
    "silent sync does not invent feedback",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/feature/offline/presentation/state/OfflineCharacterAssignmentPolicyTest.kt",
  [
    "matching pass and play assignment is preserved",
    "mismatched saved presentation state is repaired deterministically",
    "computer resume keeps human preference and unique cpu characters",
  ],
);

console.log("Ludo Paws Phase 11 offline integration gate passed.");
