package com.ludoproof.game

/**
 * Presentation-only pacing values for readable gameplay.
 *
 * These delays never choose dice outcomes, legal moves, captures, turn order,
 * or any other authoritative game/proof state. They only control when already
 * committed actions are presented to the player.
 */
object LudoPawsGameplayPacingPolicy {
    const val DICE_SETTLE_MILLIS = 700L
    const val SINGLE_LEGAL_AUTO_MOVE_MILLIS = 800L

    // Capture is intentionally staged: attacker arrives first, then the victim
    // absorbs the contact/push, returns to the yard, and visibly settles.
    const val CAPTURE_SEQUENCE_MILLIS = 1_900L
    const val POST_ACTION_BREATH_MILLIS = 500L

    fun postMovePresentationMillis(
        visualSteps: Int,
        moveStepMillis: Long,
        captures: Int,
    ): Long {
        val movementMillis =
            visualSteps
                .coerceAtLeast(1)
                .toLong() *
                moveStepMillis.coerceAtLeast(1L)
        val captureMillis =
            if (captures > 0) CAPTURE_SEQUENCE_MILLIS else 0L
        return movementMillis +
            captureMillis +
            POST_ACTION_BREATH_MILLIS
    }
}
