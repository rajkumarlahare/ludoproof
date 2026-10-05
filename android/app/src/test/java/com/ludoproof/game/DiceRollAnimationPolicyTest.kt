package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollAnimationPolicyTest {
    @Test
    fun rollingFrameCyclesFacesWithoutClaimingOutcome() {
        val first =
            DiceRollAnimationPolicy.rollingFrame(0L)
        val later =
            DiceRollAnimationPolicy.rollingFrame(160L)

        assertTrue(first.face in 1..6)
        assertTrue(later.face in 1..6)
        assertTrue(first.face != later.face)
        assertFalse(first.finished)
        assertFalse(later.finished)
    }

    @Test
    fun rollingFrameHasPhysicalTransform() {
        val frame =
            DiceRollAnimationPolicy.rollingFrame(170L)

        assertTrue(kotlin.math.abs(frame.rotationDegrees) > .01f)
        assertTrue(frame.scale > .9f)
        assertTrue(frame.translationYFraction <= 0f)
        assertTrue(frame.borderPulse in 0f..1f)
    }

    @Test
    fun settleKeepsVerifiedOutcomeAndEndsAtIdentity() {
        val middle =
            DiceRollAnimationPolicy.settleFrame(
                elapsedMillis = 150L,
                outcome = 6,
                startRotationDegrees = 48f,
            )
        val end =
            DiceRollAnimationPolicy.settleFrame(
                elapsedMillis = DiceRollAnimationPolicy.SETTLE_DURATION_MILLIS,
                outcome = 6,
                startRotationDegrees = 48f,
            )

        assertEquals(6, middle.face)
        assertEquals(6, end.face)
        assertFalse(middle.finished)
        assertTrue(end.finished)
        assertEquals(0f, end.rotationDegrees, .0001f)
        assertEquals(1f, end.scale, .0001f)
        assertEquals(0f, end.translationYFraction, .0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun settleRejectsInvalidOutcome() {
        DiceRollAnimationPolicy.settleFrame(
            elapsedMillis = 1L,
            outcome = 7,
        )
    }

    @Test
    fun reducedMotionStillCyclesReadableFaces() {
        val first =
            DiceRollAnimationPolicy.reducedMotionRollingFace(0L)
        val later =
            DiceRollAnimationPolicy.reducedMotionRollingFace(330L)

        assertTrue(first in 1..6)
        assertTrue(later in 1..6)
        assertTrue(first != later)
    }
}
