package com.ludoproof.game.feature.mode.presentation

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.GameMode
import com.ludoproof.game.GameModeIntent
import com.ludoproof.game.LudoProofTheme

class RemoteModeEntryActivity : Activity() {
    private val gameMode:
        GameMode by lazy {
        GameMode
            .fromWireValue(
                intent.getStringExtra(
                    GameModeIntent.EXTRA_GAME_MODE,
                ),
            )
            ?.takeIf {
                it ==
                    GameMode.TEAM_UP ||
                    it ==
                    GameMode.FRIENDS
            }
            ?: GameMode.FRIENDS
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme.configureWindow(
            this,
        )

        val (root, host) =
            LudoProofTheme.arcadeRoot(
                this,
            )

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(28),
                )
            }

        content.addView(
            Button(this).apply {
                text =
                    "‹  BACK"
                LudoProofTheme.secondary(
                    this,
                )
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50),
            ),
        )

        content.addView(
            TextView(this).apply {
                text =
                    if (
                        gameMode ==
                        GameMode.TEAM_UP
                    ) {
                        "TEAM UP"
                    } else {
                        "PLAY WITH FRIENDS"
                    }
                LudoProofTheme.title(
                    this,
                    30f,
                    gold = true,
                )
                gravity =
                    Gravity.CENTER
                setPadding(
                    dp(8),
                    dp(30),
                    dp(8),
                    dp(8),
                )
            },
        )

        content.addView(
            TextView(this).apply {
                text =
                    if (
                        gameMode ==
                        GameMode.TEAM_UP
                    ) {
                        "2 VS 2 ONLINE"
                    } else {
                        "PRIVATE ONLINE ROOMS"
                    }
                LudoProofTheme.body(
                    this,
                    15f,
                    centered = true,
                    bright = true,
                )
                setTextColor(
                    0xFF69E8FF.toInt(),
                )
            },
        )

        content.addView(
            modePanel(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(24)
            },
        )

        content.addView(
            TextView(this).apply {
                text =
                    if (
                        gameMode ==
                        GameMode.TEAM_UP
                    ) {
                        "Team matchmaking is not enabled yet. This screen is intentionally non-playable until server-side teams, teammate ownership and team win conditions are active."
                    } else {
                        "Private friend rooms are not enabled yet. This screen is intentionally non-playable until friend identity, presence and secure room invites are active."
                    }
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(16),
                    dp(22),
                    dp(16),
                    dp(16),
                )
            },
        )

        scroll.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        setContentView(
            root,
        )
    }

    private fun modePanel():
        LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18),
            )
            background =
                LudoProofTheme.hudPanelDrawable(
                    this@RemoteModeEntryActivity,
                    goldBorder = true,
                )

            val lines =
                if (
                    gameMode ==
                    GameMode.TEAM_UP
                ) {
                    listOf(
                        "4 PLAYERS",
                        "2 VS 2 PARTNERS",
                        "CLASSIC V1",
                        "SERVER-AUTHORITATIVE",
                    )
                } else {
                    listOf(
                        "2 / 3 / 4 PLAYERS",
                        "PRIVATE ROOM",
                        "INVITE & SHARE",
                        "CLASSIC V1",
                    )
                }

            lines.forEach {
                    value ->
                addView(
                    TextView(
                        this@RemoteModeEntryActivity,
                    ).apply {
                        text =
                            value
                        LudoProofTheme.body(
                            this,
                            14f,
                            centered = true,
                            bright = true,
                        )
                        setPadding(
                            dp(8),
                            dp(8),
                            dp(8),
                            dp(8),
                        )
                    },
                )
            }
        }

    private fun dp(
        value: Int,
    ): Int =
        (
            value *
                resources
                    .displayMetrics
                    .density
            )
            .toInt()
}
