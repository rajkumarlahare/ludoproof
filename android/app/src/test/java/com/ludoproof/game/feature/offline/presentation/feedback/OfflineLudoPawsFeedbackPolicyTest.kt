package com.ludoproof.game.feature.offline.presentation.feedback

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLudoPawsFeedbackPolicyTest {
    @Test
    fun `normal committed move uses move sound`() {
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.MOVE,
                reactions = emptyList(),
                tokenMovementCommitted = true,
            )

        assertEquals(
            OfflineFeedbackSound.MOVE,
            decision.sound,
        )
    }

    @Test
    fun `capture replaces generic move sound`() {
        val capture =
            reaction(
                cue = VoiceCue.CAPTURE,
                animation = AnimationCue.CAPTURE,
                priority = 88,
            )
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.MOVE,
                reactions = listOf(capture),
                tokenMovementCommitted = true,
            )

        assertEquals(
            OfflineFeedbackSound.CAPTURE,
            decision.sound,
        )
    }

    @Test
    fun `victory wins over home feedback`() {
        val home =
            reaction(
                cue = VoiceCue.HOME,
                animation = AnimationCue.HOME,
                priority = 82,
            )
        val victory =
            reaction(
                cue = VoiceCue.VICTORY,
                animation = AnimationCue.VICTORY,
                priority = 100,
            )
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.MOVE,
                reactions = listOf(home, victory),
                tokenMovementCommitted = true,
            )

        assertEquals(
            OfflineFeedbackSound.VICTORY,
            decision.sound,
        )
    }

    @Test
    fun `six gets result chime after roll`() {
        val six =
            reaction(
                cue = VoiceCue.SIX,
                animation = AnimationCue.EXCITED,
                priority = 66,
            )
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.ROLL,
                reactions = listOf(six),
                tokenMovementCommitted = false,
            )

        assertEquals(
            OfflineFeedbackSound.SIX,
            decision.sound,
        )
    }

    @Test
    fun `third six keeps haptic reaction without duplicate result sound`() {
        val thirdSix =
            reaction(
                cue = VoiceCue.THIRD_SIX,
                animation = AnimationCue.ANGRY,
                priority = 92,
            )
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.ROLL,
                reactions = listOf(thirdSix),
                tokenMovementCommitted = false,
            )

        assertEquals(
            OfflineFeedbackSound.NONE,
            decision.sound,
        )
        assertTrue(
            decision.reactions.contains(thirdSix),
        )
    }

    @Test
    fun `silent sync does not invent feedback`() {
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.ROLL,
                reactions = emptyList(),
                tokenMovementCommitted = false,
            )

        assertEquals(
            OfflineFeedbackSound.NONE,
            decision.sound,
        )
        assertTrue(
            decision.reactions.isEmpty(),
        )
    }

    private fun reaction(
        cue: VoiceCue,
        animation: AnimationCue,
        priority: Int,
    ): LudoPawsReaction =
        LudoPawsReaction(
            playerId = "p1",
            seat = 0,
            voiceCue = cue,
            animationCue = animation,
            priority = priority,
            matchId = "local-test",
            eventIndex = 7,
            reactionKey = "local-test:$cue",
        )
}
