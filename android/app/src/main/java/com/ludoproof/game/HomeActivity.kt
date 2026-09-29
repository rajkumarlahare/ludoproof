package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

class HomeActivity : Activity() {
    private lateinit var connectivityText:
        TextView
    private lateinit var connectivityMonitor:
        ConnectivityMonitor

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme.configureWindow(
            this,
        )

        val (
            root,
            host,
        ) =
            LudoProofTheme.arcadeRoot(
                this,
            )

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }
        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(22),
                )
            }
        scroll.addView(content)
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams
                    .MATCH_PARENT,
                FrameLayout.LayoutParams
                    .MATCH_PARENT,
            ),
        )

        content.addView(
            profileHud(),
        )
        content.addView(
            quickActions(),
        )

        val spacer =
            View(this)
        content.addView(
            spacer,
            LinearLayout.LayoutParams(
                1,
                dp(120),
            ),
        )

        val modeRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }
        modeRow.addView(
            modeCard(
                mode =
                    ModeArtView
                        .Mode
                        .ONLINE,
                title =
                    "Online",
                footerPositive =
                    false,
            ) {
                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java,
                    ),
                )
            },
            LinearLayout.LayoutParams(
                0,
                dp(245),
                1f,
            ).apply {
                marginEnd =
                    dp(8)
            },
        )
        modeRow.addView(
            modeCard(
                mode =
                    ModeArtView
                        .Mode
                        .LOCAL,
                title =
                    "Local",
                footerPositive =
                    true,
            ) {
                startActivity(
                    Intent(
                        this,
                        OfflineGameActivity::class.java,
                    ),
                )
            },
            LinearLayout.LayoutParams(
                0,
                dp(245),
                1f,
            ).apply {
                marginStart =
                    dp(8)
            },
        )
        content.addView(modeRow)

        val bonus =
            Button(this).apply {
                text =
                    "★  FAIR PLAY • VERIFIED ONLINE  ★"
                LudoProofTheme.positive(
                    this,
                )
                isEnabled =
                    false
            }
        content.addView(
            bonus,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                dp(58),
            ).apply {
                setMargins(
                    dp(58),
                    dp(20),
                    dp(58),
                    0,
                )
            },
        )

        continueButton()
            ?.let {
                button ->
                content.addView(
                    button,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                            .MATCH_PARENT,
                        dp(64),
                    ).apply {
                        setMargins(
                            dp(66),
                            dp(24),
                            dp(66),
                            0,
                        )
                    },
                )
            }

        val flex =
            View(this)
        content.addView(
            flex,
            LinearLayout.LayoutParams(
                1,
                dp(150),
            ),
        )

        content.addView(
            bottomActions(),
        )

        setContentView(root)

        connectivityMonitor =
            ConnectivityMonitor(
                this,
            ) {
                    online ->
                runOnUiThread {
                    connectivityText.text =
                        if (
                            online
                        ) {
                            "● ONLINE"
                        } else {
                            "● OFFLINE"
                        }
                    connectivityText
                        .setTextColor(
                            if (
                                online
                            ) {
                                0xFF68F053
                                    .toInt()
                            } else {
                                LudoProofTheme
                                    .GOLD
                            },
                        )
                }
            }
    }

    override fun onStart() {
        super.onStart()
        connectivityMonitor
            .start()
    }

    override fun onStop() {
        connectivityMonitor
            .stop()
        super.onStop()
    }

    private fun profileHud():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL

            val avatar =
                TextView(
                    this@HomeActivity,
                ).apply {
                    text = "LP"
                    LudoProofTheme.title(
                        this,
                        15f,
                    )
                    gravity =
                        Gravity.CENTER
                    background =
                        LudoProofTheme
                            .rounded(
                                0xFF0879D9
                                    .toInt(),
                                12f,
                                LudoProofTheme
                                    .CYAN_BORDER,
                                2f,
                                this@HomeActivity,
                            )
                }
            addView(
                avatar,
                LinearLayout.LayoutParams(
                    dp(62),
                    dp(62),
                ),
            )

            val identity =
                LinearLayout(
                    this@HomeActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    setPadding(
                        dp(12),
                        0,
                        0,
                        0,
                    )
                }

            identity.addView(
                TextView(
                    this@HomeActivity,
                ).apply {
                    text =
                        "LudoProof Player"
                    LudoProofTheme.body(
                        this,
                        19f,
                        bright = true,
                    )
                },
            )

            val level =
                LinearLayout(
                    this@HomeActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }
            level.addView(
                TextView(
                    this@HomeActivity,
                ).apply {
                    text = "★"
                    setTextColor(
                        LudoProofTheme
                            .GOLD,
                    )
                    textSize = 22f
                },
            )
            level.addView(
                ProgressBar(
                    this@HomeActivity,
                    null,
                    android.R.attr
                        .progressBarStyleHorizontal,
                ).apply {
                    max = 100
                    progress = 72
                    progressTintList =
                        android.content.res
                            .ColorStateList
                            .valueOf(
                                LudoProofTheme
                                    .GOLD,
                            )
                    progressBackgroundTintList =
                        android.content.res
                            .ColorStateList
                            .valueOf(
                                0xFF123875
                                    .toInt(),
                            )
                },
                LinearLayout.LayoutParams(
                    dp(108),
                    dp(12),
                ).apply {
                    marginStart =
                        dp(4)
                },
            )
            identity.addView(level)
            addView(
                identity,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                    1f,
                ),
            )

            connectivityText =
                TextView(
                    this@HomeActivity,
                ).apply {
                    text =
                        "● CHECKING"
                    LudoProofTheme.body(
                        this,
                        12f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(13),
                        dp(9),
                        dp(13),
                        dp(9),
                    )
                    background =
                        LudoProofTheme
                            .rounded(
                                0xE4071739
                                    .toInt(),
                                18f,
                                0x553F86FF,
                                1f,
                                this@HomeActivity,
                            )
                }
            addView(
                connectivityText,
            )
        }

    private fun quickActions():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER
            setPadding(
                0,
                dp(32),
                0,
                0,
            )

            addView(
                quickAction(
                    "★",
                    "RULES",
                ) {
                    ArcadeDialogs
                        .showProofHistory(
                            this@HomeActivity,
                            "CLASSIC RULES",
                            """
                            • Roll 6 to leave the yard.
                            • Exact roll is required to reach home.
                            • Rolling 6 gives another turn.
                            • Capturing gives another turn.
                            • Three consecutive sixes forfeit the turn.
                            • Safe cells cannot be captured.
                            """.trimIndent(),
                        )
                },
                weighted(),
            )
            addView(
                quickAction(
                    "✓",
                    "PROOFS",
                ) {
                    ArcadeDialogs
                        .showProofHistory(
                            this@HomeActivity,
                            "PROOF MODE",
                            "Online matches use server-authoritative EntroNex v4 commitments and attestations.\n\nOffline matches use the same v4 HKDF, rejection sampling and Natural World derivation locally, then recompute the local proof. Offline has no remote EntroNex attestation.",
                        )
                },
                weighted(),
            )
            addView(
                quickAction(
                    "↗",
                    "SHARE",
                ) {
                    val share =
                        Intent(
                            Intent.ACTION_SEND,
                        ).apply {
                            type =
                                "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Play LudoProof — verified online Ludo with an offline local mode.",
                            )
                        }
                    startActivity(
                        Intent
                            .createChooser(
                                share,
                                "Share LudoProof",
                            ),
                    )
                },
                weighted(),
            )
            addView(
                quickAction(
                    "⚙",
                    "SETTINGS",
                ) {
                    ArcadeDialogs
                        .showSettings(
                            this@HomeActivity,
                        )
                },
                weighted(),
            )
        }

    private fun quickAction(
        symbol: String,
        label: String,
        action: () -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER

            addView(
                Button(
                    this@HomeActivity,
                ).apply {
                    LudoProofTheme
                        .circularAction(
                            this,
                            symbol,
                        )
                    setOnClickListener {
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(56),
                    dp(56),
                ),
            )
            addView(
                TextView(
                    this@HomeActivity,
                ).apply {
                    text = label
                    LudoProofTheme.body(
                        this,
                        11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        0,
                        dp(4),
                        0,
                        0,
                    )
                },
            )
        }

    private fun modeCard(
        mode: ModeArtView.Mode,
        title: String,
        footerPositive: Boolean,
        action: () -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5),
            )
            background =
                LudoProofTheme
                    .panelDrawable(
                        this@HomeActivity,
                    )
            elevation =
                dp(8)
                    .toFloat()

            val art =
                ModeArtView(
                    this@HomeActivity,
                ).apply {
                    this.mode =
                        mode
                }
            addView(
                art,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    0,
                    1f,
                ),
            )

            addView(
                Button(
                    this@HomeActivity,
                ).apply {
                    text = title
                    if (
                        footerPositive
                    ) {
                        LudoProofTheme
                            .primary(
                                this,
                            )
                    } else {
                        LudoProofTheme
                            .positive(
                                this,
                            )
                    }
                    textSize = 22f
                    setOnClickListener {
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    dp(66),
                ),
            )
        }

    private fun continueButton():
        Button? {
        val online =
            SecureSessionStore(
                this,
            ).load()
        val offline =
            OfflineGameEngine(
                this,
            ).hasSavedGame()

        if (
            online == null &&
            !offline
        ) {
            return null
        }

        return Button(this).apply {
            text =
                "Continue Last Game  ›"
            LudoProofTheme.positive(
                this,
            )
            textSize = 20f
            setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        if (
                            online != null
                        ) {
                            MainActivity::class.java
                        } else {
                            OfflineGameActivity::class.java
                        },
                    ),
                )
            }
        }
    }

    private fun bottomActions():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER

            addView(
                bottomCircle(
                    "★",
                ) {
                    ArcadeDialogs
                        .showProofHistory(
                            this@HomeActivity,
                            "FAIR PLAY",
                            "LudoProof uses the same v4 derivation math online and offline. Online adds remote EntroNex authority; offline is locally reproducible only.",
                        )
                },
                weighted(),
            )
            addView(
                bottomCircle(
                    "↗",
                ) {
                    val share =
                        Intent(
                            Intent.ACTION_SEND,
                        ).apply {
                            type =
                                "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "LudoProof",
                            )
                        }
                    startActivity(
                        Intent
                            .createChooser(
                                share,
                                "Share",
                            ),
                    )
                },
                weighted(),
            )
            addView(
                bottomCircle(
                    "⚙",
                ) {
                    ArcadeDialogs
                        .showSettings(
                            this@HomeActivity,
                        )
                },
                weighted(),
            )
        }

    private fun bottomCircle(
        symbol: String,
        action: () -> Unit,
    ): Button =
        Button(this).apply {
            LudoProofTheme
                .circularAction(
                    this,
                    symbol,
                )
            setOnClickListener {
                action()
            }
            layoutParams =
                LinearLayout.LayoutParams(
                    dp(60),
                    dp(60),
                )
        }

    private fun weighted():
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams
                .WRAP_CONTENT,
            1f,
        )

    private fun dp(
        value: Int,
    ): Int =
        LudoProofTheme.dp(
            this,
            value,
        )
}
