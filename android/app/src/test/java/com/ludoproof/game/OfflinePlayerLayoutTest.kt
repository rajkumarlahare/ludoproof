package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflinePlayerLayoutTest {
    @Test
    fun twoPlayerUsesOppositeBoardCorners() {
        assertEquals(
            listOf(
                "BLUE",
                "GREEN",
            ),
            OfflinePlayerLayout
                .colorsFor(
                    2,
                    "BLUE",
                ),
        )
        assertEquals(
            listOf(
                "RED",
                "YELLOW",
            ),
            OfflinePlayerLayout
                .colorsFor(
                    2,
                    "RED",
                ),
        )
    }

    @Test
    fun threeAndFourPlayerKeepDeterministicColorOrder() {
        assertEquals(
            listOf(
                "BLUE",
                "RED",
                "GREEN",
            ),
            OfflinePlayerLayout
                .colorsFor(
                    3,
                    "BLUE",
                ),
        )
        assertEquals(
            listOf(
                "RED",
                "GREEN",
                "YELLOW",
                "BLUE",
            ),
            OfflinePlayerLayout
                .colorsFor(
                    4,
                    "RED",
                ),
        )
    }

    @Test
    fun colorsMapToReferenceBoardCorners() {
        assertEquals(
            OfflinePlayerLayout
                .Slot
                .TOP_LEFT,
            OfflinePlayerLayout
                .slotForColor(
                    "RED",
                ),
        )
        assertEquals(
            OfflinePlayerLayout
                .Slot
                .TOP_RIGHT,
            OfflinePlayerLayout
                .slotForColor(
                    "GREEN",
                ),
        )
        assertEquals(
            OfflinePlayerLayout
                .Slot
                .BOTTOM_LEFT,
            OfflinePlayerLayout
                .slotForColor(
                    "BLUE",
                ),
        )
        assertEquals(
            OfflinePlayerLayout
                .Slot
                .BOTTOM_RIGHT,
            OfflinePlayerLayout
                .slotForColor(
                    "YELLOW",
                ),
        )
    }
}
