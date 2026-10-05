package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnlineTurnTransitionFeedbackPolicyTest {
    @Test
    fun nonSixWithoutLegalMoveExplainsTurnPass() {
        val previous = snapshot(turnSeat = 0, randomEventIndex = 7)
        val current =
            snapshot(
                turnSeat = 1,
                randomEventIndex = 8,
                history = listOf(rollEvent(eventIndex = 7, outcome = 4)),
            )

        assertEquals(
            "Player 1 rolled 4 • no legal move • Player 2's turn",
            OnlineTurnTransitionFeedbackPolicy.message(previous, current),
        )
    }

    @Test
    fun thirdNaturalSixExplainsForfeitedTurn() {
        val previous =
            snapshot(
                turnSeat = 0,
                randomEventIndex = 11,
                history =
                    listOf(
                        rollEvent(eventIndex = 9, outcome = 6, moveTokenIndex = 0),
                        rollEvent(eventIndex = 10, outcome = 6, moveTokenIndex = 1),
                    ),
            )
        val current =
            snapshot(
                turnSeat = 1,
                randomEventIndex = 12,
                history =
                    previous.history +
                        rollEvent(eventIndex = 11, outcome = 6),
            )

        assertEquals(
            "Player 1 rolled a third 6 • turn forfeited • Player 2's turn",
            OnlineTurnTransitionFeedbackPolicy.message(previous, current),
        )
    }

    @Test
    fun openingSixDoesNotMasqueradeAsThirdSix() {
        val previous =
            snapshot(
                turnSeat = 0,
                randomEventIndex = 2,
                history =
                    listOf(
                        rollEvent(
                            eventIndex = 0,
                            outcome = 6,
                            openingRollApplied = true,
                            moveTokenIndex = 0,
                        ),
                        rollEvent(eventIndex = 1, outcome = 6, moveTokenIndex = 1),
                    ),
            )
        val current =
            snapshot(
                turnSeat = 1,
                randomEventIndex = 3,
                history = previous.history + rollEvent(eventIndex = 2, outcome = 6),
            )

        assertEquals(
            "Player 1 rolled 6 • no legal move • Player 2's turn",
            OnlineTurnTransitionFeedbackPolicy.message(previous, current),
        )
    }

    @Test
    fun sixWithoutLegalMoveExplainsExtraRollWhenActorStays() {
        val previous = snapshot(turnSeat = 0, randomEventIndex = 3)
        val current =
            snapshot(
                turnSeat = 0,
                randomEventIndex = 4,
                history = listOf(rollEvent(eventIndex = 3, outcome = 6)),
            )

        assertEquals(
            "Player 1 rolled 6 • no legal move • roll again",
            OnlineTurnTransitionFeedbackPolicy.message(previous, current),
        )
    }

    @Test
    fun teamActingSeatNamesActualNextActor() {
        val previous = snapshot(turnSeat = 0, randomEventIndex = 4, matchMode = "TEAM_UP")
        val current =
            snapshot(
                turnSeat = 1,
                actingSeat = 3,
                randomEventIndex = 5,
                matchMode = "TEAM_UP",
                history = listOf(rollEvent(eventIndex = 4, outcome = 2)),
            )

        assertEquals(
            "Player 1 rolled 2 • no legal move • Player 4's turn",
            OnlineTurnTransitionFeedbackPolicy.message(previous, current),
        )
    }

    @Test
    fun duplicateStateRefreshDoesNotReplayMessage() {
        val previous = snapshot(turnSeat = 1, randomEventIndex = 8)
        val current =
            snapshot(
                turnSeat = 1,
                randomEventIndex = 8,
                history = listOf(rollEvent(eventIndex = 7, outcome = 4)),
            )

        assertNull(OnlineTurnTransitionFeedbackPolicy.message(previous, current))
    }

    @Test
    fun normalCompletedMoveDoesNotProduceAutomaticMessage() {
        val previous = snapshot(turnSeat = 0, randomEventIndex = 5)
        val current =
            snapshot(
                turnSeat = 1,
                randomEventIndex = 6,
                history =
                    listOf(
                        rollEvent(
                            eventIndex = 5,
                            outcome = 3,
                            moveTokenIndex = 0,
                        ),
                    ),
            )

        assertNull(OnlineTurnTransitionFeedbackPolicy.message(previous, current))
    }

    private fun snapshot(
        turnSeat: Int,
        randomEventIndex: Int,
        history: List<HistoryEventSnapshot> = emptyList(),
        actingSeat: Int? = null,
        matchMode: String = "ONLINE",
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "LPABCDEFGH",
            status = "ACTIVE",
            hostPlayerId = "p1",
            targetPlayerCount = 4,
            matchMode = matchMode,
            players =
                listOf(
                    player("p1", "Player 1", "RED", 0),
                    player("p2", "Player 2", "GREEN", 1),
                    player("p3", "Player 3", "YELLOW", 2),
                    player("p4", "Player 4", "BLUE", 3),
                ),
            turnSeat = turnSeat,
            randomEventIndex = randomEventIndex,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId =
                if (matchMode == "TEAM_UP") {
                    "ludoproof-team-v3"
                } else {
                    "ludoproof-standard-v3"
                },
            history = history,
            actingSeat = actingSeat,
            teamAssignments =
                if (matchMode == "TEAM_UP") listOf("A", "B", "A", "B") else emptyList(),
        )

    private fun player(
        id: String,
        name: String,
        color: String,
        seat: Int,
    ) =
        PlayerSnapshot(
            playerId = id,
            displayName = name,
            color = color,
            seat = seat,
            tokens = listOf(-1, -1, -1, -1),
            teamId =
                when (seat) {
                    0, 2 -> "A"
                    else -> "B"
                },
        )

    private fun rollEvent(
        eventIndex: Int,
        outcome: Int,
        moveTokenIndex: Int? = null,
        openingRollApplied: Boolean = false,
    ) =
        HistoryEventSnapshot(
            eventIndex = eventIndex,
            playerId = "p1",
            roundId = "round-$eventIndex",
            proofDigest = "proof-$eventIndex",
            outcome = outcome,
            moveTokenIndex = moveTokenIndex,
            captures = 0,
            effectiveOutcome = outcome,
            openingRollApplied = openingRollApplied,
        )
}
