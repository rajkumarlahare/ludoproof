import fs from "node:fs";
import path from "node:path";

const roots = [
  "server/src",
  "android/app/src/main",
  ".github/workflows",
];

const findings = [];
const files = [];

for (const root of roots) {
  walk(root);
}

for (const file of files.sort()) {
  const content =
    fs.readFileSync(
      file,
      "utf8",
    );

  for (const rule of [
    [
      "non-cryptographic RNG",
      /\bMath\.random\s*\(/g,
    ],
    [
      "dynamic eval",
      /(^|[^A-Za-z0-9_$])eval\s*\(/g,
    ],
    [
      "dynamic Function constructor",
      /\bnew\s+Function\s*\(/g,
    ],
    [
      "cleartext Android traffic",
      /cleartextTrafficPermitted\s*=\s*"true"/g,
    ],
  ]) {
    for (
      const match of
      content.matchAll(rule[1])
    ) {
      findings.push(
        file +
          ":" +
          lineFor(
            content,
            match.index ?? 0,
          ) +
          ": " +
          rule[0],
      );
    }
  }

  if (
    file.startsWith(
      ".github/workflows/",
    )
  ) {
    for (
      const match of
      content.matchAll(
        /^\s*uses:\s+([^\s]+)\s*$/gm,
      )
    ) {
      const value =
        match[1];
      if (
        !/@[0-9a-f]{40}(?:\s*#.*)?$/i.test(
          value,
        )
      ) {
        findings.push(
          file +
            ":" +
            lineFor(
              content,
              match.index ?? 0,
            ) +
            ": GitHub action is not pinned to a 40-character commit SHA",
        );
      }
    }
  }
}

const gitignore =
  fs.readFileSync(
    ".gitignore",
    "utf8",
  );
for (const pattern of [
  "*.jks",
  "*.keystore",
  "key.properties",
  "keystore.properties",
]) {
  if (!gitignore.includes(pattern)) {
    findings.push(
      ".gitignore: missing Android signing-secret pattern " +
        pattern,
    );
  }
}

const serverPackage =
  JSON.parse(
    fs.readFileSync(
      "server/package.json",
      "utf8",
    ),
  );
const androidBuild =
  fs.readFileSync(
    "android/app/build.gradle.kts",
    "utf8",
  );
const androidVersion =
  androidBuild.match(
    /versionName\s*=\s*"([^"]+)"/,
  )?.[1] ?? null;
if (
  !androidVersion ||
  serverPackage.version !==
    androidVersion
) {
  findings.push(
    "release version drift between server and Android",
  );
}

for (const file of [
  "android/app/src/main/java/com/ludoproof/game/PendingRollStore.kt",
  "android/app/src/main/java/com/ludoproof/game/SecureSessionStore.kt",
]) {
  const content =
    fs.readFileSync(
      file,
      "utf8",
    );
  if (
    !content.includes(
      "cipher.updateAAD(",
    )
  ) {
    findings.push(
      file +
        ": encrypted metadata is not AAD-bound",
    );
  }
}

const ci =
  fs.readFileSync(
    ".github/workflows/ci.yml",
    "utf8",
  );
for (const invariant of [
  ":app:testDebugUnitTest",
  ":app:bundleRelease",
  "hash-android-release.mjs",
]) {
  if (!ci.includes(invariant)) {
    findings.push(
      ".github/workflows/ci.yml: missing release gate " +
        invariant,
    );
  }
}

const deployWorkflowPath =
  ".github/workflows/deploy-cloudflare.yml";
if (
  fs.existsSync(
    deployWorkflowPath,
  )
) {
  const deployWorkflow =
    fs.readFileSync(
      deployWorkflowPath,
      "utf8",
    );
  for (const invariant of [
    "CLOUDFLARE_DEPLOY_ENABLED",
    "CLOUDFLARE_API_TOKEN",
    "CLOUDFLARE_ACCOUNT_ID",
    "ENTRONEX_API_TOKEN",
    "LUDOPROOF_SESSION_HMAC_KEY",
    "ENTRONEX_SIGNING_KEY_FINGERPRINT",
    "ENTRONEX_SIGNING_PUBLIC_KEY_PEM_B64",
    "cloudflare-deploy-preflight.mjs",
    "/health",
    "/ready",
  ]) {
    if (
      !deployWorkflow.includes(
        invariant,
      )
    ) {
      findings.push(
        deployWorkflowPath +
          ": missing production deploy invariant " +
          invariant,
      );
    }
  }
}

if (findings.length > 0) {
  console.error(
    "LudoProof security gate failed:",
  );
  for (const finding of findings) {
    console.error(
      "- " + finding,
    );
  }
  process.exit(2);
}

console.log(
  "LudoProof security gate passed (" +
    files.length +
    " files inspected).",
);

function walk(current) {
  if (!fs.existsSync(current)) {
    return;
  }
  const stat =
    fs.statSync(current);
  if (stat.isFile()) {
    if (
      /\.(?:js|mjs|kt|xml|yml|yaml)$/.test(
        current,
      )
    ) {
      files.push(
        current.replaceAll(
          "\\",
          "/",
        ),
      );
    }
    return;
  }

  for (
    const entry of
    fs.readdirSync(
      current,
      {
        withFileTypes: true,
      },
    )
  ) {
    if (
      entry.name ===
        "build" ||
      entry.name ===
        ".gradle"
    ) {
      continue;
    }
    walk(
      path.join(
        current,
        entry.name,
      ),
    );
  }
}

function lineFor(
  content,
  offset,
) {
  return (
    content
      .slice(0, offset)
      .split("\n")
      .length
  );
}
