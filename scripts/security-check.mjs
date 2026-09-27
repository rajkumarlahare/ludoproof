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
