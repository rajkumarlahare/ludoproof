package com.ludoproof.game.ui.home

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
import com.ludoproof.game.*

internal fun HomeActivity.modeSection():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL

        addView(
            TextView(this@modeSection).apply {
                text =
                    "CHOOSE YOUR MODE"
                LudoProofTheme.title(
                    this,
                    if (isCompact()) 20f else 22f,
                )
            },
        )

        addView(
            TextView(this@modeSection).apply {
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
            LinearLayout(this@modeSection).apply {
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
                        this@modeSection,
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
                        this@modeSection,
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
                            .isExpandedWidth(this@modeSection)
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

internal fun HomeActivity.modeCard(
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
                .panelDrawable(this@modeCard)
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
            ModeArtView(this@modeCard).apply {
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
            TextView(this@modeCard).apply {
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
            TextView(this@modeCard).apply {
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
            Button(this@modeCard).apply {
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

internal fun HomeActivity.continueButton():
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
                    this@continueButton,
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

internal fun HomeActivity.fairPlayStrip():
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
                    this@fairPlayStrip,
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
                    this@fairPlayStrip,
                    "FAIR PLAY",
                    "LudoProof uses the same v4 derivation math online and offline. Online adds remote EntroNex authority; offline is locally reproducible only.",
                )
        }
    }

internal fun HomeActivity.footer():
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

internal fun HomeActivity.shareLudoProof() {
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
