package com.ludoproof.game.feature.online

import android.view.View
import com.ludoproof.game.*
import com.ludoproof.game.ui.online.*

internal fun MainActivity.updateVerification(
    state: MatchSnapshot,
) {
    val pending = state.pendingRoll
    val presentation =
        OnlineVerifiedDicePresentationPolicy.resolve(state)

    verificationText.text =
        if (presentation != null) {
            val shouldAnimate =
                uiStateHolder.value.presentedDiceEventKey !=
                    presentation.eventKey
            diceView.showOutcome(
                outcome = presentation.outcome,
                animate = shouldAnimate,
            )
            if (shouldAnimate) {
                uiStateHolder.update {
                    it.copy(
                        presentedDiceEventKey = presentation.eventKey,
                    )
                }
            }

            buildString {
                append("✓ VERIFIED • Dice ")
                append(presentation.outcome)
                if (presentation.openingRollApplied) {
                    append(" • opening bonus")
                }
                presentation.eventIndex?.let { eventIndex ->
                    append("\nEvent ")
                    append(eventIndex)
                }
                append("  •  Proof ")
                append(shortDigest(presentation.proofDigest))
            }
        } else {
            if (pending != null) {
                diceView.startRolling()
            } else {
                diceView.stopRolling()
            }

            when (pending?.status) {
                "CREATING" -> "COMMITTING • Preparing server commitment…"
                "COMMITTED" ->
                    "COMMITTED • Server commitment secured. Reveal can resume safely."
                "RESOLVING" -> "VERIFYING • Resolving committed EntroNex round…"
                "RESOLVED" ->
                    "VERIFYING • Waiting for the complete proof before showing a verified result."
                else -> "No verified roll yet."
            }
        }
}

/**
 * Cached snapshots are display-only. Losing connectivity also invalidates the
 * in-memory action authority so a reconnect must obtain a fresh server snapshot
 * before any mutation can be submitted.
 */
internal fun MainActivity.hasAuthoritativeActionState(): Boolean {
    if (!isOnline) {
        if (uiStateHolder.value.currentStateSource != null) {
            uiStateHolder.update {
                it.copy(
                    currentStateSource = null,
                )
            }
        }
        return false
    }
    return uiStateHolder.value.currentStateSource ==
        OnlineStateSource.AUTHORITATIVE
}

internal fun MainActivity.onlineRollActionDecision(
    state: MatchSnapshot,
): OnlineRollActionDecision {
    val mySeat =
        state.players.indexOfFirst {
            it.playerId == playerId
        }
    val actionableSeat = state.actingSeat ?: state.turnSeat
    val myTurn =
        state.status == "ACTIVE" &&
            mySeat >= 0 &&
            actionableSeat == mySeat
    val secret = pendingSecret

    return OnlineRollActionPolicy.resolve(
        isOnline = isOnline,
        hasAuthoritativeState = hasAuthoritativeActionState(),
        matchStatus = state.status,
        myTurn = myTurn,
        pendingStatus = state.pendingRoll?.status,
        remoteClientCommitment = state.pendingRoll?.clientCommitment,
        currentMatchId = state.matchId,
        localSecretMatchId = secret?.matchId,
        localClientCommitment = secret?.clientCommitment,
    )
}

