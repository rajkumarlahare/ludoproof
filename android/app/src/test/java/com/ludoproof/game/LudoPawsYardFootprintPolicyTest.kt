package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsYardFootprintPolicyTest {
    @Test
    fun `footprints appear only for tokens no longer in their yard slots`() {
        val snapshot =
            snapshot(
                redTokens = listOf(0, -1, LudoPathEncoding.HOME_POSITION, -1),
                greenTokens = listOf(-1, 7, -1, -1),
            )

        val prints =
            LudoPawsYardFootprintPolicy.footprints(
                snapshot = snapshot,
                characterIdsBySeat = listOf("duck", "cat", "dog", "goat"),
            )

        assertEquals(
            listOf(
                "red-player:0:DUCK",
                "red-player:2:DUCK",
                "green-player:1:CAT",
            ),
            prints.map { "${it.playerId}:${it.tokenIndex}:${it.species.name}" },
        )
    }

    @Test
    fun `selected character identity wins over team default species`() {
        val prints =
            LudoPawsYardFootprintPolicy.footprints(
                snapshot =
                    snapshot(
                        redTokens = listOf(0, -1, -1, -1),
                        greenTokens = listOf(0, -1, -1, -1),
                        yellowTokens = listOf(0, -1, -1, -1),
                        blueTokens = listOf(0, -1, -1, -1),
                    ),
                characterIdsBySeat = listOf("duck", "cat", "dog", "goat"),
            )

        assertEquals(
            listOf(
                LudoPaws3DSpecies.DUCK,
                LudoPaws3DSpecies.CAT,
                LudoPaws3DSpecies.DOG,
                LudoPaws3DSpecies.GOAT,
            ),
            prints.map { it.species },
        )
    }

    @Test
    fun `missing character assignments use deterministic existing team fallbacks`() {
        val prints =
            LudoPawsYardFootprintPolicy.footprints(
                snapshot =
                    snapshot(
                        redTokens = listOf(0, -1, -1, -1),
                        greenTokens = listOf(0, -1, -1, -1),
                        yellowTokens = listOf(0, -1, -1, -1),
                        blueTokens = listOf(0, -1, -1, -1),
                    ),
                characterIdsBySeat = emptyList(),
            )

        assertEquals(
            listOf(
                LudoPaws3DSpecies.DOG,
                LudoPaws3DSpecies.GOAT,
                LudoPaws3DSpecies.DUCK,
                LudoPaws3DSpecies.CAT,
            ),
            prints.map { it.species },
        )
    }

    @Test
    fun `a token returned to its yard no longer leaves a visible footprint`() {
        val prints =
            LudoPawsYardFootprintPolicy.footprints(
                snapshot =
                    snapshot(
                        redTokens = listOf(-1, -1, -1, -1),
                    ),
                characterIdsBySeat = listOf("duck"),
            )

        assertTrue(prints.isEmpty())
    }

    @Test
    fun `null match state produces no stale footprints`() {
        assertTrue(
            LudoPawsYardFootprintPolicy.footprints(
                snapshot = null,
                characterIdsBySeat = listOf("duck", "cat", "dog", "goat"),
            ).isEmpty(),
        )
    }

    private fun snapshot(
        redTokens: List<Int> = listOf(-1, -1, -1, -1),
        greenTokens: List<Int> = listOf(-1, -1, -1, -1),
        yellowTokens: List<Int> = listOf(-1, -1, -1, -1),
        blueTokens: List<Int> = listOf(-1, -1, -1, -1),
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "footprint-test",
            status = "ACTIVE",
            hostPlayerId = "red-player",
            players =
                listOf(
                    PlayerSnapshot(
                        playerId = "red-player",
                        displayName = "Red",
                        color = "RED",
                        seat = 0,
                        tokens = redTokens,
                    ),
                    PlayerSnapshot(
                        playerId = "green-player",
                        displayName = "Green",
                        color = "GREEN",
                        seat = 1,
                        tokens = greenTokens,
                    ),
                    PlayerSnapshot(
                        playerId = "yellow-player",
                        displayName = "Yellow",
                        color = "YELLOW",
                        seat = 2,
                        tokens = yellowTokens,
                    ),
                    PlayerSnapshot(
                        playerId = "blue-player",
                        displayName = "Blue",
                        color = "BLUE",
                        seat = 3,
                        tokens = blueTokens,
                    ),
                ),
            turnSeat = 0,
            randomEventIndex = 0,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "footprint-test",
            history = emptyList(),
        )
}
