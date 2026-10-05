package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnlineVerifiedDicePresentationPolicyTest {
    @Test
    fun committedPendingNeverBorrowsPreviousVerifiedHistory() {
        val state =
            state(
                pending = pending(status = "COMMITTED", eventIndex = 8),
                history = listOf(history(eventIndex = 7, outcome = 6, proofDigest = "old-proof")),
            )

        assertNull(OnlineVerifiedDicePresentationPolicy.resolve(state))
    }

    @Test
    fun resolvingPendingNeverBorrowsPreviousVerifiedHistory() {
        val state =
            state(
                pending = pending(status = "RESOLVING", eventIndex = 8),
                history = listOf(history(eventIndex = 7, outcome = 5, proofDigest = "old-proof")),
            )

        assertNull(OnlineVerifiedDicePresentationPolicy.resolve(state))
    }

    @Test
    fun resolvedPendingUsesOnlyItsOwnOutcomeAndProof() {
        val state =
            state(
                pending =
                    pending(
                        status = "RESOLVED",
                        eventIndex = 8,
                        outcome = 2,
                        proofDigest = "new-proof",
                        openingRollApplied = true,
                    ),
                history = listOf(history(eventIndex = 7, outcome = 6, proofDigest = "old-proof")),
            )

        val presentation = requireNotNull(OnlineVerifiedDicePresentationPolicy.resolve(state))
        assertEquals(2, presentation.outcome)
        assertEquals("new-proof", presentation.proofDigest)
        assertEquals(8, presentation.eventIndex)
        assertEquals(true, presentation.openingRollApplied)
        assertEquals("LPABCDEFGH:8", presentation.eventKey)
    }

    @Test
    fun resolvedPendingWithoutProofFailsClosedInsteadOfUsingHistory() {
        val state =
            state(
                pending =
                    pending(
                        status = "RESOLVED",
                        eventIndex = 8,
                        outcome = 3,
                        proofDigest = null,
                    ),
                history = listOf(history(eventIndex = 7, outcome = 6, proofDigest = "old-proof")),
            )

        assertNull(OnlineVerifiedDicePresentationPolicy.resolve(state))
    }

    @Test
    fun noPendingUsesLatestVerifiedHistory() {
        val state =
            state(
                pending = null,
                history =
                    listOf(
                        history(eventIndex = 6, outcome = 1, proofDigest = "older-proof"),
                        history(
                            eventIndex = 7,
                            outcome = 4,
                            effectiveOutcome = 5,
                            proofDigest = "latest-proof",
                        ),
                    ),
            )

        val presentation = requireNotNull(OnlineVerifiedDicePresentationPolicy.resolve(state))
        assertEquals(5, presentation.outcome)
        assertEquals("latest-proof", presentation.proofDigest)
        assertEquals(7, presentation.eventIndex)
        assertEquals("LPABCDEFGH:7", presentation.eventKey)
    }

    @Test
    fun eventKeyIsStableForRefreshAndChangesForNextEventEvenWithSameOutcome() {
        val first =
            requireNotNull(
                OnlineVerifiedDicePresentationPolicy.resolve(
                    state(
                        pending = pending("RESOLVED", 8, outcome = 4, proofDigest = "proof-8"),
                    ),
                ),
            )
        val refreshed =
            requireNotNull(
                OnlineVerifiedDicePresentationPolicy.resolve(
                    state(
                        pending = pending("RESOLVED", 8, outcome = 4, proofDigest = "proof-8"),
                    ),
                ),
            )
        val next =
            requireNotNull(
                OnlineVerifiedDicePresentationPolicy.resolve(
                    state(
                        pending = pending("RESOLVED", 9, outcome = 4, proofDigest = "proof-9"),
                    ),
                ),
            )

        assertEquals(first.eventKey, refreshed.eventKey)
        assertNotEquals(first.eventKey, next.eventKey)
    }

    private fun state(
        pending: PendingRollSnapshot?,
        history: List<HistoryEventSnapshot> = emptyList(),
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "LPABCDEFGH",
            status = "ACTIVE",
            hostPlayerId = "p0",
            players =
                listOf(
                    PlayerSnapshot("p0", "Player 0", "RED", 0, listOf(-1, -1, -1, -1)),
                    PlayerSnapshot("p1", "Player 1", "GREEN", 1, listOf(-1, -1, -1, -1)),
                ),
            turnSeat = 0,
            randomEventIndex = 10,
            pendingRoll = pending,
            winnerPlayerId = null,
            rulesetId = "ludoproof-standard-v1",
            history = history,
        )

    private fun pending(
        status: String,
        eventIndex: Int,
        outcome: Int? = null,
        proofDigest: String? = null,
        openingRollApplied: Boolean = false,
    ): PendingRollSnapshot =
        PendingRollSnapshot(
            status = status,
            seat = 0,
            eventIndex = eventIndex,
            eventId = "roll:$eventIndex",
            roundId = "round-$eventIndex",
            serverCommitment = "server-$eventIndex",
            clientCommitment = "client-$eventIndex",
            revealDeadlineAt = 123456789L,
            proofDigest = proofDigest,
            outcome = outcome,
            legalTokenIndexes = if (status == "RESOLVED") setOf(0) else emptySet(),
            openingRollApplied = openingRollApplied,
        )

    private fun history(
        eventIndex: Int,
        outcome: Int,
        proofDigest: String,
        effectiveOutcome: Int? = null,
    ): HistoryEventSnapshot =
        HistoryEventSnapshot(
            eventIndex = eventIndex,
            playerId = "p0",
            roundId = "round-$eventIndex",
            proofDigest = proofDigest,
            outcome = outcome,
            moveTokenIndex = 0,
            captures = 0,
            effectiveOutcome = effectiveOutcome,
        )
}
