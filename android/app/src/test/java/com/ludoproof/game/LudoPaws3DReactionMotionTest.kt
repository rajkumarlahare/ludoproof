package com.ludoproof.game

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LudoPaws3DReactionMotionTest {
    @Test
    fun everySpeciesAndCueHasSafeFiniteMotion() {
        for (species in LudoPaws3DSpecies.entries) {
            for (cue in AnimationCue.entries) {
                assertTrue(
                    "duration must be positive for $species/$cue",
                    LudoPaws3DReactionMotion.durationMillis(species, cue) > 0L,
                )
                for (progress in listOf(0f, .25f, .5f, .75f, 1f)) {
                    val pose = LudoPaws3DReactionMotion.sample(species, cue, progress)
                    val values =
                        listOf(
                            pose.liftY,
                            pose.bodyYawDegrees,
                            pose.headTiltDegrees,
                            pose.primaryAppendageDegrees,
                            pose.secondaryAppendageDegrees,
                            pose.tertiaryAppendageDegrees,
                            pose.scale,
                        )
                    assertTrue(
                        "all motion values must stay finite for $species/$cue@$progress",
                        values.all(Float::isFinite),
                    )
                    assertTrue(
                        "reaction scale must stay subtle for $species/$cue@$progress",
                        pose.scale in .85f..1.15f,
                    )
                }
            }
        }
    }

    @Test
    fun homeAddsBodyLanguageButNeverAddsSecondFullSpin() {
        for (species in LudoPaws3DSpecies.entries) {
            for (progress in listOf(.25f, .5f, .75f)) {
                val home =
                    LudoPaws3DReactionMotion.sample(
                        species = species,
                        cue = AnimationCue.HOME,
                        progress = progress,
                    )
                assertEquals(
                    "base HOME timeline owns rotation for $species",
                    0f,
                    home.bodyYawDegrees,
                    .001f,
                )
            }
        }
    }

    @Test
    fun victoryKeepsIndependentCelebrationRotation() {
        for (species in LudoPaws3DSpecies.entries) {
            val pose =
                LudoPaws3DReactionMotion.sample(
                    species = species,
                    cue = AnimationCue.VICTORY,
                    progress = .5f,
                )
            assertTrue(
                "victory needs readable celebration rotation for $species",
                abs(pose.bodyYawDegrees) >= 120f,
            )
        }
    }

    @Test
    fun speciesIdentityIsVisibleInReactionAnatomy() {
        val dogExcited =
            LudoPaws3DReactionMotion.sample(
                LudoPaws3DSpecies.DOG,
                AnimationCue.EXCITED,
                .5f,
            )
        val goatCapture =
            LudoPaws3DReactionMotion.sample(
                LudoPaws3DSpecies.GOAT,
                AnimationCue.CAPTURE,
                .5f,
            )
        val duckExcited =
            LudoPaws3DReactionMotion.sample(
                LudoPaws3DSpecies.DUCK,
                AnimationCue.EXCITED,
                .5f,
            )
        val catCapture =
            LudoPaws3DReactionMotion.sample(
                LudoPaws3DSpecies.CAT,
                AnimationCue.CAPTURE,
                .5f,
            )

        assertTrue(abs(dogExcited.secondaryAppendageDegrees) >= 20f)
        assertTrue(goatCapture.headTiltDegrees <= -10f)
        assertTrue(duckExcited.primaryAppendageDegrees >= 25f)
        assertTrue(abs(catCapture.secondaryAppendageDegrees) >= 20f)
    }
}
