import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const catalogPath = path.join(
  root,
  "android/app/src/main/assets/ludo_paws/catalog.json",
);
const drawableRoot = path.join(
  root,
  "android/app/src/main/res/drawable",
);
const assetsRoot = path.join(
  root,
  "android/app/src/main/assets",
);

const fail = (message) => {
  throw new Error(`Ludo Paws asset gate: ${message}`);
};

if (!fs.existsSync(catalogPath)) {
  fail("missing catalog.json");
}

const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
const idPattern = /^[a-z][a-z0-9_]{1,31}$/;
const expectedStarterIds = ["duck", "squirrel", "hedgehog", "sheep"];
const expectedExpressions = [
  "happy",
  "sad",
  "angry",
  "nervous",
  "excited",
  "victory",
  "defeat",
];
const expectedVoiceCues = [
  "six",
  "capture",
  "captured",
  "safe",
  "home",
  "frustrated",
  "third_six",
  "idle",
  "nervous",
  "victory",
  "defeat",
];

if (catalog.schemaVersion !== 1) fail("schemaVersion must be 1");
if (catalog.packId !== "starter_paws") fail("Phase 2 packId must be starter_paws");
if (!Array.isArray(catalog.characters) || catalog.characters.length !== 4) {
  fail("starter_paws must contain exactly four starter characters");
}

const sameSet = (left, right) =>
  left.length === right.length &&
  [...left].sort().every((value, index) => value === [...right].sort()[index]);

if (!sameSet(catalog.allowedExpressions ?? [], expectedExpressions)) {
  fail("allowedExpressions does not match the production contract");
}
if (!sameSet(catalog.allowedVoiceCues ?? [], expectedVoiceCues)) {
  fail("allowedVoiceCues does not match the production contract");
}

const expectedBudgets = {
  singleBitmapDecodedBytes: 3 * 1024 * 1024,
  activeCharacterArtBytes: 12 * 1024 * 1024,
};
for (const [key, value] of Object.entries(expectedBudgets)) {
  if (catalog.memoryBudget?.[key] !== value) {
    fail(`memoryBudget.${key} must be ${value}`);
  }
}

const limits = {
  pawnBytes: 120 * 1024,
  portraitBytes: 260 * 1024,
  fullBodyBytes: 480 * 1024,
  expressionBytes: 260 * 1024,
  voiceBytes: 180 * 1024,
  voiceDurationMs: 2500,
};
for (const [key, value] of Object.entries(limits)) {
  if (catalog.limits?.[key] !== value) {
    fail(`limits.${key} must be ${value}`);
  }
}

const seenIds = new Set();
const expectedPathsFor = (id) => ({
  pawn: `ludo_paws/characters/${id}/pawn.webp`,
  portrait: `ludo_paws/characters/${id}/portrait.webp`,
  fullBody: `ludo_paws/characters/${id}/full_body.webp`,
  audioRoot: `ludo_paws/audio/${id}`,
});

const safeAssetPath = (value) =>
  typeof value === "string" &&
  value.startsWith("ludo_paws/") &&
  !value.startsWith("/") &&
  !value.includes("..") &&
  !value.includes("\\");

const assertFileWithinLimit = (assetRelativePath, maxBytes) => {
  if (!safeAssetPath(assetRelativePath)) {
    fail(`unsafe asset path: ${assetRelativePath}`);
  }
  const absolute = path.join(assetsRoot, ...assetRelativePath.split("/"));
  if (!fs.existsSync(absolute)) {
    fail(`production-ready asset is missing: ${assetRelativePath}`);
  }
  const size = fs.statSync(absolute).size;
  if (size <= 0) fail(`asset is empty: ${assetRelativePath}`);
  if (size > maxBytes) {
    fail(`${assetRelativePath} is ${size} bytes; limit is ${maxBytes}`);
  }
};

for (const character of catalog.characters) {
  const id = character?.id;
  if (!idPattern.test(id ?? "")) fail(`invalid character id: ${id}`);
  if (seenIds.has(id)) fail(`duplicate character id: ${id}`);
  seenIds.add(id);

  const fallback = character?.fallbackDrawable;
  if (!/^lp_starter_[a-z0-9_]+$/.test(fallback ?? "")) {
    fail(`invalid fallback drawable for ${id}`);
  }
  const fallbackPath = path.join(drawableRoot, `${fallback}.xml`);
  if (!fs.existsSync(fallbackPath)) {
    fail(`missing fallback drawable for ${id}: ${fallback}.xml`);
  }

  const expected = expectedPathsFor(id);
  const visuals = character?.visuals;
  if (!visuals || typeof visuals.productionReady !== "boolean") {
    fail(`${id}.visuals.productionReady must be boolean`);
  }
  for (const slot of ["pawn", "portrait", "fullBody"]) {
    if (visuals[slot] !== expected[slot]) {
      fail(`${id}.visuals.${slot} must be ${expected[slot]}`);
    }
  }
  if (!visuals.expressions || typeof visuals.expressions !== "object" || Array.isArray(visuals.expressions)) {
    fail(`${id}.visuals.expressions must be an object`);
  }
  for (const [expression, assetPath] of Object.entries(visuals.expressions)) {
    if (!expectedExpressions.includes(expression)) {
      fail(`${id} has unsupported expression: ${expression}`);
    }
    const expectedExpressionPath =
      `ludo_paws/characters/${id}/expressions/${expression}.webp`;
    if (assetPath !== expectedExpressionPath) {
      fail(`${id}.${expression} must be ${expectedExpressionPath}`);
    }
    if (visuals.productionReady) {
      assertFileWithinLimit(assetPath, limits.expressionBytes);
    }
  }

  if (visuals.productionReady) {
    assertFileWithinLimit(visuals.pawn, limits.pawnBytes);
    assertFileWithinLimit(visuals.portrait, limits.portraitBytes);
    assertFileWithinLimit(visuals.fullBody, limits.fullBodyBytes);
  }

  const audio = character?.audio;
  if (!audio || typeof audio.productionReady !== "boolean") {
    fail(`${id}.audio.productionReady must be boolean`);
  }
  if (audio.root !== expected.audioRoot) {
    fail(`${id}.audio.root must be ${expected.audioRoot}`);
  }

  if (audio.productionReady) {
    const audioDir = path.join(assetsRoot, ...audio.root.split("/"));
    if (!fs.existsSync(audioDir)) fail(`audio directory missing for ${id}`);
    const files = fs.readdirSync(audioDir).filter((name) => name.endsWith(".ogg"));
    if (files.length === 0) fail(`no OGG voice files found for production-ready ${id}`);
    for (const file of files) {
      const match = /^([a-z][a-z0-9_]*)_(\d{2})\.ogg$/.exec(file);
      if (!match || !expectedVoiceCues.includes(match[1])) {
        fail(`invalid voice filename for ${id}: ${file}`);
      }
      assertFileWithinLimit(`${audio.root}/${file}`, limits.voiceBytes);
    }
  }
}

if (!sameSet([...seenIds], expectedStarterIds)) {
  fail(`starter character ids must be: ${expectedStarterIds.join(", ")}`);
}

console.log(
  "Ludo Paws asset gate passed: catalog, starter fallbacks, paths and budgets are valid.",
);
