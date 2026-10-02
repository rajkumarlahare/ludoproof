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
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*

internal fun OfflineGameActivity.showGame(snapshot: MatchSnapshot?) {
    val state =
        snapshot ?: run {
            showSetup()
            return
        }

    prepareOfflineUiTransition()

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
    val isExpandedWidth =
        LudoProofTheme
            .isExpandedWidth(this)
    val contentWidth =
        if (isExpandedWidth) {
            minOf(
                resources.displayMetrics.widthPixels,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(this),
                ),
            )
        } else {
            resources.displayMetrics.widthPixels
        }
    val sectionSideMargin =
        if (isExpandedWidth) {
            0
        } else {
            dp(horizontalPaddingDp)
        }

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
            if (
                isComputerMode
            ) {
                "COMPUTER • CLASSIC"
            } else {
                "LOCAL • CLASSIC"
            },
        ),
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            leftMargin =
                sectionSideMargin
            rightMargin =
                sectionSideMargin
        },
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
        ).apply {
            leftMargin =
                sectionSideMargin
            rightMargin =
                sectionSideMargin
        },
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
                    GameSoundFeedback.move(
                        this@showGame,
                    )
                    renderGame(next)
                }.onFailure {
                        error ->
                    showStatus(
                        error.message
                            ?: "Move failed",
                    )
                }
            }
        }

    topPlayerRail =
        playerRail()
    content.addView(
        requireNotNull(
            topPlayerRail,
        ),
        gameplaySectionParams(
            if (isCompactSetup()) 10 else 12,
        ).apply {
            leftMargin =
                sectionSideMargin
            rightMargin =
                sectionSideMargin
        },
    )

    content.addView(
        boardView,
        gameplaySectionParams(
            if (isCompactSetup()) 6 else 8,
        ),
    )

    bottomPlayerRail =
        playerRail()
    content.addView(
        requireNotNull(
            bottomPlayerRail,
        ),
        gameplaySectionParams(
            if (isCompactSetup()) 6 else 8,
        ).apply {
            leftMargin =
                sectionSideMargin
            rightMargin =
                sectionSideMargin
        },
    )

    content.addView(
        gameplayActionPanel(),
        gameplaySectionParams(
            if (isCompactSetup()) 4 else 6,
        ).apply {
            leftMargin =
                sectionSideMargin
            rightMargin =
                sectionSideMargin
        },
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
                            if (
                                isComputerMode
                            ) {
                                "COMPUTER GAME COMPLETE"
                            } else {
                                "LOCAL GAME COMPLETE"
                            }
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
