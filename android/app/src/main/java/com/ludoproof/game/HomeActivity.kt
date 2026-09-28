package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class HomeActivity : Activity() {
    private lateinit var connectivityText: TextView
    private lateinit var connectivityMonitor:
        ConnectivityMonitor

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        val content =
            LudoProofTheme.screen(this)

        val top =
            TextView(this).apply {
                text =
                    "LUDOPROOF  •  FAIR PLAY"
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                )
                setPadding(
                    0,
                    4,
                    0,
                    12,
                )
            }
        content.addView(top)

        val title =
            TextView(this).apply {
                text = "LudoProof"
                LudoProofTheme.title(
                    this,
                    34f,
                )
            }
        content.addView(title)

        val subtitle =
            TextView(this).apply {
                text =
                    "One board. Two modes. Verified online play and comfortable offline local play."
                LudoProofTheme.body(
                    this,
                    15f,
                    centered = true,
                )
                setPadding(
                    12,
                    6,
                    12,
                    14,
                )
            }
        content.addView(subtitle)

        val dice =
            DiceView(this).apply {
                showOutcome(6)
            }
        content.addView(
            dice,
            LinearLayout.LayoutParams(
                LudoProofTheme.dp(
                    this,
                    112,
                ),
                LudoProofTheme.dp(
                    this,
                    112,
                ),
            ).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
            },
        )

        connectivityText =
            TextView(this).apply {
                text =
                    "Checking connection…"
                LudoProofTheme.body(
                    this,
                    13f,
                    centered = true,
                )
                setPadding(
                    0,
                    8,
                    0,
                    12,
                )
            }
        content.addView(
            connectivityText,
        )

        content.addView(
            modeCard(
                title =
                    "ONLINE MATCH",
                detail =
                    "Server-authoritative multiplayer with locally verified EntroNex v4 proofs.",
                positive = false,
            ) {
                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java,
                    ),
                )
            },
        )

        val onlineSession =
            SecureSessionStore(
                this,
            ).load()
        if (onlineSession != null) {
            content.addView(
                compactButton(
                    "Continue Online Match",
                    positive = false,
                ) {
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java,
                        ),
                    )
                },
            )
        }

        content.addView(
            modeCard(
                title =
                    "OFFLINE LOCAL",
                detail =
                    "2–4 players on one phone. Works without internet. Offline rolls are local and are not EntroNex proofs.",
                positive = true,
            ) {
                startActivity(
                    Intent(
                        this,
                        OfflineGameActivity::class.java,
                    ),
                )
            },
        )

        if (
            OfflineGameEngine(
                this,
            ).hasSavedGame()
        ) {
            content.addView(
                compactButton(
                    "Continue Offline Game",
                    positive = true,
                ) {
                    startActivity(
                        Intent(
                            this,
                            OfflineGameActivity::class.java,
                        ),
                    )
                },
            )
        }

        val footer =
            TextView(this).apply {
                text =
                    "Online and offline use the same board, controls and visual language. Trust labels stay mode-specific."
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                )
                setPadding(
                    12,
                    20,
                    12,
                    4,
                )
            }
        content.addView(footer)

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    LudoProofTheme.BLUE_DARK,
                )
                addView(content)
            },
        )

        connectivityMonitor =
            ConnectivityMonitor(
                this,
            ) { online ->
                runOnUiThread {
                    connectivityText.text =
                        if (online) {
                            "● Online — verified multiplayer available"
                        } else {
                            "● Offline — local play available"
                        }
                    connectivityText.setTextColor(
                        if (online) {
                            LudoProofTheme.GREEN
                        } else {
                            LudoProofTheme.ORANGE
                        },
                    )
                }
            }
    }

    override fun onStart() {
        super.onStart()
        connectivityMonitor.start()
    }

    override fun onStop() {
        connectivityMonitor.stop()
        super.onStop()
    }

    private fun modeCard(
        title: String,
        detail: String,
        positive: Boolean,
        action: () -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                LudoProofTheme.dp(
                    this@HomeActivity,
                    14,
                ),
                LudoProofTheme.dp(
                    this@HomeActivity,
                    14,
                ),
                LudoProofTheme.dp(
                    this@HomeActivity,
                    14,
                ),
                LudoProofTheme.dp(
                    this@HomeActivity,
                    14,
                ),
            )
            LudoProofTheme.card(
                this,
                alternate =
                    positive,
            )

            addView(
                TextView(
                    this@HomeActivity,
                ).apply {
                    text = title
                    setTextColor(
                        LudoProofTheme.WHITE,
                    )
                    textSize = 19f
                    gravity =
                        Gravity.CENTER_HORIZONTAL
                },
            )
            addView(
                TextView(
                    this@HomeActivity,
                ).apply {
                    text = detail
                    LudoProofTheme.body(
                        this,
                        13f,
                        centered = true,
                    )
                    setPadding(
                        4,
                        6,
                        4,
                        10,
                    )
                },
            )
            addView(
                Button(
                    this@HomeActivity,
                ).apply {
                    text =
                        if (positive) {
                            "PLAY OFFLINE"
                        } else {
                            "PLAY ONLINE"
                        }
                    if (positive) {
                        LudoProofTheme.positive(
                            this,
                        )
                    } else {
                        LudoProofTheme.primary(
                            this,
                        )
                    }
                    setOnClickListener {
                        action()
                    }
                },
            )
        }.also {
            it.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                ).apply {
                    setMargins(
                        0,
                        LudoProofTheme.dp(
                            this@HomeActivity,
                            8,
                        ),
                        0,
                        LudoProofTheme.dp(
                            this@HomeActivity,
                            8,
                        ),
                    )
                }
        }

    private fun compactButton(
        label: String,
        positive: Boolean,
        action: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            if (positive) {
                LudoProofTheme.positive(
                    this,
                )
            } else {
                LudoProofTheme.secondary(
                    this,
                )
            }
            setOnClickListener {
                action()
            }
        }
}
