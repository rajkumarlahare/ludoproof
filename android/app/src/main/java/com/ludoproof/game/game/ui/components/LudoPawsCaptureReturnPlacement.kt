package com.ludoproof.game

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Presentation-only placement for a captured 3D pawn.
 *
 * Gameplay has already committed the captured pawn to the yard. This policy
 * reconstructs the visible trip from the old track cell to that authoritative
 * yard destination without mutating token state, proof state, or turn state.
 */
internal object LudoPawsCaptureReturnPlacement {
    data class Placement(
        val x: Float,
        val y: Float,
        val scale: Float,
        val phase: LudoPawsCaptureReturnPhase,
    )

    fun sample(
        from: Pair<Float, Float>,
        to: Pair<Float, Float>,
        cell: Float,
        progress: Float,
    ): Placement {
        val frame =
            LudoPawsPawnAnimationPolicy
                .captureReturnFrame(progress)
        val route = frame.routeProgress.coerceIn(0f, 1f)
        val linearX = from.first + (to.first - from.first) * route
        val linearY = from.second + (to.second - from.second) * route

        val dx = to.first - from.first
        val dy = to.second - from.second
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val curveAmount =
            if (distance > 0.001f) {
                sin(route * PI.toFloat()) * cell * CURVE_CELLS
            } else {
                0f
            }
        val perpendicularX =
            if (distance > 0.001f) -dy / distance else 0f
        val perpendicularY =
            if (distance > 0.001f) dx / distance else 0f

        return Placement(
            x =
                linearX +
                    perpendicularX * curveAmount +
                    frame.shakeXCells * cell,
            y =
                linearY +
                    perpendicularY * curveAmount -
                    frame.liftCells * cell,
            scale = frame.scale,
            phase = frame.phase,
        )
    }

    private const val CURVE_CELLS = .34f
}
