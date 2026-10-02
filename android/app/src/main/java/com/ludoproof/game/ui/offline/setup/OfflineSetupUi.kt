package com.ludoproof.game.ui.offline.setup

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
import com.ludoproof.game.ui.offline.gameplay.*

internal fun OfflineGameActivity.showSetup() {
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
            "LOCAL PLAY",
        ),
    )

    content.addView(
        setupHero(),
        setupSectionParams(
            if (isCompactSetup()) 14 else 18,
        ),
    )

    content.addView(
        gameTypePanel(),
        setupSectionParams(
            if (isCompactSetup()) 14 else 18,
        ),
    )

    content.addView(
        playersPanel(),
        setupSectionParams(
            if (isCompactSetup()) 14 else 18,
        ),
    )

    content.addView(
        colorPanel(),
        setupSectionParams(
            if (isCompactSetup()) 14 else 18,
        ),
    )

    refreshSetupSelections()

    content.addView(
        localTrustStrip(),
        setupSectionParams(
            if (isCompactSetup()) 14 else 18,
        ),
    )

    content.addView(
        Button(this).apply {
            text =
                "START LOCAL GAME  ›"
            textSize =
                if (isCompactSetup()) 18f else 20f
            LudoProofTheme
                .primary(this)
            setOnClickListener {
                showGame(
                    engine.start(
                        selectedPlayers,
                        selectedColor,
                    ),
                )
            }
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(
                if (isCompactSetup()) 60 else 64,
            ),
        ).apply {
            setMargins(
                if (isCompactSetup()) dp(10) else dp(42),
                dp(20),
                if (isCompactSetup()) dp(10) else dp(42),
                0,
            )
        },
    )

    setContentView(root)
}

internal fun OfflineGameActivity.setupHero():
    FrameLayout =
    FrameLayout(this).apply {
        val heroHeight =
            dp(
                if (isCompactSetup()) {
                    162
                } else {
                    184
                },
            )

        addView(
            OfflineSetupArtView(
                this@setupHero,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )

        val copy =
            LinearLayout(
                this@setupHero,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactSetup()) 98 else 138),
                    dp(14),
                    dp(if (isCompactSetup()) 12 else 26),
                    dp(14),
                )
            }

        copy.addView(
            TextView(this@setupHero).apply {
                text =
                    "LOCAL LUDO"
                LudoProofTheme.title(
                    this,
                    if (isCompactSetup()) 23f else 28f,
                    gold = true,
                )
                gravity =
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
            },
        )

        copy.addView(
            TextView(this@setupHero).apply {
                text =
                    "Pass & play on one device"
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 12f else 13f,
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

        copy.addView(
            TextView(this@setupHero).apply {
                text =
                    "Same v4 derivation • local recomputation"
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 10.5f else 11.5f,
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

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )
    }

internal fun OfflineGameActivity.gameTypePanel():
    LinearLayout =
    selectionPanel(
        "SELECT GAME",
        "Choose the local ruleset",
    ).apply {
        val row =
            LinearLayout(
                this@gameTypePanel,
            ).apply {
                orientation =
                    if (isCompactSetup()) {
                        LinearLayout.VERTICAL
                    } else {
                        LinearLayout.HORIZONTAL
                    }
                gravity =
                    Gravity.CENTER
            }

        val classic =
            choiceTile(
                "▦",
                "CLASSIC",
                "Standard Ludo rules",
                true,
                true,
            ) {}
        val rush =
            choiceTile(
                "⚡",
                "RUSH",
                "Coming soon",
                false,
                false,
            ) {}

        if (isCompactSetup()) {
            row.addView(
                classic,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(104),
                ),
            )
            row.addView(
                rush,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(92),
                ).apply {
                    topMargin =
                        dp(10)
                },
            )
        } else {
            row.addView(
                classic,
                setupTileParams(
                    112,
                ),
            )
            row.addView(
                rush,
                setupTileParams(
                    112,
                ),
            )
        }
        addView(row)

        addView(
            LinearLayout(
                this@gameTypePanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(14),
                    dp(11),
                    dp(14),
                    dp(11),
                )
                background =
                    LudoProofTheme
                        .hudPanelDrawable(
                            this@gameTypePanel,
                            goldBorder = true,
                        )

                addView(
                    TextView(
                        this@gameTypePanel,
                    ).apply {
                        text =
                            "BOARD THEME"
                        LudoProofTheme.body(
                            this,
                            11f,
                            bright = true,
                        )
                    },
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )

                addView(
                    TextView(
                        this@gameTypePanel,
                    ).apply {
                        text =
                            "CLASSIC"
                        LudoProofTheme.body(
                            this,
                            13f,
                            bright = true,
                        )
                        setTextColor(
                            LudoProofTheme.GOLD,
                        )
                    },
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(12)
            },
        )
    }

