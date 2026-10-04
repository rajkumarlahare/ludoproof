package com.ludoproof.game.feature.online

import android.view.View
import com.ludoproof.game.*
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.model.ProfileGameMode
import com.ludoproof.game.feature.profile.domain.model.ProfileMatchSource
import org.json.JSONObject

internal fun MainActivity.applyResponse(
    response: JSONObject,
    announce: Boolean = true,
) {
    val envelope =
        try {
            GameJson.envelope(response)
        } catch (error: GameSchemaException) {
            // Fail closed at the server-response boundary. Do not replace the
            // last known-good state or cache malformed authoritative data.
            diceView.stopRolling()
            showStatus(
                "Server state validation failed. Your last verified state was kept; refresh to retry.",
            )
            currentState?.let { safeState ->
                updateControls(safeState)
            }
            updateRollButton()
            return
        }

    envelope.playerId?.let { resolvedPlayerId ->
        playerId = resolvedPlayerId
        val code = matchId
        val token = playerToken
        if (code != null && token != null) {
            persistSessionSecurely(
                code = code,
                id = resolvedPlayerId,
                token = token,
            )
        }
    }

    val state = envelope.state
    if (state == null) {
        showStatus(response.toString(2))
        updateRollButton()
        return
    }

    val previousState = currentState
    currentState = state
    matchId = state.matchId

    val serverRevision =
        response.optJSONObject("state")
            ?.optInt("revision", -1)
            ?: response.optInt("revision", -1)
    if (serverRevision > lastRealtimeRevision) {
        lastRealtimeRevision = serverRevision
    }

    if (state.status == "FINISHED") {
        realtimeClient.disconnect()
        realtimeConnected = false
        updateConnectionLabel()
    }

    // Team Up v1 is intentionally unranked until a dedicated team ledger can
    // represent two co-winners without corrupting individual profile stats.
    if (state.status == "FINISHED" && gameMode.ranked) {
        runCatching {
            ProfileStore(this).recordCompletedMatch(
                matchId = state.matchId,
                mode = ProfileGameMode.CLASSIC,
                source = ProfileMatchSource.ONLINE,
                won = state.winnerPlayerId == playerId,
            )
        }
    }

    response.optJSONObject("state")?.let { safeState ->
        cachedMatchStore.save(
            envelope.playerId ?: playerId,
            safeState,
        )
    }

    reconcilePendingSecret(state)

    // Keep the proven legacy board synchronized as a safe fallback while the
    // presentation-only Ludo Paws shell owns visible remote rendering.
    boardView.bind(state, playerId)
    OnlineLudoPawsPresentation.render(
        activity = this,
        previous = previousState,
        current = state,
    )

    matchInfoText.text =
        buildString {
            append("MATCH • ")
            append(state.matchId)
            append("   •   ")
            append(state.status)
            if (state.matchMode == "TEAM_UP") {
                append("   •   2 VS 2")
            }
            if (state.rulesetId.isNotBlank()) {
                append("   •   ")
                append(state.rulesetId)
            }
        }

    playersText.text =
        state.players.joinToString(separator = "\n") { player ->
            val marker = if (player.playerId == playerId) "  •  YOU" else ""
            val team =
                if (state.matchMode == "TEAM_UP" && player.teamId != null) {
                    "  •  TEAM ${player.teamId}"
                } else {
                    ""
                }
            player.color + "  •  " + player.displayName + team + marker
        }

    updateTurnBanner(state)
    updateVerification(state)
    proofDetailsText.text = proofDetails(state)
    updateControls(state)
    updateRollButton()

    if (announce) {
        showStatus(
            if (state.status == "WAITING") {
                "Room synced • waiting for all required players."
            } else {
                "State revision updated. Event index: " + state.randomEventIndex
            },
        )
    }
}

internal fun MainActivity.reconcilePendingSecret(
    state: MatchSnapshot,
) {
    val secret = pendingSecret ?: return
    if (secret.matchId != state.matchId) {
        pendingRollStore.clear()
        pendingSecret = null
        return
    }

    val remoteCommitment = state.pendingRoll?.clientCommitment
    if (remoteCommitment == null || remoteCommitment != secret.clientCommitment) {
        pendingRollStore.clear()
        pendingSecret = null
    }
}

internal fun MainActivity.updateTurnBanner(
    state: MatchSnapshot,
) {
    when (state.status) {
        "WAITING" -> {
            turnText.text =
                if (
                    state.targetPlayerCount != null &&
                    state.players.size < state.targetPlayerCount
                ) {
                    "WAITING FOR PLAYERS • ${state.players.size}/${state.targetPlayerCount}"
                } else if (state.players.size < 2) {
                    "WAITING FOR PLAYER"
                } else if (state.hostPlayerId == playerId) {
                    "READY • START THE MATCH"
                } else {
                    "READY • WAITING FOR HOST"
                }
        }

        "FINISHED" -> {
            turnText.text = "MATCH COMPLETE"
            if (state.matchMode == "TEAM_UP") {
                val me = state.players.find { it.playerId == playerId }
                val winnerTeam = state.winnerTeamId
                val won = winnerTeam != null && me?.teamId == winnerTeam
                val winners =
                    state.players
                        .filter { it.teamId == winnerTeam }
                        .joinToString(" + ") { it.displayName }
                        .ifBlank { "Team ${winnerTeam ?: "—"}" }

                resultTitleText.text =
                    if (won) "YOUR TEAM WON" else "TEAM ${winnerTeam ?: "—"} WINS"
                resultSubtitleText.text =
                    "$winners • server-authoritative team result • verified history available"
            } else {
                val winner = state.players.find { it.playerId == state.winnerPlayerId }
                val winnerName = winner?.displayName ?: "Player"
                resultTitleText.text =
                    if (winner?.playerId == playerId) "YOU WON"
                    else "WINNER • $winnerName"
                resultSubtitleText.text =
                    if (winner?.playerId == playerId) {
                        "$winnerName • server-authoritative result • proof history available"
                    } else {
                        "Server-authoritative result • verified history available"
                    }
            }
        }

        else -> {
            val activeSeat = state.actingSeat ?: state.turnSeat
            val active = state.players.getOrNull(activeSeat)
            val mine = active?.playerId == playerId
            val handoff =
                state.matchMode == "TEAM_UP" &&
                    state.actingSeat != null &&
                    state.actingSeat != state.turnSeat
            turnText.text =
                if (mine) {
                    if (handoff) "YOUR TEAM TURN • PARTNER HANDOFF"
                    else "YOUR TURN • ROLL OR MOVE"
                } else {
                    val prefix = if (handoff) "TEAM HANDOFF • " else "WAITING • "
                    prefix + (active?.displayName ?: "Player") + "'S TURN"
                }
        }
    }
}
