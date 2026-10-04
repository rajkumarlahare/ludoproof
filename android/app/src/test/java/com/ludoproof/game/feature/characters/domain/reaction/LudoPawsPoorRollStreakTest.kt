package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PlayerSnapshot
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsPoorRollStreakTest {
    @Test
    fun threePoorRollsForSamePlayerAcrossOtherTurnsCreateOneStreakMoment() {
        val history =
            listOf(
                event(0, "p1", 1),
                event(1, "p2", 5),
                event(2, "p1", 2),
                event(3, "p2", 4),
                event(4, "p1", 1),
            )
        val previous =
            snapshot(history.take(4))
        val current =
            snapshot(history)

        val moments =
            LudoPawsGameMomentDetector.detect(
                previous = previous,
                current = current,
            )

        assertTrue(
            moments.any {
                it.type == GameMomentType.POOR_ROLL_STREAK &&
                    it.playerId == "p1" &&
                    it.eventIndex == 4 &&
                    it.value == 3
            },
        )
    }

    @Test
    fun goodRollBreaksPoorRollStreak() {
        val history =
            listOf(
                event(0, "p1", 1),
                event(1, "p2", 4),
                event(2, "p1", 5),
                event(3, "p2", 3),
                event(4, "p1", 2),
            )

        val moments =
            LudoPawsGameMomentDetector.detect(
                previous = snapshot(history.take(4)),
                current = snapshot(history),
            )

        assertFalse(
            moments.any {
                it.type == GameMomentType.POOR_ROLL_STREAK
            },
        )
    }

    @Test
    fun poorRollStreakMapsToControlledFrustrationReaction() {
        val history =
            listOf(
                event(0, "p1", 2),
                event(1, "p2", 6),
                event(2, "p1", 1),
                event(3, "p2", 4),
                event(4, "p1", 2),
            )

        val reactions =
            LudoPawsReactionEngine.derive(
                previous = snapshot(history.take(4)),
                current = snapshot(history),
            )

        assertTrue(
            reactions.any {
                it.momentType == GameMomentType.POOR_ROLL_STREAK &&
                    it.voiceCue == VoiceCue.FRUSTRATED &&
                    it.animationCue == AnimationCue.SAD
            },
        )
    }

    private fun snapshot(
        history: List<HistoryEventSnapshot>,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "poor-roll-match",
            status = "ACTIVE",
            hostPlayerId = "p1",
            players =
                listOf(
                    player("p1", "RED", 0),
                    player("p2", "GREEN", 1),
                ),
            turnSeat = 0,
            randomEventIndex =
                history
                    .lastOrNull()
                    ?.eventIndex
                    ?.plus(1)
                    ?: 0,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "classic",
            history = history,
        )

    private fun player(
        id: String,
        color: String,
        seat: Int,
    ): PlayerSnapshot =
        PlayerSnapshot(
            playerId = id,
            displayName = id,
            color = color,
            seat = seat,
            tokens = listOf(-1, -1, -1, -1),
        )

    private fun event(
        index: Int,
        playerId: String,
        outcome: Int,
    ): HistoryEventSnapshot =
        HistoryEventSnapshot(
            eventIndex = index,
            playerId = playerId,
            roundId = null,
            proofDigest = null,
            outcome = outcome,
            moveTokenIndex = null,
            captures = 0,
        )
}
