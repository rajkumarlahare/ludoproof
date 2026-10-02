package com.ludoproof.game.feature.offline

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*
import com.ludoproof.game.ui.offline.gameplay.*

internal fun OfflineGameActivity.rollOffline() {
    val button = rollButton ?: return
    button.isEnabled = false
    diceView?.startRolling()
    showStatus("Rolling locally…")

    handler.postDelayed(
        {
            runCatching { engine.roll() }
                .onSuccess { state ->
                    renderGame(state)
                    showStatus(
                        if (state.pendingRoll != null) {
                            "Move a highlighted token."
                        } else {
                            "Turn updated."
                        },
                    )
                }
                .onFailure { error ->
                    diceView?.stopRolling()
                    button.isEnabled = true
                    showStatus(error.message ?: "Roll failed")
                }
        },
        430L,
    )
}

internal fun OfflineGameActivity.offlineHistory(state: MatchSnapshot?): String {
    if (state == null || state.history.isEmpty()) {
        return "No offline rolls yet."
    }
    return buildString {
        append("Recent offline rolls\n\n")
        state.history.takeLast(16).reversed().forEach { event ->
            append("#")
            append(event.eventIndex)
            append("  dice=")
            append(event.outcome ?: "?")
            if (event.proofDigest != null) {
                append("  localV4=")
                append(event.proofDigest.take(8))
                append("…")
            }
            if (event.moveTokenIndex != null) {
                append("  token=")
                append(event.moveTokenIndex + 1)
            }
            if (event.captures > 0) {
                append("  captures=")
                append(event.captures)
            }
            append("\n")
        }
    }.trimEnd()
}

internal fun OfflineGameActivity.showStatus(value: String) {
    statusText?.text = value
}
