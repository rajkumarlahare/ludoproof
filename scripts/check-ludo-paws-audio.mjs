import fs from 'node:fs';

const requiredSources = [
  'android/app/src/main/java/com/ludoproof/game/core/audio/LudoPawsSoundPool.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameSoundFeedback.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameMusicController.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameSettingsStore.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/LudoPawsHaptics.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/data/audio/LudoPawsVoicePlayer.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsDialogUi.kt',
  'android/app/src/main/AndroidManifest.xml',
];

const requiredAudio = [
  'android/app/src/main/res/raw/lp_sfx_click.wav',
  'android/app/src/main/res/raw/lp_sfx_move.wav',
  'android/app/src/main/res/raw/lp_sfx_roll.wav',
  'android/app/src/main/res/raw/lp_sfx_capture.wav',
  'android/app/src/main/res/raw/lp_sfx_safe.wav',
  'android/app/src/main/res/raw/lp_sfx_home.wav',
  'android/app/src/main/res/raw/lp_sfx_victory.wav',
  'android/app/src/main/res/raw/lp_sfx_defeat.wav',
  'android/app/src/main/res/raw/lp_voice_duck.wav',
  'android/app/src/main/res/raw/lp_voice_squirrel.wav',
  'android/app/src/main/res/raw/lp_voice_hedgehog.wav',
  'android/app/src/main/res/raw/lp_voice_sheep.wav',
];

for (const file of [...requiredSources, ...requiredAudio]) {
  if (!fs.existsSync(file)) {
    throw new Error(`Missing Phase 9 audio file: ${file}`);
  }
}

const soundPool = fs.readFileSync(requiredSources[0], 'utf8');
const sfx = fs.readFileSync(requiredSources[1], 'utf8');
const music = fs.readFileSync(requiredSources[2], 'utf8');
const settings = fs.readFileSync(requiredSources[3], 'utf8');
const haptics = fs.readFileSync(requiredSources[4], 'utf8');
const voice = fs.readFileSync(requiredSources[5], 'utf8');
const settingsUi = fs.readFileSync(requiredSources[6], 'utf8');
const manifest = fs.readFileSync(requiredSources[7], 'utf8');

for (const marker of ['SoundPool', 'USAGE_GAME', 'CONTENT_TYPE_SONIFICATION', 'pendingByResource']) {
  if (!soundPool.includes(marker)) {
    throw new Error(`SoundPool engine is missing ${marker}`);
  }
}

if (sfx.includes('ToneGenerator')) {
  throw new Error('Phase 9 must not use one-ToneGenerator-per-event feedback');
}
for (const marker of ['LudoPawsSoundPool', 'fun reaction(', 'soundEnabled']) {
  if (!sfx.includes(marker)) {
    throw new Error(`Game SFX facade is missing ${marker}`);
  }
}

for (const marker of ['duckForVoice', 'AudioFocusRequest', 'AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK']) {
  if (!music.includes(marker)) {
    throw new Error(`Music/audio-focus controller is missing ${marker}`);
  }
}

for (const marker of ['animalVoicesEnabled', 'hapticsEnabled', 'reducedMotionEnabled']) {
  if (!settings.includes(marker)) {
    throw new Error(`Settings store is missing ${marker}`);
  }
}

for (const marker of [
  'Animal Voices',
  'Game Sounds',
  'Haptics',
  'Reduced Motion',
  'ScrollView',
]) {
  if (!settingsUi.includes(marker)) {
    throw new Error(`Settings UI is missing ${marker}`);
  }
}

for (const marker of [
  'animalVoicesEnabled',
  'LudoPawsSoundPool',
  'lp_voice_duck',
  'lp_voice_squirrel',
  'lp_voice_hedgehog',
  'lp_voice_sheep',
  'TextToSpeech',
  'duckForVoice',
  'GameSoundFeedback.reaction',
  'LudoPawsHaptics.reaction',
]) {
  if (!voice.includes(marker)) {
    throw new Error(`Animal voice coordinator is missing ${marker}`);
  }
}

if (!haptics.includes('hapticsEnabled') || !haptics.includes('VibrationEffect')) {
  throw new Error('Phase 9 haptics must be preference-gated and use VibrationEffect');
}
if (!manifest.includes('android.permission.VIBRATE')) {
  throw new Error('Phase 9 haptics require the VIBRATE manifest permission');
}

for (const file of requiredAudio) {
  const data = fs.readFileSync(file);
  if (data.length < 44 || data.subarray(0, 4).toString('ascii') !== 'RIFF') {
    throw new Error(`Audio asset is not a valid RIFF/WAV candidate: ${file}`);
  }
}

const forbiddenGameplayMarkers = [
  'OfflineGameEngine(',
  'session.roll(',
  'session.move(',
  'EntroNexV4Local',
];
for (const file of [requiredSources[0], requiredSources[1], requiredSources[4], requiredSources[5]]) {
  const source = fs.readFileSync(file, 'utf8');
  for (const marker of forbiddenGameplayMarkers) {
    if (source.includes(marker)) {
      throw new Error(`Audio presentation layer must not mutate gameplay/proof state: ${file} contains ${marker}`);
    }
  }
}

console.log('Ludo Paws Phase 9 audio engine gate passed.');
