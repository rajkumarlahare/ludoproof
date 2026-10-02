package com.ludoproof.game.feature.online

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.concurrent.Executors
import com.ludoproof.game.*
import com.ludoproof.game.ui.online.*

internal fun MainActivity.shareMatch() {
    val code =
        matchId
            ?: run {
                showStatus(
                    "Create or join a match first.",
                )
                return
            }

    val intent =
        Intent(
            Intent.ACTION_SEND,
        ).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Join my LudoProof match: $code",
            )
        }
    startActivity(
        Intent.createChooser(
            intent,
            "Share LudoProof match",
        ),
    )
}

internal fun MainActivity.playerName(): String {
    val value =
        nameInput.text
            .toString()
            .trim()
    require(
        value.length in 2..24,
    ) {
        "Player name must contain 2 to 24 characters"
    }
    return value
}

internal fun MainActivity.persistSessionSecurely(
    code: String,
    id: String,
    token: String,
) {
    try {
        secureSessionStore.save(
            PlayerSession(
                matchId = code,
                playerId = id,
                playerToken = token,
            ),
        )
    } catch (_: Exception) {
        secureSessionStore.clear()
    }
}

internal fun MainActivity.withSession(
    action: (
        String,
        String,
    ) -> Unit,
) {
    val code = matchId
    val token = playerToken

    if (
        code == null ||
        token == null
    ) {
        showStatus(
            "Create or join a match first.",
        )
        return
    }

    action(
        code,
        token,
    )
}

internal fun MainActivity.runNetwork(
    action: () -> JSONObject,
    onSuccess: (
        JSONObject,
    ) -> Unit = {
        applyResponse(it)
    },
    showWorking: Boolean = true,
) {
    if (
        !canRenderUi()
    ) {
        return
    }

    if (!isOnline) {
        diceView.stopRolling()
        showStatus(
            "Offline — this online match is read-only until internet returns.",
        )
        currentState?.let {
            updateControls(it)
        }
        return
    }

    if (
        executor.isShutdown
    ) {
        return
    }

    if (showWorking) {
        showStatus("Working…")
    }
    setNetworkControls(
        enabled = false,
    )

    val submitted =
        runCatching {
            executor.execute {
                try {
                    val response =
                        action()
                    mainHandler.post {
                        if (
                            !canRenderUi()
                        ) {
                            return@post
                        }

                        setNetworkControls(
                            enabled = true,
                        )
                        onSuccess(
                            response,
                        )
                        updateRollButton()
                    }
                } catch (
                    error: Exception,
                ) {
                    mainHandler.post {
                        if (
                            !canRenderUi()
                        ) {
                            return@post
                        }

                        diceView.stopRolling()
                        val sessionReset =
                            resetInvalidSessionIfNeeded(
                                error,
                            )
                        if (!sessionReset) {
                            setNetworkControls(
                                enabled = true,
                            )
                            showStatus(
                                "Error: " +
                                    (
                                        error.message
                                            ?: error
                                                .toString()
                                        ),
                            )
                            currentState
                                ?.let {
                                    updateControls(
                                        it,
                                    )
                                }
                        }
                        updateRollButton()
                    }
                }
            }
        }
            .isSuccess

    if (
        !submitted &&
        canRenderUi()
    ) {
        diceView.stopRolling()
        setNetworkControls(
            enabled = true,
        )
        showStatus(
            "Request was cancelled because the screen is closing.",
        )
        updateRollButton()
    }
}

internal fun MainActivity.resetInvalidSessionIfNeeded(
    error: Exception,
): Boolean {
    val code =
        (error as? GameApiException)
            ?.code
            ?: return false
    if (
        code != "MATCH_NOT_FOUND" &&
        code != "AUTH_INVALID"
    ) {
        return false
    }

    secureSessionStore.clear()
    pendingRollStore.clear()
    pendingSecret = null
    matchId = null
    playerToken = null
    playerId = null
    currentState = null
    cachedMatchStore.clear()

    matchInput.setText("")
    boardView.bind(
        null,
        null,
    )
    diceView.stopRolling()
    matchInfoText.text =
        "No active match"
    playersText.text =
        "Players will appear here."
    turnText.text =
        "Create or join a new match."
    verificationText.text =
        "No verified roll yet."
    proofDetailsText.text = ""
    proofDetailsText.visibility =
        View.GONE

    lobbyPanel.visibility =
        View.VISIBLE
    nameInput.visibility =
        View.VISIBLE
    matchInput.visibility =
        View.VISIBLE
    createButton.visibility =
        View.VISIBLE
    joinButton.visibility =
        View.VISIBLE
    startButton.visibility =
        View.GONE
    rollButton.visibility =
        View.GONE

    nameInput.isEnabled = true
    matchInput.isEnabled = true
    createButton.isEnabled = true
    joinButton.isEnabled = true
    refreshButton.isEnabled = false
    shareButton.isEnabled = false

    showStatus(
        "Previous match session is no longer available. Create or join a new match.",
    )
    return true
}

internal fun MainActivity.setNetworkControls(
    enabled: Boolean,
) {
    if (!enabled) {
        createButton.isEnabled = false
        joinButton.isEnabled = false
        refreshButton.isEnabled = false
        startButton.isEnabled = false
        rollButton.isEnabled = false
        return
    }

    val state = currentState
    if (state != null) {
        updateControls(state)
    } else {
        createButton.isEnabled =
            isOnline
        joinButton.isEnabled =
            isOnline
        nameInput.isEnabled = true
        matchInput.isEnabled = true
        refreshButton.isEnabled =
            isOnline &&
                playerToken != null
        startButton.isEnabled = false
        rollButton.isEnabled = false
    }
}

internal fun MainActivity.showStatus(
    value: String,
) {
    statusText.text = value
}

internal fun MainActivity.shortDigest(
    value: String?,
): String {
    if (
        value.isNullOrBlank()
    ) {
        return "—"
    }
    return if (
        value.length <= 16
    ) {
        value
    } else {
        value.take(8) +
            "…" +
            value.takeLast(6)
    }
}
