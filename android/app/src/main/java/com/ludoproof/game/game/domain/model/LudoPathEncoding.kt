package com.ludoproof.game

/**
 * Stable token-position encoding for the classic board.
 *
 * The physical outer track has 52 shared cells, but each player leaves that track after
 * relative position 50 and enters their colored home lane on the next movement step.
 * Position 51 is therefore reserved as a legacy compatibility gap; new moves must skip it.
 * Home-lane cells remain encoded as 52..56 and the finished center remains 57 so existing
 * snapshots, proof history and deployed ruleset identifiers stay compatible.
 */
object LudoPathEncoding {
    const val LAST_TRACK_POSITION = 50
    const val RESERVED_LEGACY_ENTRY_POSITION = 51
    const val FIRST_HOME_LANE_POSITION = 52
    const val LAST_HOME_LANE_POSITION = 56
    const val HOME_POSITION = 57

    fun destinationForRoll(
        position: Int,
        roll: Int,
    ): Int? {
        if (roll !in 1..6) return null
        if (position == -1) return if (roll == 6) 0 else null
        if (position == HOME_POSITION) return null

        val normalized = normalizeLegacyEntry(position)
        if (normalized !in 0..LAST_HOME_LANE_POSITION) return null

        var destination = normalized + roll
        if (
            normalized <= LAST_TRACK_POSITION &&
            destination > LAST_TRACK_POSITION
        ) {
            destination += 1
        }

        return destination.takeIf { it <= HOME_POSITION }
    }

    fun normalizeLegacyEntry(position: Int): Int =
        if (position == RESERVED_LEGACY_ENTRY_POSITION) {
            FIRST_HOME_LANE_POSITION
        } else {
            position
        }

    fun isTrackPosition(position: Int): Boolean =
        position in 0..LAST_TRACK_POSITION

    fun visualStepCount(
        fromPosition: Int,
        toPosition: Int,
    ): Int {
        val from = logicalPosition(fromPosition) ?: return 0
        val to = logicalPosition(toPosition) ?: return 0
        return (to - from).coerceAtLeast(0)
    }

    fun positionAtVisualStep(
        fromPosition: Int,
        step: Int,
    ): Int? {
        if (step < 0) return null
        val from = logicalPosition(fromPosition) ?: return null
        return encodedPosition(from + step)
    }

    private fun logicalPosition(position: Int): Int? {
        val normalized = normalizeLegacyEntry(position)
        return when {
            normalized == -1 -> -1
            normalized in 0..LAST_TRACK_POSITION -> normalized
            normalized in FIRST_HOME_LANE_POSITION..HOME_POSITION -> normalized - 1
            else -> null
        }
    }

    private fun encodedPosition(position: Int): Int? =
        when {
            position == -1 -> -1
            position in 0..LAST_TRACK_POSITION -> position
            position in (LAST_TRACK_POSITION + 1)..(HOME_POSITION - 1) -> position + 1
            else -> null
        }
}
