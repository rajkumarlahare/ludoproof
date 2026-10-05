package com.ludoproof.game.feature.characters.prototype

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class Cat3DMotion {
    IDLE,
    HOP,
    HOME,
}

data class Cat3DPose(
    val liftY: Float = 0f,
    val bodyYawDegrees: Float = 0f,
    val headTiltDegrees: Float = 0f,
    val earTwitchDegrees: Float = 0f,
    val tailSwayDegrees: Float = 0f,
)

/** Pure, testable motion timeline for the code-generated 3D cat pawn. */
object Cat3DMotionTimeline {
    const val IDLE_DURATION_MILLIS = 2_000L
    const val HOP_DURATION_MILLIS = 650L
    const val HOME_DURATION_MILLIS = 1_250L

    fun durationMillis(
        motion: Cat3DMotion,
    ): Long =
        when (motion) {
            Cat3DMotion.IDLE -> IDLE_DURATION_MILLIS
            Cat3DMotion.HOP -> HOP_DURATION_MILLIS
            Cat3DMotion.HOME -> HOME_DURATION_MILLIS
        }

    fun sample(
        motion: Cat3DMotion,
        progress: Float,
    ): Cat3DPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion) {
            Cat3DMotion.IDLE -> {
                val wave = sin(p * PI * 2.0).toFloat()
                val tail = sin(p * PI * 3.0).toFloat()
                Cat3DPose(
                    liftY = 0.015f * wave,
                    headTiltDegrees = 2.8f * wave,
                    earTwitchDegrees = 3.5f * sin(p * PI * 4.0).toFloat(),
                    tailSwayDegrees = 28f * tail,
                )
            }

            Cat3DMotion.HOP -> {
                val arc = sin(p * PI).toFloat().coerceAtLeast(0f)
                Cat3DPose(
                    liftY = 0.60f * arc,
                    headTiltDegrees = -5.5f * arc,
                    earTwitchDegrees = 12f * arc,
                    tailSwayDegrees = 22f * sin(p * PI * 2.0).toFloat(),
                )
            }

            Cat3DMotion.HOME -> {
                val bounce =
                    abs(sin(p * PI * 3.0).toFloat()) *
                        (1f - p * 0.32f)
                Cat3DPose(
                    liftY = 0.30f * bounce,
                    bodyYawDegrees = 360f * p,
                    headTiltDegrees = 8f * sin(p * PI * 2.0).toFloat(),
                    earTwitchDegrees = 10f * sin(p * PI * 4.0).toFloat(),
                    tailSwayDegrees = 50f * sin(p * PI * 5.0).toFloat(),
                )
            }
        }
    }
}
