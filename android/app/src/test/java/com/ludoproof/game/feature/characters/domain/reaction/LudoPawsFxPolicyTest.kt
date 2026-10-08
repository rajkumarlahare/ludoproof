package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsFxPolicyTest {
    @Test
    fun `reduced motion removes translation rotation shake and confetti`() {
        AnimationCue.entries.forEach { cue ->
            val plan =
                LudoPawsFxPolicy.plan(
                    cue = cue,
                    reducedMotion = true,
                )

            assertFalse(plan.allowTranslation)
            assertFalse(plan.allowRotation)
            assertFalse(plan.allowShake)
            assertFalse(plan.allowConfetti)
            assertTrue(plan.durationMs <= 250L)
            assertTrue(plan.particleCount <= 4)
        }
    }

    @Test
    fun `victory enables dense confetti only in full motion`() {
        val full =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.VICTORY,
                reducedMotion = false,
            )
        val reduced =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.VICTORY,
                reducedMotion = true,
            )

        assertTrue(full.allowConfetti)
        assertTrue(full.particleCount > reduced.particleCount)
        assertFalse(reduced.allowConfetti)
    }

    @Test
    fun `captured reaction supports return movement in full motion`() {
        val captured =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.CAPTURED,
                reducedMotion = false,
            )

        assertTrue(captured.allowTranslation)
        assertTrue(captured.allowShake)
        assertTrue(captured.durationMs >= 900L)
    }

    @Test
    fun `idle stays intentionally subtle`() {
        val idle =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.IDLE,
                reducedMotion = false,
            )

        assertFalse(idle.allowTranslation)
        assertFalse(idle.allowShake)
        assertFalse(idle.allowConfetti)
        assertTrue(idle.particleCount == 0)
    }
}
