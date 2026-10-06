import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const failures = [];

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function requireMatch(name, text, pattern, message) {
  if (!pattern.test(text)) {
    failures.push(`${name}: ${message}`);
  }
}

function walk(directory) {
  const results = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      results.push(...walk(absolute));
    } else {
      results.push(absolute);
    }
  }
  return results;
}

const rootBuild = read("android/build.gradle.kts");
const appBuild = read("android/app/build.gradle.kts");
const manifest = read("android/app/src/main/AndroidManifest.xml");
const application = read(
  "android/app/src/main/java/com/ludoproof/game/core/app/LudoProofApplication.kt",
);
const immersiveWindow = read(
  "android/app/src/main/java/com/ludoproof/game/core/ui/window/ImmersiveGameWindow.kt",
);
const baseTheme = read("android/app/src/main/res/values/styles.xml");
const api30Theme = read("android/app/src/main/res/values-v30/styles.xml");

requireMatch(
  "android/build.gradle.kts",
  rootBuild,
  /id\("com\.android\.application"\) version "8\.10\.1"/,
  "AGP must stay on the reviewed API 36-compatible 8.10.1 baseline.",
);
requireMatch(
  "android/build.gradle.kts",
  rootBuild,
  /id\("org\.jetbrains\.kotlin\.android"\) version "2\.2\.10"/,
  "Kotlin plugin must stay in the reviewed AGP 8.10-compatible range.",
);
requireMatch(
  "android/app/build.gradle.kts",
  appBuild,
  /compileSdk\s*=\s*36\b/,
  "compileSdk must remain 36.",
);
requireMatch(
  "android/app/build.gradle.kts",
  appBuild,
  /targetSdk\s*=\s*36\b/,
  "targetSdk must remain 36.",
);
requireMatch(
  "AndroidManifest.xml",
  manifest,
  /android:appCategory="game"/,
  "LudoProof must declare itself as a game for platform form-factor semantics.",
);
requireMatch(
  "AndroidManifest.xml",
  manifest,
  /android:theme="@style\/Theme\.LudoPaws"/,
  "the app must use the fullscreen game launch theme so cold starts are edge-to-edge.",
);
requireMatch(
  "res/values/styles.xml",
  baseTheme,
  /android:statusBarColor">@android:color\/transparent</,
  "the launch theme must keep the status-bar surface transparent.",
);
requireMatch(
  "res/values/styles.xml",
  baseTheme,
  /android:navigationBarColor">@android:color\/transparent</,
  "the launch theme must keep the navigation-bar surface transparent.",
);
requireMatch(
  "res/values/styles.xml",
  baseTheme,
  /android:windowLayoutInDisplayCutoutMode">shortEdges</,
  "API 28-29 must opt into available short-edge display-cutout rendering.",
);
requireMatch(
  "res/values-v30/styles.xml",
  api30Theme,
  /android:windowLayoutInDisplayCutoutMode">always</,
  "API 30+ must allow the game background through every display-cutout region.",
);
requireMatch(
  "ImmersiveGameWindow.kt",
  immersiveWindow,
  /setDecorFitsSystemWindows\(false\)/,
  "edge-to-edge must remain explicitly supported.",
);
requireMatch(
  "ImmersiveGameWindow.kt",
  immersiveWindow,
  /WindowInsets\.Type\.systemBars\(\)/,
  "system-bar handling must remain explicit for immersive gameplay.",
);
requireMatch(
  "ImmersiveGameWindow.kt",
  immersiveWindow,
  /LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS/,
  "modern Android must retain full display-cutout coverage.",
);
requireMatch(
  "ImmersiveGameWindow.kt",
  immersiveWindow,
  /LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES/,
  "Android 9-10 compatibility must retain short-edge display-cutout coverage.",
);
requireMatch(
  "ImmersiveGameWindow.kt",
  immersiveWindow,
  /statusBarColor\s*=\s*Color\.TRANSPARENT/,
  "runtime status-bar surfaces must remain transparent.",
);
requireMatch(
  "ImmersiveGameWindow.kt",
  immersiveWindow,
  /navigationBarColor\s*=\s*Color\.TRANSPARENT/,
  "runtime navigation-bar surfaces must remain transparent.",
);
requireMatch(
  "LudoProofApplication.kt",
  application,
  /ImmersiveGameWindow\.configure\(activity\)/,
  "every Activity must inherit the fullscreen game-window contract.",
);
requireMatch(
  "LudoProofApplication.kt",
  application,
  /ImmersiveGameWindow\.restore\(activity\)/,
  "immersive fullscreen must be restored when an Activity resumes.",
);

if (/windowOptOutEdgeToEdgeEnforcement/.test(manifest)) {
  failures.push(
    "AndroidManifest.xml: deprecated edge-to-edge opt-out is not allowed when targeting API 36.",
  );
}

const mainSource = path.join(repoRoot, "android/app/src/main");
for (const file of walk(mainSource)) {
  if (file.endsWith(".kt")) {
    const source = fs.readFileSync(file, "utf8");
    if (/override\s+fun\s+onBackPressed\s*\(/.test(source)) {
      failures.push(
        `${path.relative(repoRoot, file)}: legacy onBackPressed override is incompatible with the Android 16 predictive-back baseline.`,
      );
    }
    if (/KeyEvent\.KEYCODE_BACK/.test(source)) {
      failures.push(
        `${path.relative(repoRoot, file)}: KEYCODE_BACK handling is not allowed in the Android 16 release baseline.`,
      );
    }
  }

  if (file.endsWith(".xml")) {
    const xml = fs.readFileSync(file, "utf8");
    if (/windowOptOutEdgeToEdgeEnforcement/.test(xml)) {
      failures.push(
        `${path.relative(repoRoot, file)}: remove windowOptOutEdgeToEdgeEnforcement; Android 16 disables this opt-out.`,
      );
    }
  }
}

if (failures.length > 0) {
  console.error("Android 16 release gate failed:");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log(
  "Android 16 release gate passed: API 36 target, cutout-safe immersive edge-to-edge, transparent system-bar surfaces, and predictive-back baseline are intact.",
);
