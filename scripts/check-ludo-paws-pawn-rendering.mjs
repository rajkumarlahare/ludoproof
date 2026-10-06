import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (source, token, message) => {
  if (!source.includes(token)) {
    throw new Error(message);
  }
};
const requireAbsent = (path, message) => {
  if (fs.existsSync(path)) {
    throw new Error(message);
  }
};

const boardPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsBoardView.kt";
const reactiveBoardPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt";
const scenePath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DSceneView.kt";
const rendererPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DSceneRenderer.kt";
const policyPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DCharacterPolicy.kt";
const cadencePolicyPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPaws3DRenderCadencePolicy.kt";
const layoutPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsPawnLayout.kt";
const activityPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/OfflineGameActivity.kt";
const screenPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt";
const controlsPath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayControlsUi.kt";
const characterStatePath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/state/OfflineCharacterPresentationState.kt";
const testsPath =
  "android/app/src/test/java/com/ludoproof/game/LudoPawsPawnLayoutTest.kt";
const policyTestsPath =
  "android/app/src/test/java/com/ludoproof/game/LudoPaws3DCharacterPolicyTest.kt";
const cadenceTestsPath =
  "android/app/src/test/java/com/ludoproof/game/LudoPaws3DRenderCadencePolicyTest.kt";

const board = read(boardPath);
const reactiveBoard = read(reactiveBoardPath);
const scene = read(scenePath);
const renderer = read(rendererPath);
const policy = read(policyPath);
const cadencePolicy = read(cadencePolicyPath);
const layout = read(layoutPath);
const activity = read(activityPath);
const screen = read(screenPath);
const controls = read(controlsPath);
const characterState = read(characterStatePath);
const tests = read(testsPath);
const policyTests = read(policyTestsPath);
const cadenceTests = read(cadenceTestsPath);

for (const [token, message] of [
  ["class LudoPawsBoardView", "Production board shell is missing."],
  ["LudoBoardView(context)", "Production shell must keep the authoritative LudoBoardView underneath."],
  ["density(380f)", "Approved 380dp board-size lock must remain intact."],
]) {
  requireText(board, token, message);
}

for (const token of [
  "LudoPaws3DSceneView",
  "LudoPawsGameFxOverlayView",
  "pawn3DScene.bind(",
  "characterIdsBySeat = characterIdsBySeat",
  "board.setClassicPawnFallbackVisible(false)",
  "onOperationalChanged",
  "windowVisibility == View.VISIBLE",
]) {
  requireText(
    reactiveBoard,
    token,
    `Production reactive board is missing 3D runtime marker: ${token}`,
  );
}

if (
  reactiveBoard.includes("LudoPaws3DLegalHaloView") ||
  reactiveBoard.includes("pawn3DLegalHalo")
) {
  throw new Error(
    "Production 3D gameplay must not render a follow-circle/legal-halo layer around animal pawns.",
  );
}

for (const token of [
  "TextureView",
  "EGL_OPENGL_ES3_BIT_KHR",
  "LudoPawsPawnAnimationPolicy",
  ".plans(",
  "LudoPawsPawnMotionKind.CAPTURE_RETURN",
  "LudoPaws3DCaptureReturnState",
  "captureReturns",
  "characterIdsBySeat",
  "bindRenderAssignments",
  "settings.gameSpeed.moveStepMs",
  "reducedMotion",
  "LudoPaws3DRenderCadencePolicy",
  ".frameDelayMillis(",
  "onVisibilityChanged",
  "onWindowVisibilityChanged",
  "setPresentationVisible",
  "awaitPresentationVisible",
  "ReentrantLock",
  "visibilityChanged.signalAll()",
]) {
  requireText(
    scene,
    token,
    `Shared 3D scene runtime is missing required marker: ${token}`,
  );
}

for (const token of [
  "ACTIVE_FRAME_DELAY_MILLIS",
  "IDLE_FRAME_DELAY_MILLIS",
  "HOME_CELEBRATION_TAIL_MILLIS",
  "hasActiveAnimation",
  "captureReturns",
  "activeReactions",
  "reducedMotion",
]) {
  requireText(
    cadencePolicy,
    token,
    `Adaptive 3D cadence policy is missing required marker: ${token}`,
  );
}

for (const token of [
  "speciesForCharacterId",
  "speciesForSeat",
  "LudoPawsCharacterCatalog",
  "bindRenderAssignments",
  "ThreadLocal",
  "RenderAssignments",
  "current.snapshot === snapshot",
  "current.characterIdsBySeat === characterIdsBySeat",
]) {
  requireText(
    policy,
    token,
    `3D character identity policy is missing selected-character/cache marker: ${token}`,
  );
}

