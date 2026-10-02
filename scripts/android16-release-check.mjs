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
const theme = read(
  "android/app/src/main/java/com/ludoproof/game/core/ui/theme/LudoProofTheme.kt",
);

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
  "LudoProofTheme.kt",
  theme,
  /setDecorFitsSystemWindows\(false\)/,
  "edge-to-edge must remain explicitly supported.",
);
requireMatch(
  "LudoProofTheme.kt",
  theme,
  /WindowInsets\.Type\.systemBars\(\)/,
  "system-bar handling must remain explicit for immersive gameplay.",
);

if (/windowOptOutEdgeToEdgeEnforcement/.test(read("android/app/src/main/AndroidManifest.xml"))) {
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
  "Android 16 release gate passed: API 36 target, supported toolchain, immersive edge-to-edge, and predictive-back baseline are intact.",
);
