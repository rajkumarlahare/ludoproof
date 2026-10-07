package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsCaptureReturnPlacementTest {
    @Test
    fun capturedPawnStartsAtImpactCell() {
        val frame =
            LudoPawsCaptureReturnPlacement.sample(
                from = 100f to 140f,
                to = 40f to 40f,
                cell = 24f,
                progress = 0f,
            )

        assertEquals(LudoPawsCaptureReturnPhase.IMPACT_SHAKE, frame.phase)
        assertEquals(100f, frame.x, .0001f)
        assertEquals(140f, frame.y, .0001f)
    }

    @Test
    fun capturedPawnUsesCurvedVisibleReturn() {
        val from = 120f to 160f
        val to = 40f to 50f
        val middle =
            LudoPawsCaptureReturnPlacement.sample(
                from = from,
                to = to,
                cell = 26f,
                progress = .62f,
            )

        val straightMidX = (from.first + to.first) * .5f
        val straightMidY = (from.second + to.second) * .5f
        assertEquals(LudoPawsCaptureReturnPhase.RETURN_TO_YARD, middle.phase)
        assertTrue(middle.x.isFinite())
        assertTrue(middle.y.isFinite())
        assertTrue(middle.scale > 0f)
        assertTrue(
            kotlin.math.abs(middle.x - straightMidX) > .1f ||
                kotlin.math.abs(middle.y - straightMidY) > .1f,
        )
    }

    @Test
    fun capturedPawnEndsExactlyAtYard() {
        val target = 44f to 52f
        val end =
            LudoPawsCaptureReturnPlacement.sample(
                from = 180f to 130f,
                to = target,
                cell = 28f,
                progress = 1f,
            )

        assertEquals(LudoPawsCaptureReturnPhase.SETTLE, end.phase)
        assertEquals(target.first, end.x, .0001f)
        assertEquals(target.second, end.y, .0001f)
        assertEquals(1f, end.scale, .0001f)
    }

    @Test
    fun impactAndPopStayNearCaptureCellBeforeTravel() {
        val from = 160f to 120f
        val to = 30f to 40f
        val impact =
            LudoPawsCaptureReturnPlacement.sample(
                from = from,
                to = to,
                cell = 30f,
                progress = .10f,
            )
        val pop =
            LudoPawsCaptureReturnPlacement.sample(
                from = from,
                to = to,
                cell = 30f,
                progress = .26f,
            )

        assertEquals(LudoPawsCaptureReturnPhase.IMPACT_SHAKE, impact.phase)
        assertEquals(LudoPawsCaptureReturnPhase.POP, pop.phase)
        assertTrue(kotlin.math.abs(impact.x - from.first) < 8f)
        assertTrue(kotlin.math.abs(pop.x - from.first) < 8f)
        assertNotEquals(1f, pop.scale, .0001f)
    }

    @Test
    fun contactPhaseIncludesVisibleShoveTowardReturnRoute() {
        val from = 100f to 100f
        val to = 40f to 100f
        val pushed =
            LudoPawsCaptureReturnPlacement.sample(
                from = from,
                to = to,
                cell = 30f,
                progress = .17f,
            )

        assertTrue(pushed.x < from.first)
        assertTrue(kotlin.math.abs(pushed.x - from.first) > 1f)
    }
}
