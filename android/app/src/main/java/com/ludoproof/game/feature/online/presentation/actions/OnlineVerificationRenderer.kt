package com.ludoproof.game.feature.online

import android.view.View
import com.ludoproof.game.*
import com.ludoproof.game.ui.online.*

internal fun MainActivity.updateVerification(
    state: MatchSnapshot,
) {
    val pending = state.pendingRoll
    val latest = state.history.lastOrNull()

    val outcome = pending?.outcome ?: latest?.outcome
    val digest = pending?.proofDigest ?: latest?.proofDigest
    val eventIndex =
        if (pending?.eventIndex != null && pending.eventIndex >= 0) {
            pending.eventIndex
        } else {
            latest?.eventIndex
        }

    verificationText.text =
        if (outcome != null && digest != null) {
            diceView.showOutcome(outcome)
            buildString {
                append("✓ VERIFIED • Dice ")
                append(outcome)
                if (eventIndex != null) {
                    append("\nEvent ")
                    append(eventIndex)
                }
                append("  •  Proof ")
                append(shortDigest(digest))
            }
        } else {
            when (pending?.status) {
                "CREATING" -> "COMMITTING • Preparing server commitment…"
                "COMMITTED" ->
                    "COMMITTED • Server commitment secured. Reveal can resume safely."
                "RESOLVING" -> "VERIFYING • Resolving committed EntroNex round…"
                else -> "No verified roll yet."
            }
        }
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

    createButton.isEnabled = canEnterAnotherMatch && isOnline
    joinButton.isEnabled = canEnterAnotherMatch && isOnline
    nameInput.isEnabled = canEnterAnotherMatch
    matchInput.isEnabled = canEnterAnotherMatch

    matchStatusPanel.visibility = View.VISIBLE
    resultPanel.visibility = if (finished) View.VISIBLE else View.GONE
    boardFrame.visibility = if (active || finished) View.VISIBLE else View.GONE
    actionPanel.visibility = View.VISIBLE

    verificationPanel.visibility =
        if (active || finished) View.VISIBLE else View.GONE

    diceView.visibility = View.VISIBLE
    verificationText.visibility = View.VISIBLE

    startButton.visibility = if (waiting) View.VISIBLE else View.GONE
    val requiredPlayers = state.targetPlayerCount ?: 2
    startButton.isEnabled =
        isOnline &&
            waiting &&
            state.players.size >= requiredPlayers &&
            state.hostPlayerId == playerId

    refreshButton.visibility = if (finished) View.GONE else View.VISIBLE
    refreshButton.isEnabled = isOnline && !finished

    // Public Team Up matches are allocated by the server. Sharing their room
    // code would bypass team matchmaking ownership, so do not expose it.
    shareButton.visibility =
        if (state.matchMode == "TEAM_UP") View.GONE else View.VISIBLE
    shareButton.isEnabled =
        state.matchMode != "TEAM_UP" && state.matchId.isNotBlank()

    proofButton.visibility =
        if (active || finished) View.VISIBLE else View.GONE

    rollButton.visibility = if (active) View.VISIBLE else View.GONE
    rollButton.isEnabled =
        isOnline &&
            myTurn &&
            state.pendingRoll?.status != "RESOLVED"
}

internal fun MainActivity.updateRollButton() {
    val secret = pendingSecret
    rollButton.text =
        if (secret != null && secret.matchId == matchId) {
            "RESUME VERIFIED ROLL"
        } else {
            "ROLL VERIFIED DICE"
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
            append(event.outcome ?: "?")
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
