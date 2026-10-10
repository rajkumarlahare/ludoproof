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
    fun attentionPulseBreathesWithoutChangingTheFace() {
        val start =
            DiceAttentionAnimationPolicy.frame(0L)
        val middle =
            DiceAttentionAnimationPolicy.frame(450L)
        val end =
            DiceAttentionAnimationPolicy.frame(900L)

        assertEquals(1, start.face)
        assertEquals(1, middle.face)
        assertEquals(1, end.face)
        assertTrue(middle.scale > start.scale)
        assertEquals(start.scale, end.scale, .0001f)
        assertTrue(middle.translationYFraction < 0f)
        assertTrue(middle.borderPulse > start.borderPulse)
    }

    @Test
    fun attentionPulseKeepsTheOuterStrokeInsideTheFixedDiceBounds() {
        val size = 54f
        val halfStroke = 2.8f / 2f
        var minimumTopEdge = Float.MAX_VALUE

        for (elapsedMillis in 0L..900L step 15L) {
            val frame =
                DiceAttentionAnimationPolicy.frame(elapsedMillis)

            assertTrue(frame.scale in 1f..1.07f)
            assertTrue(frame.translationYFraction >= -0.0021f)

            // Mirror the actual top border's centreline transform and its scaled
            // half-stroke for the smallest production dice host (54dp).
            val topBorderCenter =
                size / 2f +
                    (size * 0.07f - size / 2f) * frame.scale +
                    size * frame.translationYFraction
            val topBorderEdge =
                topBorderCenter - halfStroke * frame.scale
            minimumTopEdge = minOf(minimumTopEdge, topBorderEdge)
        }

        // Keep visible clearance, not merely a mathematically non-negative edge,
        // so antialiasing cannot make the top stroke appear chopped.
        assertTrue(minimumTopEdge > 0.5f)
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


}
