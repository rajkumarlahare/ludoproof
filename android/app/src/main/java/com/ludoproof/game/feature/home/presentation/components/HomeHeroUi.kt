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
import com.ludoproof.game.feature.proof.domain.core.FairDiceExplainer

internal fun HomeActivity.heroPanel():
    FrameLayout =
    FrameLayout(this).apply {
        val heroHeight =
            dp(
                when {
                    LudoProofTheme
                        .isExpandedWidth(this@heroPanel) ->
                        232
                    isCompact() ->
                        188
                    else ->
                        208
                },
            )

        addView(
            HomeHeroArtView(
                this@heroPanel,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )

        val copy =
            LinearLayout(
                this@heroPanel,
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
            TextView(this@heroPanel).apply {
                text =
                    getString(R.string.home_hero_title)
                LudoProofTheme.title(
                    this,
                    if (isCompact()) 22f else 28f,
                )
            },
        )
        copy.addView(
            TextView(this@heroPanel).apply {
                text =
                    getString(R.string.home_hero_tagline)
                LudoProofTheme.title(
                    this,
                    if (isCompact()) 22f else 28f,
                    gold = true,
                )
            },
        )
        copy.addView(
            TextView(this@heroPanel).apply {
                text =
                    getString(R.string.home_hero_supporting_copy)
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

internal fun HomeActivity.quickActions():
    FrameLayout =
    FrameLayout(this).apply {
        val row =
            LinearLayout(
                this@quickActions,
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
                        this@quickActions,
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
                FairDiceExplainer.HOME_ACTION_LABEL,
            ) {
                ArcadeDialogs
                    .showProofHistory(
                        this@quickActions,
                        FairDiceExplainer.DIALOG_TITLE,
                        FairDiceExplainer.dialogBody(),
                    )
            },
            weighted(),
        )
        row.addView(
            quickAction(
                symbol = "",
                label = "SHARE",
                iconKind =
                    HomeIconKind.SHARE,
            ) {
                shareLudoPaws()
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
                        this@quickActions,
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

internal fun HomeActivity.quickAction(
    symbol: String,
    label: String,
    iconKind: HomeIconKind? = null,
    action: () -> Unit,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER

        val actionSize =
            dp(
                if (
                    isCompact()
                ) {
                    50
                } else {
                    54
                },
            )
        val actionView =
            if (
                iconKind !=
                null
            ) {
                HomeIconView(
                    this@quickAction,
                ).apply {
                    kind =
                        iconKind
                    iconColor =
                        0xFF91E9FF.toInt()
                    contentDescription =
                        label.lowercase()
                    isClickable =
                        true
                    isFocusable =
                        true
                    background =
                        homeBlueCircularIconBackground(
                            this@quickAction,
                        )
                    elevation =
                        dp(7)
                            .toFloat()
                    setOnClickListener {
                        action()
                    }
                }
            } else {
                Button(
                    this@quickAction,
                ).apply {
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
                }
            }

        addView(
            actionView,
            LinearLayout.LayoutParams(
                actionSize,
                actionSize,
            ),
        )
        addView(
            TextView(this@quickAction).apply {
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
