import fs from 'node:fs';

const dicePath = 'android/app/src/main/java/com/ludoproof/game/game/ui/components/DiceView.kt';
const settingsPath = 'android/app/src/main/java/com/ludoproof/game/feature/settings/data/local/GameSettingsStore.kt';

const dice = fs.readFileSync(dicePath, 'utf8');
const settings = fs.readFileSync(settingsPath, 'utf8');

const requiredDiceTokens = [
  'Color.WHITE',
  '0xFFF4F4F4.toInt()',
  '0xFFDCDCDC.toInt()',
  '0xFF8B8B8B.toInt()',
  '0xFF3F3F3F.toInt()',
];
for (const token of requiredDiceTokens) {
  if (!dice.includes(token)) throw new Error(`Missing neutral dice token: ${token}`);
}

const requiredSpeeds = [
  'moveStepMs = 180L',
  'moveStepMs = 240L',
  'moveStepMs = 320L',
];
for (const token of requiredSpeeds) {
  if (!settings.includes(token)) throw new Error(`Missing movement speed: ${token}`);
}

console.log('Neutral dice and pawn speed tier contract passed.');
