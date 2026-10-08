package com.ludoproof.game

/**
 * Adaptive frame pacing for the shared 3D pawn surface.
 *
 * Movement, capture-return and reaction animation keep the existing near-60 fps
 * cadence. Waiting/idle board states step down to about 30 fps so the renderer
 * does not spend the same CPU/GPU budget while players are thinking or waiting
 * on the network. This policy is presentation-only and never influences game
 * state, input authority, dice outcomes or animation timing.
 */
internal object LudoPaws3DRenderCadencePolicy {
    internal const val ACTIVE_FRAME_DELAY_MILLIS = 16L
    internal const val IDLE_FRAME_DELAY_MILLIS = 33L
    internal const val HOME_CELEBRATION_TAIL_MILLIS = 1_400L
    internal const val FORWARD_LANDING_SETTLE_MILLIS = 90L

    fun frameDelayMillis(
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ): Long =
        if (hasActiveAnimation(state, nowMillis)) {
            ACTIVE_FRAME_DELAY_MILLIS
        } else {
            IDLE_FRAME_DELAY_MILLIS
        }

    internal fun hasActiveAnimation(
        state: LudoPaws3DSceneState,
        nowMillis: Long,
    ): Boolean {
        val forward = state.forwardMotion
        val forwardTailMillis =
            if (forward?.toPosition == LudoPathEncoding.HOME_POSITION) {
                HOME_CELEBRATION_TAIL_MILLIS
            } else {
                0L
            }
        val forwardActive =
            forward != null &&
                state.forwardDurationMillis > 0L &&
                nowMillis <
                state.forwardStartedAtMillis +
                    state.forwardDurationMillis +
                    forwardTailMillis
        if (forwardActive) return true

        if (
            state.captureReturns.values.any { capture ->
                capture.durationMillis > 0L &&
                    nowMillis < capture.startedAtMillis + capture.durationMillis
            }
        ) {
            return true
        }

        return state.activeReactions.values.any { reaction ->
            reaction.durationMillis > 0L &&
                nowMillis < reaction.startedAtMillis + reaction.durationMillis
        }
    }
}
