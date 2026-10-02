package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profileBadgesPanel(
    profile: ProfileSnapshot,
):
    LinearLayout =
    profilePanel().apply {
        val badges =
            ProfileProgression
                .badges(
                    profile,
                )

        val heading =
            LinearLayout(
                this@profileBadgesPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }
        heading.addView(
            profileTitle(
                "Unlocked Badges",
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        heading.addView(
            Button(
                this@profileBadgesPanel,
            ).apply {
                text =
                    "View All"
                textSize =
                    12f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                minWidth =
                    0
                minHeight =
                    0
                background =
                    null
                setOnClickListener {
                    viewAllBadges(
                        badges.joinToString(
                            "\n\n",
                        ) {
                                badge ->
                            (
                                if (
                                    badge.unlocked
                                ) {
                                    "✓ "
                                } else {
                                    "○ "
                                }
                                ) +
                                badge.title +
                                " — " +
                                badge.requirement
                        },
                    )
                }
            },
        )
        addView(
            heading,
        )

        val row =
            LinearLayout(
                this@profileBadgesPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
                setPadding(
                    0,
                    dp(12),
                    0,
                    0,
                )
            }

        badges.forEach {
                badge ->
            row.addView(
                LinearLayout(
                    this@profileBadgesPanel,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.CENTER
                    alpha =
                        if (
                            badge.unlocked
                        ) {
                            1f
                        } else {
                            .36f
                        }

                    addView(
                        TextView(
                            this@profileBadgesPanel,
                        ).apply {
                            text =
                                badge.symbol
                            textSize =
                                21f
                            setTypeface(
                                Typeface.DEFAULT_BOLD,
                            )
                            setTextColor(
                                if (
                                    badge.unlocked
                                ) {
                                    LudoProofTheme.GOLD
                                } else {
                                    0xFF9CB3D0.toInt()
                                },
                            )
                            gravity =
                                Gravity.CENTER
                            background =
                                LudoProofTheme
                                    .rounded(
                                        0xD70B3478.toInt(),
                                        999f,
                                        0x665ED8FF,
                                        1f,
                                        this@profileBadgesPanel,
                                    )
                        },
                        LinearLayout.LayoutParams(
                            dp(44),
                            dp(44),
                        ),
                    )

                    addView(
                        TextView(
                            this@profileBadgesPanel,
                        ).apply {
                            text =
                                badge.title
                            textSize =
                                7.5f
                            setTextColor(
                                Color.WHITE,
                            )
                            gravity =
                                Gravity.CENTER
                            maxLines =
                                2
                            setPadding(
                                0,
                                dp(4),
                                0,
                                0,
                            )
                        },
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )
        }

        addView(
            row,
        )
    }
