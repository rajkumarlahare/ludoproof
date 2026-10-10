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
    val source =
        if (
            response.optBoolean(
                CachedMatchStore.CACHE_SOURCE_MARKER,
                false,
            )
        ) {
            OnlineStateSource.CACHE
        } else {
            OnlineStateSource.AUTHORITATIVE
        }

    val envelope =
        try {
            GameJson.envelope(response)
        } catch (error: GameSchemaException) {
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

    val state = envelope.state
    if (state == null) {
        showStatus(response.toString(2))
        updateRollButton()
        return
    }

    if (
        source == OnlineStateSource.CACHE &&
        !OnlineCachedStateRestorePolicy.shouldApply(
            sessionMatchId = matchId,
            cachedMatchId = state.matchId,
        )
    ) {
        cachedMatchStore.clear()
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

    val previousState = currentState
    currentState = state
    LudoProofTheme.setGameplayBackground(
        root = arcadeRootView,
        enabled = state.status == "ACTIVE",
    )
    uiStateHolder.update {
        it.copy(
            currentStateSource = source,
        )
    }
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

    if (
        source == OnlineStateSource.AUTHORITATIVE &&
        state.status == "FINISHED" &&
        gameMode.ranked
    ) {
        runCatching {
            ProfileStore(this).recordCompletedMatch(
                matchId = state.matchId,
                mode = ProfileGameMode.CLASSIC,
                source = ProfileMatchSource.ONLINE,
                won = state.winnerPlayerId == playerId,
            )
        }
    }

    if (source == OnlineStateSource.AUTHORITATIVE) {
        response.optJSONObject("state")?.let { safeState ->
            cachedMatchStore.save(
                envelope.playerId ?: playerId,
                safeState,
            )
        }
    }

    reconcilePendingSecret(
        state = state,
        source = source,
    )

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

    val transitionMessage =
        OnlineTurnTransitionFeedbackPolicy.message(
            previous = previousState,
            current = state,
        )

    if (transitionMessage != null) {
        showStatus(transitionMessage)
    } else if (announce) {
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
    source: OnlineStateSource,
) {
    val secret = pendingSecret ?: return
    if (
        OnlinePendingRollRecoveryPolicy.action(
            source = source,
            secret = secret,
            state = state,
        ) == PendingRollSecretAction.CLEAR
    ) {
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
                    if (state.players.size == 4) "MATCH COMPLETE"
                    else if (winner?.playerId == playerId) "YOU WON"
                    else "WINNER • $winnerName"
                resultSubtitleText.text =
                    if (state.players.size == 4 && state.finishOrderPlayerIds.isNotEmpty()) {
                        "Final placements are shown on the board."
                    } else if (winner?.playerId == playerId) {
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
