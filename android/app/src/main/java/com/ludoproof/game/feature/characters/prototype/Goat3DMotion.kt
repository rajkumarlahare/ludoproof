package com.ludoproof.game.feature.characters.prototype

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class Goat3DMotion {
    IDLE,
    HOP,
    HOME,
}

data class Goat3DPose(
    val liftY: Float = 0f,
    val bodyYawDegrees: Float = 0f,
    val headTiltDegrees: Float = 0f,
    val earFlickDegrees: Float = 0f,
    val beardSwingDegrees: Float = 0f,
    val tailFlickDegrees: Float = 0f,
)

/** Pure, testable motion timeline for the code-generated 3D goat pawn. */
object Goat3DMotionTimeline {
    const val IDLE_DURATION_MILLIS = 2_000L
    const val HOP_DURATION_MILLIS = 700L
    const val HOME_DURATION_MILLIS = 1_350L

    fun durationMillis(
        motion: Goat3DMotion,
    ): Long =
        when (motion) {
            Goat3DMotion.IDLE -> IDLE_DURATION_MILLIS
            Goat3DMotion.HOP -> HOP_DURATION_MILLIS
            Goat3DMotion.HOME -> HOME_DURATION_MILLIS
        }

    fun sample(
        motion: Goat3DMotion,
        progress: Float,
    ): Goat3DPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion) {
            Goat3DMotion.IDLE -> {
                val wave = sin(p * PI * 2.0).toFloat()
                val quick = sin(p * PI * 4.0).toFloat()
                Goat3DPose(
                    liftY = 0.016f * wave,
                    headTiltDegrees = 2.2f * wave,
                    earFlickDegrees = 4.0f * quick,
                    beardSwingDegrees = 3.5f * -wave,
                    tailFlickDegrees = 10f * quick,
                )
            }

            Goat3DMotion.HOP -> {
                val arc = sin(p * PI).toFloat().coerceAtLeast(0f)
                val wobble = sin(p * PI * 2.0).toFloat()
                Goat3DPose(
                    liftY = 0.60f * arc,
                    headTiltDegrees = -5.5f * arc,
                    earFlickDegrees = 15f * arc,
                    beardSwingDegrees = 18f * arc + 4f * wobble,
                    tailFlickDegrees = 20f * wobble,
                )
            }

            Goat3DMotion.HOME -> {
                val bounce =
                    abs(sin(p * PI * 3.0).toFloat()) *
                        (1f - p * 0.30f)
                Goat3DPose(
                    liftY = 0.31f * bounce,
                    bodyYawDegrees = 360f * p,
                    headTiltDegrees = 8f * sin(p * PI * 2.0).toFloat(),
                    earFlickDegrees = 10f * sin(p * PI * 4.0).toFloat(),
                    beardSwingDegrees = 22f * sin(p * PI * 3.0).toFloat(),
                    tailFlickDegrees = 28f * sin(p * PI * 6.0).toFloat(),
                )
            }
        }
    }
}
