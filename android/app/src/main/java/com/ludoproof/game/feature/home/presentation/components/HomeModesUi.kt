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

        addView(
            homeModeRow(
                listOf(
                    HomeModeCard(
                        mode =
                            GameMode.ONLINE,
                        title =
                            "ONLINE",
                        subtitle =
                            "MATCHMAKING",
                        artMode =
                            ModeArtView.Mode.ONLINE,
                        labelColors =
                            intArrayOf(
                                0xFF28C7FF.toInt(),
                                0xFF126FD6.toInt(),
                            ),
                    ),
                    HomeModeCard(
                        mode =
                            GameMode.TEAM_UP,
                        title =
                            "TEAM UP",
                        subtitle =
                            "2 VS 2",
                        artMode =
                            ModeArtView.Mode.TEAM_UP,
                        labelColors =
                            intArrayOf(
                                0xFFFFC62E.toInt(),
                                0xFFE38700.toInt(),
                            ),
                    ),
                    HomeModeCard(
                        mode =
                            GameMode.FRIENDS,
                        title =
                            "FRIENDS",
                        subtitle =
                            "PRIVATE ROOM",
                        artMode =
                            ModeArtView.Mode.FRIENDS,
                        labelColors =
                            intArrayOf(
                                0xFFFF6B8A.toInt(),
                                0xFFD7285C.toInt(),
                            ),
                    ),
                ),
            ),
        )

        addView(
            homeModeRow(
                listOf(
                    HomeModeCard(
                        mode =
                            GameMode.COMPUTER,
                        title =
                            "COMPUTER",
                        subtitle =
                            "CPU",
                        artMode =
                            ModeArtView.Mode.COMPUTER,
                        labelColors =
                            intArrayOf(
                                0xFF70D82F.toInt(),
                                0xFF2FA91F.toInt(),
                            ),
                    ),
                    HomeModeCard(
                        mode =
                            GameMode.PASS_AND_PLAY,
                        title =
                            "PASS & PLAY",
                        subtitle =
                            "OFFLINE",
                        artMode =
                            ModeArtView.Mode.PASS_AND_PLAY,
                        labelColors =
                            intArrayOf(
                                0xFFFFC32A.toInt(),
                                0xFFF08A00.toInt(),
                            ),
                    ),
                ),
            ),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(
                        if (
                            isCompact()
                        ) {
                            10
                        } else {
                            12
                        },
                    )
            },
        )
    }

private data class HomeModeCard(
    val mode: GameMode,
    val title: String,
    val subtitle: String,
    val artMode: ModeArtView.Mode,
    val labelColors: IntArray,
)

private fun HomeActivity.homeModeRow(
    cards: List<HomeModeCard>,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER

        cards.forEachIndexed {
                index,
                card ->
            addView(
                homeGameCard(
                    card,
                ),
                LinearLayout.LayoutParams(
                    0,
                    dp(
                        if (
                            isCompact()
                        ) {
                            126
                        } else {
                            142
                        },
                    ),
                    1f,
                ).apply {
                    if (
                        index >
                        0
                    ) {
                        marginStart =
                            dp(
                                if (
                                    isCompact()
                                ) {
                                    5
                                } else {
                                    7
                                },
                            )
                    }
                },
            )
        }
    }

private fun HomeActivity.homeGameCard(
    card: HomeModeCard,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(3),
            dp(3),
            dp(3),
            dp(3),
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
                    dp(13).toFloat()
                setStroke(
                    dp(2),
                    0xFF5BE0FF.toInt(),
                )
            }
        elevation =
            dp(6).toFloat()
        isClickable =
            true
        isFocusable =
            true
        contentDescription =
            card.title +
                ", " +
                card.subtitle
        setOnClickListener {
            openGameMode(
                card.mode,
            )
        }

        addView(
            ModeArtView(
                this@homeGameCard,
            ).apply {
                mode =
                    card.artMode
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
            LinearLayout(
                this@homeGameCard,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        card.labelColors,
                    ).apply {
                        cornerRadius =
                            dp(9).toFloat()
                        setStroke(
                            dp(1),
                            0x66FFFFFF,
                        )
                    }

                addView(
                    TextView(
                        this@homeGameCard,
                    ).apply {
                        text =
                            card.title
                        textSize =
                            if (
                                isCompact()
                            ) {
                                11f
                            } else {
                                12.5f
                            }
                        setTypeface(
                            android.graphics.Typeface.DEFAULT_BOLD,
                        )
                        setTextColor(
                            0xFFFFFFFF.toInt(),
                        )
                        gravity =
                            Gravity.CENTER
                        maxLines =
                            1
                    },
                )

                addView(
                    TextView(
                        this@homeGameCard,
                    ).apply {
                        text =
                            card.subtitle
                        textSize =
                            if (
                                isCompact()
                            ) {
                                8.5f
                            } else {
                                9.5f
                            }
                        setTextColor(
                            0xE6FFFFFF.toInt(),
                        )
                        gravity =
                            Gravity.CENTER
                        maxLines =
                            1
                    },
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(
                    if (
                        isCompact()
                    ) {
                        38
                    } else {
                        42
                    },
                ),
            ),
        )
    }

internal fun HomeActivity.continueButton():
    Button? {
    val online =
        SecureSessionStore(
            this,
        ).load()
    val local =
        LocalMatchSession(
            context =
                this,
            mode =
                GameMode.PASS_AND_PLAY,
        )
            .hasSavedGame()
    val computer =
        LocalMatchSession(
            context =
                this,
            mode =
                GameMode.COMPUTER,
        )
            .hasSavedGame()

    if (
        online ==
            null &&
        !local &&
        !computer
    ) {
        return null
    }

    return Button(this).apply {
        text =
            when {
                online !=
                    null ->
                    "Continue Online Match  ›"

                local ->
                    "Continue Pass & Play  ›"

                else ->
                    "Continue Computer Game  ›"
            }
        LudoProofTheme.positive(
            this,
        )
        textSize =
            if (
                isCompact()
            ) {
                17f
            } else {
                19f
            }
        setOnClickListener {
            if (
                online !=
                null
            ) {
                openGameMode(
                    GameMode.ONLINE,
                )
                return@setOnClickListener
            }

            openGameMode(
                if (
                    local
                ) {
                    GameMode.PASS_AND_PLAY
                } else {
                    GameMode.COMPUTER
                },
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
