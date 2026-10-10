import fs from 'node:fs';

const required = {
  asset: 'android/app/src/main/res/drawable-nodpi/ludo_paws_game_background.webp',
  backdrop: 'android/app/src/main/java/com/ludoproof/game/core/ui/art/ArcadeBackdropView.kt',
  theme: 'android/app/src/main/java/com/ludoproof/game/core/ui/theme/LudoProofTheme.kt',
  offlineGameplay: 'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineGameplayScreenUi.kt',
  offlineSetup: 'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/setup/OfflineSetupScreenUi.kt',
  onlineMain: 'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/MainActivity.kt',
  onlineRenderer: 'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/actions/OnlineResponseRenderer.kt',
  onlineSession: 'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/actions/OnlineSessionActions.kt',
  remoteExit: 'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/actions/RemoteExitActions.kt',
  keep: 'android/app/src/main/res/raw/ludo_paws_background_keep.xml',
};

for (const [label, file] of Object.entries(required)) {
  if (!fs.existsSync(file)) throw new Error(`Gameplay background gate: missing ${label}: ${file}`);
}

const read = label => fs.readFileSync(required[label], 'utf8');
const asset = fs.readFileSync(required.asset);
if (asset.length < 1000 || asset.length > 1_000_000) {
  throw new Error(`Gameplay background must be optimized (1 KB–1 MB); got ${asset.length} bytes.`);
}
if (
  asset.toString('ascii', 0, 4) !== 'RIFF' ||
  asset.toString('ascii', 8, 12) !== 'WEBP'
) {
  throw new Error('Gameplay background asset is not a valid RIFF/WEBP file.');
}

const backdrop = read('backdrop');
for (const marker of ['useGameplayBackground()', 'useUiBackground()', 'ludo_paws_game_background', 'ludo_paws_ui_background']) {
  if (!backdrop.includes(marker)) throw new Error(`Backdrop implementation is missing ${marker}`);
}

const theme = read('theme');
for (const marker of ['gameplayBackground: Boolean = false', 'setGameplayBackground(', 'useGameplayBackground()']) {
  if (!theme.includes(marker)) throw new Error(`Theme does not isolate the gameplay background: missing ${marker}`);
}

if (!read('offlineGameplay').includes('gameplayBackground = true')) {
  throw new Error('Offline gameplay must opt into the dedicated background.');
}
if (!read('offlineSetup').includes('LudoProofTheme.arcadeRoot(this)')) {
  throw new Error('Offline setup must retain the shared UI background.');
}
if (!read('onlineMain').includes('arcadeRootView = root')) {
  throw new Error('Online Activity must retain the root for active-state background switching.');
}
if (!read('onlineRenderer').includes('enabled = state.status == "ACTIVE"')) {
  throw new Error('Online gameplay background must be enabled only for ACTIVE matches.');
}
if (!read('onlineSession').includes('enabled = false')) {
  throw new Error('Returning an online match to its lobby must restore the shared UI background.');
}
if (!read('remoteExit').includes('enabled = false')) {
  throw new Error('Leaving an online match must restore the shared UI background.');
}
const keep = read('keep');
for (const marker of ['@drawable/ludo_paws_ui_background', '@drawable/ludo_paws_game_background']) {
  if (!keep.includes(marker)) throw new Error(`Release resource keep list is missing ${marker}`);
}

console.log(`Gameplay background gate passed; validated WebP asset (${asset.length} bytes) and offline/online background isolation.`);
