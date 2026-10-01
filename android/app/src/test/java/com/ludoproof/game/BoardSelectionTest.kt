package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardSelectionTest {
    private val roll = PendingRollSnapshot(
        status = "RESOLVED", seat = 0, eventIndex = 1,
        eventId = null, roundId = null, serverCommitment = null,
        clientCommitment = null, revealDeadlineAt = null,
        proofDigest = null, outcome = 6, legalTokenIndexes = setOf(0, 2),
    )
    private val state = MatchSnapshot(
        matchId = "test", status = "ACTIVE", hostPlayerId = "red",
        players = listOf(
            PlayerSnapshot("red", "Red", "RED", 0, listOf(-1, -1, -1, -1)),
            PlayerSnapshot("blue", "Blue", "BLUE", 1, listOf(-1, -1, -1, -1)),
        ),
        turnSeat = 0, randomEventIndex = 1, pendingRoll = roll,
        winnerPlayerId = null, rulesetId = "v1", history = emptyList(),
    )

    @Test fun onlyThePlayerWhoOwnsTheResolvedRollCanSelectTokens() {
        assertEquals(setOf(0, 2), selectableTokenIndexes(state, "red"))
        assertTrue(selectableTokenIndexes(state, "blue").isEmpty())
        assertTrue(selectableTokenIndexes(state, null).isEmpty())
        assertTrue(selectableTokenIndexes(state, "spectator").isEmpty())
    }

    @Test fun incompleteOrStaleRollsCannotEnableSelection() {
        assertTrue(selectableTokenIndexes(state.copy(pendingRoll = null), "red").isEmpty())
        assertTrue(selectableTokenIndexes(state.copy(pendingRoll = roll.copy(status = "COMMITTED")), "red").isEmpty())
        assertTrue(selectableTokenIndexes(state.copy(pendingRoll = roll.copy(seat = 1)), "red").isEmpty())
        assertTrue(selectableTokenIndexes(state.copy(turnSeat = 1), "red").isEmpty())
    }

    @Test fun inactiveBoardsAndInvalidIndexesAreNotSelectable() {
        assertTrue(selectableTokenIndexes(null, "red").isEmpty())
        for (status in listOf("WAITING", "FINISHED")) {
            assertTrue(selectableTokenIndexes(state.copy(status = status), "red").isEmpty())
        }
        assertEquals(setOf(0), selectableTokenIndexes(
            state.copy(pendingRoll = roll.copy(legalTokenIndexes = setOf(-1, 0, 4))), "red",
        ))
    }
}
