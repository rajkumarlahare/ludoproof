package com.ludoproof.game.feature.online

import android.content.Intent
import android.view.View
import com.ludoproof.game.*
import org.json.JSONObject

internal fun MainActivity.shareMatch() {
    if (gameMode == GameMode.TEAM_UP) {
        showStatus("Team Up rooms are assigned by secure matchmaking and cannot be shared.")
        return
    }
    val code =
        matchId
            ?: run {
                showStatus("Create or join a match first.")
                return
            }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Join my Ludo Paws match: $code")
    }
    startActivity(Intent.createChooser(intent, "Share Ludo Paws match"))
}

internal fun MainActivity.playerName(): String {
    val value = nameInput.text.toString().trim()
    require(value.length in 2..24) {
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
                modeWire = gameMode.wireValue,
            ),
        )
    } catch (_: Exception) {
        secureSessionStore.clear()
    }
}

internal fun MainActivity.withSession(
    action: (String, String) -> Unit,
) {
    val code = matchId
    val token = playerToken
    if (code == null || token == null) {
        showStatus("Create or join a match first.")
        return
    }
    action(code, token)
}

internal fun MainActivity.runNetwork(
    action: () -> JSONObject,
    onSuccess: (JSONObject) -> Unit = { applyResponse(it) },
    showWorking: Boolean = true,
) {
    if (!canRenderUi()) return

    if (!isOnline) {
        diceView.stopRolling()
        showStatus("Offline — this online match is read-only until internet returns.")
        currentState?.let { updateControls(it) }
        return
    }
    if (executor.isShutdown) return

    if (showWorking) showStatus("Working…")
    setNetworkControls(enabled = false)

    val submitted =
        runCatching {
            executor.execute {
                try {
                    val response = action()
                    mainHandler.post {
                        if (!canRenderUi()) return@post
                        setNetworkControls(enabled = true)
                        onSuccess(response)
                        updateRollButton()
                    }
                } catch (error: Exception) {
                    mainHandler.post {
                        if (!canRenderUi()) return@post
                        diceView.stopRolling()
                        val sessionReset = resetInvalidSessionIfNeeded(error)
                        if (!sessionReset) {
                            setNetworkControls(enabled = true)
                            showStatus("Error: " + (error.message ?: error.toString()))
                            currentState?.let { updateControls(it) }
                        }
                        updateRollButton()
                    }
                }
            }
        }.isSuccess

    if (!submitted && canRenderUi()) {
        diceView.stopRolling()
        setNetworkControls(enabled = true)
        showStatus("Request was cancelled because the screen is closing.")
        updateRollButton()
    }
}

internal fun MainActivity.resetInvalidSessionIfNeeded(
    error: Exception,
): Boolean {
    val code = (error as? GameApiException)?.code ?: return false
    if (code != "MATCH_NOT_FOUND" && code != "AUTH_INVALID") return false

    secureSessionStore.clear()
    pendingRollStore.clear()
    pendingSecret = null
    matchId = null
    playerToken = null
    playerId = null
    currentState = null
    uiStateHolder.update {
        it.copy(
            currentStateSource = null,
        )
    }
    lastRealtimeRevision = -1
    cachedMatchStore.clear()
    realtimeClient.disconnect()
    realtimeConnected = false
    OnlineLudoPawsPresentation.clear(this)

    if (gameMode != GameMode.ONLINE) {
        showStatus(
            if (gameMode == GameMode.TEAM_UP) {
                "Team Up match is no longer available. Start a new Quick Team search."
            } else {
                "Private friend room is no longer available."
            },
        )
        finish()
        return true
    }

    matchInput.setText("")
    boardView.bind(null, null)
    diceView.stopRolling()
    matchInfoText.text = "No active match"
    playersText.text = "Players will appear here."
    turnText.text = "Create or join a new match."
    verificationText.text = "No verified roll yet."
    proofDetailsText.text = ""
    proofDetailsText.visibility = View.GONE

    lobbyPanel.visibility = View.VISIBLE
    nameInput.visibility = View.VISIBLE
    matchInput.visibility = View.VISIBLE
    createButton.visibility = View.VISIBLE
    joinButton.visibility = View.VISIBLE
    startButton.visibility = View.GONE
    rollButton.visibility = View.GONE

    restorePublicMatchmakingUi()
    nameInput.isEnabled = isOnline
    matchInput.isEnabled = isOnline
    createButton.isEnabled = isOnline
    joinButton.isEnabled = isOnline
    findMatchButton.isEnabled = isOnline
    twoPlayerButton.isEnabled = isOnline
    fourPlayerButton.isEnabled = isOnline
    cancelMatchmakingButton.isEnabled =
        isOnline && publicMatchmakingStore.load() != null
    refreshButton.isEnabled = false
    shareButton.isEnabled = false

    showStatus("Previous match session is no longer available. Create or join a new match.")
    return true
}

internal fun MainActivity.setNetworkControls(
    enabled: Boolean,
) {
    if (!enabled) {
        findMatchButton.isEnabled = false
        cancelMatchmakingButton.isEnabled = false
        twoPlayerButton.isEnabled = false
        fourPlayerButton.isEnabled = false
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
    } else if (gameMode == GameMode.ONLINE) {
        val searching = publicMatchmakingStore.load() != null
        setPublicSearchUi(
            searching = searching,
            queuedPlayers = if (searching) 1 else 0,
            targetPlayerCount =
                publicMatchmakingStore.load()?.playerCount
                    ?: selectedPublicPlayerCount,
        )
        refreshButton.isEnabled = isOnline && playerToken != null
        startButton.isEnabled = false
        rollButton.isEnabled = false
    } else {
        refreshButton.isEnabled = isOnline && playerToken != null
        startButton.isEnabled = false
        rollButton.isEnabled = false
    }
}

internal fun MainActivity.showStatus(value: String) {
    statusText.text = value
}

internal fun MainActivity.shortDigest(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return if (value.length <= 16) value else value.take(8) + "…" + value.takeLast(6)
}
