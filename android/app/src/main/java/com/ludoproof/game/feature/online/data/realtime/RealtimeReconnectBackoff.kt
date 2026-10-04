package com.ludoproof.game

/**
 * Network-only reconnect timing policy.
 *
 * This randomness is intentionally limited to transport retry timing and never
 * participates in dice, game rules, proof generation, or authoritative state.
 */
internal object RealtimeReconnectBackoff {
    private const val BASE_DELAY_MS = 1_000L
    private const val MAX_EXPONENT = 4
    private const val MIN_JITTER_MULTIPLIER = 0.80
    private const val JITTER_SPAN = 0.40

    fun delayMillis(
        attempt: Int,
        jitterUnit: Double,
    ): Long {
        val exponent =
            attempt
                .coerceAtLeast(0)
                .coerceAtMost(MAX_EXPONENT)
        val baseDelay =
            BASE_DELAY_MS shl exponent
        val normalizedJitter =
            jitterUnit.coerceIn(0.0, 1.0)
        val multiplier =
            MIN_JITTER_MULTIPLIER +
                JITTER_SPAN * normalizedJitter
        return (baseDelay * multiplier).toLong()
    }
}
