package com.ludoproof.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pure presentation policy for the dice. It never chooses or changes the
 * authoritative outcome; it only turns elapsed time into a visual pose.
 */
object DiceRollAnimationPolicy {
    const val SETTLE_DURATION_MILLIS =
        LudoPawsGameplayPacingPolicy.DICE_SETTLE_DURATION_MILLIS

    private val rollingFaces =
        intArrayOf(2, 5, 3, 6, 4, 1, 5, 2, 6, 3, 1, 4)

    data class Frame(
        val face: Int,
        val rotationDegrees: Float,
        val scale: Float,
        val translationYFraction: Float,
        val borderPulse: Float,
        val finished: Boolean = false,
    )

    fun rollingFrame(
        elapsedMillis: Long,
    ): Frame {
        val elapsed = elapsedMillis.coerceAtLeast(0L)
        val faceIndex =
            ((elapsed / 52L) % rollingFaces.size)
                .toInt()
        val cycle =
            ((elapsed % 420L).toFloat() / 420f)
        val hop =
            abs(
                sin(
                    cycle *
                        PI.toFloat() *
                        2f,
                ),
            )
        val tumble =
            sin(
                elapsed.toFloat() /
                    145f *
                    PI.toFloat() *
                    2f,
            ) *
                15f +
                sin(
                    elapsed.toFloat() /
                        81f *
                        PI.toFloat() *
                        2f,
                ) *
                4f
        val pressProgress =
            (elapsed.toFloat() / 75f)
                .coerceIn(0f, 1f)
        val pressScale =
            1f -
                (1f - pressProgress) * .10f

        return Frame(
            face = rollingFaces[faceIndex],
            rotationDegrees = tumble,
            scale =
                pressScale *
                    (.95f + hop * .045f),
            translationYFraction =
                -hop * .065f,
            borderPulse =
                .45f + hop * .55f,
        )
    }

    fun settleFrame(
        elapsedMillis: Long,
        outcome: Int,
        startRotationDegrees: Float = 18f,
    ): Frame {
        require(outcome in 1..6) {
            "dice outcome must be 1..6"
        }
        val progress =
            (elapsedMillis.toFloat() /
                SETTLE_DURATION_MILLIS.toFloat())
                .coerceIn(0f, 1f)
        val remaining = 1f - progress
        val bounce =
            sin(progress * PI.toFloat()) * remaining
        val wobble =
            sin(progress * PI.toFloat() * 3f) * remaining

        return Frame(
            face = outcome,
            rotationDegrees =
                startRotationDegrees * remaining +
                    wobble * 7f,
            scale =
                1f + bounce * .075f,
            translationYFraction =
                -bounce * .055f,
            borderPulse =
                .35f + remaining * .65f,
            finished = progress >= 1f,
        )
    }

    fun reducedMotionRollingFace(
        elapsedMillis: Long,
    ): Int =
        rollingFaces[
            ((elapsedMillis.coerceAtLeast(0L) / 110L) % rollingFaces.size)
                .toInt()
        ]
}


/**
 * Pure presentation policy for the actionable dice attention cue.
 * It never chooses or changes the authoritative outcome.
 */
object DiceAttentionAnimationPolicy {
    private const val CYCLE_MILLIS = 900L

    fun frame(elapsedMillis: Long): DiceRollAnimationPolicy.Frame {
        val cycle =
            (
                elapsedMillis.coerceAtLeast(0L) %
                    CYCLE_MILLIS
            ).toFloat() /
                CYCLE_MILLIS.toFloat()
        val breath =
            (
                sin(
                    cycle *
                        PI.toFloat() *
                        2f -
                        (PI.toFloat() * .5f),
                ) +
                    1f
                ) *
                .5f

        return DiceRollAnimationPolicy.Frame(
            face = 1,
            rotationDegrees = 0f,
            scale = 1f + breath * .065f,
            translationYFraction = -breath * .028f,
            borderPulse = .20f + breath * .45f,
        )
    }
}
