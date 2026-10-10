import fs from 'node:fs';

const files = {
  quickChat: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/QuickChatUi.kt',
  settingsDrawable: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsSharedWebpDrawable.kt',
  settingsKit: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsVisualKit.kt',
  settingsUi: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsDialogUi.kt',
  offlineRail: 'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflinePlayerRailUi.kt',
  onlinePresentation: 'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/OnlineLudoPawsPresentation.kt',
  reactionPolicy: 'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineQuickReactionPolicy.kt',
  settingsEntry: 'android/app/src/main/java/com/ludoproof/game/core/ui/dialogs/ArcadeDialogs.kt',
};

for (const [label, file] of Object.entries(files)) {
  if (!fs.existsSync(file)) throw new Error(`Quick Chat/settings gate: missing ${label}: ${file}`);
}
const read = label => fs.readFileSync(files[label], 'utf8');

const quickChat = read('quickChat');
const emojiBlock = quickChat.match(/val EMOJIS: List<String> = listOf\(([\s\S]*?)\n    \)/);
if (!emojiBlock) throw new Error('Quick Chat emoji catalog is missing.');
const emojis = [...emojiBlock[1].matchAll(/"([^"]+)"/g)].map(match => match[1]);
if (emojis.length !== 24 || new Set(emojis).size !== 24) {
  throw new Error(`Quick Chat must contain 24 unique emojis; found ${emojis.length}.`);
}
for (const marker of ['class QuickChatButtonView', 'fun showQuickChatPopup(', 'GridLayout', 'fun animateQuickChatReaction(']) {
  if (!quickChat.includes(marker)) throw new Error(`Quick Chat UI is missing ${marker}`);
}
if (!quickChat.includes('QuickChatEmojiCatalog.EMOJIS.chunked(6)')) {
  throw new Error('Quick Chat must render a 6-column emoji grid.');
}

for (const [label, source] of [
  ['offline profile rail', read('offlineRail')],
  ['online profile rail', read('onlinePresentation')],
]) {
  for (const marker of ['QuickChatButtonView', 'showQuickChatPopup', 'quickChatEnabled']) {
    if (!source.includes(marker)) throw new Error(`${label} is missing ${marker}`);
  }
}
if (!read('reactionPolicy').includes('QuickChatEmojiCatalog.EMOJIS.toSet()')) {
  throw new Error('Offline reactions must accept the complete Quick Chat emoji catalog.');
}
if (!read('offlineRail').includes('senderPlayerId = player.playerId')) {
  throw new Error('Offline Quick Chat must attribute the reaction to the profile whose icon was selected.');
}

const settings = read('settingsUi');
for (const label of ['Music', 'Sound', 'Quick chat', 'Game Speed', 'Boards', 'Dice']) {
  if (!settings.includes(`label = "${label}"`)) {
    throw new Error(`Settings UI is missing the required ${label} row.`);
  }
}
for (const removed of ['label = "Animal Voices"', 'label = "Haptics"', 'label = "Game Sounds"']) {
  if (settings.includes(removed)) throw new Error(`Obsolete Settings row is still visible: ${removed}`);
}
if (!settings.includes('settingsPrivacyLink(')) {
  throw new Error('Settings UI must retain its Privacy Policy link.');
}
if (!read('settingsKit').includes('background = SettingsSharedWebpDrawable(context)')) {
  throw new Error('Settings popup must use the shared WebP image as its background.');
}
if (!read('settingsDrawable').includes('R.drawable.ludo_paws_game_background')) {
  throw new Error('Settings WebP background must reference the shared gameplay drawable.');
}
if (!read('settingsEntry').includes('onChanged: (() -> Unit)? = null')) {
  throw new Error('Settings changes must notify the online UI so Quick Chat visibility refreshes.');
}

console.log('Ludo Paws Quick Chat/settings UI gate passed: 24 unique emojis, profile controls on offline/online, compact Settings rows and shared WebP panel background.');
