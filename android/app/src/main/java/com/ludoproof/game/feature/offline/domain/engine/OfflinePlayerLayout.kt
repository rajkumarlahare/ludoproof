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
        when (color) {
            "RED" ->
                Slot.TOP_LEFT
            "GREEN" ->
                Slot.TOP_RIGHT
            "BLUE" ->
                Slot.BOTTOM_LEFT
            "YELLOW" ->
                Slot.BOTTOM_RIGHT
            else ->
                error(
                    "Unsupported Ludo color: $color",
                )
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
