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

const board = read(boardPath);
const reactiveBoard = read(reactiveBoardPath);
const scene = read(scenePath);
const renderer = read(rendererPath);
const layout = read(layoutPath);
const activity = read(activityPath);
const screen = read(screenPath);
const controls = read(controlsPath);
const characterState = read(characterStatePath);
const tests = read(testsPath);

for (const [token, message] of [
  ["class LudoPawsBoardView", "Production board shell is missing."],
  ["LudoBoardView(context)", "Production shell must keep the authoritative LudoBoardView underneath."],
  ["density(380f)", "Approved 380dp board-size lock must remain intact."],
]) {
  requireText(board, token, message);
}

for (const token of [
  "LudoPaws3DSceneView",
  "LudoPaws3DLegalHaloView",
  "LudoPawsGameFxOverlayView",
  "pawn3DScene.bind(",
  "pawn3DLegalHalo.bind(",
  "onOperationalChanged",
]) {
  requireText(
    reactiveBoard,
    token,
    `Production reactive board is missing 3D runtime marker: ${token}`,
  );
}

for (const token of [
  "TextureView",
  "EGL_OPENGL_ES3_BIT_KHR",
  "LudoPawsPawnAnimationPolicy",
  ".plans(",
  "LudoPawsPawnMotionKind.CAPTURE_RETURN",
  "captureHiddenUntilMillis",
  "settings.gameSpeed.moveStepMs",
  "reducedMotion",
]) {
  requireText(
    scene,
    token,
    `Shared 3D scene runtime is missing required marker: ${token}`,
  );
}

for (const token of [
  "LudoPawsFxBoardGeometry",
  "LudoPathEncoding",
  ".positionAtVisualStep(",
  "LudoPawsPawnLayout",
  ".tokenOffsetFraction(",
  ".radiusScale(",
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

requireText(layout, "radiusScale", "Production pawn sizing policy is missing.");
requireText(layout, "tokenOffsetFraction", "Deterministic shared-cell placement is missing.");

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
  "Ludo Paws pawn rendering gate passed: one shared 3D animal runtime, locked authoritative board, legal halo, movement sync, shared-cell sizing and legacy renderer cleanup are intact.",
);
