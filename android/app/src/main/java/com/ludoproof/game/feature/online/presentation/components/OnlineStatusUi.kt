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

internal fun MainActivity.onlineMatchStatusPanel():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(if (isCompactOnline()) 12 else 16),
            dp(12),
            dp(if (isCompactOnline()) 12 else 16),
            dp(12),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    this@onlineMatchStatusPanel,
                    goldBorder = true,
                )
        elevation =
            dp(6).toFloat()

        matchInfoText =
            TextView(
                this@onlineMatchStatusPanel,
            ).apply {
                text =
                    "No active match"
                LudoProofTheme.body(
                    this,
                    11f,
                    centered = true,
                    bright = true,
                )
                setTextColor(
                    0xFF6EE7FF.toInt(),
                )
            }
        addView(
            matchInfoText,
        )

        turnText =
            TextView(
                this@onlineMatchStatusPanel,
            ).apply {
                text =
                    "Waiting for a match."
                LudoProofTheme.title(
                    this,
                    if (isCompactOnline()) 20f else 23f,
                    gold = true,
                )
                setPadding(
                    dp(6),
                    dp(5),
                    dp(6),
                    dp(5),
                )
            }
        addView(
            turnText,
        )

        playersText =
            TextView(
                this@onlineMatchStatusPanel,
            ).apply {
                text =
                    "Players will appear here."
                LudoProofTheme.body(
                    this,
                    if (isCompactOnline()) 11f else 12f,
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
            playersText,
        )
    }

internal fun MainActivity.onlineResultPanel():
    FrameLayout =
    FrameLayout(this).apply {
        val height =
            dp(
                if (isCompactOnline()) {
                    164
                } else {
                    184
                },
            )

        addView(
            GameResultArtView(
                this@onlineResultPanel,
            ).apply {
                mode =
                    GameResultArtView.Mode.ONLINE
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                height,
            ),
        )

        val copy =
            LinearLayout(
                this@onlineResultPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactOnline()) 118 else 154),
                    dp(14),
                    dp(14),
                    dp(14),
                )

                addView(
                    TextView(
                        this@onlineResultPanel,
                    ).apply {
                        text =
                            "MATCH COMPLETE"
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
                        this@onlineResultPanel,
                    ).apply {
                        text =
                            "WINNER"
                        LudoProofTheme.title(
                            this,
                            if (isCompactOnline()) 21f else 25f,
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
                        this@onlineResultPanel,
                    ).apply {
                        text =
                            "Server-authoritative result • verified history available"
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 10.5f else 11.5f,
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
