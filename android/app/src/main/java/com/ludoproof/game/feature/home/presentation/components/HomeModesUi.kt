package com.ludoproof.game.ui.home

import android.app.Activity
import android.graphics.drawable.GradientDrawable
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
        gravity =
            Gravity.CENTER_HORIZONTAL

        val cardSize =
            dp(
                if (isCompact()) {
                    146
                } else {
                    162
                },
            )

        addView(
            homeGameCard(
                title =
                    "ONLINE GAME",
                artMode =
                    ModeArtView.Mode.ONLINE,
                placeholder =
                    null,
                labelColors =
                    intArrayOf(
                        0xFF28C7FF.toInt(),
                        0xFF126FD6.toInt(),
                    ),
            ) {
                startActivity(
                    Intent(
                        this@modeSection,
                        MainActivity::class.java,
                    ),
                )
            },
            LinearLayout.LayoutParams(
                cardSize,
                cardSize,
            ),
        )

        val bottomRow =
            LinearLayout(
                this@modeSection,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }

        bottomRow.addView(
            homeGameCard(
                title =
                    "COMPUTER",
                artMode =
                    null,
                placeholder =
                    "CPU",
                labelColors =
                    intArrayOf(
                        0xFF70D82F.toInt(),
                        0xFF2FA91F.toInt(),
                    ),
            ) {
                // Computer mode logic will be connected later.
            },
            LinearLayout.LayoutParams(
                cardSize,
                cardSize,
            ).apply {
                marginEnd =
                    dp(if (isCompact()) 6 else 8)
            },
        )

        bottomRow.addView(
            homeGameCard(
                title =
                    "LOCAL",
                artMode =
                    ModeArtView.Mode.LOCAL,
                placeholder =
                    null,
                labelColors =
                    intArrayOf(
                        0xFFFFC32A.toInt(),
                        0xFFF08A00.toInt(),
                    ),
            ) {
                startActivity(
                    Intent(
                        this@modeSection,
                        OfflineGameActivity::class.java,
                    ),
                )
            },
            LinearLayout.LayoutParams(
                cardSize,
                cardSize,
            ).apply {
                marginStart =
                    dp(if (isCompact()) 6 else 8)
            },
        )

        addView(
            bottomRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(if (isCompact()) 12 else 14)
            },
        )
    }

private fun HomeActivity.homeGameCard(
    title: String,
    artMode: ModeArtView.Mode?,
    placeholder: String?,
    labelColors: IntArray,
    action: () -> Unit,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4),
        )
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF168EEA.toInt(),
                    0xFF0D5BB9.toInt(),
                    0xFF073E91.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(14).toFloat()
                setStroke(
                    dp(2),
                    0xFF5BE0FF.toInt(),
                )
            }
        elevation =
            dp(7).toFloat()
        isClickable = true
        isFocusable = true
        contentDescription =
            title
        setOnClickListener {
            action()
        }

        val artHost =
            FrameLayout(
                this@homeGameCard,
            )

        if (artMode != null) {
            artHost.addView(
                ModeArtView(
                    this@homeGameCard,
                ).apply {
                    mode =
                        artMode
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        } else {
            artHost.addView(
                TextView(
                    this@homeGameCard,
                ).apply {
                    text =
                        placeholder ?: "GAME"
                    textSize =
                        if (isCompact()) 28f else 31f
                    setTypeface(
                        android.graphics.Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        0xFFEAF8FF.toInt(),
                    )
                    gravity =
                        Gravity.CENTER
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }

        addView(
            artHost,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        addView(
            TextView(
                this@homeGameCard,
            ).apply {
                text =
                    title
                textSize =
                    if (isCompact()) 14f else 15.5f
                setTypeface(
                    android.graphics.Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFFFFFFFF.toInt(),
                )
                gravity =
                    Gravity.CENTER
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        labelColors,
                    ).apply {
                        cornerRadius =
                            dp(10).toFloat()
                        setStroke(
                            dp(1),
                            0x66FFFFFF,
                        )
                    }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (isCompact()) 34 else 38),
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
    val storeUrl =
        "https://play.google.com/store/apps/details?id=$packageName"
    val shareText =
        buildString {
            append(
                "Play LudoProof — fair, verifiable Ludo online or locally.",
            )
            append("\n\n")
            append(storeUrl)
        }

    val share =
        Intent(
            Intent.ACTION_SEND,
        ).apply {
            type =
                "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                "LudoProof",
            )
            putExtra(
                Intent.EXTRA_TEXT,
                shareText,
            )
        }

    startActivity(
        Intent.createChooser(
            share,
            "Share LudoProof",
        ),
    )
}
