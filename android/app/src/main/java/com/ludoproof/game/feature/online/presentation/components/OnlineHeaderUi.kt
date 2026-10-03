package com.ludoproof.game.ui.online

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import android.window.OnBackInvokedDispatcher
import org.json.JSONObject
import java.util.concurrent.Executors
import com.ludoproof.game.*
import com.ludoproof.game.feature.online.*

internal fun MainActivity.onlineTopBar():
    LinearLayout =
    LinearLayout(this).apply {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            onBackInvokedDispatcher
                .registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                ) {
                    abandonRemoteSessionState()
                    finish()
                }
        }

        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            Button(
                this@onlineTopBar,
            ).apply {
                LudoProofTheme
                    .homeCircularAction(
                        this,
                        "‹",
                    )
                contentDescription =
                    "Back"
                setOnClickListener {
                    abandonRemoteSessionState()
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                dp(50),
                dp(50),
            ),
        )

        addView(
            LinearLayout(
                this@onlineTopBar,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER
                setPadding(
                    dp(8),
                    0,
                    dp(8),
                    0,
                )

                addView(
                    TextView(
                        this@onlineTopBar,
                    ).apply {
                        text =
                            "ONLINE MATCH"
                        LudoProofTheme.title(
                            this,
                            if (isCompactOnline()) 17f else 19f,
                        )
                    },
                )

                connectionText =
                    TextView(
                        this@onlineTopBar,
                    ).apply {
                        text =
                            "● CHECKING"
                        LudoProofTheme.body(
                            this,
                            10.5f,
                            centered = true,
                            bright = true,
                        )
                        setPadding(
                            dp(10),
                            dp(3),
                            dp(10),
                            dp(3),
                        )
                        background =
                            LudoProofTheme
                                .rounded(
                                    0xCC071C49.toInt(),
                                    999f,
                                    0x6647D7FF,
                                    1f,
                                    this@onlineTopBar,
                                )
                        accessibilityLiveRegion =
                            View.ACCESSIBILITY_LIVE_REGION_POLITE
                    }
                addView(
                    connectionText,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        addView(
            Button(
                this@onlineTopBar,
            ).apply {
                LudoProofTheme
                    .homeCircularAction(
                        this,
                        "⚙",
                    )
                contentDescription =
                    "Settings"
                setOnClickListener {
                    ArcadeDialogs
                        .showSettings(
                            this@onlineTopBar,
                        )
                }
            },
            LinearLayout.LayoutParams(
                dp(50),
                dp(50),
            ),
        )
    }

internal fun MainActivity.onlineHero():
    FrameLayout =
    FrameLayout(this).apply {
        val heroHeight =
            dp(
                if (isCompactOnline()) {
                    160
                } else {
                    182
                },
            )

        addView(
            OnlineLobbyArtView(
                this@onlineHero,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )

        val copy =
            LinearLayout(
                this@onlineHero,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactOnline()) 104 else 146),
                    dp(14),
                    dp(if (isCompactOnline()) 12 else 24),
                    dp(14),
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            "VERIFIED ONLINE"
                        LudoProofTheme.title(
                            this,
                            if (isCompactOnline()) 21f else 27f,
                            gold = true,
                        )
                        gravity =
                            Gravity.START or
                                Gravity.CENTER_VERTICAL
                    },
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            "Create a room or join with a match code"
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 11f else 12.5f,
                            bright = true,
                        )
                        gravity =
                            Gravity.START
                        setPadding(
                            0,
                            dp(4),
                            0,
                            0,
                        )
                    },
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            "Remote EntroNex authority • exit abandons this match"
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 9.5f else 10.5f,
                        )
                        gravity =
                            Gravity.START
                        setPadding(
                            0,
                            dp(3),
                            0,
                            0,
                        )
                    },
                )
            }

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )
    }
