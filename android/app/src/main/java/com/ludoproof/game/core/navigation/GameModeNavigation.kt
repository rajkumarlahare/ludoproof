package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import com.ludoproof.game.feature.mode.presentation.RemoteModeEntryActivity

fun Activity.openGameMode(
    mode: GameMode,
) {
    val target =
        when (
            GameModeRouting
                .destination(
                    mode,
                )
        ) {
            GameModeDestination.ONLINE_ACTIVITY ->
                MainActivity::class.java

            GameModeDestination.REMOTE_MODE_ENTRY ->
                RemoteModeEntryActivity::class.java

            GameModeDestination.OFFLINE_ACTIVITY ->
                OfflineGameActivity::class.java
        }

    startActivity(
        Intent(
            this,
            target,
        ).putExtra(
            GameModeIntent.EXTRA_GAME_MODE,
            mode.wireValue,
        ),
    )
}
