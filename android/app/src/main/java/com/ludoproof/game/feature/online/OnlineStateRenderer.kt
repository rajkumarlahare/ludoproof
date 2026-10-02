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

internal fun MainActivity.updateVerification(
    state: MatchSnapshot,
) {
    val pending =
        state.pendingRoll
    val latest =
        state.history
            .lastOrNull()

    val outcome =
        pending?.outcome
            ?: latest?.outcome
    val digest =
        pending?.proofDigest
            ?: latest?.proofDigest
    val eventIndex =
        if (
            pending?.eventIndex !=
            null &&
            pending.eventIndex >=
            0
        ) {
            pending.eventIndex
        } else {
            latest?.eventIndex
        }

    verificationText.text =
        if (
            outcome !=
                null &&
            digest !=
                null
        ) {
            diceView.showOutcome(
                outcome,
            )
            buildString {
                append(
                    "✓ VERIFIED • Dice ",
                )
                append(
                    outcome,
                )
                if (
                    eventIndex !=
                    null
                ) {
                    append(
                        "\nEvent ",
                    )
                    append(
                        eventIndex,
                    )
                }
                append(
                    "  •  Proof ",
                )
                append(
                    shortDigest(
                        digest,
                    ),
                )
            }
        } else {
            when (
                pending?.status
            ) {
                "CREATING" ->
                    "COMMITTING • Preparing server commitment…"

                "COMMITTED" ->
                    "COMMITTED • Server commitment secured. Reveal can resume safely."

                "RESOLVING" ->
                    "VERIFYING • Resolving committed EntroNex round…"

                else ->
                    "No verified roll yet."
            }
        }
}

internal fun MainActivity.updateControls(
    state: MatchSnapshot,
) {
    val mySeat =
        state.players
            .indexOfFirst {
                it.playerId ==
                    playerId
            }
    val myTurn =
        state.status ==
            "ACTIVE" &&
            mySeat >=
            0 &&
            state.turnSeat ==
            mySeat

    val waiting =
        state.status ==
            "WAITING"
    val active =
        state.status ==
            "ACTIVE"
    val finished =
        state.status ==
            "FINISHED"
    val canEnterAnotherMatch =
        finished

    lobbyPanel.visibility =
        if (
            canEnterAnotherMatch
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

    nameInput.visibility =
        if (
            canEnterAnotherMatch
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    matchInput.visibility =
        if (
            canEnterAnotherMatch
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    createButton.visibility =
        if (
            canEnterAnotherMatch
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    joinButton.visibility =
        if (
            canEnterAnotherMatch
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

    createButton.isEnabled =
        canEnterAnotherMatch &&
            isOnline
    joinButton.isEnabled =
        canEnterAnotherMatch &&
            isOnline
    nameInput.isEnabled =
        canEnterAnotherMatch
    matchInput.isEnabled =
        canEnterAnotherMatch

    matchStatusPanel.visibility =
        View.VISIBLE
    resultPanel.visibility =
        if (
            finished
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    boardFrame.visibility =
        if (
            active ||
            finished
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    actionPanel.visibility =
        View.VISIBLE

    verificationPanel.visibility =
        if (
            active ||
            finished
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

    diceView.visibility =
        View.VISIBLE
    verificationText.visibility =
        View.VISIBLE

    startButton.visibility =
        if (
            waiting
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    startButton.isEnabled =
        isOnline &&
            waiting &&
            state.players.size >=
            2 &&
            state.hostPlayerId ==
            playerId

    refreshButton.visibility =
        if (
            finished
        ) {
            View.GONE
        } else {
            View.VISIBLE
        }
    refreshButton.isEnabled =
        isOnline &&
            !finished

    shareButton.visibility =
        View.VISIBLE
    shareButton.isEnabled =
        state.matchId
            .isNotBlank()

    proofButton.visibility =
        if (
            active ||
            finished
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

    rollButton.visibility =
        if (
            active
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    rollButton.isEnabled =
        isOnline &&
            myTurn &&
            state.pendingRoll
                ?.status !=
            "RESOLVED"
}

internal fun MainActivity.updateRollButton() {
    val secret =
        pendingSecret
    rollButton.text =
        if (
            secret != null &&
            secret.matchId ==
            matchId
        ) {
            "RESUME VERIFIED ROLL"
        } else {
            "ROLL VERIFIED DICE"
        }
}

internal fun MainActivity.proofDetails(
    state: MatchSnapshot,
): String {
    val latest =
        state.history
            .takeLast(8)
            .reversed()

    if (latest.isEmpty()) {
        return "No proof history yet."
    }

    return buildString {
        append(
            "Recent verified events\n",
        )
        latest.forEach {
            event ->
            append(
                "#",
            )
            append(
                event.eventIndex,
            )
            append(
                "  dice=",
            )
            append(
                event.outcome
                    ?: "?",
            )
            append(
                "  proof=",
            )
            append(
                shortDigest(
                    event.proofDigest,
                ),
            )
            append(
                "  round=",
            )
            append(
                shortDigest(
                    event.roundId,
                ),
            )
            if (
                event.moveTokenIndex !=
                null
            ) {
                append(
                    "  token=",
                )
                append(
                    event.moveTokenIndex +
                        1,
                )
            }
            if (
                event.captures > 0
            ) {
                append(
                    "  captures=",
                )
                append(
                    event.captures,
                )
            }
            append("\n")
        }
    }.trimEnd()
}

internal fun MainActivity.toggleProofDetails() {
    ArcadeDialogs.showProofHistory(
        this,
        "VERIFIED HISTORY",
        proofDetailsText.text
            .toString()
            .ifBlank {
                "No proof history yet."
            },
    )
}
