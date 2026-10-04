package com.ludoproof.game.feature.offline.presentation.feedback

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PendingRollSnapshot
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFeedbackLedger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFeedbackIdempotencyTest {
    @Test
    fun rollAndMoveHaveDistinctKeysForSameFairnessEvent() {
        val rolled =
            snapshot(
                pendingEventIndex = 7,
                history = emptyList(),
            )
        val moved =
            snapshot(
                pendingEventIndex = null,
                history = listOf(moveHistory(eventIndex = 7)),
            )

        assertEquals(
            "ROLL:7",
            OfflineLudoPawsFeedbackPolicy.committedActionKey(
                current = rolled,
                action = OfflineFeedbackAction.ROLL,
            ),
        )
        assertEquals(
            "MOVE:7",
            OfflineLudoPawsFeedbackPolicy.committedActionKey(
                current = moved,
                action = OfflineFeedbackAction.MOVE,
            ),
        )
    }

    @Test
    fun duplicatedOfflineCallbackIsSuppressedBySharedLedger() {
        val ledger = LudoPawsFeedbackLedger()
        val state =
            snapshot(
                pendingEventIndex = null,
                history = listOf(moveHistory(eventIndex = 11)),
            )
        val actionKey =
            requireNotNull(
                OfflineLudoPawsFeedbackPolicy.committedActionKey(
                    current = state,
                    action = OfflineFeedbackAction.MOVE,
                ),
            )

        assertTrue(
            ledger.once(
                matchId = state.matchId,
                key = "OFFLINE:$actionKey",
            ),
        )
        assertFalse(
            ledger.once(
                matchId = state.matchId,
                key = "OFFLINE:$actionKey",
            ),
        )
    }

    @Test
    fun rollWithoutPendingStateFallsBackToRecordedHistoryEvent() {
        val state =
            snapshot(
                pendingEventIndex = null,
                history = listOf(moveHistory(eventIndex = 3)),
            )

        assertEquals(
            "ROLL:3",
            OfflineLudoPawsFeedbackPolicy.committedActionKey(
                current = state,
                action = OfflineFeedbackAction.ROLL,
            ),
        )
    }

    private fun snapshot(
        pendingEventIndex: Int?,
        history: List<HistoryEventSnapshot>,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "local-match-a",
            status = "ACTIVE",
            hostPlayerId = "p1",
            players = emptyList(),
            turnSeat = 0,
            randomEventIndex = 12,
            pendingRoll =
                pendingEventIndex?.let {
                    PendingRollSnapshot(
                        status = "RESOLVED",
                        seat = 0,
                        eventIndex = it,
                        eventId = "roll:$it",
                        roundId = null,
                        serverCommitment = null,
                        clientCommitment = null,
                        revealDeadlineAt = null,
                        proofDigest = null,
                        outcome = 6,
                        legalTokenIndexes = setOf(0),
                    )
                },
            winnerPlayerId = null,
            rulesetId = "offline-local-v4",
            history = history,
        )

    private fun moveHistory(
        eventIndex: Int,
    ): HistoryEventSnapshot =
        HistoryEventSnapshot(
            eventIndex = eventIndex,
            playerId = "p1",
            roundId = null,
            proofDigest = null,
            outcome = 4,
            moveTokenIndex = 0,
            captures = 1,
        )
}