internal fun OfflineGameActivity.playersPanel():
    LinearLayout =
    selectionPanel(
        "SELECT PLAYERS",
        "2–4 players on this device",
    ).apply {
        val playersRow =
            LinearLayout(
                this@playersPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }
        val playerMap =
            linkedMapOf<Int, Button>()

        listOf(
            2 to "♟♟",
            3 to "♟♟♟",
            4 to "♟♟♟♟",
        ).forEach {
                entry ->
            val button =
                tileButton(
                    entry.second +
                        "\n" +
                        entry.first +
                        " PLAYERS",
                ) {
                    selectedPlayers =
                        entry.first
                    refreshSetupSelections()
                }
            playerMap[entry.first] =
                button
            playersRow.addView(
                button,
                setupTileParams(
                    if (isCompactSetup()) 98 else 106,
                ),
            )
        }

        playerButtons =
            playerMap
        addView(playersRow)
    }

internal fun OfflineGameActivity.colorPanel():
    LinearLayout =
    selectionPanel(
        "CHOOSE YOUR COLOR",
        "Your seat starts with this color",
    ).apply {
        val colorRow =
            LinearLayout(
                this@colorPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }

        val colors =
            linkedMapOf(
                "BLUE" to
                    0xFF298DFF.toInt(),
                "RED" to
                    0xFFF22E35.toInt(),
                "GREEN" to
                    0xFF1FB257.toInt(),
                "YELLOW" to
                    0xFFFFD324.toInt(),
            )

        val colorMap =
            linkedMapOf<String, Button>()

        colors.forEach {
                entry ->
            val button =
                Button(
                    this@colorPanel,
                ).apply {
                    text =
                        "●\n" +
                            entry.key
                    textSize =
                        if (isCompactSetup()) 13f else 14f
                    setTextColor(
                        entry.value,
                    )
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        dp(2),
                        dp(6),
                        dp(2),
                        dp(6),
                    )
                    setOnClickListener {
                        selectedColor =
                            entry.key
                        refreshSetupSelections()
                    }
                }
            colorMap[entry.key] =
                button
            colorRow.addView(
                button,
                setupTileParams(
                    if (isCompactSetup()) 86 else 94,
                    marginDp =
                        if (isCompactSetup()) 3 else 5,
                ),
            )
        }

        colorButtons =
            colorMap
        addView(colorRow)
    }

internal fun OfflineGameActivity.localTrustStrip():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(12),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    this@localTrustStrip,
                    goldBorder = false,
                )
        elevation =
            dp(4).toFloat()

        addView(
            TextView(
                this@localTrustStrip,
            ).apply {
                text =
                    "✓"
                textSize =
                    25f
                setTextColor(
                    0xFF68F053.toInt(),
                )
                gravity =
                    Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                dp(38),
                dp(38),
            ),
        )

        addView(
            LinearLayout(
                this@localTrustStrip,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL

                addView(
                    TextView(
                        this@localTrustStrip,
                    ).apply {
                        text =
                            "LOCAL V4 RECOMPUTATION"
                        LudoProofTheme.body(
                            this,
                            12f,
                            bright = true,
                        )
                    },
                )
                addView(
                    TextView(
                        this@localTrustStrip,
                    ).apply {
                        text =
                            "Works offline • no remote server attestation"
                        LudoProofTheme.body(
                            this,
                            10.5f,
                        )
                        setPadding(
                            0,
                            dp(2),
                            0,
                            0,
                        )
                    },
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart =
                    dp(8)
            },
        )
    }

