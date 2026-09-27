import test from "node:test";
import assert from "node:assert/strict";

import {
  normalizeDisplayName,
} from "../src/crypto.js";

test("display names are NFC-normalized and whitespace-collapsed", () => {
  assert.equal(
    normalizeDisplayName(
      "  Jose\u0301   Player  ",
    ),
    "José Player",
  );
});

test("display names count Unicode code points rather than UTF-16 units", () => {
  assert.equal(
    normalizeDisplayName("😀 Player"),
    "😀 Player",
  );
});

test("display names reject bidi and invisible spoofing controls", () => {
  for (const value of [
    "Alice\u202EBob",
    "Alice\u2066Bob",
    "Alice\u200BBob",
    "Alice\u061CBob",
    "Alice\uFEFFBob",
  ]) {
    assert.throws(
      () =>
        normalizeDisplayName(value),
      (error) =>
        error.code ===
        "INVALID_DISPLAY_NAME",
    );
  }
});

test("display names keep legitimate zero-width joiner emoji sequences", () => {
  assert.equal(
    normalizeDisplayName(
      "👨‍👩‍👧 Family",
    ),
    "👨‍👩‍👧 Family",
  );
});
