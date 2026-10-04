import fs from 'node:fs';

const requiredFiles = [
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsReactionEngine.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsVoiceLines.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/data/audio/LudoPawsVoicePlayer.kt',
  'android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt',
  'android/app/src/test/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsReactionEngineTest.kt',
];

for (const file of requiredFiles) {
  if (!fs.existsSync(file)) {
    throw new Error(`Missing Phase 6 reaction file: ${file}`);
  }
}

const engine = fs.readFileSync(requiredFiles[0], 'utf8');
const voice = fs.readFileSync(requiredFiles[2], 'utf8');
const board = fs.readFileSync(requiredFiles[3], 'utf8');

for (const marker of [
  'VoiceCue.SIX',
  'VoiceCue.THIRD_SIX',
  'VoiceCue.CAPTURE',
  'VoiceCue.CAPTURED',
  'VoiceCue.SAFE',
  'VoiceCue.HOME',
  'VoiceCue.FRUSTRATED',
  'VoiceCue.VICTORY',
  'VoiceCue.DEFEAT',
]) {
  if (!engine.includes(marker)) {
    throw new Error(`Reaction engine is missing ${marker}`);
  }
}

if (!voice.includes('GameSettingsStore') || !voice.includes('TextToSpeech')) {
  throw new Error('Phase 6 voice player must respect sound settings and use on-device TTS');
}

if (!board.includes('LudoPawsReactionEngine.detect') || !board.includes('LudoPawsVoicePlayer')) {
  throw new Error('Reactive board is not wired to reactions and voice playback');
}

if (engine.includes('OfflineGameEngine(') || engine.includes('session.move(') || engine.includes('session.roll(')) {
  throw new Error('Reaction engine must remain presentation-only and must not mutate gameplay');
}

console.log('Ludo Paws Phase 6 reaction gate passed.');
