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
        // Keep the production gate marker explicit: captureReturnFrame(progress)
        // is still the source policy; only the input is clamped before sampling.
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

        // Before the actual yard trip starts, visibly shove the captured animal
        // away from the contact square and let it rebound. Because SceneView now
        // begins this timeline only after the attacker reaches the square, the
        // sequence reads as contact -> push/stumble -> return instead of a pawn
        // disappearing before the hit lands.
        val contactPush =
            if (safeProgress < CONTACT_END) {
                sin(
                    (safeProgress / CONTACT_END) *
                        PI.toFloat(),
                ) *
                    cell * CONTACT_PUSH_CELLS
            } else {
                0f
            }

        return Placement(
            x =
                linearX +
                    directionX * contactPush +
                    perpendicularX * curveAmount +
                    frame.shakeXCells * cell,
            y =
                linearY +
                    directionY * contactPush +
                    perpendicularY * curveAmount -
                    frame.liftCells * cell,
            scale = frame.scale,
            phase = frame.phase,
        )
    }

    private const val CONTACT_END = .34f
    private const val CONTACT_PUSH_CELLS = .14f
    private const val CURVE_CELLS = .34f
}
