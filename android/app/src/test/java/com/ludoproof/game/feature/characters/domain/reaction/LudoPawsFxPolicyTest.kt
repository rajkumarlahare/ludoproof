package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsFxPolicyTest {
    @Test
    fun `victory enables dense confetti in full motion`() {
        val plan =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.VICTORY,
            )

        assertTrue(plan.allowConfetti)
        assertTrue(plan.allowTranslation)
        assertTrue(plan.allowRotation)
        assertTrue(plan.particleCount >= 32)
    }

    @Test
    fun `captured reaction supports full return movement`() {
        val plan =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.CAPTURED,
            )

        assertTrue(plan.allowTranslation)
        assertTrue(plan.allowRotation)
        assertTrue(plan.allowShake)
        assertFalse(plan.allowConfetti)
        assertTrue(plan.durationMs >= 900L)
    }

    @Test
    fun `idle stays intentionally subtle`() {
        val plan =
            LudoPawsFxPolicy.plan(
                cue = AnimationCue.IDLE,
            )

        assertFalse(plan.allowTranslation)
        assertFalse(plan.allowShake)
        assertFalse(plan.allowConfetti)
        assertTrue(plan.particleCount == 0)
    }

    @Test
    fun `every animation cue has a positive production duration`() {
        AnimationCue.entries.forEach { cue ->
            val plan =
                LudoPawsFxPolicy.plan(
                    cue = cue,
                )
            assertTrue(plan.durationMs > 0L)
            assertTrue(plan.particleCount >= 0)
        }
    }
}
