package com.ludoproof.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFairnessChainTest {
    @Test
    fun fairnessChainDetectsTamperingAndSupportsRetainedHistoryWindows() {
        val history =
            mutableListOf<
                OfflineFairnessMaterial
                >()

        repeat(
            5,
        ) {
                index ->
            val event =
                OfflineFairnessMaterial(
                    eventIndex =
                        index,
                    playerId =
                        "player-" +
                            (index % 2),
                    color =
                        if (
                            index % 2 ==
                                0
                        ) {
                            "RED"
                        } else {
                            "GREEN"
                        },
                    roundId =
                        "round-$index",
                    serverCommitment =
                        "a".repeat(
                            64,
                        ),
                    clientCommitment =
                        "b".repeat(
                            64,
                        ),
                    actorHash =
                        "c".repeat(
                            64,
                        ),
                    previousStateHash =
                        "d".repeat(
                            64,
                        ),
                    rulesetHash =
                        "e".repeat(
                            64,
                        ),
                    proofDigest =
                        "f".repeat(
                            64,
                        ),
                    outcome =
                        (
                            index %
                                6
                            ) +
                            1,
                )
            history +=
                OfflineFairnessChain
                    .seal(
                        event,
                        history
                            .lastOrNull()
                            ?.fairnessDigest,
                    )
        }

        assertTrue(
            OfflineFairnessChain
                .verifyHistory(
                    history,
                ),
        )
        assertTrue(
            OfflineFairnessChain
                .verifyHistory(
                    history
                        .drop(
                            2,
                        ),
                ),
        )

        val tampered =
            history.toMutableList()
        tampered[
            2
        ] =
            tampered[
                2
            ].copy(
                outcome =
                    6,
            )
        assertFalse(
            OfflineFairnessChain
                .verifyHistory(
                    tampered,
                ),
        )
    }
}
