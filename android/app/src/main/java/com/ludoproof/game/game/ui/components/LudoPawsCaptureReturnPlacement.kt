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
        val safeProgress = progress.coerceIn(0f, 1f)
        val frame =
            LudoPawsPawnAnimationPolicy
                .captureReturnFrame(safeProgress)
        val route = frame.routeProgress.coerceIn(0f, 1f)
        val linearX = from.first + (to.first - from.first) * route
        val linearY = from.second + (to.second - from.second) * route

        val dx = to.first - from.first
        val dy = to.second - from.second
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val directionX =
            if (distance > 0.001f) dx / distance else 0f
        val directionY =
            if (distance > 0.001f) dy / distance else 0f
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

        // The captured pawn stays on its track square until contact. At impact it
        // is visibly displaced away from the attacker, then that shove blends
        // smoothly into the existing curved trip back to the yard.
        val shoveCells =
            when {
                safeProgress < IMPACT_END -> {
                    val local = safeProgress / IMPACT_END
                    SHOVE_AT_IMPACT_CELLS * smoothStep(local)
                }

                safeProgress < POP_END -> {
                    val local =
                        (safeProgress - IMPACT_END) /
                            (POP_END - IMPACT_END)
                    SHOVE_AT_IMPACT_CELLS +
                        (SHOVE_PEAK_CELLS - SHOVE_AT_IMPACT_CELLS) *
                        smoothStep(local)
                }

                safeProgress < SHOVE_BLEND_END -> {
                    val local =
                        (safeProgress - POP_END) /
                            (SHOVE_BLEND_END - POP_END)
                    SHOVE_PEAK_CELLS * (1f - smoothStep(local))
                }

                else -> 0f
            }

        return Placement(
            x =
                linearX +
                    directionX * shoveCells * cell +
                    perpendicularX * curveAmount +
                    frame.shakeXCells * cell,
            y =
                linearY +
                    directionY * shoveCells * cell +
                    perpendicularY * curveAmount -
                    frame.liftCells * cell,
            scale = frame.scale,
            phase = frame.phase,
        )
    }

    private fun smoothStep(value: Float): Float {
        val safe = value.coerceIn(0f, 1f)
        return safe * safe * (3f - 2f * safe)
    }

    private const val IMPACT_END = .08f
    private const val POP_END = .17f
    private const val SHOVE_BLEND_END = .30f
    private const val SHOVE_AT_IMPACT_CELLS = .09f
    private const val SHOVE_PEAK_CELLS = .24f
    private const val CURVE_CELLS = .34f
}
