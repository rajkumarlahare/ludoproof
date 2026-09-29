package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntroNexV4LocalConformanceTest {
    @Test
    fun candidateVectorMatchesFrozenCrossLanguageExpectedValues() {
        val config =
            LocalV4Config(
                outcomes =
                    listOf(
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                    ),
                context =
                    LocalV4Context(
                        applicationId =
                            "ludo-no-cheat",
                        sessionId =
                            "match-vector-1",
                        eventId =
                            "turn-18-roll-1",
                        eventType =
                            "DICE_ROLL",
                        eventIndex =
                            18,
                        subjectHash =
                            null,
                        previousStateHash =
                            null,
                        metadataDigest =
                            null,
                    ),
                world =
                    LocalV4WorldConfig(
                        cellsPerOutcome =
                            8,
                        timelineTicks =
                            512,
                        epochCount =
                            8,
                        probeCount =
                            3,
                    ),
            )

        val result =
            EntroNexV4Local.resolve(
                roundId =
                    "entronex-v4-conformance-1",
                serverSeed =
                    "0".repeat(
                        64,
                    ),
                clientSeed =
                    "1".repeat(
                        64,
                    ),
                config =
                    config,
            )

        assertEquals(
            "07f8641963a9fb2da6435c1bc04682d3929ec683f56fef41083be8bb6d13fb13",
            result.serverCommitment,
        )
        assertEquals(
            "3206df6b42dfc2267cf717778a78c8b20ce5b3a5ba221f2d0f93c3d79863f729",
            result.clientCommitment,
        )
        assertEquals(
            "07538d7a18941f650ce90d6e3fef8cec3afdc93d0414a8484d2d399c8dc05924",
            result.configDigest,
        )
        assertEquals(
            "81d0cf37723578e3269395b11e5d630e0755387f6fd84b17bab116e1263015a3",
            result.contextDigest,
        )
        assertEquals(
            "273bbdb99a37fbfb571aeb94ea3362f216df2cb01b4edc28d1f1d4ab8c4c165c",
            result.eventBindingDigest,
        )
        assertEquals(
            "cf52da8dddca6370a824f14d2449855f78a73205ba6a81a3670525e67daa43fc",
            result.transcriptDigest,
        )
        assertEquals(
            1,
            result.outcomeIndex,
        )
        assertEquals(
            2,
            result.outcome,
        )

        assertEquals(
            8,
            result.world.width,
        )
        assertEquals(
            6,
            result.world.height,
        )
        assertEquals(
            169,
            result.world.sampleTick,
        )
        assertEquals(
            2,
            result.world.layoutEpoch,
        )
        assertEquals(
            2,
            result.world.selectedProbe,
        )
        assertEquals(
            25,
            result.world.sampleIndex,
        )
        assertEquals(
            "scatter-wave",
            result.world.motionProfile,
        )
        assertTrue(
            result.world.witness.swapped,
        )
        assertEquals(
            46,
            result.world.witness.sourceIndex,
        )
        assertEquals(
            25,
            result.world.witness.targetIndex,
        )
        assertEquals(
            "eabd09e6dad8342bb5cc8724c9bcbb90ae95263fb5dc847449739fa14ac102a3",
            result.world.fieldDigest,
        )
        assertEquals(
            "0cdf48f6a5c0afbf3837ff073a73963b57415397fb70339e3949be7e16caeffe",
            result.world.worldDigest,
        )
        assertEquals(
            "bb2b3f384f6a1ef71c2680402c30c01bbe23a5e7faf2b025553095d7ab36d27e",
            result.proofDigest,
        )
        assertTrue(
            EntroNexV4Local.verify(
                result,
            ),
        )
    }

    @Test
    fun ludoOfflineProfileUsesSameLockedWorldShapeAsOnlineWorker() {
        val config =
            LocalV4Config(
                outcomes =
                    listOf(
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                    ),
                context =
                    LocalV4Context(
                        applicationId =
                            "ludoproof",
                        sessionId =
                            "offline-test",
                        eventId =
                            "roll:0",
                        eventType =
                            "DICE_ROLL",
                        eventIndex =
                            0,
                    ),
                world =
                    LocalV4WorldConfig(
                        cellsPerOutcome =
                            16,
                        timelineTicks =
                            512,
                        epochCount =
                            8,
                        probeCount =
                            3,
                    ),
            )

        val result =
            EntroNexV4Local.resolve(
                roundId =
                    "offline-test-round",
                serverSeed =
                    "2".repeat(
                        64,
                    ),
                clientSeed =
                    "3".repeat(
                        64,
                    ),
                config =
                    config,
            )

        assertEquals(
            96,
            result.world.fieldSize,
        )
        assertEquals(
            12,
            result.world.width,
        )
        assertEquals(
            8,
            result.world.height,
        )
        assertEquals(
            16,
            result.world.field
                .count {
                    it == 1
                },
        )
        assertEquals(
            16,
            result.world.field
                .count {
                    it == 2
                },
        )
        assertEquals(
            16,
            result.world.field
                .count {
                    it == 3
                },
        )
        assertEquals(
            16,
            result.world.field
                .count {
                    it == 4
                },
        )
        assertEquals(
            16,
            result.world.field
                .count {
                    it == 5
                },
        )
        assertEquals(
            16,
            result.world.field
                .count {
                    it == 6
                },
        )
        assertEquals(
            result.outcome,
            result.world.sampledOutcome,
        )
        assertTrue(
            EntroNexV4Local.verify(
                result,
            ),
        )
    }
}
