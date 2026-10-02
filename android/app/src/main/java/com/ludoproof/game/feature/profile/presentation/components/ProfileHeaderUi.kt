package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.core.ui.components.ProfilePlaceholderView
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profileTopBar():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            Button(
                this@profileTopBar,
            ).apply {
                text =
                    "‹"
                textSize =
                    30f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minWidth =
                    0
                minHeight =
                    0
                setPadding(
                    0,
                    0,
                    0,
                    dp(3),
                )
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            0xFFFFC928.toInt(),
                            0xFFF07A00.toInt(),
                        ),
                    ).apply {
                        shape =
                            GradientDrawable.OVAL
                        setStroke(
                            dp(2),
                            0xFFFFE47A.toInt(),
                        )
                    }
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                dp(50),
                dp(50),
            ),
        )

        addView(
            TextView(
                this@profileTopBar,
            ).apply {
                text =
                    "Profile"
                textSize =
                    25f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER_VERTICAL
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

        addView(
            Button(
                this@profileTopBar,
            ).apply {
                text =
                    "G  SIGN IN"
                textSize =
                    11f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF4A4A4A.toInt(),
                )
                minWidth =
                    0
                minHeight =
                    0
                setPadding(
                    dp(12),
                    0,
                    dp(12),
                    0,
                )
                background =
                    LudoProofTheme
                        .rounded(
                            Color.WHITE,
                            11f,
                            0xFFDDDDDD.toInt(),
                            1f,
                            this@profileTopBar,
                        )
                setOnClickListener {
                    showGoogleSetupInfo()
                }
            },
            LinearLayout.LayoutParams(
                dp(104),
                dp(42),
            ),
        )
    }

internal fun ProfileActivity.profileIdentityPanel(
    profile: ProfileSnapshot,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL

        val identity =
            LinearLayout(
                this@profileIdentityPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val avatar =
            FrameLayout(
                this@profileIdentityPanel,
            ).apply {
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            0xFF168EEA.toInt(),
                            0xFF083D91.toInt(),
                        ),
                    ).apply {
                        cornerRadius =
                            dp(14)
                                .toFloat()
                        setStroke(
                            dp(2),
                            0xFF5BE0FF.toInt(),
                        )
                    }

                addView(
                    ProfilePlaceholderView(
                        this@profileIdentityPanel,
                    ).apply {
                        contentDescription =
                            "Profile placeholder"
                        setPadding(
                            dp(16),
                            dp(16),
                            dp(16),
                            dp(16),
                        )
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )

                addView(
                    Button(
                        this@profileIdentityPanel,
                    ).apply {
                        text =
                            "✎"
                        textSize =
                            14f
                        setTextColor(
                            Color.WHITE,
                        )
                        minWidth =
                            0
                        minHeight =
                            0
                        setPadding(
                            0,
                            0,
                            0,
                            0,
                        )
                        background =
                            GradientDrawable().apply {
                                shape =
                                    GradientDrawable.OVAL
                                setColor(
                                    0xFFFF9800.toInt(),
                                )
                            }
                        setOnClickListener {
                            showNameEditor()
                        }
                    },
                    FrameLayout.LayoutParams(
                        dp(30),
                        dp(30),
                        Gravity.BOTTOM or
                            Gravity.END,
                    ),
                )
            }

        identity.addView(
            avatar,
            LinearLayout.LayoutParams(
                dp(112),
                dp(112),
            ),
        )

        val copy =
            LinearLayout(
                this@profileIdentityPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(14),
                    0,
                    0,
                    0,
                )
            }

        copy.addView(
            TextView(
                this@profileIdentityPanel,
            ).apply {
                text =
                    profile.displayName
                textSize =
                    22f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
            },
        )

        val progress =
            ProfileProgression
                .levelProgress(
                    profile.totalXp,
                )
        val levelRow =
            LinearLayout(
                this@profileIdentityPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    0,
                    dp(10),
                    0,
                    0,
                )
            }

        levelRow.addView(
            TextView(
                this@profileIdentityPanel,
            ).apply {
                text =
                    "★" +
                        progress.level
                textSize =
                    22f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                gravity =
                    Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                dp(58),
                dp(38),
            ),
        )

        val track =
            FrameLayout(
                this@profileIdentityPanel,
            ).apply {
                background =
                    LudoProofTheme
                        .rounded(
                            0xD908245B.toInt(),
                            999f,
                            0xFF4A91D9.toInt(),
                            1f,
                            this@profileIdentityPanel,
                        )
            }
        track.addView(
            TextView(
                this@profileIdentityPanel,
            ).apply {
                text =
                    progress.xpIntoLevel
                        .toString() +
                        " / " +
                        progress.xpForNextLevel +
                        " XP"
                textSize =
                    11f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(
                            0xFFFFD43B.toInt(),
                            0xFFFFB20F.toInt(),
                        ),
                    ).apply {
                        cornerRadius =
                            dp(999)
                                .toFloat()
                    }
            },
            FrameLayout.LayoutParams(
                0,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                width =
                    dp(
                        (
                            180 *
                                progress.fraction
                            )
                            .toInt()
                            .coerceAtLeast(
                                32,
                            ),
                    )
            },
        )
        track.addView(
            TextView(
                this@profileIdentityPanel,
            ).apply {
                text =
                    progress.xpIntoLevel
                        .toString() +
                        " / " +
                        progress.xpForNextLevel +
                        " XP"
                textSize =
                    11f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        levelRow.addView(
            track,
            LinearLayout.LayoutParams(
                0,
                dp(30),
                1f,
            ),
        )
        copy.addView(
            levelRow,
        )

        identity.addView(
            copy,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        addView(
            identity,
        )

        addView(
            TextView(
                this@profileIdentityPanel,
            ).apply {
                text =
                    profile.totalGames
                        .toString() +
                        " games · " +
                        profile.currentDayStreak +
                        " day streak · " +
                        profile.totalWins +
                        " wins"
                textSize =
                    16f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                setPadding(
                    0,
                    dp(16),
                    0,
                    0,
                )
            },
        )
    }
