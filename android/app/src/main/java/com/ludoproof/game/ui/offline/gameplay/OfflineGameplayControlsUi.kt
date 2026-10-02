package com.ludoproof.game.ui.offline.gameplay

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
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*

internal fun OfflineGameActivity.gameplayActionPanel():
    LinearLayout =
    LudoProofTheme
        .panel(this)
        .apply {
            setPadding(
                dp(if (isCompactSetup()) 12 else 14),
                dp(if (isCompactSetup()) 12 else 14),
                dp(if (isCompactSetup()) 12 else 14),
                dp(if (isCompactSetup()) 12 else 14),
            )

            val diceRow =
                LinearLayout(
                    this@gameplayActionPanel,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            val diceDock =
                FrameLayout(
                    this@gameplayActionPanel,
                ).apply {
                    background =
                        LudoProofTheme
                            .hudPanelDrawable(
                                this@gameplayActionPanel,
                                goldBorder = true,
                            )
                    elevation =
                        dp(5).toFloat()
                }

            diceView =
                DiceView(
                    this@gameplayActionPanel,
                )
            diceDock.addView(
                diceView,
                FrameLayout.LayoutParams(
                    dp(
                        if (isCompactSetup()) 92 else 104,
                    ),
                    dp(
                        if (isCompactSetup()) 92 else 104,
                    ),
                    Gravity.CENTER,
                ),
            )

            diceRow.addView(
                diceDock,
                LinearLayout.LayoutParams(
                    dp(
                        if (isCompactSetup()) 110 else 122,
                    ),
                    dp(
                        if (isCompactSetup()) 110 else 122,
                    ),
                ),
            )

            val statusColumn =
                LinearLayout(
                    this@gameplayActionPanel,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    setPadding(
                        dp(12),
                        0,
                        0,
                        0,
                    )

                    addView(
                        TextView(
                            this@gameplayActionPanel,
                        ).apply {
                            text =
                                "LOCAL V4"
                            LudoProofTheme.body(
                                this,
                                10f,
                                bright = true,
                            )
                            setTextColor(
                                0xFF68F053.toInt(),
                            )
                            setPadding(
                                dp(10),
                                dp(5),
                                dp(10),
                                dp(5),
                            )
                            background =
                                LudoProofTheme
                                    .rounded(
                                        0xCC073A56.toInt(),
                                        999f,
                                        0xFF46E8A4.toInt(),
                                        1f,
                                        this@gameplayActionPanel,
                                    )
                        },
                    )

                    statusText =
                        TextView(
                            this@gameplayActionPanel,
                        ).apply {
                            text =
                                "Offline v4 local engine ready"
                            LudoProofTheme.body(
                                this,
                                if (isCompactSetup()) 12f else 13f,
                                bright = true,
                            )
                            setPadding(
                                0,
                                dp(8),
                                0,
                                0,
                            )
                        }
                    addView(
                        requireNotNull(
                            statusText,
                        ),
                    )

                    addView(
                        TextView(
                            this@gameplayActionPanel,
                        ).apply {
                            text =
                                "Dice result is applied only after local proof recomputation."
                            LudoProofTheme.body(
                                this,
                                10f,
                            )
                            setPadding(
                                0,
                                dp(3),
                                0,
                                0,
                            )
                        },
                    )
                }

            diceRow.addView(
                statusColumn,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            addView(diceRow)

            rollButton =
                Button(
                    this@gameplayActionPanel,
                ).apply {
                    text =
                        "ROLL DICE"
                    textSize =
                        if (isCompactSetup()) 18f else 20f
                    LudoProofTheme
                        .primary(this)
                    setOnClickListener {
                        rollOffline()
                    }
                }

            addView(
                requireNotNull(
                    rollButton,
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(
                        if (isCompactSetup()) 58 else 62,
                    ),
                ).apply {
                    topMargin =
                        dp(12)
                },
            )

            addView(
                gameplayTools(),
            )

            addView(
                TextView(
                    this@gameplayActionPanel,
                ).apply {
                    text =
                        "LOCAL V4 • Same EntroNex derivation • No remote attestation"
                    LudoProofTheme.body(
                        this,
                        10f,
                        centered = true,
                    )
                    setPadding(
                        dp(4),
                        dp(12),
                        dp(4),
                        0,
                    )
                },
            )
        }

internal fun OfflineGameActivity.gameplayTools():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            if (isCompactSetup()) {
                LinearLayout.VERTICAL
            } else {
                LinearLayout.HORIZONTAL
            }

        val history =
            Button(
                this@gameplayTools,
            ).apply {
                text =
                    "HISTORY"
                LudoProofTheme
                    .secondary(this)
                setOnClickListener {
                    ArcadeDialogs
                        .showProofHistory(
                            this@gameplayTools,
                            "OFFLINE HISTORY",
                            offlineHistory(
                                engine.snapshot(),
                            ),
                        )
                }
            }

        val engineMap =
            Button(
                this@gameplayTools,
            ).apply {
                text =
                    "ENGINE MAP"
                LudoProofTheme
                    .secondary(this)
                setOnClickListener {
                    ArcadeDialogs
                        .showNaturalWorldAudit(
                            this@gameplayTools,
                            engine
                                .lastRandomnessAudit(),
                        )
                }
            }

        val newGame =
            Button(
                this@gameplayTools,
            ).apply {
                text =
                    "NEW GAME"
                LudoProofTheme
                    .positive(this)
                setOnClickListener {
                    engine.clear()
                    showSetup()
                }
            }

        if (isCompactSetup()) {
            listOf(
                history,
                engineMap,
                newGame,
            ).forEach {
                    button ->
                addView(
                    button,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54),
                    ).apply {
                        topMargin =
                            dp(9)
                    },
                )
            }
        } else {
            addView(
                history,
                gameplayToolParams(),
            )
            addView(
                engineMap,
                gameplayToolParams(),
            )
            addView(
                newGame,
                gameplayToolParams(),
            )
        }
    }

