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
for (const marker of ['class QuickChatButtonView', 'fun showQuickChatPopup(', 'fun animateQuickChatReaction(']) {
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
  throw new Error('Settings popup must use the rough Settings texture.');
}
const settingsDrawable = read('settingsDrawable');
for (const marker of ['LinearGradient', 'val grain', 'canvas.drawCircle', 'canvas.drawLine']) {
  if (!settingsDrawable.includes(marker)) throw new Error(`Settings rough texture is missing ${marker}`);
}
if (settingsDrawable.includes('BitmapFactory') || settingsDrawable.includes('ludo_paws_game_background')) {
  throw new Error('Settings background must not fall back to the glossy gameplay image.');
}
if (!read('offlineRail').includes('isComputerMode') || !read('offlineRail').includes('hasComputerOpponent')) {
  throw new Error('Offline Quick Chat must be reserved for the human in Computer mode, never Pass & Play.');
}
if (!read('onlinePresentation').includes('quickChatEnabled && localPlayer')) {
  throw new Error('Online Quick Chat must be visible only on the local player profile.');
}
if (!read('onlinePresentation').includes('sendQuickChat(emoji)') || !read('onlinePresentation').includes('presentRemoteQuickChat')) {
  throw new Error('Online Quick Chat must send and render real remote reactions.');
}
const realtime = fs.readFileSync('android/app/src/main/java/com/ludoproof/game/feature/online/data/realtime/MatchRealtimeClient.kt', 'utf8');
if (!realtime.includes('QUICK_CHAT_SEND') || !realtime.includes('type == "QUICK_CHAT"')) {
  throw new Error('Realtime client must implement Quick Chat send/receive.');
}
const serverRoom = fs.readFileSync('server/src/match-room.js', 'utf8');
for (const marker of ['QUICK_CHAT_SEND', 'QUICK_CHAT', 'getTags(socket)', 'QUICK_CHAT_COOLDOWN_MS']) {
  if (!serverRoom.includes(marker)) throw new Error(`Server Quick Chat handler is missing ${marker}`);
}
if (!read('settingsKit').includes('background = SettingsSharedWebpDrawable(context)')) {
  throw new Error('Settings options dialog must use the shared rough-texture background.');
}
if (!read('settingsEntry').includes('onChanged: (() -> Unit)? = null')) {
  throw new Error('Settings changes must notify the online UI so Quick Chat visibility refreshes.');
}

console.log('Ludo Paws Quick Chat/settings UI gate passed: 24 unique emojis, profile controls on offline/online, compact Settings rows and shared WebP panel background.');
