package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLudoV4BindingTest {
    @Test
    fun offlineBindingUsesTheSameStateRulesAndWorldContractAsOnlineLudoProof() {
        val players =
            listOf(
                OfflineBindingPlayer(
                    playerId =
                        "offline-player-1",
                    color =
                        "RED",
                    tokens =
                        listOf(
                            -1,
                            -1,
                            -1,
                            -1,
                        ),
                ),
                OfflineBindingPlayer(
                    playerId =
                        "offline-player-2",
                    color =
                        "GREEN",
                    tokens =
                        listOf(
                            -1,
                            -1,
                            -1,
                            -1,
                        ),
                ),
            )

        val result =
            OfflineLudoV4Binding
                .deriveRoll(
                    matchId =
                        "offline-test-session",
                    status =
                        "ACTIVE",
                    players =
                        players,
                    turnSeat =
                        0,
                    eventIndex =
                        0,
                    consecutiveSixes =
                        listOf(
                            0,
                            0,
                        ),
                    winnerPlayerId =
                        null,
                    history =
                        emptyList(),
                )

        assertEquals(
            "ludoproof",
            result.config
                .context
                .applicationId,
        )
        assertEquals(
            "offline-test-session",
            result.config
                .context
                .sessionId,
        )
        assertEquals(
            "roll:0",
            result.config
                .context
                .eventId,
        )
        assertEquals(
            "DICE_ROLL",
            result.config
                .context
                .eventType,
        )
        assertEquals(
            0L,
            result.config
                .context
                .eventIndex,
        )
        assertEquals(
            EntroNexV4Local.sha256Hex(
                "ludoproof:actor:v1:offline-player-1",
            ),
            result.config
                .context
                .subjectHash,
        )

        assertNotNull(
            result.config
                .context
                .previousStateHash,
        )
        assertNotNull(
            result.config
                .context
                .metadataDigest,
        )

        assertEquals(
            listOf(
                1,
                2,
                3,
                4,
                5,
                6,
            ),
            result.config
                .outcomes,
        )
        assertEquals(
            16,
            result.config
                .world
                .cellsPerOutcome,
        )
        assertEquals(
            512,
            result.config
                .world
                .timelineTicks,
        )
        assertEquals(
            8,
            result.config
                .world
                .epochCount,
        )
        assertEquals(
            3,
            result.config
                .world
                .probeCount,
        )

        assertEquals(
            96,
            result.world
                .fieldSize,
        )
        assertEquals(
            12,
            result.world
                .width,
        )
        assertEquals(
            8,
            result.world
                .height,
        )
        assertEquals(
            result.outcome,
            result.world
                .sampledOutcome,
        )

        for (
            outcome in
            1..6
        ) {
            assertEquals(
                16,
                result.world
                    .field
                    .count {
                        it ==
                            outcome
                    },
            )
        }

        assertTrue(
            result.outcome in
                1..6,
        )
        assertTrue(
            EntroNexV4Local.verify(
                result,
            ),
        )
    }
}
