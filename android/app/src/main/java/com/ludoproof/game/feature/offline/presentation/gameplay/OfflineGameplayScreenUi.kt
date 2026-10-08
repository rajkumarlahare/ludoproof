package com.ludoproof.game.ui.offline.gameplay

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.feature.offline.presentation.feedback.OfflineFeedbackAction
import com.ludoproof.game.feature.offline.presentation.feedback.OfflineLudoPawsFeedbackDispatcher
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
            dp(8)
        } else {
            dp(horizontalPaddingDp)
        }
    // BOARD WIDTH LOCK:
    // Use the available gameplay width; the 1dp side inset is the only intentional gap.
    // The board stage stays square, so increasing width increases height uniformly.
    val boardStageWidth =
        (contentWidth - dp(2))
            .coerceAtLeast(dp(1))

    val content =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            clipToPadding = false
            setPadding(
                0,
                dp(if (isCompactSetup()) 6 else 8),
                0,
                dp(if (isCompactSetup()) 8 else 10),
            )
        }

    host.addView(
        content,
        FrameLayout.LayoutParams(
            contentWidth,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        ),
    )

    content.addView(
        backHeader(null),
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            leftMargin = sectionSideMargin
            rightMargin = sectionSideMargin
        },
    )

    resultPanel =
        offlineResultPanel()
            .apply {
                visibility = View.GONE
            }
    content.addView(
        resultPanel,
        gameplaySectionParams(
            if (isCompactSetup()) 5 else 7,
        ).apply {
            leftMargin = sectionSideMargin
            rightMargin = sectionSideMargin
        },
    )

    boardView =
        LudoPawsReactiveBoardView(this).apply {
            elevation = dp(8).toFloat()
            onTokenSelected = {
                    tokenIndex ->
                val previous =
                    session.snapshot()
                runCatching {
                    session.move(
                        tokenIndex,
                    )
                }.onSuccess {
                        next ->
                    OfflineLudoPawsFeedbackDispatcher.committed(
                        context = this@showGame,
                        previous = previous,
                        current = next,
                        action = OfflineFeedbackAction.MOVE,
                    )
                    renderCommittedMove(
                        previous = previous,
                        current = next,
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

    topPlayerRail = playerRail()
    content.addView(
        requireNotNull(topPlayerRail),
        gameplaySectionParams(
            if (isCompactSetup()) 4 else 6,
        ),
    )

    // BOARD GEOMETRY LOCK:
    // Keep the approved board size independent from optional gameplay controls.
    // A weighted vertical slot would remeasure/shrink the square board whenever
    // quick chat or another future action adds height below it.
    val boardStage =
        FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            addView(
                requireNotNull(boardView),
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }
    content.addView(
        boardStage,
        LinearLayout.LayoutParams(
            boardStageWidth,
            boardStageWidth,
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(if (isCompactSetup()) 2 else 4)
        },
    )

    bottomPlayerRail = playerRail()
    content.addView(
        requireNotNull(bottomPlayerRail),
        gameplaySectionParams(
            if (isCompactSetup()) 2 else 4,
        ),
    )

    content.addView(
        gameplayActionPanel(),
        gameplaySectionParams(
            if (isCompactSetup()) 2 else 4,
        ).apply {
            leftMargin = sectionSideMargin
            rightMargin = sectionSideMargin
        },
    )

    setContentView(root)

    // CENTER LOCK:
    // Center the board in the real device viewport, not in the vertical flow between
    // the player rails. This preserves the existing rail/dice spacing around the board.
    host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        centerBoardInViewport()
    }
    boardView?.post {
        centerBoardInViewport()
        renderGame(state)
    }
}

internal fun OfflineGameActivity.centerBoardInViewport() {
    val boardStage =
        boardView?.parent as? FrameLayout
            ?: return
    val content =
        boardStage.parent as? LinearLayout
            ?: return
    val viewport =
        content.parent as? FrameLayout
            ?: return

    if (viewport.height <= 0 || boardStage.height <= 0) {
        return
    }

    val boardCenterY =
        boardStage.top + boardStage.height / 2f
    val viewportCenterY =
        viewport.height / 2f

    content.translationY =
        viewportCenterY - boardCenterY
}

internal fun OfflineGameActivity.gameplayHud(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        contentDescription =
            if (isComputerMode) {
                "VS COMPUTER"
            } else {
                "LOCAL MATCH"
            }
        setPadding(
            dp(if (isCompactSetup()) 10 else 14),
            dp(if (isCompactSetup()) 5 else 7),
            dp(if (isCompactSetup()) 10 else 14),
            dp(if (isCompactSetup()) 5 else 7),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    this@gameplayHud,
                    goldBorder = false,
                )
        elevation = dp(3).toFloat()

        turnText =
            TextView(
                this@gameplayHud,
            ).apply {
                LudoProofTheme.title(
                    this,
                    if (isCompactSetup()) 16f else 18f,
                    gold = true,
                )
                setPadding(
                    dp(3),
                    0,
                    dp(3),
                    0,
                )
            }
        addView(requireNotNull(turnText))

        infoText =
            TextView(
                this@gameplayHud,
            ).apply {
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 9.5f else 10.5f,
                    centered = true,
                    bright = true,
                )
                setTextColor(0xFFBFE8FF.toInt())
                setPadding(
                    dp(3),
                    dp(1),
                    dp(3),
                    0,
                )
            }
        addView(requireNotNull(infoText))
    }

internal fun OfflineGameActivity.offlineResultPanel(): FrameLayout =
    FrameLayout(this).apply {
        val height =
            dp(
                if (isCompactSetup()) {
                    148
                } else {
                    166
                },
            )

        addView(
            GameResultArtView(
                this@offlineResultPanel,
            ).apply {
                mode = GameResultArtView.Mode.OFFLINE
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
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactSetup()) 108 else 142),
                    dp(12),
                    dp(12),
                    dp(12),
                )

                addView(
                    TextView(
                        this@offlineResultPanel,
                    ).apply {
                        text =
                            if (isComputerMode) {
                                "MATCH COMPLETE"
                            } else {
                                "LOCAL MATCH COMPLETE"
                            }
                        LudoProofTheme.body(
                            this,
                            9.5f,
                            bright = true,
                        )
                        setTextColor(0xFF68E8FF.toInt())
                    },
                )

                resultTitleText =
                    TextView(
                        this@offlineResultPanel,
                    ).apply {
                        text = "WINNER"
                        LudoProofTheme.title(
                            this,
                            if (isCompactSetup()) 20f else 23f,
                            gold = true,
                        )
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        setPadding(0, dp(3), 0, 0)
                    }
                addView(resultTitleText)

                resultSubtitleText =
                    TextView(
                        this@offlineResultPanel,
                    ).apply {
                        text = "All paws are home"
                        LudoProofTheme.body(
                            this,
                            if (isCompactSetup()) 10f else 11f,
                            bright = true,
                        )
                        gravity = Gravity.START
                        setPadding(0, dp(3), 0, 0)
                    }
                addView(resultSubtitleText)
            }

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                height,
            ),
        )
    }
