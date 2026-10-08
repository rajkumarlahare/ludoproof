package com.ludoproof.game.ui.online

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.feature.online.*

internal fun MainActivity.onlineActionPanel(): LinearLayout =
    LudoProofTheme
        .panel(this)
        .apply {
            setPadding(
                dp(if (isCompactOnline()) 8 else 10),
                dp(if (isCompactOnline()) 7 else 9),
                dp(if (isCompactOnline()) 8 else 10),
                dp(if (isCompactOnline()) 7 else 9),
            )

            verificationPanel =
                LinearLayout(
                    this@onlineActionPanel,
                ).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(
                        dp(if (isCompactOnline()) 6 else 8),
                        dp(5),
                        dp(if (isCompactOnline()) 6 else 8),
                        dp(5),
                    )
                    background =
                        LudoProofTheme.hudPanelDrawable(
                            this@onlineActionPanel,
                            goldBorder = false,
                        )

                    val diceDock =
                        FrameLayout(
                            this@onlineActionPanel,
                        ).apply {
                            background = null
                            elevation = 0f
                            clipChildren = false
                            clipToPadding = false
                        }

                    diceView = DiceView(this@onlineActionPanel)
                    diceDock.addView(
                        diceView,
                        FrameLayout.LayoutParams(
                            dp(if (isCompactOnline()) 66 else 74),
                            dp(if (isCompactOnline()) 66 else 74),
                            Gravity.CENTER,
                        ),
                    )

                    addView(
                        diceDock,
                        LinearLayout.LayoutParams(
                            dp(if (isCompactOnline()) 76 else 84),
                            dp(if (isCompactOnline()) 76 else 84),
                        ),
                    )

                    val proofColumn =
                        LinearLayout(
                            this@onlineActionPanel,
                        ).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(dp(8), 0, 0, 0)

                            addView(
                                TextView(
                                    this@onlineActionPanel,
                                ).apply {
                                    text = "VERIFIED DICE"
                                    LudoProofTheme.body(
                                        this,
                                        9.5f,
                                        bright = true,
                                    )
                                    setTextColor(0xFF68F053.toInt())
                                },
                            )

                            verificationText =
                                infoText(
                                    "No verified roll yet.",
                                    if (isCompactOnline()) 10f else 11f,
                                ).apply {
                                    gravity = Gravity.START
                                    setPadding(0, dp(2), 0, 0)
                                    maxLines = 3
                                }
                            addView(verificationText)
                        }

                    addView(
                        proofColumn,
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        ),
                    )
                }

            addView(verificationPanel)

            rollButton =
                button(
                    "ROLL VERIFIED DICE",
                ) {
                    rollVerifiedDice()
                }.apply {
                    textSize = if (isCompactOnline()) 15f else 17f
                }
            LudoProofTheme.primary(rollButton)
            addView(
                rollButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(if (isCompactOnline()) 48 else 52),
                ).apply {
                    topMargin = dp(6)
                },
            )

            startButton =
                button(
                    "START MATCH",
                ) {
                    withSession {
                            code,
                            token,
                        ->
                        runNetwork(
                            action = {
                                api.start(
                                    code,
                                    token,
                                )
                            },
                        )
                    }
                }.apply {
                    textSize = if (isCompactOnline()) 10.5f else 11.5f
                }
            LudoProofTheme.positive(startButton)

            refreshButton =
                button(
                    "SYNC",
                ) {
                    refreshState()
                }.apply {
                    textSize = if (isCompactOnline()) 10.5f else 11.5f
                }

            addView(
                responsiveControlRow(
                    startButton,
                    refreshButton,
                ),
            )

            shareButton =
                button(
                    "SHARE",
                ) {
                    shareMatch()
                }.apply {
                    textSize = if (isCompactOnline()) 10.5f else 11.5f
                }
            proofButton =
                button(
                    "VERIFIED HISTORY",
                ) {
                    toggleProofDetails()
                }.apply {
                    textSize = if (isCompactOnline()) 10f else 11f
                }
            addView(
                responsiveControlRow(
                    shareButton,
                    proofButton,
                ),
            )

            proofDetailsText =
                TextView(
                    this@onlineActionPanel,
                ).apply {
                    visibility = View.GONE
                }

            statusText =
                TextView(
                    this@onlineActionPanel,
                ).apply {
                    text = "Create or join a match to begin."
                    LudoProofTheme.body(
                        this,
                        if (isCompactOnline()) 9.5f else 10.5f,
                        centered = true,
                    )
                    setPadding(dp(6), dp(5), dp(6), 0)
                    maxLines = 2
                }
        }

internal fun MainActivity.responsiveControlRow(
    vararg views: View,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER

        for (view in views) {
            addView(
                view,
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactOnline()) 40 else 44),
                    1f,
                ).apply {
                    setMargins(
                        dp(3),
                        dp(5),
                        dp(3),
                        0,
                    )
                },
            )
        }
    }

internal fun MainActivity.onlineSectionParams(
    topMarginDp: Int,
): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin = dp(topMarginDp)
    }

internal fun MainActivity.isCompactOnline(): Boolean =
    LudoProofTheme.isCompactWidth(this)
