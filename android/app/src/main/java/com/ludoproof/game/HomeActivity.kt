package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)

        val (root, host) =
            LudoProofTheme.arcadeRoot(this)

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
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
            LudoProofTheme.pageHorizontalPaddingDp(this)
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
                    dp(16),
                    0,
                    dp(26),
                )
            }
        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                contentWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
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
            profileHud(),
        )

        content.addView(
            heroPanel(),
            fullWidthSection(
                LudoProofTheme.sectionGapDp(this),
            ),
        )

        content.addView(
            quickActions(),
            fullWidthSection(
                if (isCompact()) 14 else 18,
            ),
        )

        content.addView(
            modeSection(),
            fullWidthSection(
                if (isCompact()) 20 else 24,
            ),
        )

        continueButton()
            ?.let { button ->
                content.addView(
                    button,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(if (isCompact()) 58 else 62),
                    ).apply {
                        setMargins(
                            if (isCompact()) dp(12) else dp(34),
                            dp(18),
                            if (isCompact()) dp(12) else dp(34),
                            0,
                        )
                    },
                )
            }

        content.addView(
            fairPlayStrip(),
            fullWidthSection(
                if (isCompact()) 16 else 20,
            ),
        )

        content.addView(
            footer(),
            fullWidthSection(14),
        )

        setContentView(root)

        connectivityMonitor =
            ConnectivityMonitor(this) { online ->
                runOnUiThread {
                    connectivityText.text =
                        if (online) {
                            "● ONLINE"
                        } else {
                            "● OFFLINE"
                        }
                    connectivityText
                        .setTextColor(
                            if (online) {
                                0xFF68F053.toInt()
                            } else {
                                LudoProofTheme.GOLD
                            },
                        )
                    connectivityText.contentDescription =
                        if (online) {
                            "Online"
                        } else {
                            "Offline"
                        }
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

    private fun profileHud():
        LinearLayout =
        if (isCompact()) {
            compactProfileHud()
        } else {
            regularProfileHud()
        }

    private fun compactProfileHud():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(12),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(this@HomeActivity)
            elevation =
                dp(5).toFloat()

            val identityRow =
                LinearLayout(this@HomeActivity).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }
            identityRow.addView(
                avatar(),
                LinearLayout.LayoutParams(
                    dp(52),
                    dp(52),
                ),
            )
            identityRow.addView(
                identityBlock(),
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        dp(11)
                },
            )
            addView(identityRow)

            addView(
                connectivityChip(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42),
                ).apply {
                    topMargin =
                        dp(10)
                },
            )
        }

    private fun regularProfileHud():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(this@HomeActivity)
            elevation =
                dp(5).toFloat()

            addView(
                avatar(),
                LinearLayout.LayoutParams(
                    dp(58),
                    dp(58),
                ),
            )
            addView(
                identityBlock(),
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        dp(12)
                },
            )
            addView(
                connectivityChip(),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(42),
                ),
            )
        }

    private fun avatar():
        TextView =
        TextView(this).apply {
            text = "LP"
            LudoProofTheme.title(
                this,
                17f,
                gold = true,
            )
            gravity =
                Gravity.CENTER
            contentDescription =
                "LudoProof"
            background =
                LudoProofTheme
                    .brandBadgeDrawable(
                        this@HomeActivity,
                    )
            elevation =
                dp(7).toFloat()
        }

    private fun identityBlock():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER_VERTICAL

            val brandRow =
                LinearLayout(
                    this@HomeActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            brandRow.addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "LUDO"
                    LudoProofTheme.title(
                        this,
                        if (isCompact()) 18f else 21f,
                    )
                    gravity =
                        Gravity.START or
                            Gravity.CENTER_VERTICAL
                },
            )
            brandRow.addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "PROOF"
                    LudoProofTheme.title(
                        this,
                        if (isCompact()) 18f else 21f,
                        gold = true,
                    )
                    gravity =
                        Gravity.START or
                            Gravity.CENTER_VERTICAL
                },
            )
            addView(brandRow)

            addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "VERIFIABLE PLAY • ENTRONEX V4"
                    LudoProofTheme.body(
                        this,
                        if (isCompact()) 9.5f else 11f,
                        bright = true,
                    )
                    setTextColor(
                        0xFF5FE4FF.toInt(),
                    )
                    setPadding(
                        0,
                        dp(2),
                        0,
                        0,
                    )
                },
            )
        }

    private fun connectivityChip():
        TextView =
        TextView(this).apply {
            connectivityText =
                this
            text =
                "● CHECKING"
            LudoProofTheme.body(
                this,
                11f,
                centered = true,
                bright = true,
            )
            gravity =
                Gravity.CENTER
            setPadding(
                dp(13),
                0,
                dp(13),
                0,
            )
            background =
                LudoProofTheme
                    .rounded(
                        0xE4071739.toInt(),
                        18f,
                        0x6647D7FF,
                        1f,
                        this@HomeActivity,
                    )
            accessibilityLiveRegion =
                View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

    private fun heroPanel():
        FrameLayout =
        FrameLayout(this).apply {
            val heroHeight =
                dp(
                    when {
                        LudoProofTheme
                            .isExpandedWidth(this@HomeActivity) ->
                            232
                        isCompact() ->
                            188
                        else ->
                            208
                    },
                )

            addView(
                HomeHeroArtView(
                    this@HomeActivity,
                ),
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heroHeight,
                ),
            )

            val copy =
                LinearLayout(
                    this@HomeActivity,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        dp(if (isCompact()) 62 else 92),
                        dp(16),
                        dp(if (isCompact()) 62 else 92),
                        dp(16),
                    )
                }

            copy.addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "PLAY LUDO."
                    LudoProofTheme.title(
                        this,
                        if (isCompact()) 22f else 28f,
                    )
                },
            )
            copy.addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "VERIFY THE DICE."
                    LudoProofTheme.title(
                        this,
                        if (isCompact()) 22f else 28f,
                        gold = true,
                    )
                },
            )
            copy.addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "Online uses remote EntroNex authority.\nLocal keeps the same v4 derivation on-device."
                    LudoProofTheme.body(
                        this,
                        if (isCompact()) 10.5f else 12f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(4),
                        dp(7),
                        dp(4),
                        0,
                    )
                    setShadowLayer(
                        3f,
                        0f,
                        dp(1).toFloat(),
                        0xCC001239.toInt(),
                    )
                },
            )

            addView(
                copy,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heroHeight,
                    Gravity.CENTER,
                ),
            )
        }

    private fun quickActions():
        FrameLayout =
        FrameLayout(this).apply {
            val row =
                LinearLayout(
                    this@HomeActivity,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER
                }

            row.addView(
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
            row.addView(
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
            row.addView(
                quickAction(
                    "↗",
                    "SHARE",
                ) {
                    shareLudoProof()
                },
                weighted(),
            )
            row.addView(
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

            val maxActionWidth =
                minOf(
                    dp(480),
                    (
                        resources.displayMetrics.widthPixels -
                            dp(28)
                        ).coerceAtLeast(1),
                )
            addView(
                row,
                FrameLayout.LayoutParams(
                    maxActionWidth,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
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
                Button(this@HomeActivity).apply {
                    LudoProofTheme
                        .homeCircularAction(
                            this,
                            symbol,
                        )
                    contentDescription =
                        label.lowercase()
                    setOnClickListener {
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(if (isCompact()) 50 else 54),
                    dp(if (isCompact()) 50 else 54),
                ),
            )
            addView(
                TextView(this@HomeActivity).apply {
                    text = label
                    LudoProofTheme.body(
                        this,
                        if (isCompact()) 10f else 11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        0,
                        dp(5),
                        0,
                        0,
                    )
                },
            )
        }

    private fun modeSection():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL

            addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "CHOOSE YOUR MODE"
                    LudoProofTheme.title(
                        this,
                        if (isCompact()) 20f else 22f,
                    )
                },
            )

            addView(
                TextView(this@HomeActivity).apply {
                    text =
                        "Remote verified play or local pass-and-play"
                    LudoProofTheme.body(
                        this,
                        12f,
                        centered = true,
                    )
                    setPadding(
                        0,
                        dp(4),
                        0,
                        dp(13),
                    )
                },
            )

            val modes =
                LinearLayout(this@HomeActivity).apply {
                    orientation =
                        if (isCompact()) {
                            LinearLayout.VERTICAL
                        } else {
                            LinearLayout.HORIZONTAL
                        }
                    gravity =
                        Gravity.CENTER
                }

            val onlineCard =
                modeCard(
                    mode =
                        ModeArtView.Mode.ONLINE,
                    title =
                        "ONLINE MATCH",
                    subtitle =
                        "Remote EntroNex authority",
                    buttonLabel =
                        "Play Online  ›",
                    usePositiveAction =
                        true,
                ) {
                    startActivity(
                        Intent(
                            this@HomeActivity,
                            MainActivity::class.java,
                        ),
                    )
                }

            val localCard =
                modeCard(
                    mode =
                        ModeArtView.Mode.LOCAL,
                    title =
                        "LOCAL PLAY",
                    subtitle =
                        "Pass & play • local v4",
                    buttonLabel =
                        "Play Local  ›",
                    usePositiveAction =
                        false,
                ) {
                    startActivity(
                        Intent(
                            this@HomeActivity,
                            OfflineGameActivity::class.java,
                        ),
                    )
                }

            if (isCompact()) {
                modes.addView(
                    onlineCard,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(228),
                    ),
                )
                modes.addView(
                    localCard,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(228),
                    ).apply {
                        topMargin =
                            dp(12)
                    },
                )
            } else {
                val cardHeight =
                    dp(
                        if (
                            LudoProofTheme
                                .isExpandedWidth(this@HomeActivity)
                        ) {
                            270
                        } else {
                            242
                        },
                    )
                modes.addView(
                    onlineCard,
                    LinearLayout.LayoutParams(
                        0,
                        cardHeight,
                        1f,
                    ).apply {
                        marginEnd =
                            dp(7)
                    },
                )
                modes.addView(
                    localCard,
                    LinearLayout.LayoutParams(
                        0,
                        cardHeight,
                        1f,
                    ).apply {
                        marginStart =
                            dp(7)
                    },
                )
            }

            addView(modes)
        }

    private fun modeCard(
        mode: ModeArtView.Mode,
        title: String,
        subtitle: String,
        buttonLabel: String,
        usePositiveAction: Boolean,
        action: () -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(6),
            )
            background =
                LudoProofTheme
                    .panelDrawable(this@HomeActivity)
            elevation =
                dp(8).toFloat()
            isClickable =
                true
            isFocusable =
                true
            contentDescription =
                "$title. $subtitle"
            setOnClickListener {
                action()
            }

            addView(
                ModeArtView(this@HomeActivity).apply {
                    this.mode =
                        mode
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )

            addView(
                TextView(this@HomeActivity).apply {
                    text =
                        title
                    LudoProofTheme.title(
                        this,
                        if (isCompact()) 17f else 18f,
                    )
                    setPadding(
                        0,
                        dp(6),
                        0,
                        0,
                    )
                },
            )

            addView(
                TextView(this@HomeActivity).apply {
                    text =
                        subtitle
                    LudoProofTheme.body(
                        this,
                        if (isCompact()) 10.5f else 11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(4),
                        dp(2),
                        dp(4),
                        dp(6),
                    )
                },
            )

            addView(
                Button(this@HomeActivity).apply {
                    text =
                        buttonLabel
                    if (usePositiveAction) {
                        LudoProofTheme
                            .positive(this)
                    } else {
                        LudoProofTheme
                            .primary(this)
                    }
                    textSize =
                        if (isCompact()) 16f else 17f
                    setOnClickListener {
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54),
                ),
            )
        }

    private fun continueButton():
        Button? {
        val online =
            SecureSessionStore(this).load()
        val offline =
            OfflineGameEngine(this).hasSavedGame()

        if (
            online == null &&
            !offline
        ) {
            return null
        }

        return Button(this).apply {
            text =
                if (online != null) {
                    "Continue Online Match  ›"
                } else {
                    "Continue Local Game  ›"
                }
            LudoProofTheme.positive(this)
            textSize =
                if (isCompact()) 17f else 19f
            setOnClickListener {
                startActivity(
                    Intent(
                        this@HomeActivity,
                        if (online != null) {
                            MainActivity::class.java
                        } else {
                            OfflineGameActivity::class.java
                        },
                    ),
                )
            }
        }
    }

    private fun fairPlayStrip():
        TextView =
        TextView(this).apply {
            text =
                "✓  FAIR PLAY\nOnline: remote authority  •  Local: on-device v4 recomputation"
            LudoProofTheme.body(
                this,
                if (isCompact()) 11f else 12f,
                centered = true,
                bright = true,
            )
            setPadding(
                dp(14),
                dp(13),
                dp(14),
                dp(13),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(
                        this@HomeActivity,
                        goldBorder = true,
                    )
            elevation =
                dp(4).toFloat()
            isClickable =
                true
            isFocusable =
                true
            contentDescription =
                "Fair play information"
            setOnClickListener {
                ArcadeDialogs
                    .showProofHistory(
                        this@HomeActivity,
                        "FAIR PLAY",
                        "LudoProof uses the same v4 derivation math online and offline. Online adds remote EntroNex authority; offline is locally reproducible only.",
                    )
            }
        }

    private fun footer():
        TextView =
        TextView(this).apply {
            text =
                "SERVER-AUTHORITATIVE ONLINE • NO OUTCOME REROLLS"
            LudoProofTheme.body(
                this,
                10f,
                centered = true,
            )
            setPadding(
                dp(8),
                dp(4),
                dp(8),
                0,
            )
        }

    private fun shareLudoProof() {
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
            Intent.createChooser(
                share,
                "Share LudoProof",
            ),
        )
    }

    private fun fullWidthSection(
        topMarginDp: Int,
    ): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(topMarginDp)
        }

    private fun weighted():
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        )

    private fun isCompact():
        Boolean =
        LudoProofTheme
            .isCompactWidth(this)

    private fun dp(
        value: Int,
    ): Int =
        LudoProofTheme.dp(
            this,
            value,
        )
}
