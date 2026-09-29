package com.ludoproof.game

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

class OfflineGameActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val engine by lazy { OfflineGameEngine(this) }

    private var selectedPlayers = 2
    private var selectedColor = "BLUE"
    private lateinit var playerButtons: Map<Int, Button>
    private lateinit var colorButtons: Map<String, Button>

    private var boardView: LudoBoardView? = null
    private var diceView: DiceView? = null
    private var turnText: TextView? = null
    private var infoText: TextView? = null
    private var statusText: TextView? = null
    private var rollButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        if (engine.hasSavedGame()) showGame(engine.snapshot()) else showSetup()
    }

    private fun showSetup() {
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

    private fun setupHero():
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
                    this@OfflineGameActivity,
                ),
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heroHeight,
                ),
            )

            val copy =
                LinearLayout(
                    this@OfflineGameActivity,
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
                TextView(this@OfflineGameActivity).apply {
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
                TextView(this@OfflineGameActivity).apply {
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
                TextView(this@OfflineGameActivity).apply {
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

    private fun gameTypePanel():
        LinearLayout =
        selectionPanel(
            "SELECT GAME",
            "Choose the local ruleset",
        ).apply {
            val row =
                LinearLayout(
                    this@OfflineGameActivity,
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
                    this@OfflineGameActivity,
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
                                this@OfflineGameActivity,
                                goldBorder = true,
                            )

                    addView(
                        TextView(
                            this@OfflineGameActivity,
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
                            this@OfflineGameActivity,
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

    private fun playersPanel():
        LinearLayout =
        selectionPanel(
            "SELECT PLAYERS",
            "2–4 players on this device",
        ).apply {
            val playersRow =
                LinearLayout(
                    this@OfflineGameActivity,
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

    private fun colorPanel():
        LinearLayout =
        selectionPanel(
            "CHOOSE YOUR COLOR",
            "Your seat starts with this color",
        ).apply {
            val colorRow =
                LinearLayout(
                    this@OfflineGameActivity,
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
                        this@OfflineGameActivity,
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

    private fun localTrustStrip():
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
                        this@OfflineGameActivity,
                        goldBorder = false,
                    )
            elevation =
                dp(4).toFloat()

            addView(
                TextView(
                    this@OfflineGameActivity,
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
                    this@OfflineGameActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    addView(
                        TextView(
                            this@OfflineGameActivity,
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
                            this@OfflineGameActivity,
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

    private fun showGame(snapshot: MatchSnapshot?) {
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

    private fun gameplayHud():
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
                        this@OfflineGameActivity,
                        goldBorder = true,
                    )
            elevation =
                dp(6).toFloat()

            addView(
                TextView(
                    this@OfflineGameActivity,
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
                    this@OfflineGameActivity,
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
                    this@OfflineGameActivity,
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

    private fun gameplayActionPanel():
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
                        this@OfflineGameActivity,
                    ).apply {
                        orientation =
                            LinearLayout.HORIZONTAL
                        gravity =
                            Gravity.CENTER_VERTICAL
                    }

                val diceDock =
                    FrameLayout(
                        this@OfflineGameActivity,
                    ).apply {
                        background =
                            LudoProofTheme
                                .hudPanelDrawable(
                                    this@OfflineGameActivity,
                                    goldBorder = true,
                                )
                        elevation =
                            dp(5).toFloat()
                    }

                diceView =
                    DiceView(
                        this@OfflineGameActivity,
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
                        this@OfflineGameActivity,
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
                                this@OfflineGameActivity,
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
                                            this@OfflineGameActivity,
                                        )
                            },
                        )

                        statusText =
                            TextView(
                                this@OfflineGameActivity,
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
                                this@OfflineGameActivity,
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
                        this@OfflineGameActivity,
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
                        this@OfflineGameActivity,
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

    private fun gameplayTools():
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
                    this@OfflineGameActivity,
                ).apply {
                    text =
                        "HISTORY"
                    LudoProofTheme
                        .secondary(this)
                    setOnClickListener {
                        ArcadeDialogs
                            .showProofHistory(
                                this@OfflineGameActivity,
                                "OFFLINE HISTORY",
                                offlineHistory(
                                    engine.snapshot(),
                                ),
                            )
                    }
                }

            val engineMap =
                Button(
                    this@OfflineGameActivity,
                ).apply {
                    text =
                        "ENGINE MAP"
                    LudoProofTheme
                        .secondary(this)
                    setOnClickListener {
                        ArcadeDialogs
                            .showNaturalWorldAudit(
                                this@OfflineGameActivity,
                                engine
                                    .lastRandomnessAudit(),
                            )
                    }
                }

            val newGame =
                Button(
                    this@OfflineGameActivity,
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

    private fun gameplaySectionParams(
        topMarginDp: Int,
    ) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(topMarginDp)
        }

    private fun gameplayToolParams() =
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

    private fun renderGame(
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

        turnText?.text =
            if (
                state.status ==
                "FINISHED"
            ) {
                "WINNER • " +
                    (
                        winner
                            ?.displayName
                            ?: "Player"
                        )
            } else {
                "TURN • " +
                    (
                        active
                            ?.displayName
                            ?: "Player"
                        )
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

    private fun rollOffline() {
        val button = rollButton ?: return
        button.isEnabled = false
        diceView?.startRolling()
        showStatus("Rolling locally…")

        handler.postDelayed(
            {
                runCatching { engine.roll() }
                    .onSuccess { state ->
                        renderGame(state)
                        showStatus(
                            if (state.pendingRoll != null) {
                                "Move a highlighted token."
                            } else {
                                "Turn updated."
                            },
                        )
                    }
                    .onFailure { error ->
                        diceView?.stopRolling()
                        button.isEnabled = true
                        showStatus(error.message ?: "Roll failed")
                    }
            },
            430L,
        )
    }

    private fun selectionPanel(
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
                TextView(this@OfflineGameActivity).apply {
                    text =
                        title
                    LudoProofTheme.title(
                        this,
                        if (isCompactSetup()) 18f else 20f,
                    )
                },
            )

            addView(
                TextView(this@OfflineGameActivity).apply {
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

    private fun choiceTile(
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
                TextView(this@OfflineGameActivity).apply {
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
                TextView(this@OfflineGameActivity).apply {
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
                TextView(this@OfflineGameActivity).apply {
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

    private fun tileButton(
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

    private fun refreshSetupSelections() {
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

    private fun backHeader(label: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                Button(this@OfflineGameActivity).apply {
                    LudoProofTheme.homeCircularAction(this, "‹")
                    setOnClickListener { finish() }
                },
                LinearLayout.LayoutParams(dp(52), dp(52)),
            )
            addView(
                TextView(this@OfflineGameActivity).apply {
                    text = label
                    LudoProofTheme.body(this, 14f, centered = true, bright = true)
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background =
                        LudoProofTheme.darkPanelDrawable(
                            this@OfflineGameActivity,
                            goldBorder = true,
                        )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply { marginStart = dp(12) },
            )
        }

    private fun offlineHistory(state: MatchSnapshot?): String {
        if (state == null || state.history.isEmpty()) {
            return "No offline rolls yet."
        }
        return buildString {
            append("Recent offline rolls\n\n")
            state.history.takeLast(16).reversed().forEach { event ->
                append("#")
                append(event.eventIndex)
                append("  dice=")
                append(event.outcome ?: "?")
                if (event.proofDigest != null) {
                    append("  localV4=")
                    append(event.proofDigest.take(8))
                    append("…")
                }
                if (event.moveTokenIndex != null) {
                    append("  token=")
                    append(event.moveTokenIndex + 1)
                }
                if (event.captures > 0) {
                    append("  captures=")
                    append(event.captures)
                }
                append("\n")
            }
        }.trimEnd()
    }

    private fun showStatus(value: String) {
        statusText?.text = value
    }

    private fun setupSectionParams(
        topMarginDp: Int,
    ) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(topMarginDp)
        }

    private fun setupTileParams(
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

    private fun isCompactSetup():
        Boolean =
        LudoProofTheme
            .isCompactWidth(this)

    private fun dp(value: Int): Int =
        LudoProofTheme.dp(this, value)

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
