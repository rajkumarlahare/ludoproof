import test from "node:test";
import assert from "node:assert/strict";

import {
  deriveLeaderboardIdentity,
  issueLeaderboardProfileAssertion,
  normalizeDisplayName,
  verifyLeaderboardProfileAssertion,
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


test(
  "leaderboard identity is deterministic, secret-backed, and UUID v4 shaped",
  async () => {
    const env = {
      LUDOPROOF_SESSION_HMAC_KEY:
        "lp_test_session_hmac_key_1234567890abcdef",
    };
    const requestId =
      "00000000-0000-4000-8000-000000000321";
    const first =
      await deriveLeaderboardIdentity(
        env,
        requestId,
      );
    const replay =
      await deriveLeaderboardIdentity(
        env,
        requestId,
      );
    const other =
      await deriveLeaderboardIdentity(
        env,
        "00000000-0000-4000-8000-000000000322",
      );

    assert.deepEqual(
      replay,
      first,
    );
    assert.match(
      first.profileId,
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
    );
    assert.match(
      first.profileToken,
      /^lpp_[A-Za-z0-9_-]{32,}$/,
    );
    assert.notEqual(
      first.profileId,
      requestId,
    );
    assert.notEqual(
      first.profileId,
      other.profileId,
    );
    assert.notEqual(
      first.profileToken,
      other.profileToken,
    );
  },
);

test(
  "matchmaking profile assertions are signed, scoped, and expire",
  async () => {
    const env = {
      LUDOPROOF_SESSION_HMAC_KEY:
        "lp_test_session_hmac_key_1234567890abcdef",
    };
    const claims = {
      matchId:
        "LPABCDEFGH",
      profileId:
        "550e8400-e29b-41d4-a716-446655440000",
      clientRequestId:
        "00000000-0000-4000-8000-000000000111",
      expiresAt:
        20_000,
    };
    const token =
      await issueLeaderboardProfileAssertion(
        env,
        claims,
      );

    assert.match(
      token,
      /^lpa_[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/,
    );
    assert.deepEqual(
      await verifyLeaderboardProfileAssertion(
        env,
        token,
        19_000,
      ),
      claims,
    );

    const tampered =
      token.slice(0, -1) +
      (
        token.endsWith("A")
          ? "B"
          : "A"
      );
    await assert.rejects(
      verifyLeaderboardProfileAssertion(
        env,
        tampered,
        19_000,
      ),
      (error) =>
        error?.code ===
          "PROFILE_ASSERTION_INVALID",
    );

    await assert.rejects(
      verifyLeaderboardProfileAssertion(
        env,
        token,
        20_000,
      ),
      (error) =>
        error?.code ===
          "PROFILE_ASSERTION_EXPIRED",
    );

    const unrankedClaims = {
      matchId:
        "LPABCDEFGH",
      profileId:
        null,
      clientRequestId:
        "00000000-0000-4000-8000-000000000112",
      expiresAt:
        30_000,
    };
    const unrankedToken =
      await issueLeaderboardProfileAssertion(
        env,
        unrankedClaims,
      );
    assert.deepEqual(
      await verifyLeaderboardProfileAssertion(
        env,
        unrankedToken,
        29_000,
      ),
      unrankedClaims,
      "matchmaker must be able to authorize a legacy seat without creating leaderboard identity",
    );
  },
);
