package com.ludoproof.game.ui.online

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.concurrent.Executors
import com.ludoproof.game.*
import com.ludoproof.game.feature.online.*
import com.ludoproof.game.feature.profile.data.local.ProfileStore

internal fun MainActivity.onlineLobbyPanel():
    LinearLayout =
    LudoProofTheme
        .panel(this)
        .apply {
            setPadding(
                dp(if (isCompactOnline()) 12 else 15),
                dp(if (isCompactOnline()) 12 else 15),
                dp(if (isCompactOnline()) 12 else 15),
                dp(if (isCompactOnline()) 12 else 15),
            )

            addView(
                TextView(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "QUICK ONLINE"
                    LudoProofTheme.title(
                        this,
                        if (isCompactOnline()) 21f else 24f,
                        gold = true,
                    )
                },
            )

            addView(
                TextView(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "Choose 2 or 4 players. LudoProof will find real players and start the match automatically."
                    LudoProofTheme.body(
                        this,
                        11f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(5),
                        dp(3),
                        dp(5),
                        dp(13),
                    )
                },
            )

            addView(
                inputLabel(
                    "PLAYER NAME",
                ),
            )

            nameInput =
                EditText(
                    this@onlineLobbyPanel,
                ).apply {
                    hint =
                        "Your display name"
                    setText(
                        ProfileStore(
                            this@onlineLobbyPanel,
                        )
                            .snapshot()
                            .displayName,
                    )
                    inputType =
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    LudoProofTheme.input(
                        this,
                    )
                    setSingleLine(
                        true,
                    )
                }
            addView(
                nameInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54),
                ).apply {
                    bottomMargin =
                        dp(12)
                },
            )

            addView(
                inputLabel(
                    "PLAYERS",
                ),
            )

            val playerCountRow =
                LinearLayout(
                    this@onlineLobbyPanel,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                }

            twoPlayerButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "✓  2 PLAYERS"
                    LudoProofTheme.positive(
                        this,
                    )
                    setOnClickListener {
                        selectPublicPlayerCount(
                            2,
                        )
                    }
                }

            fourPlayerButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "4 PLAYERS"
                    LudoProofTheme.secondary(
                        this,
                    )
                    setOnClickListener {
                        selectPublicPlayerCount(
                            4,
                        )
                    }
                }

            playerCountRow.addView(
                twoPlayerButton,
                LinearLayout.LayoutParams(
                    0,
                    dp(54),
                    1f,
                ).apply {
                    marginEnd =
                        dp(5)
                },
            )
            playerCountRow.addView(
                fourPlayerButton,
                LinearLayout.LayoutParams(
                    0,
                    dp(54),
                    1f,
                ).apply {
                    marginStart =
                        dp(5)
                },
            )
            addView(
                playerCountRow,
            )

            findMatchButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "FIND MATCH"
                    LudoProofTheme.primary(
                        this,
                    )
                    textSize =
                        if (isCompactOnline()) 17f else 19f
                    setOnClickListener {
                        beginPublicMatchmaking()
                    }
                }
            addView(
                findMatchButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(if (isCompactOnline()) 58 else 62),
                ).apply {
                    topMargin =
                        dp(12)
                },
            )

            matchmakingStatusText =
                TextView(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "SEARCHING FOR PLAYERS…"
                    visibility =
                        View.GONE
                    LudoProofTheme.title(
                        this,
                        if (isCompactOnline()) 16f else 18f,
                        gold = true,
                    )
                    setPadding(
                        dp(8),
                        dp(14),
                        dp(8),
                        dp(7),
                    )
                }
            addView(
                matchmakingStatusText,
            )

            cancelMatchmakingButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "CANCEL SEARCH"
                    visibility =
                        View.GONE
                    LudoProofTheme.danger(
                        this,
                    )
                    setOnClickListener {
                        cancelPublicMatchmaking()
                    }
                }
            addView(
                cancelMatchmakingButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52),
                ),
            )

            addView(
                TextView(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "PRIVATE MATCH CODE"
                    LudoProofTheme.body(
                        this,
                        10f,
                        centered = true,
                        bright = true,
                    )
                    setTextColor(
                        0xFF70E7FF.toInt(),
                    )
                    setPadding(
                        dp(4),
                        dp(20),
                        dp(4),
                        dp(7),
                    )
                },
            )

            matchInput =
                EditText(
                    this@onlineLobbyPanel,
                ).apply {
                    hint =
                        "LPXXXXXXXX"
                    setText(
                        matchId
                            .orEmpty(),
                    )
                    inputType =
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    LudoProofTheme.input(
                        this,
                    )
                    setSingleLine(
                        true,
                    )
                }
            addView(
                matchInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52),
                ),
            )

            val privateActions =
                LinearLayout(
                    this@onlineLobbyPanel,
                ).apply {
                    orientation =
                        if (isCompactOnline()) {
                            LinearLayout.VERTICAL
                        } else {
                            LinearLayout.HORIZONTAL
                        }
                }

            createButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "CREATE PRIVATE"
                    LudoProofTheme.secondary(
                        this,
                    )
                    setOnClickListener {
                        createMatch()
                    }
                }

            joinButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "JOIN CODE"
                    LudoProofTheme.secondary(
                        this,
                    )
                    setOnClickListener {
                        joinMatch()
                    }
                }

            if (isCompactOnline()) {
                privateActions.addView(
                    createButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50),
                    ).apply {
                        topMargin =
                            dp(9)
                    },
                )
                privateActions.addView(
                    joinButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50),
                    ).apply {
                        topMargin =
                            dp(7)
                    },
                )
            } else {
                privateActions.addView(
                    createButton,
                    LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1f,
                    ).apply {
                        setMargins(
                            0,
                            dp(9),
                            dp(5),
                            0,
                        )
                    },
                )
                privateActions.addView(
                    joinButton,
                    LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1f,
                    ).apply {
                        setMargins(
                            dp(5),
                            dp(9),
                            0,
                            0,
                        )
                    },
                )
            }

            addView(
                privateActions,
            )
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
