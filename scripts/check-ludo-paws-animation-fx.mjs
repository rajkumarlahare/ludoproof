import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (path, needles) => {
  const text = read(path);
  for (const needle of needles) {
    if (!text.includes(needle)) {
      throw new Error(`${path} is missing required Phase 10 marker: ${needle}`);
    }
  }
};

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt",
  [
    "LudoPawsGameFxOverlayView",
    "LudoPawsCharacterReactionOverlayView",
    "reducedMotionEnabled",
    "previous = previous",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsGameFxOverlayView.kt",
  [
    "isCaptureReturn",
    "drawCaptureReturn",
    "drawPawTrail",
    "drawSafeShield",
    "drawHomeStars",
    "drawConfetti",
    "drawAngryBolts",
    "drawSadDrops",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsCharacterReactionOverlayView.kt",
  [
    "drawCharacterReaction",
    "drawEmotionBadge",
    "AnimationCue.NERVOUS",
    "AnimationCue.IDLE",
    "LudoPawsCharacterCatalog",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsFxPolicy.kt",
  [
    "reducedMotion",
    "allowTranslation",
    "allowConfetti",
    "REDUCED_DURATION_MS",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsFxPolicyTest.kt",
  [
    "reduced motion removes translation",
    "victory enables dense confetti",
    "captured reaction supports return movement",
  ],
);

console.log("Ludo Paws Phase 10 animation/FX gate passed.");
