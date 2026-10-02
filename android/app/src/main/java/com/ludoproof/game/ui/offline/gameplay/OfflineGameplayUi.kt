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

internal fun OfflineGameActivity.showGame(snapshot: MatchSnapshot?) {
    val state =
        snapshot ?: run {
            showSetup()
            return
        }

    val (root, host) =
        LudoProofTheme.arcadeRoot(this)

    val scroll =
        ScrollView(this).apply {
            isFillViewport =
                true
            overScrollMode =
                View.OVER_SCROLL_NEVER
        }

    val contentHost =
        FrameLayout(this)
    scroll.addView(
        contentHost,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    val horizontalPaddingDp =
        LudoProofTheme
            .pageHorizontalPaddingDp(this)
    val availableWidth =
        (
            resources.displayMetrics.widthPixels -
                dp(horizontalPaddingDp * 2)
            ).coerceAtLeast(1)
    val contentWidth =
        minOf(
            availableWidth,
            dp(
                LudoProofTheme
                    .pageMaxContentWidthDp(this),
            ),
        )

    val content =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                0,
                dp(14),
                0,
                dp(28),
            )
        }

    contentHost.addView(
        content,
        FrameLayout.LayoutParams(
            contentWidth,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or
                Gravity.CENTER_HORIZONTAL,
        ),
    )

    host.addView(
        scroll,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )

    content.addView(
        backHeader(
            "LOCAL • CLASSIC",
        ),
    )

    content.addView(
        gameplayHud(),
        gameplaySectionParams(
            if (isCompactSetup()) 12 else 16,
        ),
    )

    resultPanel =
        offlineResultPanel()
            .apply {
                visibility =
                    View.GONE
            }
    content.addView(
        resultPanel,
        gameplaySectionParams(
            if (isCompactSetup()) 12 else 14,
        ),
    )

    boardView =
        LudoBoardView(this).apply {
            onTokenSelected = {
                    tokenIndex ->
                runCatching {
                    engine.move(
                        tokenIndex,
                    )
                }.onSuccess {
                        next ->
                    renderGame(next)
                    showStatus(
                        "Move accepted.",
                    )
                }.onFailure {
                        error ->
                    showStatus(
                        error.message
                            ?: "Move failed",
                    )
                }
            }
        }

    val frame =
        LudoProofTheme
            .boardFrame(this)
    frame.addView(
        boardView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    content.addView(
        frame,
        gameplaySectionParams(
            if (isCompactSetup()) 12 else 14,
        ).apply {
            leftMargin =
                dp(2)
            rightMargin =
                dp(2)
        },
    )

    content.addView(
        gameplayActionPanel(),
        gameplaySectionParams(
            if (isCompactSetup()) 12 else 14,
        ),
    )

    setContentView(root)
    renderGame(state)
}

internal fun OfflineGameActivity.gameplayHud():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(if (isCompactSetup()) 12 else 16),
            dp(12),
            dp(if (isCompactSetup()) 12 else 16),
            dp(12),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    this@gameplayHud,
                    goldBorder = true,
                )
        elevation =
            dp(6).toFloat()

        addView(
            TextView(
                this@gameplayHud,
            ).apply {
                text =
                    "LOCAL MATCH"
                LudoProofTheme.body(
                    this,
                    10f,
                    centered = true,
                    bright = true,
                )
                setTextColor(
                    0xFF6DE7FF.toInt(),
                )
            },
        )

        turnText =
            TextView(
                this@gameplayHud,
            ).apply {
                LudoProofTheme.title(
                    this,
                    if (isCompactSetup()) 21f else 24f,
                    gold = true,
                )
                setPadding(
                    dp(4),
                    dp(3),
                    dp(4),
                    dp(5),
                )
            }
        addView(
            requireNotNull(
                turnText,
            ),
        )

        infoText =
            TextView(
                this@gameplayHud,
            ).apply {
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 11f else 12f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0,
                )
            }
        addView(
            requireNotNull(
                infoText,
            ),
        )
    }

internal fun OfflineGameActivity.offlineResultPanel():
    FrameLayout =
    FrameLayout(this).apply {
        val height =
            dp(
                if (isCompactSetup()) {
                    160
                } else {
                    180
                },
            )

        addView(
            GameResultArtView(
                this@offlineResultPanel,
            ).apply {
                mode =
                    GameResultArtView.Mode.OFFLINE
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                height,
            ),
        )

        val copy =
            LinearLayout(
                this@offlineResultPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactSetup()) 116 else 150),
                    dp(14),
                    dp(14),
                    dp(14),
                )

                addView(
                    TextView(
                        this@offlineResultPanel,
                    ).apply {
                        text =
                            "LOCAL GAME COMPLETE"
                        LudoProofTheme.body(
                            this,
                            10f,
                            bright = true,
                        )
                        setTextColor(
                            0xFF68E8FF.toInt(),
                        )
                    },
                )

                resultTitleText =
                    TextView(
                        this@offlineResultPanel,
                    ).apply {
                        text =
                            "WINNER"
                        LudoProofTheme.title(
                            this,
                            if (isCompactSetup()) 21f else 25f,
                            gold = true,
                        )
                        gravity =
                            Gravity.START or
                                Gravity.CENTER_VERTICAL
                        setPadding(
                            0,
                            dp(4),
                            0,
                            0,
                        )
                    }
                addView(
                    resultTitleText,
                )

                resultSubtitleText =
                    TextView(
                        this@offlineResultPanel,
                    ).apply {
                        text =
                            "Local v4 result • history and engine map remain available"
                        LudoProofTheme.body(
                            this,
                            if (isCompactSetup()) 10.5f else 11.5f,
                            bright = true,
                        )
                        gravity =
                            Gravity.START
                        setPadding(
                            0,
                            dp(5),
                            0,
                            0,
                        )
                    }
                addView(
                    resultSubtitleText,
                )
            }

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                height,
            ),
        )
    }

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
