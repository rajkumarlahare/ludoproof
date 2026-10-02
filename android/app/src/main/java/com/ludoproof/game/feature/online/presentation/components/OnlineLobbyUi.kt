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
                        "CREATE OR JOIN"
                    LudoProofTheme.title(
                        this,
                        if (isCompactOnline()) 19f else 21f,
                    )
                },
            )

            addView(
                TextView(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "Use your name, then create a new match or enter an existing code."
                    LudoProofTheme.body(
                        this,
                        11f,
                        centered = true,
                    )
                    setPadding(
                        dp(4),
                        dp(3),
                        dp(4),
                        dp(12),
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
                        "Player",
                    )
                    inputType =
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_FLAG_CAP_WORDS
                    LudoProofTheme
                        .input(this)
                    setSingleLine(true)
                }
            addView(
                nameInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54),
                ).apply {
                    bottomMargin =
                        dp(11)
                },
            )

            addView(
                inputLabel(
                    "MATCH CODE",
                ),
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
                    LudoProofTheme
                        .input(this)
                    setSingleLine(true)
                }
            addView(
                matchInput,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54),
                ),
            )

            val actions =
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
                        "＋  CREATE MATCH"
                    LudoProofTheme
                        .positive(this)
                    textSize =
                        if (isCompactOnline()) 16f else 17f
                    setOnClickListener {
                        createMatch()
                    }
                }

            joinButton =
                Button(
                    this@onlineLobbyPanel,
                ).apply {
                    text =
                        "⇥  JOIN MATCH"
                    LudoProofTheme
                        .primary(this)
                    textSize =
                        if (isCompactOnline()) 16f else 17f
                    setOnClickListener {
                        joinMatch()
                    }
                }

            if (isCompactOnline()) {
                actions.addView(
                    createButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(56),
                    ).apply {
                        topMargin =
                            dp(13)
                    },
                )
                actions.addView(
                    joinButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(56),
                    ).apply {
                        topMargin =
                            dp(9)
                    },
                )
            } else {
                actions.addView(
                    createButton,
                    LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1f,
                    ).apply {
                        setMargins(
                            0,
                            dp(13),
                            dp(6),
                            0,
                        )
                    },
                )
                actions.addView(
                    joinButton,
                    LinearLayout.LayoutParams(
                        0,
                        dp(60),
                        1f,
                    ).apply {
                        setMargins(
                            dp(6),
                            dp(13),
                            0,
                            0,
                        )
                    },
                )
            }

            addView(actions)
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
