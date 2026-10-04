import fs from "node:fs";

const read = (path) => fs.readFileSync(path, "utf8");
const requireText = (source, token, message) => {
  if (!source.includes(token)) throw new Error(message);
};
const rejectText = (source, token, message) => {
  if (source.includes(token)) throw new Error(message);
};

const strings = read("android/app/src/main/res/values/strings.xml");
const homeHero = read(
  "android/app/src/main/java/com/ludoproof/game/feature/home/presentation/components/HomeHeroUi.kt",
);
const fairDice = read(
  "android/app/src/main/java/com/ludoproof/game/feature/proof/domain/core/FairDiceExplainer.kt",
);
const fairDiceTest = read(
  "android/app/src/test/java/com/ludoproof/game/feature/proof/domain/core/FairDiceExplainerTest.kt",
);
const profileActivity = read(
  "android/app/src/main/java/com/ludoproof/game/feature/profile/presentation/ProfileActivity.kt",
);
const profilePaw = read(
  "android/app/src/main/java/com/ludoproof/game/feature/profile/presentation/components/ProfilePawIdentityUi.kt",
);
const storeActivity = read(
  "android/app/src/main/java/com/ludoproof/game/feature/store/presentation/StoreActivity.kt",
);
const buildGradle = read("android/app/build.gradle.kts");
const wrangler = read("server/wrangler.json");

requireText(
  strings,
  "Verified Fair Dice",
  "Home/store-facing copy must expose the consumer-friendly Verified Fair Dice wording.",
);
requireText(
  strings,
  "talking animal pawns",
  "Home copy must keep the Ludo Paws character USP ahead of proof jargon.",
);
requireText(
  homeHero,
  "FairDiceExplainer.HOME_ACTION_LABEL",
  "Home must use the centralized Fair Dice action label.",
);
requireText(
  homeHero,
  "FairDiceExplainer.dialogBody()",
  "Home must route Fair Dice into the centralized explanation.",
);
requireText(
  fairDice,
  'HOME_ACTION_LABEL =\n        "FAIR DICE"',
  "Fair Dice must replace raw PROOFS wording in the consumer quick action.",
);
requireText(
  fairDice,
  "Advanced details:",
  "Advanced proof vocabulary must remain available behind the friendly summary.",
);
requireText(
  fairDice,
  "EntroNex v4",
  "Advanced Fair Dice details must retain the online proof mechanism name.",
);
requireText(
  fairDice,
  "HKDF",
  "Advanced Fair Dice details must retain the offline derivation vocabulary.",
);
rejectText(
  fairDice.match(/FRIENDLY_SUMMARY =[\s\S]*?ONLINE_SUMMARY =/)?.[0] ?? "",
  "HKDF",
  "Friendly Fair Dice summary must not front-load protocol jargon.",
);

requireText(
  profileActivity,
  "profilePawIdentityPanel()",
  "Profile must surface the equipped Ludo Paws character identity.",
);
requireText(
  profileActivity,
  "override fun onRestart()",
  "Profile must refresh after returning from character/store selection.",
);
requireText(
  profilePaw,
  "CharacterSelectionStore",
  "Profile Paw identity must come from the real persisted character selection.",
);
requireText(
  profilePaw,
  "importantForAccessibility",
  "Equipped character portrait must be exposed to accessibility services.",
);
requireText(
  profilePaw,
  "Character personality is presentation-only and never changes dice or legal moves.",
  "Profile must keep the cosmetic/gameplay trust boundary explicit.",
);
requireText(
  profilePaw,
  "StoreActivity.EXTRA_INITIAL_TAB",
  "Profile must deep-link to the Paws store instead of dropping users into an unrelated tab.",
);
requireText(
  storeActivity,
  "requestedInitialTab()",
  "Store must safely resolve direct-tab requests.",
);
requireText(
  storeActivity,
  "StoreTab.entries",
  "Store direct-tab routing must validate against known tabs.",
);

for (const marker of [
  "homeLabelUsesConsumerFriendlyFairDiceLanguage",
  "friendlySummaryAvoidsProtocolJargon",
  "advancedDetailsStillExposeAuditVocabulary",
  "dialogBodyKeepsFriendlyExplanationBeforeAdvancedDetails",
]) {
  requireText(
    fairDiceTest,
    marker,
    `Phase 15 Fair Dice unit coverage is missing: ${marker}`,
  );
}

requireText(
  buildGradle,
  'applicationId = "com.ludoproof.game"',
  "Phase 15 must not migrate the Android package identity.",
);
requireText(
  wrangler,
  '"name": "ludoproof-game-api"',
  "Phase 15 must not rename the deployed Cloudflare Worker.",
);

console.log(
  "Ludo Paws Phase 15 hardening gate passed: social character identity, Verified Fair Dice UX, accessibility hooks, deep-link safety and compatibility identifiers are preserved.",
);
