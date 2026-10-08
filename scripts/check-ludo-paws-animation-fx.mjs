import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (path, needles) => {
  const text = read(path);
  for (const needle of needles) {
    if (!text.includes(needle)) {
      throw new Error(`${path} is missing required production animation/FX marker: ${needle}`);
    }
  }
};
const forbidText = (path, needles) => {
  const text = read(path);
  for (const needle of needles) {
    if (text.includes(needle)) {
      throw new Error(`${path} contains retired production animation/FX marker: ${needle}`);
    }
  }
};
const requireAbsent = (path) => {
  if (fs.existsSync(path)) {
    throw new Error(`Retired drawable animation layer must stay removed: ${path}`);
  }
};

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt",
  [
    "LudoPaws3DSceneView",
    "LudoPawsGameFxOverlayView",
    "pawn3DScene.bind",
    "pawn3DScene.playReactions(reactions)",
    "board.setClassicPawnFallbackVisible(false)",
    "previous = previous",
  ],
);
forbidText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt",
  ["LudoPaws3DLegalHaloView", "pawn3DLegalHalo"],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DSceneView.kt",
  [
    "LudoPawsPawnAnimationPolicy",
    ".plans(",
    "LudoPawsPawnMotionKind.CAPTURE_RETURN",
    "LudoPaws3DCaptureReturnState",
    "captureReturns",
    "retainedCaptureReturns",
    "newCaptureReturns",
    "activeReactions",
    "fun playReactions(",
    "reaction.priority >= previous.priority",
    "LudoPaws3DReactionMotion.durationMillis",
    "settings.gameSpeed.moveStepMs",
  ],
);
forbidText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DSceneView.kt",
  ["captureHiddenUntilMillis"],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DReactionMotion.kt",
  [
    "data class LudoPaws3DReactionPose",
    "data class LudoPaws3DActiveReaction",
    "fun durationMillis(",
    "fun sample(",
    "LudoPaws3DSpecies.DOG",
    "LudoPaws3DSpecies.GOAT",
    "LudoPaws3DSpecies.DUCK",
    "LudoPaws3DSpecies.CAT",
    "AnimationCue.CAPTURE",
    "AnimationCue.CAPTURED",
    "AnimationCue.NERVOUS",
    "AnimationCue.VICTORY",
    "HOME never adds another full spin",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DSceneRenderer.kt",
  [
    "LudoPawsFxBoardGeometry",
    "LudoPathEncoding",
    ".positionAtVisualStep(",
    "SceneMotion.HOP",
    "SceneMotion.HOME",
    "reactionPose(",
    "state.activeReactions[pawn.key]",
    "state.captureReturns[pawn.key]",
    "captureReturnVisual(",
    "LudoPawsCaptureReturnPlacement.sample",
    "presentationScale",
    "attentionScale",
    "legalAttentionScale",
    "FORWARD_LANDING_SETTLE_MILLIS",
    "LudoPaws3DReactionMotion.sample",
    "earBounceDegrees = base.earBounceDegrees + reaction.primaryAppendageDegrees",
    "beardSwingDegrees = base.beardSwingDegrees + reaction.secondaryAppendageDegrees",
    "wingFlapDegrees = base.wingFlapDegrees + reaction.primaryAppendageDegrees",
    "tailSwayDegrees = base.tailSwayDegrees + reaction.secondaryAppendageDegrees",
    "Dog3DMotionTimeline",
    "Goat3DMotionTimeline",
    "Duck3DMotionTimeline",
    "Cat3DMotionTimeline",
  ],
);
forbidText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DSceneRenderer.kt",
  ["captureHiddenUntilMillis"],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsCaptureReturnPlacement.kt",
  [
    "object LudoPawsCaptureReturnPlacement",
    "captureReturnFrame(progress)",
    "CURVE_CELLS",
    "frame.shakeXCells",
    "frame.liftCells",
    "scale = frame.scale",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/LudoPawsCaptureReturnPlacementTest.kt",
  [
    "capturedPawnStartsAtImpactCell",
    "capturedPawnUsesCurvedVisibleReturn",
    "capturedPawnEndsExactlyAtYard",
    "impactAndPopStayNearCaptureCellBeforeTravel",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/LudoPaws3DReactionMotionTest.kt",
  [
    "everySpeciesAndCueHasSafeFiniteMotion",
    "homeAddsBodyLanguageButNeverAddsSecondFullSpin",
    "victoryKeepsIndependentCelebrationRotation",
    "speciesIdentityIsVisibleInReactionAnatomy",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsPawnAnimationPolicy.kt",
  [
    "CAPTURE_RETURN",
    "IMPACT_SHAKE",
    "RETURN_TO_YARD",
    "SETTLE",
    "captureReturnFrame",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/LudoPawsPawnAnimationPolicyTest.kt",
  [
    "captureSnapshotProducesMoverAndVictimAnimationsTogether",
    "captureReturnUsesImpactPopTravelAndSettleStages",
    "captureReturnEndsExactlyAtStableYardPose",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/DiceRollAnimationPolicy.kt",
  [
    "SETTLE_DURATION_MILLIS",
    "rollingFrame(",
    "settleFrame(",
    "dice outcome must be 1..6",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/DiceView.kt",
  [
    "DiceRollAnimationPolicy",
    "DiceAttentionAnimationPolicy",
    ".rollingFrame(",
    ".settleFrame(",
    "setAttentionEnabled(",
    "attentionPulsing",
    "fun showOutcome(",
    "animate: Boolean = true",
    "postOnAnimation(animationTicker)",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/DiceRollAnimationPolicyTest.kt",
  [
    "rollingFrameCyclesFacesWithoutClaimingOutcome",
    "rollingFrameHasPhysicalTransform",
    "settleKeepsVerifiedOutcomeAndEndsAtIdentity",
    "attentionPulseBreathesWithoutChangingTheFace",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsGameFxOverlayView.kt",
  [
    "isCaptureReturn",
    "drawCaptureReturn",
    "drawPawTrail",
    "drawHomeStars",
    "LudoPawsFxPolicy",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsFxPainter.kt",
  [
    "drawSafeShield",
    "drawConfetti",
    "drawAngryBolts",
    "drawNervousOrbit",
    "drawSadDrops",
    "drawPawMark",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsFxBoardGeometry.kt",
  [
    "tokenCenter",
    "yardReactionAnchor",
    "HOME_LANES",
    "START_OFFSETS",
  ],
);

requireText(
  "android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsFxPolicy.kt",
  [
    "allowTranslation",
    "allowConfetti",
  ],
);

requireText(
  "android/app/src/test/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsFxPolicyTest.kt",
  [
    "victory enables dense confetti in full motion",
    "captured reaction supports full return movement",
    "every animation cue has a positive production duration",
  ],
);


for (const retired of [
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsCaptureReturnOverlayView.kt",
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsCharacterReactionOverlayView.kt",
]) {
  requireAbsent(retired);
}

console.log(
  "Ludo Paws production animation/FX gate passed: species-specific 3D body language, shared hop/home motion, visible captured-animal return, physical dice settle, circle-free pawn presentation and full-motion game FX are locked.",
);
