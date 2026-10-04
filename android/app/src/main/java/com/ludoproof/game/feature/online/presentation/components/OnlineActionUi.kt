package com.ludoproof.game.ui.online

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
import com.ludoproof.game.feature.online.*

internal fun MainActivity.onlineActionPanel():
    LinearLayout =
    LudoProofTheme
        .panel(this)
        .apply {
            setPadding(
                dp(if (isCompactOnline()) 12 else 14),
                dp(if (isCompactOnline()) 12 else 14),
                dp(if (isCompactOnline()) 12 else 14),
                dp(if (isCompactOnline()) 12 else 14),
            )

            verificationPanel =
                LinearLayout(
                    this@onlineActionPanel,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                    setPadding(
                        dp(if (isCompactOnline()) 10 else 12),
                        dp(10),
                        dp(if (isCompactOnline()) 10 else 12),
                        dp(10),
                    )
                    background =
                        LudoProofTheme
                            .hudPanelDrawable(
                                this@onlineActionPanel,
                                goldBorder = true,
                            )

                    val diceDock =
                        FrameLayout(
                            this@onlineActionPanel,
                        ).apply {
                            // The dice owns its visual surface. Keep this host neutral
                            // so remote modes cannot reintroduce the old navy/cyan skin.
                            background = null
                            elevation = 0f
                            clipChildren = false
                            clipToPadding = false
                        }

                    diceView =
                        DiceView(
                            this@onlineActionPanel,
                        )
                    diceDock.addView(
                        diceView,
                        FrameLayout.LayoutParams(
                            dp(
                                if (isCompactOnline()) 88 else 100,
                            ),
                            dp(
                                if (isCompactOnline()) 88 else 100,
                            ),
                            Gravity.CENTER,
                        ),
                    )

                    addView(
                        diceDock,
                        LinearLayout.LayoutParams(
                            dp(
                                if (isCompactOnline()) 104 else 116,
                            ),
                            dp(
                                if (isCompactOnline()) 104 else 116,
                            ),
                        ),
                    )

                    val proofColumn =
                        LinearLayout(
                            this@onlineActionPanel,
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
                                    this@onlineActionPanel,
                                ).apply {
                                    text =
                                        "REMOTE AUTHORITY"
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
                                                this@onlineActionPanel,
                                            )
                                },
                            )

                            addView(
                                TextView(
                                    this@onlineActionPanel,
                                ).apply {
                                    text =
                                        "ENTRONEX V4 VERIFIED DICE"
                                    LudoProofTheme.body(
                                        this,
                                        if (isCompactOnline()) 11.5f else 12.5f,
                                        bright = true,
                                    )
                                    setTextColor(
                                        LudoProofTheme.GOLD,
                                    )
                                    setPadding(
                                        0,
                                        dp(7),
                                        0,
                                        0,
                                    )
                                },
                            )

                            verificationText =
                                infoText(
                                    "No verified roll yet.",
                                    if (isCompactOnline()) 11f else 12f,
                                ).apply {
                                    gravity =
                                        Gravity.START
                                    setPadding(
                                        0,
                                        dp(4),
                                        0,
                                        0,
                                    )
                                }
                            addView(
                                verificationText,
                            )
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

            addView(
                verificationPanel,
            )

            rollButton =
                button(
                    "ROLL VERIFIED DICE",
                ) {
                    rollVerifiedDice()
                }.apply {
                    textSize =
                        if (isCompactOnline()) 18f else 20f
                }
            LudoProofTheme
                .primary(
                    rollButton,
                )
            addView(
                rollButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(
                        if (isCompactOnline()) 58 else 62,
                    ),
                ).apply {
                    topMargin =
                        dp(12)
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
                }
            LudoProofTheme
                .positive(
                    startButton,
                )

            refreshButton =
                button(
                    "SYNC STATE",
                ) {
                    refreshState()
                }

            addView(
                responsiveControlRow(
                    startButton,
                    refreshButton,
                ),
            )

            shareButton =
                button(
                    "SHARE CODE",
                ) {
                    shareMatch()
                }
            proofButton =
                button(
                    "VERIFIED HISTORY",
                ) {
                    toggleProofDetails()
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
                    visibility =
                        View.GONE
                }

            statusText =
                TextView(
                    this@onlineActionPanel,
                ).apply {
                    text =
                        "Create or join a match to begin."
                    LudoProofTheme.body(
                        this,
                        if (isCompactOnline()) 11f else 11.5f,
                        centered = true,
                    )
                    setPadding(
                        dp(8),
                        dp(11),
                        dp(8),
                        0,
                    )
                }
            addView(
                statusText,
            )

            addView(
                TextView(
                    this@onlineActionPanel,
                ).apply {
                    text =
                        "SERVER-AUTHORITATIVE ONLINE • COMMITTED ROLLS • RESUMABLE REVEAL"
                    LudoProofTheme.body(
                        this,
                        9.5f,
                        centered = true,
                    )
                    setPadding(
                        dp(4),
                        dp(10),
                        dp(4),
                        0,
                    )
                },
            )
        }

internal fun MainActivity.responsiveControlRow(
    vararg views: View,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            if (isCompactOnline()) {
                LinearLayout.VERTICAL
            } else {
                LinearLayout.HORIZONTAL
            }

        for (
            view in
            views
        ) {
            addView(
                view,
                if (isCompactOnline()) {
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54),
                    ).apply {
                        topMargin =
                            dp(9)
                    }
                } else {
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(10),
                            dp(4),
                            0,
                        )
                    }
                },
            )
        }
    }

internal fun MainActivity.onlineSectionParams(
    topMarginDp: Int,
):
    LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(
                topMarginDp,
            )
    }

internal fun MainActivity.isCompactOnline():
    Boolean =
    LudoProofTheme
        .isCompactWidth(this)
