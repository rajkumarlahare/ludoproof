package com.ludoproof.game.feature.characters.domain.audio

import com.ludoproof.game.feature.characters.domain.model.AnimalSpecies
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import com.ludoproof.game.feature.characters.domain.reaction.GameMomentType
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsReactionAudioProfileTest {
    @Test
    fun stableNamesMakeCharacterClipsDropInReplaceable() {
        val names =
            LudoPawsReactionAudioProfile.resourceNames(
                characterId = "dog",
                cue = VoiceCue.CAPTURE,
            )
        assertEquals(
            listOf(
                "lp_vocal_dog_capture_01",
                "lp_vocal_dog_capture_02",
            ),
            names,
        )
    }

    @Test
    fun silentVisualReactionNeverRequestsVocal() {
        assertNull(
            LudoPawsReactionAudioProfile.plan(
                characterId = "cat",
                species = AnimalSpecies.CAT,
                reaction = reaction(VoiceCue.SILENT, GameMomentType.TURN_STARTED),
            ),
        )
    }

    @Test
    fun captureAndHomeAreGuaranteedWhileSixIsControlled() {
        val capture =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "duck",
                    AnimalSpecies.DUCK,
                    reaction(VoiceCue.CAPTURE, GameMomentType.CAPTURE_MADE),
                ),
            )
        val home =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "goat",
                    AnimalSpecies.GOAT,
                    reaction(VoiceCue.HOME, GameMomentType.HOME_REACHED),
                ),
            )
        val six =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "dog",
                    AnimalSpecies.DOG,
                    reaction(VoiceCue.SIX, GameMomentType.SIX_ROLLED),
                ),
            )

        assertEquals(100, capture.probabilityPercent)
        assertEquals(100, home.probabilityPercent)
        assertTrue(six.probabilityPercent in 60..75)
    }

    @Test
    fun poorRollStreakVocalIsGuaranteedButSingleNoMoveIsQuiet() {
        val streak =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "cat",
                    AnimalSpecies.CAT,
                    reaction(VoiceCue.FRUSTRATED, GameMomentType.POOR_ROLL_STREAK),
                ),
            )
        val noMove =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "cat",
                    AnimalSpecies.CAT,
                    reaction(VoiceCue.FRUSTRATED, GameMomentType.NO_LEGAL_MOVE),
                ),
            )
        assertEquals(100, streak.probabilityPercent)
        assertTrue(noMove.probabilityPercent < 50)
    }

    @Test
    fun deterministicProbabilityAndVariantNeverDependOnRuntimeRandom() {
        val plan =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "dog",
                    AnimalSpecies.DOG,
                    reaction(VoiceCue.SIX, GameMomentType.SIX_ROLLED),
                ),
            )
        val key = "match:7:SIX_ROLLED:p1:0"
        val first = LudoPawsReactionAudioProfile.shouldPlay(plan, key)
        val second = LudoPawsReactionAudioProfile.shouldPlay(plan, key)
        assertEquals(first, second)

        val variantA =
            LudoPawsReactionAudioProfile.deterministicVariantIndex(
                key,
                plan.rawResourceNames.size,
            )
        val variantB =
            LudoPawsReactionAudioProfile.deterministicVariantIndex(
                key,
                plan.rawResourceNames.size,
            )
        assertEquals(variantA, variantB)
        assertTrue(variantA in plan.rawResourceNames.indices)
    }

    @Test
    fun captureVictimIsDelayedAfterAttackerForReadableComedyTiming() {
        val attacker =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "dog",
                    AnimalSpecies.DOG,
                    reaction(VoiceCue.CAPTURE, GameMomentType.CAPTURE_MADE),
                ),
            )
        val victim =
            requireNotNull(
                LudoPawsReactionAudioProfile.plan(
                    "cat",
                    AnimalSpecies.CAT,
                    reaction(VoiceCue.CAPTURED, GameMomentType.TOKEN_CAPTURED),
                ),
            )
        assertTrue(victim.delayMillis > attacker.delayMillis)
        assertFalse(victim.delayMillis > 300L)
    }

    private fun reaction(
        cue: VoiceCue,
        type: GameMomentType,
    ): LudoPawsReaction =
        LudoPawsReaction(
            playerId = "p1",
            seat = 0,
            tokenIndex = 0,
            voiceCue = cue,
            animationCue = AnimationCue.IDLE,
            priority = 50,
            matchId = "match",
            eventIndex = 7,
            momentType = type,
            reactionKey = "match:7:${type.name}:p1:0",
        )
}
