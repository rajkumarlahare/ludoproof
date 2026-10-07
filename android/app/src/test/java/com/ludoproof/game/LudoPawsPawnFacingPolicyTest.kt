package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Test

class LudoPawsPawnFacingPolicyTest {
    @Test
    fun visibleCardinalDirectionsMapToExpectedYaw() {
        assertEquals(
            90f,
            LudoPawsPawnFacingPolicy.yawForVisibleDelta(1f, 0f),
            0.001f,
        )
        assertEquals(
            -90f,
            LudoPawsPawnFacingPolicy.yawForVisibleDelta(-1f, 0f),
            0.001f,
        )
        assertEquals(
            0f,
            LudoPawsPawnFacingPolicy.yawForVisibleDelta(0f, 1f),
            0.001f,
        )
        assertEquals(
            180f,
            LudoPawsPawnFacingPolicy.yawForVisibleDelta(0f, -1f),
            0.001f,
        )
    }

    @Test
    fun zeroTravelKeepsPawnFacingPlayer() {
        assertEquals(
            0f,
            LudoPawsPawnFacingPolicy.yawForVisibleDelta(0f, 0f),
            0.001f,
        )
    }

    @Test
    fun movingYawTurnsSmoothlyAtStartOfHop() {
        assertEquals(
            0f,
            LudoPawsPawnFacingPolicy.movingYaw(
                previousYawDegrees = 0f,
                targetYawDegrees = 90f,
                stepProgress = 0f,
            ),
            0.001f,
        )
        assertEquals(
            90f,
            LudoPawsPawnFacingPolicy.movingYaw(
                previousYawDegrees = 0f,
                targetYawDegrees = 90f,
                stepProgress = 0.28f,
            ),
            0.001f,
        )
    }

    @Test
    fun settlingYawReturnsToPlayerFacing() {
        assertEquals(
            90f,
            LudoPawsPawnFacingPolicy.settlingYaw(
                lastTravelYawDegrees = 90f,
                elapsedAfterMoveMillis = 0L,
            ),
            0.001f,
        )
        assertEquals(
            0f,
            LudoPawsPawnFacingPolicy.settlingYaw(
                lastTravelYawDegrees = 90f,
                elapsedAfterMoveMillis =
                    LudoPawsPawnFacingPolicy.RETURN_TO_PLAYER_MILLIS,
            ),
            0.001f,
        )
    }

    @Test
    fun interpolationUsesShortestTurnAcrossBackFacingBoundary() {
        assertEquals(
            180f,
            LudoPawsPawnFacingPolicy.interpolateYaw(
                fromYawDegrees = 90f,
                toYawDegrees = -90f,
                progress = 0.5f,
            ),
            0.001f,
        )
    }
}
