package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsPawnAnimationPolicyTest {
    @Test
    fun forwardMoveKeepsStepwiseMotionPlan() {
        val motion =
            LudoPawsPawnAnimationPolicy.transition(
                playerId = "p1",
                tokenIndex = 2,
                from = 10,
                to = 16,
            )

        assertEquals(LudoPawsPawnMotionKind.FORWARD, motion?.kind)
        assertEquals(6, motion?.visualSteps)
    }

    @Test
    fun capturedTrackTokenGetsReturnToYardMotion() {
        val motion =
            LudoPawsPawnAnimationPolicy.transition(
                playerId = "p2",
                tokenIndex = 1,
                from = 31,
                to = -1,
            )

        assertEquals(LudoPawsPawnMotionKind.CAPTURE_RETURN, motion?.kind)
        assertEquals(-1, motion?.toPosition)
        assertTrue(requireNotNull(motion).visualSteps > 1)
    }

    @Test
    fun invalidBackwardTransitionIsNotAnimated() {
        assertNull(
            LudoPawsPawnAnimationPolicy.transition(
                playerId = "p1",
                tokenIndex = 0,
                from = 20,
                to = 8,
            ),
        )
    }

    @Test
    fun captureSnapshotProducesMoverAndVictimAnimationsTogether() {
        val previous =
            snapshot(
                listOf(
                    player("p1", "RED", 0, listOf(5, -1, -1, -1)),
                    player("p2", "GREEN", 1, listOf(44, -1, -1, -1)),
                ),
            )
        val current =
            snapshot(
                listOf(
                    player("p1", "RED", 0, listOf(8, -1, -1, -1)),
                    player("p2", "GREEN", 1, listOf(-1, -1, -1, -1)),
                ),
            )

        val motions =
            LudoPawsPawnAnimationPolicy.plans(
                previous = previous,
                current = current,
            )

        assertEquals(2, motions.size)
        assertTrue(
            motions.any {
                it.playerId == "p1" &&
                    it.kind == LudoPawsPawnMotionKind.FORWARD
            },
        )
        assertTrue(
            motions.any {
                it.playerId == "p2" &&
                    it.kind == LudoPawsPawnMotionKind.CAPTURE_RETURN
            },
        )
    }

    @Test
    fun captureReturnUsesImpactPopTravelAndSettleStages() {
        val impact =
            LudoPawsPawnAnimationPolicy.captureReturnFrame(.08f)
        val pop =
            LudoPawsPawnAnimationPolicy.captureReturnFrame(.25f)
        val travel =
            LudoPawsPawnAnimationPolicy.captureReturnFrame(.62f)
        val settle =
            LudoPawsPawnAnimationPolicy.captureReturnFrame(.96f)

        assertEquals(
            LudoPawsCaptureReturnPhase.IMPACT_SHAKE,
            impact.phase,
        )
        assertEquals(0f, impact.routeProgress, 0.0001f)
        assertTrue(kotlin.math.abs(impact.shakeXCells) > 0.001f)

        assertEquals(
            LudoPawsCaptureReturnPhase.POP,
            pop.phase,
        )
        assertEquals(0f, pop.routeProgress, 0.0001f)
        assertTrue(pop.liftCells > 0f)
        assertTrue(pop.scale > 1f)

        assertEquals(
            LudoPawsCaptureReturnPhase.RETURN_TO_YARD,
            travel.phase,
        )
        assertTrue(travel.routeProgress in 0.01f..0.99f)
        assertTrue(travel.liftCells > 0f)

        assertEquals(
            LudoPawsCaptureReturnPhase.SETTLE,
            settle.phase,
        )
        assertEquals(1f, settle.routeProgress, 0.0001f)
    }

    @Test
    fun captureReturnEndsExactlyAtStableYardPose() {
        val end =
            LudoPawsPawnAnimationPolicy.captureReturnFrame(1f)

        assertEquals(
            LudoPawsCaptureReturnPhase.SETTLE,
            end.phase,
        )
        assertEquals(1f, end.routeProgress, 0.0001f)
        assertEquals(0f, end.shakeXCells, 0.0001f)
        assertEquals(0f, end.liftCells, 0.0001f)
        assertEquals(1f, end.scale, 0.0001f)
    }

    private fun snapshot(players: List<PlayerSnapshot>): MatchSnapshot =
        MatchSnapshot(
            matchId = "capture-animation-match",
            status = "ACTIVE",
            hostPlayerId = "p1",
            players = players,
            turnSeat = 0,
            randomEventIndex = 1,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "classic",
            history = emptyList(),
        )

    private fun player(
        id: String,
        color: String,
        seat: Int,
        tokens: List<Int>,
    ): PlayerSnapshot =
        PlayerSnapshot(
            playerId = id,
            displayName = id,
            color = color,
            seat = seat,
            tokens = tokens,
        )
}
