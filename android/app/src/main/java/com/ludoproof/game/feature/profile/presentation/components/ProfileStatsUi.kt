package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.domain.model.ProfileModeStats
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profileModeStatsPanel(
    profile: ProfileSnapshot,
):
    LinearLayout =
    profilePanel().apply {
        addView(
            modeRow(
                symbol = "▦",
                label = "Classic",
                stats =
                    profile.classic,
            ),
        )
        addView(
            modeRow(
                symbol = "⚡",
                label = "Rush",
                stats =
                    profile.rush,
            ),
            rowSpacing(),
        )
        addView(
            modeRow(
                symbol = "↟",
                label = "Snake & Ladder",
                stats =
                    profile.snakeLadder,
            ),
            rowSpacing(),
        )
    }

private fun ProfileActivity.modeRow(
    symbol: String,
    label: String,
    stats: ProfileModeStats,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL

        val top =
            LinearLayout(
                this@modeRow,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        top.addView(
            TextView(
                this@modeRow,
            ).apply {
                text =
                    symbol
                textSize =
                    20f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                background =
                    LudoProofTheme
                        .rounded(
                            0xE20A2F72.toInt(),
                            999f,
                            0x665ED8FF,
                            1f,
                            this@modeRow,
                        )
            },
            LinearLayout.LayoutParams(
                dp(44),
                dp(44),
            ),
        )

        top.addView(
            TextView(
                this@modeRow,
            ).apply {
                text =
                    label
                textSize =
                    17f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                setPadding(
                    dp(12),
                    0,
                    0,
                    0,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        top.addView(
            TextView(
                this@modeRow,
            ).apply {
                text =
                    stats.wins
                        .toString() +
                        " wins · " +
                        stats.games +
                        " games  ›"
                textSize =
                    13f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
            },
        )
        addView(
            top,
        )

        val ratio =
            if (
                stats.games >
                    0
            ) {
                stats.wins
                    .toFloat() /
                    stats.games
                        .toFloat()
            } else {
                0f
            }

        val progress =
            FrameLayout(
                this@modeRow,
            ).apply {
                background =
                    LudoProofTheme
                        .rounded(
                            0xCC092A63.toInt(),
                            999f,
                            0x5537C9FF,
                            1f,
                            this@modeRow,
                        )
            }
        progress.addView(
            View(
                this@modeRow,
            ).apply {
                background =
                    LudoProofTheme
                        .rounded(
                            LudoProofTheme.GOLD,
                            999f,
                            LudoProofTheme.GOLD,
                            0f,
                            this@modeRow,
                        )
            },
            FrameLayout.LayoutParams(
                dp(
                    (
                        260 *
                            ratio
                        )
                        .toInt()
                        .coerceAtLeast(
                            if (
                                stats.games >
                                    0
                            ) {
                                6
                            } else {
                                0
                            },
                        ),
                ),
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(9),
            ).apply {
                topMargin =
                    dp(8)
                marginStart =
                    dp(56)
            },
        )
    }

private fun ProfileActivity.rowSpacing():
    LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(14)
    }
