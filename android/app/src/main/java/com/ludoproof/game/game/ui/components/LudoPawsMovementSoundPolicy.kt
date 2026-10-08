package com.ludoproof.game

/**
 * Pure mapping from a committed forward visual step to the physical SFX that
 * should be heard at that exact board-cell arrival.
 *
 * Movement animation remains authoritative for timing. This class only decides
 * which cue belongs to a completed visual step, so the same rule can be tested
 * without Android audio or rendering.
 */
internal enum class LudoPawsMovementSoundCue {
    STEP,
    YARD_EXIT,
    SAFE_RELIEF,
    HOME_LANE,
    HOME,
}

internal object LudoPawsMovementSoundPolicy {
    private val safeGlobalCells =
        setOf(
            0,
            8,
            13,
            21,
            26,
            34,
            39,
            47,
        )

    private val startOffsets =
        mapOf(
            "RED" to 0,
            "GREEN" to 13,
            "YELLOW" to 26,
            "BLUE" to 39,
        )

    fun cuesForVisualStep(
        color: String?,
        fromPosition: Int,
        step: Int,
    ): List<LudoPawsMovementSoundCue> {
        if (step <= 0) return emptyList()

        val destination =
            LudoPathEncoding.positionAtVisualStep(
                fromPosition = fromPosition,
                step = step,
            ) ?: return emptyList()

        // Leaving the yard is its own physical action. Use the dedicated launch
        // sound instead of stacking it with the generic first movement tick.
        if (
            fromPosition == -1 &&
            step == 1 &&
            destination == 0
        ) {
            return listOf(LudoPawsMovementSoundCue.YARD_EXIT)
        }

        val cues =
            mutableListOf(
                LudoPawsMovementSoundCue.STEP,
            )

        if (
            destination in
            LudoPathEncoding.FIRST_HOME_LANE_POSITION..
                LudoPathEncoding.LAST_HOME_LANE_POSITION
        ) {
            val previousDestination =
                LudoPathEncoding.positionAtVisualStep(
                    fromPosition = fromPosition,
                    step = step - 1,
                )
            if (
                previousDestination !in
                LudoPathEncoding.FIRST_HOME_LANE_POSITION..
                    LudoPathEncoding.LAST_HOME_LANE_POSITION
            ) {
                cues += LudoPawsMovementSoundCue.HOME_LANE
            }
        }

        if (destination == LudoPathEncoding.HOME_POSITION) {
            cues += LudoPawsMovementSoundCue.HOME
        }

        if (isSafePosition(color = color, position = destination)) {
            cues += LudoPawsMovementSoundCue.SAFE_RELIEF
        }

        return cues
    }

    private fun isSafePosition(
        color: String?,
        position: Int,
    ): Boolean {
        if (color == null || position !in 0..LudoPathEncoding.LAST_TRACK_POSITION) {
            return false
        }
        val start = startOffsets[color] ?: return false
        val global = (start + position) % 52
        return global in safeGlobalCells
    }
}
