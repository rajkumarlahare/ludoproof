package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PendingRollSnapshot
import com.ludoproof.game.PlayerSnapshot
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LudoPawsReactionEngineTest {
    @Before
    fun resetPlaybackDirector() {
        LudoPawsReactionEngine.resetPlaybackStateForTests()
    }

    @Test
    fun detectsSixWithoutTouchingGameplayState() {
        val previous =
            state(
                history = emptyList(),
            )
        val current =
            state(
                history =
                    listOf(
                        event(
                            eventIndex = 0,
                            playerId = "p1",
                            outcome = 6,
                        ),
                    ),
                pending =
                    pending(
                        eventIndex = 0,
                        outcome = 6,
                    ),
            )

        val reactions =
            LudoPawsReactionEngine.detect(
                previous,
                current,
            )

        assertTrue(
            reactions.any {
                it.playerId == "p1" &&
                    it.voiceCue == VoiceCue.SIX
            },
        )
        assertFalse(
            reactions.any {
                it.voiceCue == VoiceCue.THIRD_SIX
            },
        )
    }

    @Test
    fun thirdConsecutiveSixWinsOverNormalSixReaction() {
        val first =
            event(
                eventIndex = 0,
                playerId = "p1",
                outcome = 6,
            )
        val second =
            event(
                eventIndex = 1,
                playerId = "p1",
                outcome = 6,
            )
        val third =
            event(
                eventIndex = 2,
                playerId = "p1",
                outcome = 6,
            )
        val previous =
            state(
                history =
                    listOf(
                        first,
                        second,
                    ),
            )
        val current =
            state(
                history =
                    listOf(
                        first,
                        second,
                        third,
                    ),
            )

        val reactions =
            LudoPawsReactionEngine.detect(
                previous,
                current,
            )

        assertTrue(
            reactions.any {
                it.voiceCue == VoiceCue.THIRD_SIX
            },
        )
        assertFalse(
            reactions.any {
                it.voiceCue == VoiceCue.SIX
            },
        )
    }

    @Test
    fun captureProducesAttackerAndCapturedReactions() {
        val rollEvent =
            event(
                eventIndex = 4,
                playerId = "p1",
                outcome = 3,
            )
        val previous =
            state(
                players =
                    listOf(
                        player(
                            id = "p1",
                            color = "RED",
                            seat = 0,
                            tokens = listOf(5, -1, -1, -1),
                        ),
                        player(
                            id = "p2",
                            color = "GREEN",
                            seat = 1,
                            tokens = listOf(47, -1, -1, -1),
                        ),
                    ),
                history =
                    listOf(
                        rollEvent,
                    ),
                pending =
                    pending(
                        eventIndex = 4,
                        outcome = 3,
                    ),
            )
        val current =
            state(
                players =
                    listOf(
                        player(
                            id = "p1",
                            color = "RED",
                            seat = 0,
                            tokens = listOf(8, -1, -1, -1),
                        ),
                        player(
                            id = "p2",
                            color = "GREEN",
                            seat = 1,
                            tokens = listOf(-1, -1, -1, -1),
                        ),
                    ),
                history =
                    listOf(
                        rollEvent.copy(
                            moveTokenIndex = 0,
                            captures = 1,
                        ),
                    ),
            )

        val reactions =
            LudoPawsReactionEngine.detect(
                previous,
                current,
            )

        assertTrue(
            reactions.any {
                it.playerId == "p1" &&
                    it.voiceCue == VoiceCue.CAPTURE
            },
        )
        assertTrue(
            reactions.any {
                it.playerId == "p2" &&
                    it.voiceCue == VoiceCue.CAPTURED
            },
        )
    }

    @Test
    fun safeHomeFrustratedAndFinishAreDerivedFromSnapshots() {
        val safeRoll =
            event(
                eventIndex = 0,
                playerId = "p1",
                outcome = 1,
                moveTokenIndex = 0,
            )
        val safePrevious =
            state(
                players =
                    listOf(
                        player(
                            id = "p1",
                            color = "RED",
                            seat = 0,
                            tokens = listOf(7, -1, -1, -1),
                        ),
                        player(
                            id = "p2",
                            color = "GREEN",
                            seat = 1,
                        ),
                    ),
                history =
                    listOf(
                        safeRoll.copy(
                            moveTokenIndex = null,
                        ),
                    ),
                pending =
                    pending(
                        eventIndex = 0,
                        outcome = 1,
                    ),
            )
        val safeCurrent =
            state(
                players =
                    listOf(
                        player(
                            id = "p1",
                            color = "RED",
                            seat = 0,
                            tokens = listOf(8, -1, -1, -1),
                        ),
                        player(
                            id = "p2",
                            color = "GREEN",
                            seat = 1,
                        ),
                    ),
                history =
                    listOf(
                        safeRoll,
                    ),
            )
        assertTrue(
            LudoPawsReactionEngine
                .derive(
                    safePrevious,
                    safeCurrent,
                )
                .any {
                    it.voiceCue == VoiceCue.SAFE
                },
        )

        val homePrevious =
            state(
                players =
                    listOf(
                        player(
                            id = "p1",
                            color = "RED",
                            seat = 0,
                            tokens = listOf(56, 57, 57, 57),
                        ),
                        player(
                            id = "p2",
                            color = "GREEN",
                            seat = 1,
                        ),
                    ),
                history =
                    listOf(
                        event(
                            eventIndex = 5,
                            playerId = "p1",
                            outcome = 1,
                        ),
                    ),
                pending =
                    pending(
                        eventIndex = 5,
                        outcome = 1,
                    ),
            )
        val homeCurrent =
            state(
                status = "FINISHED",
                winnerPlayerId = "p1",
                players =
                    listOf(
                        player(
                            id = "p1",
                            color = "RED",
                            seat = 0,
                            tokens = listOf(57, 57, 57, 57),
                        ),
                        player(
                            id = "p2",
                            color = "GREEN",
                            seat = 1,
                        ),
                    ),
                history =
                    listOf(
                        event(
                            eventIndex = 5,
                            playerId = "p1",
                            outcome = 1,
                            moveTokenIndex = 0,
                        ),
                    ),
            )
        val finishReactions =
            LudoPawsReactionEngine.derive(
                homePrevious,
                homeCurrent,
            )
        assertTrue(
            finishReactions.any {
                it.voiceCue == VoiceCue.HOME
            },
        )
        assertTrue(
            finishReactions.any {
                it.playerId == "p1" &&
                    it.voiceCue == VoiceCue.VICTORY
            },
        )
        assertTrue(
            finishReactions.any {
                it.playerId == "p2" &&
                    it.voiceCue == VoiceCue.DEFEAT
            },
        )

        val frustrated =
            LudoPawsReactionEngine.derive(
                state(
                    history = emptyList(),
                ),
                state(
                    history =
                        listOf(
                            event(
                                eventIndex = 9,
                                playerId = "p1",
                                outcome = 2,
                            ),
                        ),
                ),
            )
        assertTrue(
            frustrated.any {
                it.voiceCue == VoiceCue.FRUSTRATED
            },
        )
    }

    private fun state(
        status: String = "ACTIVE",
        winnerPlayerId: String? = null,
        players: List<PlayerSnapshot> =
            listOf(
                player(
                    id = "p1",
                    color = "RED",
                    seat = 0,
                ),
                player(
                    id = "p2",
                    color = "GREEN",
                    seat = 1,
                ),
            ),
        history: List<HistoryEventSnapshot> = emptyList(),
        pending: PendingRollSnapshot? = null,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "reaction-match",
            status = status,
            hostPlayerId = "p1",
            players = players,
            turnSeat = 0,
            randomEventIndex = history.size,
            pendingRoll = pending,
            winnerPlayerId = winnerPlayerId,
            rulesetId = "classic",
            history = history,
        )

    private fun player(
        id: String,
        color: String,
        seat: Int,
        tokens: List<Int> =
            listOf(
                -1,
                -1,
                -1,
                -1,
            ),
    ): PlayerSnapshot =
        PlayerSnapshot(
            playerId = id,
            displayName = id,
            color = color,
            seat = seat,
            tokens = tokens,
        )

    private fun event(
        eventIndex: Int,
        playerId: String,
        outcome: Int,
        moveTokenIndex: Int? = null,
        captures: Int = 0,
    ): HistoryEventSnapshot =
        HistoryEventSnapshot(
            eventIndex = eventIndex,
            playerId = playerId,
            roundId = null,
            proofDigest = null,
            outcome = outcome,
            moveTokenIndex = moveTokenIndex,
            captures = captures,
        )

    private fun pending(
        eventIndex: Int,
        outcome: Int,
    ): PendingRollSnapshot =
        PendingRollSnapshot(
            status = "RESOLVED",
            seat = 0,
            eventIndex = eventIndex,
            eventId = null,
            roundId = null,
            serverCommitment = null,
            clientCommitment = null,
            revealDeadlineAt = null,
            proofDigest = null,
            outcome = outcome,
            legalTokenIndexes = setOf(0),
        )
}
