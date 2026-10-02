package com.ludoproof.game.ui.online

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.MainActivity
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.core.ui.components.ProfilePlaceholderView
import com.ludoproof.game.feature.online.beginPublicMatchmaking
import com.ludoproof.game.feature.online.cancelPublicMatchmaking
import com.ludoproof.game.feature.online.selectPublicPlayerCount

internal fun MainActivity.onlineLobbyPanel():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER_HORIZONTAL

        val profile =
            ProfileStore(
                this@onlineLobbyPanel,
            )
                .snapshot()

        nameInput =
            EditText(
                this@onlineLobbyPanel,
            ).apply {
                setText(
                    profile.displayName,
                )
                visibility =
                    View.GONE
            }

        matchInput =
            EditText(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
            }
        createButton =
            Button(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
            }
        joinButton =
            Button(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
            }

        matchmakingSetupPanel =
            LinearLayout(
                this@onlineLobbyPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            20
                        } else {
                            28
                        },
                    ),
                    dp(24),
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            20
                        } else {
                            28
                        },
                    ),
                    dp(24),
                )
                background =
                    LudoProofTheme
                        .darkPanelDrawable(
                            this@onlineLobbyPanel,
                            goldBorder = true,
                        )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "SELECT PLAYERS"
                        LudoProofTheme
                            .title(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    21f
                                } else {
                                    24f
                                },
                                gold = true,
                            )
                        gravity =
                            Gravity.CENTER
                    },
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "Choose match size"
                        LudoProofTheme
                            .body(
                                this,
                                11f,
                                centered = true,
                                bright = true,
                            )
                        setPadding(
                            0,
                            dp(4),
                            0,
                            dp(16),
                        )
                    },
                )

                twoPlayerButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        minWidth =
                            0
                        minHeight =
                            0
                        setPadding(
                            dp(14),
                            0,
                            dp(14),
                            0,
                        )
                        setOnClickListener {
                            selectPublicPlayerCount(
                                2,
                            )
                        }
                    }
                addView(
                    twoPlayerButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(46),
                    ).apply {
                        leftMargin =
                            dp(
                                if (
                                    isCompactOnline()
                                ) {
                                    22
                                } else {
                                    54
                                },
                            )
                        rightMargin =
                            leftMargin
                    },
                )

                fourPlayerButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        minWidth =
                            0
                        minHeight =
                            0
                        setPadding(
                            dp(14),
                            0,
                            dp(14),
                            0,
                        )
                        setOnClickListener {
                            selectPublicPlayerCount(
                                4,
                            )
                        }
                    }
                addView(
                    fourPlayerButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(46),
                    ).apply {
                        leftMargin =
                            dp(
                                if (
                                    isCompactOnline()
                                ) {
                                    22
                                } else {
                                    54
                                },
                            )
                        rightMargin =
                            leftMargin
                        topMargin =
                            dp(8)
                    },
                )

                findMatchButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "PLAY"
                        LudoProofTheme
                            .primary(
                                this,
                            )
                        textSize =
                            if (
                                isCompactOnline()
                            ) {
                                16f
                            } else {
                                18f
                            }
                        setOnClickListener {
                            beginPublicMatchmaking()
                        }
                    }
                addView(
                    findMatchButton,
                    LinearLayout.LayoutParams(
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                138
                            } else {
                                156
                            },
                        ),
                        dp(46),
                    ).apply {
                        gravity =
                            Gravity.CENTER_HORIZONTAL
                        topMargin =
                            dp(18)
                    },
                )
            }

        addView(
            matchmakingSetupPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        matchmakingSearchPanel =
            LinearLayout(
                this@onlineLobbyPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL
                visibility =
                    View.GONE
                setPadding(
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            14
                        } else {
                            20
                        },
                    ),
                    dp(18),
                    dp(
                        if (
                            isCompactOnline()
                        ) {
                            14
                        } else {
                            20
                        },
                    ),
                    dp(18),
                )
                background =
                    LudoProofTheme
                        .darkPanelDrawable(
                            this@onlineLobbyPanel,
                            goldBorder = true,
                        )

                val ownAvatar =
                    FrameLayout(
                        this@onlineLobbyPanel,
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
                                    dp(12)
                                        .toFloat()
                                setStroke(
                                    dp(2),
                                    0xFF5BE0FF.toInt(),
                                )
                            }
                        addView(
                            ProfilePlaceholderView(
                                this@onlineLobbyPanel,
                            ).apply {
                                setPadding(
                                    dp(10),
                                    dp(10),
                                    dp(10),
                                    dp(10),
                                )
                            },
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }
                addView(
                    ownAvatar,
                    LinearLayout.LayoutParams(
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                72
                            } else {
                                82
                            },
                        ),
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                72
                            } else {
                                82
                            },
                        ),
                    ),
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            profile.displayName
                        LudoProofTheme
                            .title(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    15f
                                } else {
                                    17f
                                },
                            )
                        gravity =
                            Gravity.CENTER
                        setPadding(
                            0,
                            dp(5),
                            0,
                            0,
                        )
                    },
                )

                addView(
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "VS"
                        textSize =
                            if (
                                isCompactOnline()
                            ) {
                                25f
                            } else {
                                29f
                            }
                        setTypeface(
                            Typeface.DEFAULT_BOLD,
                        )
                        setTextColor(
                            LudoProofTheme.GOLD,
                        )
                        gravity =
                            Gravity.CENTER
                        setPadding(
                            0,
                            dp(10),
                            0,
                            dp(8),
                        )
                    },
                )

                matchmakingOpponentRail =
                    MatchmakingOpponentRailView(
                        this@onlineLobbyPanel,
                    )
                addView(
                    matchmakingOpponentRail,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                118
                            } else {
                                132
                            },
                        ),
                    ),
                )

                matchmakingStatusText =
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "SEARCHING FOR PLAYERS…"
                        LudoProofTheme
                            .title(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    15f
                                } else {
                                    17f
                                },
                                gold = true,
                            )
                        gravity =
                            Gravity.CENTER
                        setPadding(
                            dp(6),
                            dp(12),
                            dp(6),
                            dp(4),
                        )
                    }
                addView(
                    matchmakingStatusText,
                )

                matchmakingTimerText =
                    TextView(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "SEARCH  00:00"
                        LudoProofTheme
                            .body(
                                this,
                                if (
                                    isCompactOnline()
                                ) {
                                    12f
                                } else {
                                    13f
                                },
                                centered = true,
                                bright = true,
                            )
                        setTextColor(
                            0xFF70E7FF.toInt(),
                        )
                    }
                addView(
                    matchmakingTimerText,
                )

                cancelMatchmakingButton =
                    Button(
                        this@onlineLobbyPanel,
                    ).apply {
                        text =
                            "CANCEL"
                        LudoProofTheme
                            .danger(
                                this,
                            )
                        setOnClickListener {
                            cancelPublicMatchmaking()
                        }
                    }
                addView(
                    cancelMatchmakingButton,
                    LinearLayout.LayoutParams(
                        dp(
                            if (
                                isCompactOnline()
                            ) {
                                132
                            } else {
                                148
                            },
                        ),
                        dp(44),
                    ).apply {
                        gravity =
                            Gravity.CENTER_HORIZONTAL
                        topMargin =
                            dp(13)
                    },
                )
            }

        addView(
            matchmakingSearchPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        matchmakingSlotsText =
            TextView(
                this@onlineLobbyPanel,
            ).apply {
                visibility =
                    View.GONE
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        addView(
            matchmakingSlotsText,
            LinearLayout.LayoutParams(
                1,
                1,
            ),
        )

        stylePublicPlayerCountButton(
            twoPlayerButton,
            selected = true,
        )
        stylePublicPlayerCountButton(
            fourPlayerButton,
            selected = false,
        )
    }

internal fun MainActivity.stylePublicPlayerCountButton(
    button: Button,
    selected: Boolean,
) {
    button.text =
        if (
            selected
        ) {
            "◉   " +
                if (
                    button ===
                    twoPlayerButton
                ) {
                    "2 PLAYERS"
                } else {
                    "4 PLAYERS"
                }
        } else {
            "○   " +
                if (
                    button ===
                    twoPlayerButton
                ) {
                    "2 PLAYERS"
                } else {
                    "4 PLAYERS"
                }
        }

    button.textSize =
        if (
            isCompactOnline()
        ) {
            13.5f
        } else {
            15f
        }
    button.setTypeface(
        Typeface.DEFAULT_BOLD,
    )
    button.setTextColor(
        if (
            selected
        ) {
            0xFFFFD45E.toInt()
        } else {
            Color.WHITE
        },
    )
    button.gravity =
        Gravity.CENTER_VERTICAL or
            Gravity.START
    button.background =
        GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (
                selected
            ) {
                intArrayOf(
                    0xEE0A3B7D.toInt(),
                    0xEE08265B.toInt(),
                )
            } else {
                intArrayOf(
                    0xCC071C49.toInt(),
                    0xCC051638.toInt(),
                )
            },
        ).apply {
            cornerRadius =
                dp(999)
                    .toFloat()
            setStroke(
                dp(
                    if (
                        selected
                    ) {
                        2
                    } else {
                        1
                    },
                ),
                if (
                    selected
                ) {
                    0xFFFFD45E.toInt()
                } else {
                    0x6653DFFF
                },
            )
        }
}

internal fun MainActivity.inputLabel(
    label: String,
):
    TextView =
    TextView(this).apply {
        text =
            label
        LudoProofTheme.body(
            this,
            10.5f,
            bright = true,
        )
        setTextColor(
            0xFF70E7FF.toInt(),
        )
        setPadding(
            dp(4),
            0,
            dp(4),
            dp(5),
        )
    }
