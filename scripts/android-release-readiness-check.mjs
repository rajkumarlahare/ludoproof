import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const failures = [];

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function requireText(name, source, token, message) {
  if (!source.includes(token)) failures.push(`${name}: ${message}`);
}

function requirePattern(name, source, pattern, message) {
  if (!pattern.test(source)) failures.push(`${name}: ${message}`);
}

const appBuild = read("android/app/build.gradle.kts");
const snapshotTests = read(
  "android/app/src/test/java/com/ludoproof/game/GameJsonAuthoritativeSnapshotTest.kt",
);
const rulesets = read("server/src/rulesets.js");

requirePattern(
  "android/app/build.gradle.kts",
  appBuild,
  /versionCode\s*=\s*2\b/,
  "RC2 must use monotonically increasing versionCode 2.",
);
requirePattern(
  "android/app/build.gradle.kts",
  appBuild,
  /versionName\s*=\s*"1\.0\.0-rc2"/,
  "RC2 must expose versionName 1.0.0-rc2.",
);

for (const variable of [
  "LUDOPROOF_RELEASE_STORE_FILE",
  "LUDOPROOF_RELEASE_STORE_PASSWORD",
  "LUDOPROOF_RELEASE_KEY_ALIAS",
  "LUDOPROOF_RELEASE_KEY_PASSWORD",
]) {
  requireText(
    "android/app/build.gradle.kts",
    appBuild,
    `environmentVariable("${variable}")`,
    `release signing must read ${variable} from the environment.`,
  );
}
requireText(
  "android/app/build.gradle.kts",
  appBuild,
  "releaseSigningConfigured",
  "release signing must be explicit and all-or-nothing.",
);
requireText(
  "android/app/build.gradle.kts",
  appBuild,
  "releaseSigningPartiallyConfigured",
  "partial release signing configuration must be detected.",
);
requireText(
  "android/app/build.gradle.kts",
  appBuild,
  "org.gradle.api.GradleException",
  "partial release signing configuration must fail closed.",
);
requireText(
  "android/app/build.gradle.kts",
  appBuild,
  'signingConfig = signingConfigs.getByName("release")',
  "release build must use the secure release signing config when configured.",
);

if (/storePassword\s*=\s*"[^"$]+"/.test(appBuild)) {
  failures.push("android/app/build.gradle.kts: release store password must never be hardcoded.");
}
if (/keyPassword\s*=\s*"[^"$]+"/.test(appBuild)) {
  failures.push("android/app/build.gradle.kts: release key password must never be hardcoded.");
}

for (const rulesetId of [
  "ludoproof-standard-v1",
  "ludoproof-standard-v2",
  "ludoproof-team-v1",
  "ludoproof-team-v2",
]) {
  requireText(
    "GameJsonAuthoritativeSnapshotTest.kt",
    snapshotTests,
    rulesetId,
    `Android authoritative snapshot coverage must include ${rulesetId}.`,
  );
}

for (const policyToken of [
  "extraTurnOnHome: true",
  'rollTimeoutPolicy: "FORFEIT_ROLL_AND_ADVANCE_TURN"',
  'ownTokenStacking: "ALLOWED"',
  'opponentStackCapture: "CAPTURE_ALL_ON_UNSAFE_CELL"',
  'startingPlayerPolicy: "HOST_SEAT_ZERO"',
]) {
  requireText(
    "server/src/rulesets.js",
    rulesets,
    policyToken,
    `ruleset v2 release contract is missing ${policyToken}.`,
  );
}

if (failures.length > 0) {
  console.error("Android RC2 release-readiness gate failed:");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(
  "Android RC2 release-readiness gate passed: version monotonicity, fail-closed secure signing, V1/V2 dual-read coverage, and explicit Ruleset V2 contract are intact.",
);
