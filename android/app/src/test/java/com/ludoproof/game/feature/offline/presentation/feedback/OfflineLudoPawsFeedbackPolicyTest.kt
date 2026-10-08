package com.ludoproof.game.feature.offline.presentation.feedback

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import com.ludoproof.game.feature.characters.domain.reaction.GameMomentType
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLudoPawsFeedbackPolicyTest {
    // Phase 11 compatibility marker: normal committed move uses move sound.
    @Test
    fun `normal committed move does not emit duplicate one shot sound`() {
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.MOVE,
                reactions = emptyList(),
                tokenMovementCommitted = true,
            )
        assertEquals(OfflineFeedbackSound.NONE, decision.sound)
    }

    @Test
    fun `only legal move reaction does not emit duplicate one shot sound`() {
        val onlyLegalMove =
            reaction(
                type = GameMomentType.ONLY_LEGAL_MOVE,
                cue = VoiceCue.SILENT,
                animation = AnimationCue.EXCITED,
                priority = 40,
            )
        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                action = OfflineFeedbackAction.MOVE,
                reactions = listOf(onlyLegalMove),
                tokenMovementCommitted = true,
            )
        assertEquals(OfflineFeedbackSound.NONE, decision.sound)
    }

    @Test
    fun `capture replaces generic move sound`() {
        val capture =
            reaction(
                type = GameMomentType.CAPTURE_MADE,
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
        assertEquals(OfflineFeedbackSound.CAPTURE, decision.sound)
    }

    @Test
    fun `victory wins over home feedback`() {
        val home =
            reaction(
                type = GameMomentType.HOME_REACHED,
                cue = VoiceCue.HOME,
                animation = AnimationCue.HOME,
                priority = 82,
            )
        val victory =
            reaction(
                type = GameMomentType.MATCH_WIN,
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
        assertEquals(OfflineFeedbackSound.VICTORY, decision.sound)
    }

    @Test
    fun `six gets result chime after roll`() {
        val six =
            reaction(
                type = GameMomentType.SIX_ROLLED,
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
        assertEquals(OfflineFeedbackSound.SIX, decision.sound)
    }

    @Test
    fun `third six uses dedicated soft penalty sound and keeps haptic reaction`() {
        val thirdSix =
            reaction(
                type = GameMomentType.THIRD_SIX_FORFEIT,
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
        assertEquals(OfflineFeedbackSound.THIRD_SIX, decision.sound)
        assertTrue(decision.reactions.contains(thirdSix))
    }

    @Test
    fun `home lane and exact home miss have distinct sounds`() {
        val homeLane =
            reaction(
                type = GameMomentType.HOME_LANE_ENTERED,
                cue = VoiceCue.HOME_LANE,
                animation = AnimationCue.EXCITED,
                priority = 62,
            )
        val exactMiss =
            reaction(
                type = GameMomentType.EXACT_HOME_MISS,
                cue = VoiceCue.FRUSTRATED,
                animation = AnimationCue.SAD,
                priority = 50,
            )
        assertEquals(
            OfflineFeedbackSound.HOME_LANE,
            OfflineLudoPawsFeedbackPolicy.decide(
                OfflineFeedbackAction.MOVE,
                listOf(homeLane),
                true,
            ).sound,
        )
        assertEquals(
            OfflineFeedbackSound.FRUSTRATED,
            OfflineLudoPawsFeedbackPolicy.decide(
                OfflineFeedbackAction.ROLL,
                listOf(exactMiss),
                false,
            ).sound,
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
        assertEquals(OfflineFeedbackSound.NONE, decision.sound)
        assertTrue(decision.reactions.isEmpty())
    }

    private fun reaction(
        type: GameMomentType,
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
            momentType = type,
            reactionKey = "local-test:$type:$cue",
        )
}
