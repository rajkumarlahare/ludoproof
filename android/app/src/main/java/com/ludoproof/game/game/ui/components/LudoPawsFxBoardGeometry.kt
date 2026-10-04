package com.ludoproof.game

internal object LudoPawsFxBoardGeometry {
    const val BOARD_SIZE = 15f

    fun tokenCenter(
        color: String,
        tokenIndex: Int,
        position: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        if (position == -1) {
            return yardTokenCenter(
                color = color,
                tokenIndex = tokenIndex,
                cell = cell,
            )
        }
        if (position in 0..51) {
            val offset = START_OFFSETS[color] ?: return null
            val coord = TRACK[(offset + position) % TRACK.size]
            return centerForCell(coord.first, coord.second, cell)
        }
        if (position in 52..56) {
            val lane = HOME_LANES[color] ?: return null
            val coord = lane[position - 52]
            return centerForCell(coord.first, coord.second, cell)
        }
        if (position == 57) {
            val unit =
                when (color) {
                    "RED" -> 6.9f to 7.5f
                    "GREEN" -> 7.5f to 6.9f
                    "YELLOW" -> 8.1f to 7.5f
                    "BLUE" -> 7.5f to 8.1f
                    else -> 7.5f to 7.5f
                }
            return unit.first * cell to unit.second * cell
        }
        return null
    }

    fun yardReactionAnchor(
        color: String,
        cell: Float,
    ): Pair<Float, Float>? =
        when (color) {
            "RED" -> 3f * cell to 3f * cell
            "GREEN" -> 12f * cell to 3f * cell
            "YELLOW" -> 12f * cell to 12f * cell
            "BLUE" -> 3f * cell to 12f * cell
            else -> null
        }

    private fun yardTokenCenter(
        color: String,
        tokenIndex: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        val origin =
            when (color) {
                "RED" -> 0f to 0f
                "GREEN" -> 0f to 9f
                "YELLOW" -> 9f to 9f
                "BLUE" -> 9f to 0f
                else -> return null
            }
        val slot =
            when (tokenIndex) {
                0 -> 2f to 2f
                1 -> 2f to 4f
                2 -> 4f to 2f
                else -> 4f to 4f
            }
        return (origin.second + slot.second) * cell to
            (origin.first + slot.first) * cell
    }

    private fun centerForCell(
        row: Int,
        col: Int,
        cell: Float,
    ): Pair<Float, Float> =
        (col + .5f) * cell to (row + .5f) * cell

    private val START_OFFSETS =
        mapOf(
            "RED" to 0,
            "GREEN" to 13,
            "YELLOW" to 26,
            "BLUE" to 39,
        )

    private val TRACK =
        listOf(
            6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
            5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6,
            0 to 6, 0 to 7, 0 to 8, 1 to 8, 2 to 8,
            3 to 8, 4 to 8, 5 to 8, 6 to 9, 6 to 10,
            6 to 11, 6 to 12, 6 to 13, 6 to 14, 7 to 14,
            8 to 14, 8 to 13, 8 to 12, 8 to 11, 8 to 10,
            8 to 9, 9 to 8, 10 to 8, 11 to 8, 12 to 8,
            13 to 8, 14 to 8, 14 to 7, 14 to 6, 13 to 6,
            12 to 6, 11 to 6, 10 to 6, 9 to 6, 8 to 5,
            8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
            7 to 0, 6 to 0,
        )

    private val HOME_LANES =
        mapOf(
            "RED" to listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),
            "GREEN" to listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),
            "YELLOW" to listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),
            "BLUE" to listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7),
        )
}
