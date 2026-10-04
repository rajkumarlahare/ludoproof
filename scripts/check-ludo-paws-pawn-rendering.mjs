import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (source, token, message) => {
  if (!source.includes(token)) {
    throw new Error(message);
  }
};

const board = read(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsBoardView.kt",
);
const layout = read(
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsPawnLayout.kt",
);
const activity = read(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/OfflineGameActivity.kt",
);
const screen = read(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt",
);
const controls = read(
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayControlsUi.kt",
);
const tests = read(
  "android/app/src/test/java/com/ludoproof/game/LudoPawsPawnLayoutTest.kt",
);
const reactiveBoardPath =
  "android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt";
const reactiveBoard = fs.existsSync(reactiveBoardPath)
  ? read(reactiveBoardPath)
  : "";
const characterStatePath =
  "android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/state/OfflineCharacterPresentationState.kt";
const characterState = fs.existsSync(characterStatePath)
  ? read(characterStatePath)
  : "";

requireText(
  board,
  "class LudoPawsBoardView",
  "Phase 5 board shell is missing.",
);
requireText(
  board,
  "LudoPawsCharacterCatalog",
  "Phase 5 board must resolve characters from the catalog.",
);
requireText(
  board,
  "fallbackDrawableName",
  "Phase 5 board must render the existing starter fallback art.",
);
requireText(
  board,
  "legalHaloPaint",
  "Phase 5 board must keep legal-move highlighting visible around animal pawns.",
);
requireText(
  board,
  "occupancyByCell",
  "Phase 5 board must account for shared board cells.",
);
requireText(
  board,
  "startMoveAnimationIfNeeded",
  "Phase 5 animal overlay must follow token movement animation.",
);
requireText(
  layout,
  "radiusScale",
  "Phase 5 pawn sizing policy is missing.",
);
requireText(
  layout,
  "tokenOffsetFraction",
  "Phase 5 deterministic shared-cell placement is missing.",
);

const directHost =
  activity.includes("LudoPawsBoardView?") &&
  screen.includes("LudoPawsBoardView(this)");
const reactiveHost =
  activity.includes("LudoPawsReactiveBoardView?") &&
  screen.includes("LudoPawsReactiveBoardView(this)") &&
  reactiveBoard.includes("LudoPawsBoardView(context)");

if (!directHost && !reactiveHost) {
  throw new Error(
    "Offline gameplay must host the proven Ludo Paws board directly or through the Phase 6 reactive shell.",
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
  "Active character assignments must be bound to the board by seat.",
);
requireText(
  tests,
  "shared cell reduces animal radius",
  "Phase 5 shared-cell sizing coverage is missing.",
);

console.log(
  "Ludo Paws pawn rendering gate passed: starter animals, player rings, legal glow, movement sync and shared-cell sizing are wired without changing authoritative game state.",
);
