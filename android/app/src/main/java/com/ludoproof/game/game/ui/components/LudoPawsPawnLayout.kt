package com.ludoproof.game

/**
 * Presentation-only sizing and placement rules for Ludo Paws animal pawns.
 *
 * This policy deliberately contains no match-state mutation. It only converts
 * already-authoritative token positions into deterministic visual geometry.
 */
object LudoPawsPawnLayout {
    fun characterIdForSeat(
        characterIdsBySeat: List<String>,
        seat: Int,
    ): String? =
        characterIdsBySeat
            .getOrNull(seat)
            ?.trim()
            ?.takeIf(String::isNotBlank)

    fun radiusScale(
        position: Int,
        occupancy: Int,
    ): Float {
        val base =
            when (position) {
                -1 -> 0.38f
                57 -> 0.33f
                else -> 0.35f
            }
        val crowdScale =
            when {
                occupancy <= 1 -> 1f
                occupancy == 2 -> 0.96f
                occupancy == 3 -> 0.92f
                else -> 0.88f
            }
        return base * crowdScale
    }

    fun tokenOffsetFraction(
        slot: Int,
        position: Int,
    ): Pair<Float, Float> {
        if (position !in 0..57) {
            return 0f to 0f
        }

        return when (Math.floorMod(slot, 4)) {
            0 -> -0.13f to -0.13f
            1 -> 0.13f to -0.13f
            2 -> -0.13f to 0.13f
            else -> 0.13f to 0.13f
        }
    }
}
