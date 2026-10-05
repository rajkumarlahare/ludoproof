package com.ludoproof.game

import com.ludoproof.game.feature.online.OnlineRollActionKind
import com.ludoproof.game.feature.online.OnlineRollActionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineRollActionPolicyTest {
    @Test
    fun freshTurnStartsNewVerifiedRoll() {
        val decision = decision()
        assertEquals(OnlineRollActionKind.NEW_ROLL, decision.kind)
        assertTrue(decision.enabled)
        assertEquals("ROLL VERIFIED DICE", decision.label)
    }

    @Test
    fun creatingRoundWithMatchingSecretResumesCommitThenReveal() {
        val decision =
            decision(
                pendingStatus = "CREATING",
                remoteCommitment = "commit-a",
                localCommitment = "commit-a",
            )
        assertEquals(OnlineRollActionKind.RESUME_COMMIT_THEN_REVEAL, decision.kind)
        assertTrue(decision.enabled)
    }

    @Test
    fun committedRoundWithMatchingSecretResumesRevealOnly() {
        val decision =
            decision(
                pendingStatus = "COMMITTED",
                remoteCommitment = "commit-a",
                localCommitment = "commit-a",
            )
        assertEquals(OnlineRollActionKind.RESUME_REVEAL_ONLY, decision.kind)
        assertTrue(decision.enabled)
    }

    @Test
    fun resolvingRoundWithMatchingSecretRetriesRevealOnly() {
        val decision =
            decision(
                pendingStatus = "RESOLVING",
                remoteCommitment = "commit-a",
                localCommitment = "commit-a",
            )
        assertEquals(OnlineRollActionKind.RESUME_REVEAL_ONLY, decision.kind)
        assertTrue(decision.enabled)
    }

    @Test
    fun lockedRoundWithoutLocalSecretWaitsInsteadOfStartingAnotherRoll() {
        val decision =
            decision(
                pendingStatus = "COMMITTED",
                remoteCommitment = "commit-a",
                localCommitment = null,
            )
        assertEquals(OnlineRollActionKind.WAIT_FOR_RECOVERY, decision.kind)
        assertFalse(decision.enabled)
    }

    @Test
    fun commitmentMismatchFailsClosed() {
        val decision =
            decision(
                pendingStatus = "CREATING",
                remoteCommitment = "commit-server",
                localCommitment = "commit-other",
            )
        assertEquals(OnlineRollActionKind.WAIT_FOR_RECOVERY, decision.kind)
        assertFalse(decision.enabled)
    }

    @Test
    fun resolvedRoundRequiresMoveNotAnotherRoll() {
        val decision = decision(pendingStatus = "RESOLVED")
        assertEquals(OnlineRollActionKind.MOVE_REQUIRED, decision.kind)
        assertFalse(decision.enabled)
        assertEquals("MOVE A TOKEN", decision.label)
    }

    @Test
    fun offlineConnectionDisablesRoll() {
        val decision = decision(isOnline = false)
        assertEquals(OnlineRollActionKind.DISABLED, decision.kind)
        assertFalse(decision.enabled)
    }

    @Test
    fun opponentTurnDisablesRoll() {
        val decision = decision(myTurn = false)
        assertEquals(OnlineRollActionKind.DISABLED, decision.kind)
        assertFalse(decision.enabled)
    }

    private fun decision(
        isOnline: Boolean = true,
        myTurn: Boolean = true,
        pendingStatus: String? = null,
        remoteCommitment: String? = null,
        localCommitment: String? = null,
    ) =
        OnlineRollActionPolicy.resolve(
            isOnline = isOnline,
            matchStatus = "ACTIVE",
            myTurn = myTurn,
            pendingStatus = pendingStatus,
            remoteClientCommitment = remoteCommitment,
            currentMatchId = "LPTESTMATCH",
            localSecretMatchId = if (localCommitment != null) "LPTESTMATCH" else null,
            localClientCommitment = localCommitment,
        )
}
