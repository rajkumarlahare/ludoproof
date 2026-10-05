package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlinePendingRollRecoveryPolicyTest {
    private val secret =
        PendingRollSecret(
            matchId = "LPABCDEFGH",
            clientSeed = "a".repeat(64),
            clientCommitment = "b".repeat(64),
        )

    @Test
    fun cachedStateNeverClearsSecretEvenWhenSnapshotIsBehind() {
        assertEquals(
            PendingRollSecretAction.KEEP,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.CACHE,
                secret = secret,
                state = snapshot(pending = null),
            ),
        )
    }

    @Test
    fun authoritativeStateWithoutPendingRollClearsStaleSecret() {
        assertEquals(
            PendingRollSecretAction.CLEAR,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(pending = null),
            ),
        )
    }

    @Test
    fun creatingRollWithoutEchoedCommitmentKeepsSecret() {
        assertEquals(
            PendingRollSecretAction.KEEP,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(pending = pending(status = "CREATING", commitment = null)),
            ),
        )
    }

    @Test
    fun committedRollWithMatchingCommitmentKeepsSecret() {
        assertEquals(
            PendingRollSecretAction.KEEP,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(
                    pending = pending(
                        status = "COMMITTED",
                        commitment = secret.clientCommitment,
                    ),
                ),
            ),
        )
    }

    @Test
    fun mismatchedAuthoritativeCommitmentClearsSecret() {
        assertEquals(
            PendingRollSecretAction.CLEAR,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(
                    pending = pending(
                        status = "COMMITTED",
                        commitment = "c".repeat(64),
                    ),
                ),
            ),
        )
    }

    @Test
    fun resolvedRollClearsSecretBecauseRevealIsComplete() {
        assertEquals(
            PendingRollSecretAction.CLEAR,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(
                    pending = pending(
                        status = "RESOLVED",
                        commitment = secret.clientCommitment,
                        outcome = 4,
                        legal = setOf(0),
                    ),
                ),
            ),
        )
    }

    @Test
    fun differentMatchAndFinishedStateClearSecret() {
        assertEquals(
            PendingRollSecretAction.CLEAR,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(matchId = "LPBCDEFGHJ", pending = null),
            ),
        )
        assertEquals(
            PendingRollSecretAction.CLEAR,
            OnlinePendingRollRecoveryPolicy.action(
                source = OnlineStateSource.AUTHORITATIVE,
                secret = secret,
                state = snapshot(status = "FINISHED", pending = null),
            ),
        )
    }

    @Test
    fun cachedRestoreRequiresSameSecureSessionMatch() {
        assertTrue(
            OnlineCachedStateRestorePolicy.shouldApply(
                sessionMatchId = "LPABCDEFGH",
                cachedMatchId = "LPABCDEFGH",
            ),
        )
        assertFalse(
            OnlineCachedStateRestorePolicy.shouldApply(
                sessionMatchId = "LPABCDEFGH",
                cachedMatchId = "LPBCDEFGHJ",
            ),
        )
        assertFalse(
            OnlineCachedStateRestorePolicy.shouldApply(
                sessionMatchId = null,
                cachedMatchId = "LPABCDEFGH",
            ),
        )
    }

    private fun snapshot(
        matchId: String = "LPABCDEFGH",
        status: String = "ACTIVE",
        pending: PendingRollSnapshot?,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = matchId,
            status = status,
            hostPlayerId = "p1",
            targetPlayerCount = 2,
            matchMode = "ONLINE",
            players =
                listOf(
                    PlayerSnapshot(
                        playerId = "p1",
                        displayName = "Player 1",
                        color = "RED",
                        seat = 0,
                        tokens = listOf(-1, -1, -1, -1),
                    ),
                    PlayerSnapshot(
                        playerId = "p2",
                        displayName = "Player 2",
                        color = "GREEN",
                        seat = 1,
                        tokens = listOf(-1, -1, -1, -1),
                    ),
                ),
            turnSeat = 0,
            randomEventIndex = if (pending?.status == "CREATING") 0 else 1,
            pendingRoll = pending,
            winnerPlayerId = if (status == "FINISHED") "p1" else null,
            rulesetId = "ludoproof-standard-v3",
            history = emptyList(),
        )

    private fun pending(
        status: String,
        commitment: String?,
        outcome: Int? = null,
        legal: Set<Int> = emptySet(),
    ): PendingRollSnapshot =
        PendingRollSnapshot(
            status = status,
            seat = 0,
            eventIndex = 0,
            eventId = "roll:0",
            roundId = "round-0",
            serverCommitment = "d".repeat(64),
            clientCommitment = commitment,
            revealDeadlineAt = 123456789L,
            proofDigest = if (status == "RESOLVED") "e".repeat(64) else null,
            outcome = outcome,
            legalTokenIndexes = legal,
        )
}
