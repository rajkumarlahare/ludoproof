package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PendingRollSnapshot
import com.ludoproof.game.PlayerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsGameMomentDetectorTest {
    @Test
    fun detectsTurnRollAndSixFromSnapshotDiff() {
        val previous = state(turnSeat = 0)
        val current =
            state(
                turnSeat = 1,
                history = listOf(event(7, "p2", 6)),
                pending = pending(1, 7, 6),
            )

        val moments = LudoPawsGameMomentDetector.detect(previous, current)
        assertTrue(moments.any { it.type == GameMomentType.TURN_STARTED && it.playerId == "p2" })
        assertTrue(moments.any { it.type == GameMomentType.ROLL_STARTED && it.eventIndex == 7 })
        assertTrue(moments.any { it.type == GameMomentType.SIX_ROLLED && it.value == 6 })
    }

    @Test
    fun detectsYardExitMoveSafeHomeAndCapturePair() {
        val captureEvent = event(3, "p1", 6)
        val beforeCapture =
            state(
                players =
                    listOf(
                        player("p1", "RED", 0, listOf(-1, 5, 56, -1)),
                        player("p2", "GREEN", 1, listOf(47, -1, -1, -1)),
                    ),
                history = listOf(captureEvent),
                pending = pending(0, 3, 6, setOf(1, 2)),
            )
        val afterCapture =
            state(
                players =
                    listOf(
                        player("p1", "RED", 0, listOf(0, 8, 57, -1)),
                        player("p2", "GREEN", 1, listOf(-1, -1, -1, -1)),
                    ),
                history =
                    listOf(
                        captureEvent.copy(
                            moveTokenIndex = 1,
                            captures = 1,
                        ),
                    ),
            )

        val moments = LudoPawsGameMomentDetector.detect(beforeCapture, afterCapture)
        assertTrue(moments.any { it.type == GameMomentType.TOKEN_LEFT_YARD && it.tokenIndex == 0 })
        assertTrue(moments.any { it.type == GameMomentType.TOKEN_MOVED && it.tokenIndex == 1 })
        assertTrue(moments.any { it.type == GameMomentType.CAPTURE_MADE && it.playerId == "p1" })
        assertTrue(moments.any { it.type == GameMomentType.TOKEN_CAPTURED && it.playerId == "p2" })
        assertTrue(moments.any { it.type == GameMomentType.SAFE_REACHED && it.tokenIndex == 1 })
        assertTrue(moments.any { it.type == GameMomentType.HOME_REACHED && it.tokenIndex == 2 })
    }

    @Test
    fun detectsOnlyLegalMoveFromResolvedAuthoritativePendingRoll() {
        val move = event(4, "p1", 3)
        val before =
            state(
                players = listOf(player("p1", "RED", 0, listOf(4, 20, -1, -1)), player("p2", "GREEN", 1)),
                history = listOf(move),
                pending = pending(0, 4, 3, setOf(1)),
            )
        val after =
            state(
                players = listOf(player("p1", "RED", 0, listOf(4, 23, -1, -1)), player("p2", "GREEN", 1)),
                history = listOf(move.copy(moveTokenIndex = 1)),
            )

        val moments = LudoPawsGameMomentDetector.detect(before, after)
        assertTrue(
            moments.any {
                it.type == GameMomentType.ONLY_LEGAL_MOVE &&
                    it.playerId == "p1" &&
                    it.tokenIndex == 1
            },
        )
    }

    @Test
    fun detectsHomeLaneEntrySeparatelyFromHomeArrival() {
        val move = event(6, "p1", 2)
        val before =
            state(
                players = listOf(player("p1", "RED", 0, listOf(50, -1, -1, -1)), player("p2", "GREEN", 1)),
                history = listOf(move),
                pending = pending(0, 6, 2),
            )
        val after =
            state(
                players = listOf(player("p1", "RED", 0, listOf(52, -1, -1, -1)), player("p2", "GREEN", 1)),
                history = listOf(move.copy(moveTokenIndex = 0)),
            )

        val moments = LudoPawsGameMomentDetector.detect(before, after)
        assertTrue(moments.any { it.type == GameMomentType.HOME_LANE_ENTERED })
        assertFalse(moments.any { it.type == GameMomentType.HOME_REACHED })
    }

    @Test
    fun exactHomeOvershootIsNotGenericNoMove() {
        val miss = event(9, "p1", 4)
        val moments =
            LudoPawsGameMomentDetector.detect(
                state(
                    players = listOf(player("p1", "RED", 0, listOf(55, -1, -1, -1)), player("p2", "GREEN", 1)),
                    history = emptyList(),
                ),
                state(
                    players = listOf(player("p1", "RED", 0, listOf(55, -1, -1, -1)), player("p2", "GREEN", 1)),
                    history = listOf(miss),
                ),
            )

        assertTrue(moments.any { it.type == GameMomentType.EXACT_HOME_MISS })
        assertFalse(moments.any { it.type == GameMomentType.NO_LEGAL_MOVE })
    }

    @Test
    fun unsafePawnCanBecomeThreatenedButSafePawnAndTeamPartnerDoNotTrigger() {
        val move = event(10, "p1", 1)
        val before =
            state(
                players =
                    listOf(
                        player("p1", "RED", 0, listOf(4, -1, -1, -1)),
                        player("p2", "GREEN", 1, listOf(43, -1, -1, -1)),
                    ),
                history = listOf(move),
                pending = pending(0, 10, 1),
            )
        val unsafeAfter =
            state(
                players =
                    listOf(
                        player("p1", "RED", 0, listOf(5, -1, -1, -1)),
                        player("p2", "GREEN", 1, listOf(43, -1, -1, -1)),
                    ),
                history = listOf(move.copy(moveTokenIndex = 0)),
            )
        assertTrue(
            LudoPawsGameMomentDetector.detect(before, unsafeAfter)
                .any { it.type == GameMomentType.TOKEN_THREATENED },
        )

        val safeBefore = before.copy(players = listOf(player("p1", "RED", 0, listOf(7, -1, -1, -1)), before.players[1]))
        val safeAfter = unsafeAfter.copy(players = listOf(player("p1", "RED", 0, listOf(8, -1, -1, -1)), unsafeAfter.players[1]))
        assertFalse(
            LudoPawsGameMomentDetector.detect(safeBefore, safeAfter)
                .any { it.type == GameMomentType.TOKEN_THREATENED },
        )

        val teamBefore =
            before.copy(
                players =
                    listOf(
                        player("p1", "RED", 0, listOf(4, -1, -1, -1), teamId = "A"),
                        player("p2", "GREEN", 1, listOf(43, -1, -1, -1), teamId = "A"),
                    ),
            )
        val teamAfter =
            unsafeAfter.copy(
                players =
                    listOf(
                        player("p1", "RED", 0, listOf(5, -1, -1, -1), teamId = "A"),
                        player("p2", "GREEN", 1, listOf(43, -1, -1, -1), teamId = "A"),
                    ),
            )
        assertFalse(
            LudoPawsGameMomentDetector.detect(teamBefore, teamAfter)
                .any { it.type == GameMomentType.TOKEN_THREATENED },
        )
    }

    @Test
    fun thirdSixIsNotMisclassifiedAsNoLegalMove() {
        val history = listOf(event(0, "p1", 6), event(1, "p1", 6), event(2, "p1", 6))
        val moments =
            LudoPawsGameMomentDetector.detect(
                state(history = history.take(2)),
                state(history = history),
            )
        assertTrue(moments.any { it.type == GameMomentType.THIRD_SIX_FORFEIT })
        assertFalse(moments.any { it.type == GameMomentType.NO_LEGAL_MOVE })
    }

    @Test
    fun detectsNoLegalMoveAndMeaningfulLeadPressure() {
        val previous =
            state(
                players = listOf(player("p1", "RED", 0), player("p2", "GREEN", 1)),
            )
        val current =
            state(
                players = listOf(player("p1", "RED", 0, listOf(12, -1, -1, -1)), player("p2", "GREEN", 1)),
                history = listOf(event(9, "p1", 2)),
            )
        val moments = LudoPawsGameMomentDetector.detect(previous, current)
        assertTrue(moments.any { it.type == GameMomentType.LOW_ROLL })
        assertTrue(moments.any { it.type == GameMomentType.NO_LEGAL_MOVE })
        assertTrue(moments.any { it.type == GameMomentType.PLAYER_LEADING && it.playerId == "p1" })
    }

    @Test
    fun idleMomentUsesExplicitClockAndDoesNotMutateState() {
        val current = state(turnSeat = 1)
        val tooEarly =
            LudoPawsGameMomentDetector.idleMoment(
                current = current,
                nowMillis = 23_999L,
                lastMeaningfulChangeAtMillis = 0L,
            )
        val idle =
            LudoPawsGameMomentDetector.idleMoment(
                current = current,
                nowMillis = 24_001L,
                lastMeaningfulChangeAtMillis = 0L,
            )
        assertEquals(null, tooEarly)
        assertNotNull(idle)
        assertEquals(GameMomentType.IDLE_WAITING, idle?.type)
        assertEquals("p2", idle?.playerId)
    }

    @Test
    fun emitsTeamWinAndLossMoments() {
        val players =
            listOf(
                player("p1", "RED", 0, teamId = "A"),
                player("p2", "GREEN", 1, teamId = "B"),
                player("p3", "YELLOW", 2, teamId = "A"),
                player("p4", "BLUE", 3, teamId = "B"),
            )
        val moments =
            LudoPawsGameMomentDetector.detect(
                state(players = players),
                state(status = "FINISHED", players = players, winnerTeamId = "A"),
            )
        assertEquals(2, moments.count { it.type == GameMomentType.TEAM_WIN })
        assertEquals(2, moments.count { it.type == GameMomentType.TEAM_LOSS })
    }

    private fun state(
        status: String = "ACTIVE",
        turnSeat: Int = 0,
        players: List<PlayerSnapshot> =
            listOf(player("p1", "RED", 0), player("p2", "GREEN", 1)),
        history: List<HistoryEventSnapshot> = emptyList(),
        pending: PendingRollSnapshot? = null,
        winnerPlayerId: String? = null,
        winnerTeamId: String? = null,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "moment-match",
            status = status,
            hostPlayerId = "p1",
            players = players,
            turnSeat = turnSeat,
            randomEventIndex = history.lastOrNull()?.eventIndex?.plus(1) ?: 0,
            pendingRoll = pending,
            winnerPlayerId = winnerPlayerId,
            rulesetId = "classic",
            history = history,
            winnerTeamId = winnerTeamId,
        )

    private fun player(
        id: String,
        color: String,
        seat: Int,
        tokens: List<Int> = listOf(-1, -1, -1, -1),
        teamId: String? = null,
    ): PlayerSnapshot =
        PlayerSnapshot(
            playerId = id,
            displayName = id,
            color = color,
            seat = seat,
            tokens = tokens,
            teamId = teamId,
        )

    private fun event(
        index: Int,
        playerId: String,
        outcome: Int,
        moveTokenIndex: Int? = null,
        captures: Int = 0,
    ): HistoryEventSnapshot =
        HistoryEventSnapshot(
            eventIndex = index,
            playerId = playerId,
            roundId = null,
            proofDigest = null,
            outcome = outcome,
            moveTokenIndex = moveTokenIndex,
            captures = captures,
        )

    private fun pending(
        seat: Int,
        index: Int,
        outcome: Int,
        legalTokenIndexes: Set<Int> = setOf(0),
    ): PendingRollSnapshot =
        PendingRollSnapshot(
            status = "RESOLVED",
            seat = seat,
            eventIndex = index,
            eventId = null,
            roundId = null,
            serverCommitment = null,
            clientCommitment = null,
            revealDeadlineAt = null,
            proofDigest = null,
            outcome = outcome,
            legalTokenIndexes = legalTokenIndexes,
        )
}
