import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";

const roots = [
  "android/app/build/outputs/apk/release",
  "android/app/build/outputs/bundle/release",
];

const files = [];
for (const root of roots) {
  collect(root);
}

const artifacts =
  files
    .filter((file) =>
      /\.(?:apk|aab)$/i.test(file),
    )
    .sort()
    .map((file) => ({
      path: file.replaceAll("\\", "/"),
      bytes:
        fs.statSync(file).size,
      sha256:
        sha256File(file),
    }));

if (artifacts.length < 2) {
  throw new Error(
    "Expected release APK and AAB artifacts",
  );
}

const appBuild =
  fs.readFileSync(
    "android/app/build.gradle.kts",
    "utf8",
  );
const versionName =
  appBuild.match(
    /versionName\s*=\s*"([^"]+)"/,
  )?.[1] ?? "unknown";
const versionCode =
  Number(
    appBuild.match(
      /versionCode\s*=\s*(\d+)/,
    )?.[1] ?? 0,
  );

const metadata = {
  schemaVersion: 1,
  product: "LudoProof",
  releaseChannel:
    "release-candidate",
  versionName,
  versionCode,
  gitCommit:
    process.env.GITHUB_SHA ??
    "local",
  generatedAt:
    new Date()
      .toISOString(),
  signing: {
    repositoryContainsSigningKey:
      false,
    ciArtifactIsStoreSigned:
      false,
    note:
      "Store/upload signing remains external to the repository and CI artifact.",
  },
  artifacts,
};

const output =
  JSON.stringify(
    metadata,
    null,
    2,
  ) + "\n";
fs.writeFileSync(
  "ludoproof-android-release.json",
  output,
);

const sums =
  artifacts
    .map(
      (artifact) =>
        artifact.sha256 +
        "  " +
        artifact.path,
    )
    .join("\n") +
  "\n";
fs.writeFileSync(
  "ludoproof-android-release.sha256",
  sums,
);

console.log(
  "Generated Android release evidence for " +
    artifacts.length +
    " artifacts.",
);

function collect(current) {
  if (!fs.existsSync(current)) {
    return;
  }
  const stat =
    fs.statSync(current);
  if (stat.isFile()) {
    files.push(current);
    return;
  }
  for (
    const entry of
    fs.readdirSync(current)
  ) {
    collect(
      path.join(
        current,
        entry,
      ),
    );
  }
}

function sha256File(file) {
  return createHash(
    "sha256",
  )
    .update(
      fs.readFileSync(file),
    )
    .digest("hex");
}
