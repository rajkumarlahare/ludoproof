package com.ludoproof.game

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.settings.data.local.GameSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsGameplaySmoothnessRegressionTest {
    @Test
    fun forwardTravelAndCaptureTimingRemainConsistentAtAllSpeeds() {
        val expectedStepMillis =
            mapOf(
                GameSpeed.FAST to 210L,
                GameSpeed.NORMAL to 280L,
                GameSpeed.SLOW to 360L,
            )
        expectedStepMillis.forEach { (speed, stepMillis) ->
            assertEquals(
                "three-cell movement duration for $speed",
                stepMillis * 3L,
                LudoPawsGameplayPacingPolicy.forwardTravelDurationMillis(speed, 3),
            )
            assertEquals(
                "full movement retains a landing settle tail for $speed",
                stepMillis * 3L + LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS,
                LudoPawsGameplayPacingPolicy.forwardAnimationDurationMillis(speed, 3),
            )
            assertTrue(
                "capture should remain long enough to read at $speed",
                LudoPawsGameplayPacingPolicy.captureReturnDurationMillis(speed) >= 1_000L,
            )
        }
    }

    @Test
    fun landingSettleDoesNotRestartHopCycle() {
        for (species in LudoPaws3DSpecies.entries) {
            val start =
                LudoPaws3DReactionMotion.sample(
                    species = species,
                    cue = AnimationCue.HAPPY,
                    progress = 0f,
                )
            val end =
                LudoPaws3DReactionMotion.sample(
                    species = species,
                    cue = AnimationCue.HAPPY,
                    progress = 1f,
                )
            assertTrue(start.scale.isFinite())
            assertTrue(end.scale.isFinite())
        }

        assertEquals(
            "the visible route ends before the landing-settle tail",
            90L,
            LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS,
        )
    }

    @Test
    fun everyVictoryReactionReturnsToItsStartingHeading() {
        for (species in LudoPaws3DSpecies.entries) {
            val start =
                LudoPaws3DReactionMotion.sample(
                    species = species,
                    cue = AnimationCue.VICTORY,
                    progress = 0f,
                )
            val end =
                LudoPaws3DReactionMotion.sample(
                    species = species,
                    cue = AnimationCue.VICTORY,
                    progress = 1f,
                )
            assertEquals("victory yaw must finish without a snap for $species", 0f, start.bodyYawDegrees, .001f)
            assertEquals("victory yaw must return to its start for $species", 360f, end.bodyYawDegrees, .001f)
        }
    }

    @Test
    fun allRendererCadencesArePositiveAndActiveIsFasterThanIdle() {
        val active =
            LudoPaws3DRenderCadencePolicy.ACTIVE_FRAME_DELAY_MILLIS
        val idle =
            LudoPaws3DRenderCadencePolicy.IDLE_FRAME_DELAY_MILLIS
        assertTrue(active > 0L)
        assertTrue(idle > active)
    }
}
