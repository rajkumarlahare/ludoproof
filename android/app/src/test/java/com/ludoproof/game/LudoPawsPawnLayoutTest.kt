package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsPawnLayoutTest {
    @Test
    fun `character assignment resolves by seat`() {
        val ids =
            listOf(
                "dog",
                "goat",
                "duck",
                "cat",
            )

        assertEquals(
            "duck",
            LudoPawsPawnLayout.characterIdForSeat(
                ids,
                2,
            ),
        )
        assertNull(
            LudoPawsPawnLayout.characterIdForSeat(
                ids,
                7,
            ),
        )
    }

    @Test
    fun `blank character assignment falls back to base pawn`() {
        assertNull(
            LudoPawsPawnLayout.characterIdForSeat(
                listOf("dog", "   "),
                1,
            ),
        )
    }

    @Test
    fun `yard animal is larger than track animal`() {
        val yard =
            LudoPawsPawnLayout.radiusScale(
                position = -1,
                occupancy = 1,
            )
        val track =
            LudoPawsPawnLayout.radiusScale(
                position = 10,
                occupancy = 1,
            )

        assertTrue(yard > track)
    }

    @Test
    fun `shared cell reduces animal radius without collapsing it`() {
        val single =
            LudoPawsPawnLayout.radiusScale(
                position = 18,
                occupancy = 1,
            )
        val crowded =
            LudoPawsPawnLayout.radiusScale(
                position = 18,
                occupancy = 4,
            )

        assertTrue(crowded < single)
        assertTrue(crowded > 0.29f)
    }

    @Test
    fun `track offsets are deterministic across four visual slots`() {
        val offsets =
            (0 until 4)
                .map {
                    LudoPawsPawnLayout.tokenOffsetFraction(
                        slot = it,
                        position = 12,
                    )
                }

        assertEquals(4, offsets.distinct().size)
        assertEquals(
            offsets[0],
            LudoPawsPawnLayout.tokenOffsetFraction(
                slot = 4,
                position = 12,
            ),
        )
    }

    @Test
    fun `yard token keeps its dedicated yard center`() {
        assertEquals(
            0f to 0f,
            LudoPawsPawnLayout.tokenOffsetFraction(
                slot = 3,
                position = -1,
            ),
        )
    }
}