internal fun OfflineGameActivity.gameplaySectionParams(
    topMarginDp: Int,
) =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(topMarginDp)
    }

internal fun OfflineGameActivity.gameplayToolParams() =
    LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f,
    ).apply {
        setMargins(
            dp(5),
            dp(12),
            dp(5),
            0,
        )
    }

internal fun OfflineGameActivity.renderGame(
    state: MatchSnapshot,
) {
    val active =
        state.players
            .getOrNull(
                state.turnSeat,
            )
    val winner =
        state.players
            .find {
                it.playerId ==
                    state.winnerPlayerId
            }

    infoText?.text =
        state.players
            .joinToString(
                separator =
                    "   •   ",
            ) {
                player ->
                player.color +
                    "  " +
                    player.displayName
            }

    if (
        state.status ==
        "FINISHED"
    ) {
        val winnerName =
            winner
                ?.displayName
                ?: "Player"

        turnText?.text =
            "MATCH COMPLETE"
        resultPanel.visibility =
            View.VISIBLE
        resultTitleText.text =
            "WINNER • " +
                winnerName
        resultSubtitleText.text =
            "Local v4 result • history and engine map remain available"
    } else {
        turnText?.text =
            "TURN • " +
                (
                    active
                        ?.displayName
                        ?: "Player"
                    )
        resultPanel.visibility =
            View.GONE
    }

    boardView?.bind(
        state,
        engine.activePlayerId(),
    )

    val pending =
        state.pendingRoll
    val latest =
        state.history
            .lastOrNull()
    val outcome =
        pending?.outcome
            ?: latest?.outcome

    if (
        outcome != null
    ) {
        diceView?.showOutcome(
            outcome,
        )
    }

    rollButton?.isEnabled =
        state.status ==
            "ACTIVE" &&
            pending ==
            null

    rollButton?.visibility =
        if (
            state.status ==
            "FINISHED"
        ) {
            View.GONE
        } else {
            View.VISIBLE
        }

    rollButton?.text =
        when {
            state.status ==
                "FINISHED" ->
                "GAME FINISHED"

            pending !=
                null ->
                "MOVE HIGHLIGHTED TOKEN"

            else ->
                "ROLL DICE"
        }

    when {
        state.status ==
            "FINISHED" ->
            showStatus(
                "Game complete • review history or start a new game.",
            )

        pending !=
            null ->
            showStatus(
                "Dice " +
                    pending.outcome +
                    " • move a highlighted token",
            )

        else ->
            showStatus(
                "Ready • roll when it is the active player's turn.",
            )
    }
}
