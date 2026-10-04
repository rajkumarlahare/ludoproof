package com.ludoproof.game.feature.characters.prototype

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class Dog3DMotion {
    IDLE,
    HOP,
    HOME,
}

data class Dog3DPose(
    val liftY: Float = 0f,
    val bodyYawDegrees: Float = 0f,
    val headTiltDegrees: Float = 0f,
    val earBounceDegrees: Float = 0f,
    val tailWagDegrees: Float = 0f,
)

/** Pure, testable motion timeline for the code-generated 3D dog pawn. */
object Dog3DMotionTimeline {
    const val IDLE_DURATION_MILLIS = 1_800L
    const val HOP_DURATION_MILLIS = 680L
    const val HOME_DURATION_MILLIS = 1_300L

    fun durationMillis(
        motion: Dog3DMotion,
    ): Long =
        when (motion) {
            Dog3DMotion.IDLE -> IDLE_DURATION_MILLIS
            Dog3DMotion.HOP -> HOP_DURATION_MILLIS
            Dog3DMotion.HOME -> HOME_DURATION_MILLIS
        }

    fun sample(
        motion: Dog3DMotion,
        progress: Float,
    ): Dog3DPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion) {
            Dog3DMotion.IDLE -> {
                val wave = sin(p * PI * 2.0).toFloat()
                val tail = sin(p * PI * 4.0).toFloat()
                Dog3DPose(
                    liftY = 0.018f * wave,
                    headTiltDegrees = 2.5f * wave,
                    earBounceDegrees = 2.0f * -wave,
                    tailWagDegrees = 24f * tail,
                )
            }

            Dog3DMotion.HOP -> {
                val arc = sin(p * PI).toFloat().coerceAtLeast(0f)
                val tail = sin(p * PI * 3.0).toFloat()
                Dog3DPose(
                    liftY = 0.58f * arc,
                    headTiltDegrees = -6f * arc,
                    earBounceDegrees = 18f * arc,
                    tailWagDegrees = 28f * tail,
                )
            }

            Dog3DMotion.HOME -> {
                val bounce =
                    abs(sin(p * PI * 3.0).toFloat()) *
                        (1f - p * 0.30f)
                Dog3DPose(
                    liftY = 0.30f * bounce,
                    bodyYawDegrees = 360f * p,
                    headTiltDegrees = 7f * sin(p * PI * 2.0).toFloat(),
                    earBounceDegrees = 12f * sin(p * PI).toFloat().coerceAtLeast(0f),
                    tailWagDegrees = 48f * sin(p * PI * 6.0).toFloat(),
                )
            }
        }
    }
}
