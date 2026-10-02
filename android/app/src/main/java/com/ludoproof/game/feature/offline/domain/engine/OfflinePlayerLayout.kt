package com.ludoproof.game

object OfflinePlayerLayout {
    val COLORS =
        listOf(
            "RED",
            "GREEN",
            "YELLOW",
            "BLUE",
        )

    enum class Slot {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }

    fun colorsFor(
        playerCount: Int,
        preferredColor: String,
    ): List<String> {
        require(
            playerCount in
                2..4,
        )
        require(
            preferredColor in
                COLORS,
        )

        if (
            playerCount ==
            2
        ) {
            return listOf(
                preferredColor,
                oppositeColor(
                    preferredColor,
                ),
            )
        }

        return (
            listOf(
                preferredColor,
            ) +
                COLORS.filter {
                    it !=
                        preferredColor
                }
            )
            .take(
                playerCount,
            )
    }

    fun slotForColor(
        color: String,
    ): Slot =
        slotForColor(
            color =
                color,
            preferredBottomLeftColor =
                "BLUE",
        )

    fun slotForColor(
        color: String,
        preferredBottomLeftColor: String,
    ): Slot {
        val rotation =
            rotationQuarterTurns(
                preferredBottomLeftColor,
            )
        val base =
            colorIndex(
                color,
            )
        return slotForIndex(
            (
                base +
                    rotation
                ) %
                4,
        )
    }

    fun rotationQuarterTurns(
        preferredBottomLeftColor: String,
    ): Int =
        (
            3 -
                colorIndex(
                    preferredBottomLeftColor,
                ) +
                4
            ) %
            4

    private fun colorIndex(
        color: String,
    ): Int =
        when (color) {
            "RED" -> 0
            "GREEN" -> 1
            "YELLOW" -> 2
            "BLUE" -> 3
            else ->
                error(
                    "Unsupported Ludo color: $color",
                )
        }

    private fun slotForIndex(
        index: Int,
    ): Slot =
        when (
            index %
                4
        ) {
            0 -> Slot.TOP_LEFT
            1 -> Slot.TOP_RIGHT
            2 -> Slot.BOTTOM_RIGHT
            else -> Slot.BOTTOM_LEFT
        }

    fun oppositeColor(
        color: String,
    ): String =
        when (color) {
            "RED" ->
                "YELLOW"
            "YELLOW" ->
                "RED"
            "GREEN" ->
                "BLUE"
            "BLUE" ->
                "GREEN"
            else ->
                error(
                    "Unsupported Ludo color: $color",
                )
        }
}
