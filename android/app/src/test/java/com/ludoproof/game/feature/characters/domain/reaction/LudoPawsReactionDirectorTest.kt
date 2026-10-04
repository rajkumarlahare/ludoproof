package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsReactionDirectorTest {
    @Test
    fun duplicateStableKeyIsConsumedOnlyOnce() {
        val director =
            LudoPawsReactionDirector()
        val reaction =
            reaction(
                eventIndex = 7,
                type = GameMomentType.SIX_ROLLED,
                cue = VoiceCue.SIX,
                priority = 66,
            )

        val first =
            director.submit(
                listOf(reaction),
                nowMillis = 0L,
            )
        val replay =
            director.submit(
                listOf(reaction),
                nowMillis = 2_000L,
            )

        assertEquals(1, first.reactions.size)
        assertTrue(replay.reactions.isEmpty())
    }

    @Test
    fun noisyCueCooldownSuppressesDifferentEventsFromSamePlayer() {
        val director =
            LudoPawsReactionDirector()
        val first =
            reaction(
                eventIndex = 1,
                type = GameMomentType.NO_LEGAL_MOVE,
                cue = VoiceCue.FRUSTRATED,
                priority = 48,
            )
        val tooSoon =
            reaction(
                eventIndex = 2,
                type = GameMomentType.NO_LEGAL_MOVE,
                cue = VoiceCue.FRUSTRATED,
                priority = 48,
            )
        val afterCooldown =
            reaction(
                eventIndex = 3,
                type = GameMomentType.NO_LEGAL_MOVE,
                cue = VoiceCue.FRUSTRATED,
                priority = 48,
            )

        assertEquals(
            1,
            director.submit(
                listOf(first),
                nowMillis = 0L,
            ).reactions.size,
        )
        assertTrue(
            director.submit(
                listOf(tooSoon),
                nowMillis = 1_000L,
            ).reactions.isEmpty(),
        )
        assertEquals(
            1,
            director.submit(
                listOf(afterCooldown),
                nowMillis = 4_000L,
            ).reactions.size,
        )
    }

    @Test
    fun lowerPriorityReactionQueuesUntilPlaybackWindowExpires() {
        val director =
            LudoPawsReactionDirector(
                playbackWindowMs = 900L,
            )
        val six =
            reaction(
                eventIndex = 1,
                type = GameMomentType.SIX_ROLLED,
                cue = VoiceCue.SIX,
                priority = 66,
            )
        val safe =
            reaction(
                eventIndex = 2,
                type = GameMomentType.SAFE_REACHED,
                cue = VoiceCue.SAFE,
                priority = 58,
            )

        val first =
            director.submit(
                listOf(six),
                nowMillis = 0L,
            )
        val queued =
            director.submit(
                listOf(safe),
                nowMillis = 100L,
            )
        val drained =
            director.submit(
                emptyList(),
                nowMillis = 1_000L,
            )

        assertEquals(VoiceCue.SIX, first.reactions.single().voiceCue)
        assertTrue(queued.reactions.isEmpty())
        assertEquals(1, queued.queuedBatchCount)
        assertEquals(VoiceCue.SAFE, drained.reactions.single().voiceCue)
    }

    @Test
    fun captureCanInterruptLowerPriorityPlayback() {
        val director =
            LudoPawsReactionDirector(
                playbackWindowMs = 900L,
            )
        val safe =
            reaction(
                eventIndex = 1,
                type = GameMomentType.SAFE_REACHED,
                cue = VoiceCue.SAFE,
                priority = 58,
            )
        val capture =
            reaction(
                eventIndex = 2,
                type = GameMomentType.CAPTURE_MADE,
                cue = VoiceCue.CAPTURE,
                priority = 88,
            )

        director.submit(
            listOf(safe),
            nowMillis = 0L,
        )
        val interrupted =
            director.submit(
                listOf(capture),
                nowMillis = 100L,
            )

        assertTrue(interrupted.interrupted)
        assertEquals(
            VoiceCue.CAPTURE,
            interrupted.reactions.single().voiceCue,
        )
    }

    @Test
    fun sameEventWinAndLossStayInOneDeterministicBatch() {
        val director =
            LudoPawsReactionDirector()
        val winner =
            reaction(
                eventIndex = 12,
                type = GameMomentType.MATCH_WIN,
                playerId = "p1",
                cue = VoiceCue.VICTORY,
                priority = 100,
            )
        val loser =
            reaction(
                eventIndex = 12,
                type = GameMomentType.MATCH_LOSS,
                playerId = "p2",
                cue = VoiceCue.DEFEAT,
                priority = 30,
            )

        val decision =
            director.submit(
                listOf(loser, winner),
                nowMillis = 0L,
            )

        assertEquals(2, decision.reactions.size)
        assertEquals(VoiceCue.VICTORY, decision.reactions.first().voiceCue)
        assertEquals(VoiceCue.DEFEAT, decision.reactions.last().voiceCue)
        assertFalse(decision.interrupted)
    }

    private fun reaction(
        eventIndex: Int,
        type: GameMomentType,
        playerId: String = "p1",
        cue: VoiceCue,
        priority: Int,
    ): LudoPawsReaction =
        LudoPawsReaction(
            playerId = playerId,
            seat = if (playerId == "p1") 0 else 1,
            voiceCue = cue,
            animationCue =
                when (cue) {
                    VoiceCue.CAPTURE -> AnimationCue.CAPTURE
                    VoiceCue.CAPTURED -> AnimationCue.CAPTURED
                    VoiceCue.SAFE -> AnimationCue.SAFE
                    VoiceCue.VICTORY -> AnimationCue.VICTORY
                    VoiceCue.DEFEAT -> AnimationCue.DEFEAT
                    VoiceCue.THIRD_SIX -> AnimationCue.ANGRY
                    VoiceCue.FRUSTRATED -> AnimationCue.SAD
                    VoiceCue.NERVOUS -> AnimationCue.NERVOUS
                    VoiceCue.HOME -> AnimationCue.HOME
                    VoiceCue.IDLE -> AnimationCue.IDLE
                    VoiceCue.SIX -> AnimationCue.EXCITED
                },
            priority = priority,
            matchId = "match-a",
            eventIndex = eventIndex,
            momentType = type,
            reactionKey =
                listOf(
                    "match-a",
                    eventIndex.toString(),
                    type.name,
                    playerId,
                    "",
                ).joinToString(":"),
        )
}
