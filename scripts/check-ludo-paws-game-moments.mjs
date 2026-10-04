import fs from 'node:fs';

const required = [
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsGameMomentDetector.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsReactionEngine.kt',
  'android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsPlayerCardView.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflinePlayerRailUi.kt',
  'android/app/src/test/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsGameMomentDetectorTest.kt',
];

for (const path of required) {
  if (!fs.existsSync(path)) {
    throw new Error(`Missing Phase 7 file: ${path}`);
  }
}

const detector = fs.readFileSync(required[0], 'utf8');
const reaction = fs.readFileSync(required[1], 'utf8');
const card = fs.readFileSync(required[2], 'utf8');
const rail = fs.readFileSync(required[3], 'utf8');
const tests = fs.readFileSync(required[4], 'utf8');

for (const marker of [
  'TURN_STARTED',
  'ROLL_STARTED',
  'SIX_ROLLED',
  'LOW_ROLL',
  'TOKEN_LEFT_YARD',
  'TOKEN_MOVED',
  'CAPTURE_MADE',
  'TOKEN_CAPTURED',
  'SAFE_REACHED',
  'HOME_REACHED',
  'NO_LEGAL_MOVE',
  'THIRD_SIX_FORFEIT',
  'PLAYER_LEADING',
  'IDLE_WAITING',
  'MATCH_WIN',
  'MATCH_LOSS',
  'TEAM_WIN',
  'TEAM_LOSS',
]) {
  if (!detector.includes(marker)) {
    throw new Error(`Phase 7 detector is missing ${marker}`);
  }
}

if (!reaction.includes('LudoPawsGameMomentDetector')) {
  throw new Error('Reaction engine must consume the shared Phase 7 moment detector.');
}

if (!card.includes('fallbackDrawableName') || !card.includes('HOME') || !card.includes('RACING')) {
  throw new Error('Character player card must render selected animal identity and token progress.');
}

const railConsumesCharacters =
  rail.includes('characterIds') ||
  rail.includes('activeCharacterIdsBySeat');
if (!rail.includes('LudoPawsPlayerCardView') || !railConsumesCharacters) {
  throw new Error('Offline player rails must use selected Ludo Paws character cards.');
}

if (!tests.includes('thirdSixIsNotMisclassifiedAsNoLegalMove') || !tests.includes('emitsTeamWinAndLossMoments')) {
  throw new Error('Phase 7 detector regression coverage is incomplete.');
}

for (const forbidden of ['OfflineGameEngine(', 'session.move(', 'session.roll(', 'EntroNexV4Local']) {
  if (detector.includes(forbidden)) {
    throw new Error(`Game moment detector must stay presentation-only: ${forbidden}`);
  }
}

console.log('Ludo Paws Phase 7 game-moment gate passed.');
