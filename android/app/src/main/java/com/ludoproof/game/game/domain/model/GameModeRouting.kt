package com.ludoproof.game

enum class GameModeDestination {
    ONLINE_ACTIVITY,
    REMOTE_MODE_ENTRY,
    FRIENDS_ACTIVITY,
    OFFLINE_ACTIVITY,
}

object GameModeRouting {
    fun destination(
        mode: GameMode,
    ): GameModeDestination =
        when (mode) {
            GameMode.ONLINE ->
                GameModeDestination.ONLINE_ACTIVITY

            GameMode.TEAM_UP ->
                GameModeDestination.REMOTE_MODE_ENTRY

            GameMode.FRIENDS ->
                GameModeDestination.FRIENDS_ACTIVITY

            GameMode.COMPUTER,
            GameMode.PASS_AND_PLAY ->
                GameModeDestination.OFFLINE_ACTIVITY
        }
}

object GameModeIntent {
    const val EXTRA_GAME_MODE =
        "ludoproof_game_mode_v1"
}
