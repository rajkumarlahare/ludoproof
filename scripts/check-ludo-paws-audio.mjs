import fs from 'node:fs';

const requiredSources = [
  'android/app/src/main/java/com/ludoproof/game/core/audio/LudoPawsSoundPool.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameSoundFeedback.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameMusicController.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameSettingsStore.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/LudoPawsHaptics.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/data/audio/LudoPawsVoicePlayer.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/data/audio/LudoPawsAudioAssetPlayer.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/data/audio/LudoPawsProceduralAudio.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/audio/LudoPawsReactionAudioProfile.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsDialogUi.kt',
  'android/app/src/main/AndroidManifest.xml',
];

const reactionFeedbackOwners = [
  'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/OnlineLudoPawsPresentation.kt',
  'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/feedback/OfflineLudoPawsFeedback.kt',
];
const feedbackLedger =
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsFeedbackLedger.kt';
const reactiveBoard =
  'android/app/src/main/java/com/ludoproof/game/game/ui/components/LudoPawsReactiveBoardView.kt';
const deletedSpokenLines =
  'android/app/src/main/java/com/ludoproof/game/feature/characters/domain/reaction/LudoPawsVoiceLines.kt';

for (const file of [...requiredSources, ...reactionFeedbackOwners, feedbackLedger, reactiveBoard]) {
  if (!fs.existsSync(file)) {
    throw new Error(`Missing production audio source: ${file}`);
  }
}
if (fs.existsSync(deletedSpokenLines)) {
  throw new Error('Spoken character commentary file must stay removed.');
}

const soundPool = fs.readFileSync(requiredSources[0], 'utf8');
const sfx = fs.readFileSync(requiredSources[1], 'utf8');
const music = fs.readFileSync(requiredSources[2], 'utf8');
const settings = fs.readFileSync(requiredSources[3], 'utf8');
const haptics = fs.readFileSync(requiredSources[4], 'utf8');
const voice = fs.readFileSync(requiredSources[5], 'utf8');
const resolver = fs.readFileSync(requiredSources[6], 'utf8');
const procedural = fs.readFileSync(requiredSources[7], 'utf8');
const profile = fs.readFileSync(requiredSources[8], 'utf8');
const settingsUi = fs.readFileSync(requiredSources[9], 'utf8');
const manifest = fs.readFileSync(requiredSources[10], 'utf8');
const ledgerSource = fs.readFileSync(feedbackLedger, 'utf8');

for (const marker of ['SoundPool', 'USAGE_GAME', 'CONTENT_TYPE_SONIFICATION', 'pendingByResource']) {
  if (!soundPool.includes(marker)) throw new Error(`SoundPool engine is missing ${marker}`);
}
for (const marker of [
  'lp_sfx_dice_roll',
  'lp_sfx_move_paw',
  'lp_sfx_move_hoof',
  'lp_sfx_move_web',
  'lp_sfx_capture_impact',
  'lp_sfx_safe_shimmer',
  'lp_sfx_home_lane',
  'lp_sfx_home_sparkle',
  'lp_sfx_third_six',
  'fun reaction(',
]) {
  if (!sfx.includes(marker)) throw new Error(`Replaceable game SFX facade is missing ${marker}`);
}
if (sfx.includes('ToneGenerator')) {
  throw new Error('Production SFX must not use one-ToneGenerator-per-event feedback.');
}

for (const marker of ['duckForVoice', 'AudioFocusRequest', 'AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK']) {
  if (!music.includes(marker)) throw new Error(`Music/audio-focus controller is missing ${marker}`);
}
for (const marker of ['animalVoicesEnabled', 'hapticsEnabled', 'reducedMotionEnabled']) {
  if (!settings.includes(marker)) throw new Error(`Settings store is missing ${marker}`);
}
for (const marker of ['Animal Voices', 'Game Sounds', 'Haptics', 'Reduced Motion', 'ScrollView']) {
  if (!settingsUi.includes(marker)) throw new Error(`Settings UI is missing ${marker}`);
}