internal fun MainActivity.updateControls(
    state: MatchSnapshot,
) {
    val mySeat =
        state.players.indexOfFirst {
            it.playerId == playerId
        }
    val actionableSeat = state.actingSeat ?: state.turnSeat
    val myTurn =
        state.status == "ACTIVE" &&
            mySeat >= 0 &&
            actionableSeat == mySeat
    val authoritativeActions =
        hasAuthoritativeActionState()

    val waiting = state.status == "WAITING"
    val active = state.status == "ACTIVE"
    val finished = state.status == "FINISHED"
    val canEnterAnotherMatch =
        finished && gameMode == GameMode.ONLINE

    lobbyPanel.visibility =
        if (canEnterAnotherMatch) View.VISIBLE else View.GONE

    if (canEnterAnotherMatch) {
        restorePublicMatchmakingUi()
    } else {
        setPublicSearchUi(searching = false)
    }

    nameInput.visibility = if (canEnterAnotherMatch) View.VISIBLE else View.GONE
    matchInput.visibility = if (canEnterAnotherMatch) View.VISIBLE else View.GONE
    createButton.visibility = if (canEnterAnotherMatch) View.VISIBLE else View.GONE
    joinButton.visibility = if (canEnterAnotherMatch) View.VISIBLE else View.GONE

    createButton.isEnabled = canEnterAnotherMatch && authoritativeActions
    joinButton.isEnabled = canEnterAnotherMatch && authoritativeActions
    nameInput.isEnabled = canEnterAnotherMatch && authoritativeActions
    matchInput.isEnabled = canEnterAnotherMatch && authoritativeActions

    matchStatusPanel.visibility = View.VISIBLE
    resultPanel.visibility = if (finished) View.VISIBLE else View.GONE
    boardFrame.visibility = if (active || finished) View.VISIBLE else View.GONE
    actionPanel.visibility = View.VISIBLE

    verificationPanel.visibility =
        if (active || finished) View.VISIBLE else View.GONE

    diceView.visibility = View.VISIBLE
    diceView.alpha = 1f
    verificationText.visibility = View.VISIBLE

    startButton.visibility = if (waiting) View.VISIBLE else View.GONE
    val requiredPlayers = state.targetPlayerCount ?: 2
    startButton.isEnabled =
        authoritativeActions &&
            waiting &&
            state.players.size >= requiredPlayers &&
            state.hostPlayerId == playerId

    refreshButton.visibility = if (finished) View.GONE else View.VISIBLE
    refreshButton.isEnabled = isOnline && !finished

    shareButton.visibility =
        if (state.matchMode == "TEAM_UP") View.GONE else View.VISIBLE
    shareButton.isEnabled =
        state.matchMode != "TEAM_UP" && state.matchId.isNotBlank()

    proofButton.visibility =
        if (active || finished) View.VISIBLE else View.GONE

    val rollAction = onlineRollActionDecision(state)
    rollButton.visibility = if (active) View.VISIBLE else View.GONE
    rollButton.isEnabled = rollAction.enabled
    rollButton.text = rollAction.label

    scheduleSingleLegalOnlineMove(
        state = state,
        myTurn = myTurn,
    )
}

private fun MainActivity.scheduleSingleLegalOnlineMove(
    state: MatchSnapshot,
    myTurn: Boolean,
) {
    val pending =
        state.pendingRoll
            ?.takeIf { it.status == "RESOLVED" }
            ?: return
    if (
        !myTurn ||
        !hasAuthoritativeActionState()
    ) {
        return
    }

    val tokenIndex =
        LudoTurnAutomationPolicy.singleLegalTokenIndex(
            pending.legalTokenIndexes,
        ) ?: return

    val eventKey =
        "single-legal:${state.matchId}:${pending.eventIndex}:$tokenIndex"
    if (rollButton.tag == eventKey) return
    rollButton.tag = eventKey

    showStatus("Only one move is possible • moving automatically…")
    rollButton.postDelayed(
        {
            val latest = currentState
            val latestPending = latest?.pendingRoll
            val stillSameMove =
                latest != null &&
                    hasAuthoritativeActionState() &&
                    latest.status == "ACTIVE" &&
                    latestPending?.status == "RESOLVED" &&
                    latestPending.eventIndex == pending.eventIndex &&
                    LudoTurnAutomationPolicy.singleLegalTokenIndex(
                        latestPending.legalTokenIndexes,
                    ) == tokenIndex &&
                    (latest.actingSeat ?: latest.turnSeat) ==
                        latest.players.indexOfFirst { player ->
                            player.playerId == playerId
                        }

            if (stillSameMove) {
                moveToken(tokenIndex)
            } else if (rollButton.tag == eventKey) {
                rollButton.tag = null
            }
        },
        320L,
    )
}

internal fun MainActivity.updateRollButton() {
    val state = currentState
    rollButton.text =
        if (state == null) {
            "ROLL VERIFIED DICE"
        } else {
            onlineRollActionDecision(state).label
        }
}

internal fun MainActivity.proofDetails(
    state: MatchSnapshot,
): String {
    val latest = state.history.takeLast(8).reversed()

    if (latest.isEmpty()) {
        return "No proof history yet."
    }

    return buildString {
        append("Recent verified events\n")
        latest.forEach { event ->
            append("#")
            append(event.eventIndex)
            append("  dice=")
            append(event.effectiveOutcome ?: event.outcome ?: "?")
            if (event.openingRollApplied) {
                append("(opening)")
            }
            append("  proof=")
            append(shortDigest(event.proofDigest))
            append("  round=")
            append(shortDigest(event.roundId))
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

internal fun MainActivity.toggleProofDetails() {
    ArcadeDialogs.showProofHistory(
        this,
        "VERIFIED HISTORY",
        proofDetailsText.text.toString().ifBlank {
            "No proof history yet."
        },
    )
}
