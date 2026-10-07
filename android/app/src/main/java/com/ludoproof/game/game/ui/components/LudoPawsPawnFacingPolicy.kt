package com.ludoproof.game

import kotlin.math.abs

/**
 * Presentation-only facing policy for the 3D animal pawns.
 *
 * A resting pawn faces the local player. During a forward move the pawn turns
 * toward the visible board-space direction of the current road segment. Turns
 * are eased so corners read like an animal changing direction instead of a
 * sprite snapping by 90 degrees.
 */
internal object LudoPawsPawnFacingPolicy {
    const val FRONT_YAW_DEGREES = 0f
    const val RETURN_TO_PLAYER_MILLIS = 180L

    private const val TURN_PORTION_OF_HOP = 0.28f

    fun yawForVisibleDelta(
        dx: Float,
        dy: Float,
    ): Float {
        if (abs(dx) <= 0.0001f && abs(dy) <= 0.0001f) {
            return FRONT_YAW_DEGREES
        }

        return if (abs(dx) >= abs(dy)) {
            if (dx >= 0f) 90f else -90f
        } else {
            // Screen Y grows downward. Down therefore points toward the local
            // player/front, while up points away and shows the animal's back.
            if (dy >= 0f) 0f else 180f
        }
    }

    fun movingYaw(
        previousYawDegrees: Float,
        targetYawDegrees: Float,
        stepProgress: Float,
    ): Float {
        val turnProgress =
            (stepProgress.coerceIn(0f, 1f) / TURN_PORTION_OF_HOP)
                .coerceIn(0f, 1f)
        return interpolateYaw(
            fromYawDegrees = previousYawDegrees,
            toYawDegrees = targetYawDegrees,
            progress = smoothStep(turnProgress),
        )
    }

    fun settlingYaw(
        lastTravelYawDegrees: Float,
        elapsedAfterMoveMillis: Long,
    ): Float {
        val progress =
            elapsedAfterMoveMillis
                .coerceAtLeast(0L)
                .toFloat() /
                RETURN_TO_PLAYER_MILLIS.toFloat()
        return interpolateYaw(
            fromYawDegrees = lastTravelYawDegrees,
            toYawDegrees = FRONT_YAW_DEGREES,
            progress = smoothStep(progress.coerceIn(0f, 1f)),
        )
    }

    internal fun interpolateYaw(
        fromYawDegrees: Float,
        toYawDegrees: Float,
        progress: Float,
    ): Float {
        val delta = shortestDelta(fromYawDegrees, toYawDegrees)
        return normalizeYaw(
            fromYawDegrees + delta * progress.coerceIn(0f, 1f),
        )
    }

    private fun shortestDelta(
        fromYawDegrees: Float,
        toYawDegrees: Float,
    ): Float =
        normalizeYaw(toYawDegrees - fromYawDegrees)

    private fun normalizeYaw(value: Float): Float {
        var normalized = value % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized <= -180f) normalized += 360f
        return normalized
    }

    private fun smoothStep(value: Float): Float =
        value * value * (3f - 2f * value)
}
