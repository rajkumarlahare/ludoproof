package com.ludoproof.game.feature.characters.prototype

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class Duck3DMotion {
    IDLE,
    HOP,
    HOME,
}

data class Duck3DPose(
    val liftY: Float = 0f,
    val bodyYawDegrees: Float = 0f,
    val wingFlapDegrees: Float = 0f,
    val headTiltDegrees: Float = 0f,
)

/**
 * Pure animation timeline for the code-generated 3D duck prototype.
 *
 * Keeping the motion curve free of Android/OpenGL dependencies makes it easy to
 * unit-test and later map the same cues to the in-game pawn renderer.
 */
object Duck3DMotionTimeline {
    const val IDLE_DURATION_MILLIS = 2_200L
    const val HOP_DURATION_MILLIS = 720L
    const val HOME_DURATION_MILLIS = 1_350L

    fun durationMillis(
        motion: Duck3DMotion,
    ): Long =
        when (motion) {
            Duck3DMotion.IDLE -> IDLE_DURATION_MILLIS
            Duck3DMotion.HOP -> HOP_DURATION_MILLIS
            Duck3DMotion.HOME -> HOME_DURATION_MILLIS
        }

    fun sample(
        motion: Duck3DMotion,
        progress: Float,
    ): Duck3DPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion) {
            Duck3DMotion.IDLE -> {
                val wave = sin((p * PI * 2.0)).toFloat()
                Duck3DPose(
                    liftY = 0.025f * wave,
                    wingFlapDegrees = 3.5f * wave,
                    headTiltDegrees = 2.5f * sin((p * PI * 2.0 + PI / 3.0)).toFloat(),
                )
            }

            Duck3DMotion.HOP -> {
                val arc = sin((p * PI)).toFloat().coerceAtLeast(0f)
                val flap = sin((p * PI * 2.0)).toFloat()
                Duck3DPose(
                    liftY = 0.62f * arc,
                    wingFlapDegrees = 34f * arc + 7f * flap,
                    headTiltDegrees = -7f * arc,
                )
            }

            Duck3DMotion.HOME -> {
                val celebrationBounce =
                    abs(sin((p * PI * 3.0)).toFloat()) *
                        (1f - p * 0.35f)
                Duck3DPose(
                    liftY = 0.34f * celebrationBounce,
                    bodyYawDegrees = 360f * p,
                    wingFlapDegrees = 52f * sin((p * PI)).toFloat().coerceAtLeast(0f),
                    headTiltDegrees = 8f * sin((p * PI * 2.0)).toFloat(),
                )
            }
        }
    }
}