internal fun OfflineGameActivity.selectionPanel(
    title: String,
    subtitle: String,
): LinearLayout =
    LudoProofTheme.panel(this).apply {
        setPadding(
            dp(if (isCompactSetup()) 12 else 14),
            dp(if (isCompactSetup()) 12 else 14),
            dp(if (isCompactSetup()) 12 else 14),
            dp(if (isCompactSetup()) 12 else 14),
        )

        addView(
            TextView(this@selectionPanel).apply {
                text =
                    title
                LudoProofTheme.title(
                    this,
                    if (isCompactSetup()) 18f else 20f,
                )
            },
        )

        addView(
            TextView(this@selectionPanel).apply {
                text =
                    subtitle
                LudoProofTheme.body(
                    this,
                    11f,
                    centered = true,
                )
                setPadding(
                    0,
                    dp(2),
                    0,
                    dp(11),
                )
            },
        )
    }

internal fun OfflineGameActivity.choiceTile(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    action: () -> Unit,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        isEnabled =
            enabled
        alpha =
            if (enabled) 1f else .52f

        if (selected) {
            LudoProofTheme.selectedTile(this)
        } else {
            LudoProofTheme.normalTile(this)
        }

        setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8),
        )

        addView(
            TextView(this@choiceTile).apply {
                text =
                    icon
                textSize =
                    27f
                gravity =
                    Gravity.CENTER
                setTextColor(
                    if (selected) {
                        0xFF053177.toInt()
                    } else {
                        Color.WHITE
                    },
                )
            },
        )
        addView(
            TextView(this@choiceTile).apply {
                text =
                    title
                LudoProofTheme.body(
                    this,
                    16f,
                    centered = true,
                    bright = true,
                )
                if (selected) {
                    setTextColor(
                        0xFF052A68.toInt(),
                    )
                }
            },
        )
        addView(
            TextView(this@choiceTile).apply {
                text =
                    subtitle
                LudoProofTheme.body(
                    this,
                    10.5f,
                    centered = true,
                    bright = true,
                )
                if (selected) {
                    setTextColor(
                        0xFF174A78.toInt(),
                    )
                }
            },
        )
        setOnClickListener {
            if (enabled) {
                action()
            }
        }
    }

internal fun OfflineGameActivity.tileButton(
    text: String,
    action: () -> Unit,
): Button =
    Button(this).apply {
        this.text =
            text
        textSize =
            if (isCompactSetup()) 12.5f else 14f
        setTextColor(
            Color.WHITE,
        )
        gravity =
            Gravity.CENTER
        setPadding(
            dp(4),
            dp(8),
            dp(4),
            dp(8),
        )
        setOnClickListener {
            action()
        }
    }

internal fun OfflineGameActivity.refreshSetupSelections() {
    if (!::playerButtons.isInitialized || !::colorButtons.isInitialized) return

    playerButtons.forEach { entry ->
        if (entry.key == selectedPlayers) {
            LudoProofTheme.selectedTile(entry.value)
        } else {
            LudoProofTheme.normalTile(entry.value)
        }
    }
    colorButtons.forEach { entry ->
        if (entry.key == selectedColor) {
            LudoProofTheme.selectedTile(entry.value)
        } else {
            LudoProofTheme.normalTile(entry.value)
        }
    }
}

internal fun OfflineGameActivity.setupSectionParams(
    topMarginDp: Int,
) =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(topMarginDp)
    }

internal fun OfflineGameActivity.setupTileParams(
    heightDp: Int,
    marginDp: Int = 6,
) =
    LinearLayout.LayoutParams(
        0,
        dp(heightDp),
        1f,
    ).apply {
        setMargins(
            dp(marginDp),
            0,
            dp(marginDp),
            0,
        )
    }
