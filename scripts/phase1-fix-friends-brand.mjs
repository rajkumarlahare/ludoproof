import fs from "node:fs";

const file = "android/app/src/main/java/com/ludoproof/game/feature/friends/presentation/FriendsActivity.kt";
let source = fs.readFileSync(file, "utf8");

const replacements = [
  ["LudoProof Friend ID", "Ludo Paws Friend ID", 2],
  ["Add me on LudoProof\\nFriend ID: $id", "Add me on Ludo Paws\\nFriend ID: $id", 1],
  ["Join my private LudoProof room: $matchId", "Join my private Ludo Paws room: $matchId", 1],
];

for (const [from, to, expectedCount] of replacements) {
  const count = source.split(from).length - 1;
  if (count !== expectedCount) {
    throw new Error(`Expected ${expectedCount} occurrence(s) of ${JSON.stringify(from)}, found ${count}`);
  }
  source = source.split(from).join(to);
}

if (source.includes("LudoProof")) {
  const lines = source
    .split(/\r?\n/)
    .map((line, index) => ({ line, number: index + 1 }))
    .filter(({ line }) => line.includes("LudoProof"));
  const consumerStrings = lines.filter(({ line }) => line.includes('"'));
  if (consumerStrings.length > 0) {
    throw new Error(
      `Legacy LudoProof text remains in FriendsActivity string-bearing lines:\n${consumerStrings
        .map(({ number, line }) => `${number}: ${line.trim()}`)
        .join("\n")}`,
    );
  }
}

fs.writeFileSync(file, source, "utf8");
console.log("FriendsActivity Ludo Paws brand replacements applied.");
