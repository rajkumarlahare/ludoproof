import test from "node:test";
import assert from "node:assert/strict";

import {
  deriveFriendIdentity,
  issueFriendRoomJoinToken,
  verifyFriendRoomJoinToken,
} from "../src/crypto.js";
import {
  addPlayer,
  newMatch,
  startMatch,
} from "../src/game.js";

const ENV = {
  LUDOPROOF_SESSION_HMAC_KEY:
    "lp_test_session_hmac_key_1234567890abcdef",
};

test(
  "friend identity is deterministic, recoverable, and secret-backed",
  async () => {
    const requestId =
      "00000000-0000-4000-8000-000000000001";
    const first =
      await deriveFriendIdentity(
        ENV,
        requestId,
      );
    const replay =
      await deriveFriendIdentity(
        ENV,
        requestId,
      );
    const other =
      await deriveFriendIdentity(
        ENV,
        "00000000-0000-4000-8000-000000000002",
      );

    assert.deepEqual(
      replay,
      first,
    );
    assert.match(
      first.friendId,
      /^LPF-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/,
    );
    assert.match(
      first.friendToken,
      /^lf_[A-Za-z0-9_-]{32,}$/,
    );
    assert.notEqual(
      first.friendId,
      other.friendId,
    );
    assert.notEqual(
      first.friendToken,
      other.friendToken,
    );
  },
);


test(
  "friend-room join credentials are signed, scoped, and expire",
  async () => {
    const claims = {
      matchId:
        "LPABCDEFGH",
      hostFriendId:
        "LPF-ABCD-EFGH-JKLM",
      friendId:
        "LPF-MNPQ-RSTU-VWXY",
      inviteId:
        "FIV-0123456789ABCDEF0123",
      expiresAt:
        10_000,
    };
    const token =
      await issueFriendRoomJoinToken(
        ENV,
        claims,
      );

    assert.match(
      token,
      /^lfj_[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/,
    );
    assert.deepEqual(
      await verifyFriendRoomJoinToken(
        ENV,
        token,
        9_000,
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
      verifyFriendRoomJoinToken(
        ENV,
        tampered,
        9_000,
      ),
      (error) =>
        error?.code ===
          "FRIEND_JOIN_TOKEN_INVALID",
    );

    await assert.rejects(
      verifyFriendRoomJoinToken(
        ENV,
        token,
        10_000,
      ),
      (error) =>
        error?.code ===
          "FRIEND_JOIN_TOKEN_EXPIRED",
    );
  },
);

test(
  "friend rooms require the exact requested seats before start",
  () => {
    const initial =
      newMatch({
        matchId:
          "LPABCDEFGH",
        hostPlayerId:
          "host",
        hostDisplayName:
          "Host",
        targetPlayerCount:
          3,
        matchMode:
          "FRIENDS",
        now:
          1,
      });

    const twoPlayers =
      addPlayer(
        initial,
        {
          playerId:
            "guest-1",
          displayName:
            "Guest One",
          tokenAuthHash:
            "hash-1",
          now:
            2,
        },
      );

    assert.throws(
      () =>
        startMatch(
          twoPlayers,
          "host",
          3,
        ),
      (error) =>
        error?.code ===
          "WAITING_FOR_PLAYERS",
    );

    const threePlayers =
      addPlayer(
        twoPlayers,
        {
          playerId:
            "guest-2",
          displayName:
            "Guest Two",
          tokenAuthHash:
            "hash-2",
          now:
            4,
        },
      );

    const started =
      startMatch(
        threePlayers,
        "host",
        5,
      );

    assert.equal(
      started.status,
      "ACTIVE",
    );
    assert.equal(
      started.players.length,
      3,
    );

    assert.throws(
      () =>
        addPlayer(
          threePlayers,
          {
            playerId:
              "guest-3",
            displayName:
              "Guest Three",
            tokenAuthHash:
              "hash-3",
            now:
              6,
          },
        ),
      (error) =>
        error?.code ===
          "MATCH_FULL",
    );
  },
);