for (const token of [
  "LudoPawsFxBoardGeometry",
  "LudoPathEncoding",
  ".positionAtVisualStep(",
  "LudoPawsPawnLayout",
  ".tokenOffsetFraction(",
  ".radiusScale(",
  "LudoPawsCaptureReturnPlacement",
  "captureReturnVisual(",
  "Dog3DMotionTimeline",
  "Goat3DMotionTimeline",
  "Duck3DMotionTimeline",
  "Cat3DMotionTimeline",
]) {
  requireText(
    renderer,
    token,
    `Shared 3D renderer is missing required production marker: ${token}`,
  );
}

for (const token of [
  "radiusScale",
  "tokenOffsetFraction",
  "StackPlacementCache",
  "WeakReference",
  "stackPlacementCache",
  "cached.snapshotRef.get() === snapshot",
]) {
  requireText(
    layout,
    token,
    `Production pawn layout/cache policy is missing required marker: ${token}`,
  );
}

const reactiveHost =
  activity.includes("LudoPawsReactiveBoardView?") &&
  screen.includes("LudoPawsReactiveBoardView(this)") &&
  reactiveBoard.includes("LudoPawsBoardView(context)");
if (!reactiveHost) {
  throw new Error(
    "Offline gameplay must host the shared production 3D runtime through LudoPawsReactiveBoardView.",
  );
}

const consumesPersistedCharacterAssignment =
  controls.includes(".loadActive()") ||
  (controls.includes("resolveOfflineCharacterIds") &&
    characterState.includes(".loadActive()"));
if (!consumesPersistedCharacterAssignment) {
  throw new Error(
    "Offline gameplay must consume the persisted active character assignment directly or through the repaired character resolver.",
  );
}
requireText(
  controls,
  "characterIdsBySeat",
  "Active character assignments must remain bound to the presentation layer by seat.",
);
requireText(
  tests,
  "shared cell reduces animal radius without collapsing it",
  "Shared-cell sizing coverage is missing.",
);
requireText(
  tests,
  "stable snapshot and cell reuse the same stack placement map",
  "Stable 3D stack-placement cache coverage is missing.",
);
requireText(
  tests,
  "stack placement cache invalidates for new snapshot or board cell size",
  "3D stack-placement cache invalidation coverage is missing.",
);
requireText(
  policyTests,
  "seat assignment wins over ludo color for production rendering",
  "Selected-character-over-color rendering coverage is missing.",
);
requireText(
  policyTests,
  "render thread color lookup follows installed seat assignments",
  "Render-thread selected-character binding coverage is missing.",
);
requireText(
  policyTests,
  "render binding refreshes when selected seat list changes",
  "3D character binding cache assignment invalidation coverage is missing.",
);
requireText(
  policyTests,
  "render binding refreshes for a new authoritative snapshot",
  "3D character binding cache snapshot invalidation coverage is missing.",
);
requireText(
  cadenceTests,
  "idle board uses lower cost cadence",
  "Idle 3D cadence regression coverage is missing.",
);
requireText(
  cadenceTests,
  "reduced motion never burns active frame cadence",
  "Reduced-motion 3D cadence regression coverage is missing.",
);

for (const retired of [
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsCaptureReturnOverlayView.kt",
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsCharacterReactionOverlayView.kt",
  "android/app/src/main/res/drawable/lp_starter_duck.xml",
  "android/app/src/main/res/drawable/lp_starter_squirrel.xml",
  "android/app/src/main/res/drawable/lp_starter_hedgehog.xml",
  "android/app/src/main/res/drawable/lp_starter_sheep.xml",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Dog3DPrototypeActivity.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Dog3DPrototypeView.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Dog3DRenderer.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Goat3DPrototypeActivity.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Goat3DPrototypeView.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Goat3DRenderer.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Duck3DPrototypeActivity.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Duck3DPrototypeView.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Duck3DRenderer.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Cat3DPrototypeActivity.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Cat3DPrototypeView.kt",
  "android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/Cat3DRenderer.kt",
]) {
  requireAbsent(
    retired,
    `Retired 2D/prototype renderer must stay removed: ${retired}`,
  );
}

for (const retainedMotion of ["Dog", "Goat", "Duck", "Cat"]) {
  const path =
    `android/app/src/main/java/com/ludoproof/game/feature/characters/prototype/${retainedMotion}3DMotion.kt`;
  if (!fs.existsSync(path)) {
    throw new Error(
      `Production renderer still consumes this pure motion timeline, so it must remain: ${path}`,
    );
  }
}

console.log(
  "Ludo Paws pawn rendering gate passed: selected seat animals drive one shared visibility-suspended, adaptive-cadence 3D runtime over the locked authoritative board, with cached stable bindings/stack geometry, movement sync, 3D capture return, shared-cell sizing, circle-free animal presentation and legacy renderer cleanup intact.",
);
