package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
        assertTrue(crowded > 0.27f)
    }

    @Test
    fun `single occupant remains exactly centered`() {
        assertEquals(
            0f to 0f,
            LudoPawsPawnLayout.stackOffsetFraction(
                slot = 0,
                occupancy = 1,
            ),
        )
    }

    @Test
    fun `two occupants split into distinct balanced positions`() {
        val offsets =
            (0 until 2).map { slot ->
                LudoPawsPawnLayout.stackOffsetFraction(
                    slot = slot,
                    occupancy = 2,
                )
            }

        assertEquals(2, offsets.distinct().size)
        assertEquals(-offsets[0].first, offsets[1].first, 0.0001f)
        assertEquals(0f, offsets[0].second, 0.0001f)
        assertEquals(0f, offsets[1].second, 0.0001f)
    }

    @Test
    fun `three occupants form three unique positions`() {
        val offsets =
            (0 until 3).map { slot ->
                LudoPawsPawnLayout.stackOffsetFraction(
                    slot = slot,
                    occupancy = 3,
                )
            }

        assertEquals(3, offsets.distinct().size)
    }

    @Test
    fun `four occupants use a deterministic two by two layout`() {
        val offsets =
            (0 until 4).map { slot ->
                LudoPawsPawnLayout.stackOffsetFraction(
                    slot = slot,
                    occupancy = 4,
                )
            }

        assertEquals(4, offsets.distinct().size)
        assertEquals(setOf(-0.16f, 0.16f), offsets.map { it.first }.toSet())
        assertEquals(setOf(-0.16f, 0.16f), offsets.map { it.second }.toSet())
    }

    @Test
    fun `safe cell crowd keeps all sixteen visual slots unique`() {
        val offsets =
            (0 until 16).map { slot ->
                LudoPawsPawnLayout.stackOffsetFraction(
                    slot = slot,
                    occupancy = 16,
                )
            }

        assertEquals(16, offsets.distinct().size)
    }

    @Test
    fun `different logical color positions sharing one physical track cell are stacked together`() {
        val snapshot = sharedCellSnapshot()

        val placements =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = snapshot,
                cell = 20f,
            )
        val red =
            placements[
                LudoPawsPawnVisualKey(
                    playerId = "red-player",
                    tokenIndex = 0,
                )
            ]
        val green =
            placements[
                LudoPawsPawnVisualKey(
                    playerId = "green-player",
                    tokenIndex = 0,
                )
            ]

        assertEquals(2, red?.occupancy)
        assertEquals(2, green?.occupancy)
        assertTrue(
            red?.offsetXFraction != green?.offsetXFraction ||
                red?.offsetYFraction != green?.offsetYFraction,
        )
    }

    @Test
    fun `stable snapshot and cell reuse the same stack placement map`() {
        val snapshot = sharedCellSnapshot()

        val first =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = snapshot,
                cell = 20f,
            )
        val second =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = snapshot,
                cell = 20f,
            )

        assertSame(first, second)
    }

    @Test
    fun `stack placement cache invalidates for new snapshot or board cell size`() {
        val firstSnapshot = sharedCellSnapshot(matchId = "stack-a")
        val secondSnapshot = sharedCellSnapshot(matchId = "stack-b")

        val first =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = firstSnapshot,
                cell = 20f,
            )
        val newSnapshot =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = secondSnapshot,
                cell = 20f,
            )
        val newCell =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = secondSnapshot,
                cell = 21f,
            )

        assertNotSame(first, newSnapshot)
        assertNotSame(newSnapshot, newCell)
        assertEquals(first, newSnapshot)
    }

    @Test
    fun `capture return offsets remain deterministic across four legacy slots`() {
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

    private fun sharedCellSnapshot(
        matchId: String = "stack-test",
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = matchId,
            status = "ACTIVE",
            hostPlayerId = "red-player",
            players =
                listOf(
                    PlayerSnapshot(
                        playerId = "red-player",
                        displayName = "Red",
                        color = "RED",
                        seat = 0,
                        tokens = listOf(13, -1, -1, -1),
                    ),
                    PlayerSnapshot(
                        playerId = "green-player",
                        displayName = "Green",
                        color = "GREEN",
                        seat = 1,
                        tokens = listOf(0, -1, -1, -1),
                    ),
                ),
            turnSeat = 0,
            randomEventIndex = 0,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "layout-test",
            history = emptyList(),
        )
}
