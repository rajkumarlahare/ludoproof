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

internal fun MainActivity.applyResponse(
    response: JSONObject,
    announce: Boolean = true,
) {
    val envelope =
        GameJson.envelope(
            response,
        )

    envelope.playerId
        ?.let {
                resolvedPlayerId ->
            playerId =
                resolvedPlayerId
            val code =
                matchId
            val token =
                playerToken
            if (
                code != null &&
                token != null
            ) {
                persistSessionSecurely(
                    code =
                        code,
                    id =
                        resolvedPlayerId,
                    token =
                        token,
                )
            }
        }

    val state =
        envelope.state
    if (
        state ==
        null
    ) {
        showStatus(
            response
                .toString(2),
        )
        updateRollButton()
        return
    }

    currentState =
        state
    matchId =
        state.matchId

    response
        .optJSONObject(
            "state",
        )
        ?.let {
                safeState ->
            cachedMatchStore
                .save(
                    envelope.playerId
                        ?: playerId,
                    safeState,
                )
        }

    reconcilePendingSecret(
        state,
    )
    boardView.bind(
        state,
        playerId,
    )

    matchInfoText.text =
        buildString {
            append(
                "MATCH • ",
            )
            append(
                state.matchId,
            )
            append(
                "   •   ",
            )
            append(
                state.status,
            )
            if (
                state.rulesetId
                    .isNotBlank()
            ) {
                append(
                    "   •   ",
                )
                append(
                    state.rulesetId,
                )
            }
        }

    playersText.text =
        state.players
            .joinToString(
                separator =
                    "\n",
            ) {
                player ->
                val marker =
                    if (
                        player.playerId ==
                        playerId
                    ) {
                        "  •  YOU"
                    } else {
                        ""
                    }
                player.color +
                    "  •  " +
                    player.displayName +
                    marker
            }

    updateTurnBanner(
        state,
    )
    updateVerification(
        state,
    )
    proofDetailsText.text =
        proofDetails(
            state,
        )
    updateControls(
        state,
    )
    updateRollButton()

    if (
        announce
    ) {
        showStatus(
            if (
                state.status ==
                "WAITING"
            ) {
                "Room synced • share the code and wait for players."
            } else {
                "State revision updated. Event index: " +
                    state.randomEventIndex
            },
        )
    }
}

internal fun MainActivity.reconcilePendingSecret(
    state: MatchSnapshot,
) {
    val secret =
        pendingSecret
            ?: return
    if (secret.matchId != state.matchId) {
        pendingRollStore.clear()
        pendingSecret = null
        return
    }

    val remoteCommitment =
        state.pendingRoll
            ?.clientCommitment
    if (
        remoteCommitment == null ||
        remoteCommitment !=
        secret.clientCommitment
    ) {
        pendingRollStore.clear()
        pendingSecret = null
    }
}

internal fun MainActivity.updateTurnBanner(
    state: MatchSnapshot,
) {
    when (
        state.status
    ) {
        "WAITING" -> {
            turnText.text =
                if (
                    state.players.size <
                    2
                ) {
                    "WAITING FOR PLAYER"
                } else if (
                    state.hostPlayerId ==
                    playerId
                ) {
                    "READY • START THE MATCH"
                } else {
                    "READY • WAITING FOR HOST"
                }
        }

        "FINISHED" -> {
            val winner =
                state.players
                    .find {
                        it.playerId ==
                            state.winnerPlayerId
                    }
            val winnerName =
                winner
                    ?.displayName
                    ?: "Player"

            turnText.text =
                "MATCH COMPLETE"

            resultTitleText.text =
                if (
                    winner?.playerId ==
                    playerId
                ) {
                    "YOU WON"
                } else {
                    "WINNER • " +
                        winnerName
                }

            resultSubtitleText.text =
                if (
                    winner?.playerId ==
                    playerId
                ) {
                    winnerName +
                        " • server-authoritative result • proof history available"
                } else {
                    "Server-authoritative result • verified history available"
                }
        }

        else -> {
            val active =
                state.players
                    .getOrNull(
                        state.turnSeat,
                    )
            val mine =
                active?.playerId ==
                    playerId
            turnText.text =
                if (
                    mine
                ) {
                    "YOUR TURN • ROLL OR MOVE"
                } else {
                    "WAITING • " +
                        (
                            active
                                ?.displayName
                                ?: "Player"
                            ) +
                        "'S TURN"
                }
        }
    }
}
