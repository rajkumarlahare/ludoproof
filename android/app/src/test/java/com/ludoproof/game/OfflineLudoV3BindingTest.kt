package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLudoV3BindingTest {
    @Test
    fun v3BindingIncludesOpeningStateAndKeepsEntroNexProofVerifiable() {
        val players =
            listOf(
                OfflineV3BindingPlayer(
                    playerId = "offline-player-1",
                    color = "RED",
                    tokens = listOf(-1, -1, -1, -1),
                ),
                OfflineV3BindingPlayer(
                    playerId = "offline-player-2",
                    color = "GREEN",
                    tokens = listOf(-1, -1, -1, -1),
                ),
            )

        val unopened =
            OfflineLudoV3Binding.deriveRoll(
                matchId = "offline-v3-test",
                status = "ACTIVE",
                players = players,
                turnSeat = 0,
                eventIndex = 0,
                consecutiveSixes = listOf(0, 0),
                openingRollConsumed = listOf(false, false),
                winnerPlayerId = null,
                history = emptyList(),
            )

        val consumed =
            OfflineLudoV3Binding.deriveRoll(
                matchId = "offline-v3-test",
                status = "ACTIVE",
                players = players,
                turnSeat = 0,
                eventIndex = 0,
                consecutiveSixes = listOf(0, 0),
                openingRollConsumed = listOf(true, false),
                winnerPlayerId = null,
                history = emptyList(),
            )

        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            unopened.config.outcomes,
        )
        assertNotEquals(
            unopened.config.context.previousStateHash,
            consumed.config.context.previousStateHash,
        )
        assertNotEquals(
            "4bd777ac5ec430c0a70956dd83a6451f4f8f0e91848b09000791e912fe3886cc",
            unopened.config.context.metadataDigest,
        )
        assertTrue(EntroNexV4Local.verify(unopened))
        assertTrue(EntroNexV4Local.verify(consumed))
    }
}
