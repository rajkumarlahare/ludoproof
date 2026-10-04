package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoPawsFxBoardGeometryTest {
    @Test
    fun `track center follows stretched locked geometry`() {
        val cell = 60f
        val center =
            requireNotNull(
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = "RED",
                    tokenIndex = 0,
                    position = 0,
                    cell = cell,
                ),
            )

        assertEquals(1.25f * cell, center.first, EPSILON)
        assertEquals((35f / 6f) * cell, center.second, EPSILON)
    }

    @Test
    fun `home lane center follows stretched three lane road`() {
        val cell = 60f
        val center =
            requireNotNull(
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = "RED",
                    tokenIndex = 0,
                    position = 52,
                    cell = cell,
                ),
            )

        assertEquals(1.25f * cell, center.first, EPSILON)
        assertEquals(7.5f * cell, center.second, EPSILON)
    }

    @Test
    fun `legacy position 51 renders on first home lane instead of extra white track cell`() {
        val cell = 60f
        val legacy =
            requireNotNull(
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = "BLUE",
                    tokenIndex = 0,
                    position = 51,
                    cell = cell,
                ),
            )
        val firstLane =
            requireNotNull(
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = "BLUE",
                    tokenIndex = 0,
                    position = 52,
                    cell = cell,
                ),
            )

        assertEquals(firstLane.first, legacy.first, EPSILON)
        assertEquals(firstLane.second, legacy.second, EPSILON)
    }

    @Test
    fun `yard token slots match fixed half cell inset white home`() {
        val cell = 60f
        val redTopLeft =
            requireNotNull(
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = "RED",
                    tokenIndex = 0,
                    position = -1,
                    cell = cell,
                ),
            )
        val yellowBottomRight =
            requireNotNull(
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = "YELLOW",
                    tokenIndex = 3,
                    position = -1,
                    cell = cell,
                ),
            )

        assertEquals(1.5f * cell, redTopLeft.first, EPSILON)
        assertEquals(1.5f * cell, redTopLeft.second, EPSILON)
        assertEquals(13.5f * cell, yellowBottomRight.first, EPSILON)
        assertEquals(13.5f * cell, yellowBottomRight.second, EPSILON)
    }

    @Test
    fun `finish centers match locked center triangles`() {
        val cell = 60f

        val red = finishCenter("RED", cell)
        val green = finishCenter("GREEN", cell)
        val yellow = finishCenter("YELLOW", cell)
        val blue = finishCenter("BLUE", cell)

        assertEquals(6.5f * cell, red.first, EPSILON)
        assertEquals(7.5f * cell, red.second, EPSILON)
        assertEquals(7.5f * cell, green.first, EPSILON)
        assertEquals(6.5f * cell, green.second, EPSILON)
        assertEquals(8.5f * cell, yellow.first, EPSILON)
        assertEquals(7.5f * cell, yellow.second, EPSILON)
        assertEquals(7.5f * cell, blue.first, EPSILON)
        assertEquals(8.5f * cell, blue.second, EPSILON)
    }

    @Test
    fun `reaction anchors stay centered in fixed physical yards`() {
        val cell = 60f

        assertPoint(2.5f, 2.5f, "RED", cell)
        assertPoint(12.5f, 2.5f, "GREEN", cell)
        assertPoint(12.5f, 12.5f, "YELLOW", cell)
        assertPoint(2.5f, 12.5f, "BLUE", cell)
        assertNull(
            LudoPawsFxBoardGeometry.yardReactionAnchor(
                color = "UNKNOWN",
                cell = cell,
            ),
        )
    }

    private fun finishCenter(
        color: String,
        cell: Float,
    ): Pair<Float, Float> =
        requireNotNull(
            LudoPawsFxBoardGeometry.tokenCenter(
                color = color,
                tokenIndex = 0,
                position = 57,
                cell = cell,
            ),
        )

    private fun assertPoint(
        xCells: Float,
        yCells: Float,
        color: String,
        cell: Float,
    ) {
        val point =
            requireNotNull(
                LudoPawsFxBoardGeometry.yardReactionAnchor(
                    color = color,
                    cell = cell,
                ),
            )
        assertEquals(xCells * cell, point.first, EPSILON)
        assertEquals(yCells * cell, point.second, EPSILON)
    }

    private companion object {
        const val EPSILON = 0.01f
    }
}