for (const forbidden of [
  'import android.speech.tts.TextToSpeech',
  'TextToSpeech(',
  'Six! Let',
  'Got you!',
  'Back to the yard',
  'Home sweet home',
  'I win!',
  'Good game.',
]) {
  if (voice.includes(forbidden)) {
    throw new Error(`Animal reactions must remain non-verbal; found ${forbidden}`);
  }
}
for (const marker of [
  'LudoPawsReactionAudioProfile',
  'LudoPawsAudioAssetPlayer',
  'animalVoicesEnabled',
  'canonicalCharacterForSeat',
  'MAX_CAPTURE_VOCALS',
]) {
  if (!voice.includes(marker)) throw new Error(`Animal vocal renderer is missing ${marker}`);
}

for (const marker of ['getIdentifier', 'LudoPawsSoundPool', 'playVocal', 'playSfx']) {
  if (!resolver.includes(marker)) throw new Error(`Audio resource resolver is missing ${marker}`);
}
for (const marker of ['AudioTrack', 'ENCODING_PCM_16BIT', 'DOG_YIP', 'GOAT_BLEAT', 'DUCK_QUACK', 'CAT_CHIRP']) {
  if (!procedural.includes(marker)) throw new Error(`Procedural fallback is missing ${marker}`);
}
for (const marker of [
  'lp_vocal_${characterId}_${cue.wireName}',
  'probabilityPercent',
  'deterministicVariantIndex',
  'DOG_WHINE',
  'GOAT_SOFT_BLEAT',
  'DUCK_SOFT_QUACK',
  'CAT_PURR',
]) {
  if (!profile.includes(marker)) throw new Error(`Reaction audio profile is missing ${marker}`);
}

for (const marker of ['class LudoPawsFeedbackLedger', 'LinkedHashSet', 'fun once(', 'fun filterReactions(']) {
  if (!ledgerSource.includes(marker)) throw new Error(`Exactly-once feedback ledger is missing ${marker}`);
}

const forbiddenSecondaryFeedbackCalls = ['GameSoundFeedback.reaction', 'LudoPawsHaptics.reaction'];
for (const marker of forbiddenSecondaryFeedbackCalls) {
  if (voice.includes(marker)) {
    throw new Error(`Animal vocal renderer must remain vocal-only; ${marker} belongs to mode dispatchers.`);
  }
}
const boardSource = fs.readFileSync(reactiveBoard, 'utf8');
for (const marker of forbiddenSecondaryFeedbackCalls) {
  if (boardSource.includes(marker)) {
    throw new Error(`Reactive board must not become a second feedback owner: ${marker}`);
  }
}
for (const file of reactionFeedbackOwners) {
  const source = fs.readFileSync(file, 'utf8');
  for (const marker of ['GameSoundFeedback.', 'LudoPawsHaptics.reaction', 'LudoPawsFeedbackLedger', '.once(']) {
    if (!source.includes(marker)) throw new Error(`Mode feedback owner ${file} is missing ${marker}`);
  }
}

if (!haptics.includes('hapticsEnabled') || !haptics.includes('VibrationEffect')) {
  throw new Error('Haptics must be preference-gated and use VibrationEffect.');
}
if (!manifest.includes('android.permission.VIBRATE')) {
  throw new Error('Haptics require the VIBRATE manifest permission.');
}

const forbiddenGameplayMarkers = ['OfflineGameEngine(', 'session.roll(', 'session.move(', 'EntroNexV4Local'];
for (const file of [requiredSources[0], requiredSources[1], requiredSources[4], requiredSources[5], requiredSources[6], requiredSources[7], requiredSources[8]]) {
  const source = fs.readFileSync(file, 'utf8');
  for (const marker of forbiddenGameplayMarkers) {
    if (source.includes(marker)) {
      throw new Error(`Audio presentation layer must not mutate gameplay/proof state: ${file} contains ${marker}`);
    }
  }
}

console.log('Ludo Paws non-verbal replaceable audio gate passed.');
