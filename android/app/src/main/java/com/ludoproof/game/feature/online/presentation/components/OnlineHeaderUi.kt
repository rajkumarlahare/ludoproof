package com.ludoproof.game.ui.online

import android.app.AlertDialog
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import com.ludoproof.game.*
import com.ludoproof.game.feature.online.*

internal fun MainActivity.onlineTopBar(): LinearLayout =
    LinearLayout(this).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) {
                requestRemoteExit()
            }
        }

        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        addView(
            LinearLayout(this@onlineTopBar).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), 0, dp(8), 0)

                addView(
                    TextView(this@onlineTopBar).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "TEAM UP • 2 VS 2"
                            } else {
                                "ONLINE MATCH"
                            }
                        LudoProofTheme.title(
                            this,
                            if (isCompactOnline()) 17f else 19f,
                        )
                    },
                )

                connectionText =
                    TextView(this@onlineTopBar).apply {
                        text = "● CHECKING"
                        LudoProofTheme.body(
                            this,
                            10.5f,
                            centered = true,
                            bright = true,
                        )
                        setPadding(dp(10), dp(3), dp(10), dp(3))
                        background =
                            LudoProofTheme.rounded(
                                0xCC071C49.toInt(),
                                999f,
                                0x6647D7FF,
                                1f,
                                this@onlineTopBar,
                            )
                        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                    }
                addView(connectionText)
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

    }

internal fun MainActivity.requestRemoteExit() {
    val activeTeamMatch =
        gameMode == GameMode.TEAM_UP &&
            currentState?.status == "ACTIVE"

    if (!activeTeamMatch) {
        abandonRemoteSessionState()
        finish()
        return
    }

    AlertDialog.Builder(this)
        .setTitle("Leave Team Up?")
        .setMessage(
            "Leaving an active 2 vs 2 match forfeits your whole team. " +
                "Cancel to stay in the match.",
        )
        .setNegativeButton("CANCEL", null)
        .setPositiveButton("EXIT TEAM") { _, _ ->
            abandonRemoteSessionState()
            finish()
        }
        .setCancelable(false)
        .show()
}

internal fun MainActivity.onlineHero(): FrameLayout =
    FrameLayout(this).apply {
        val heroHeight = dp(if (isCompactOnline()) 160 else 182)

        addView(
            OnlineLobbyArtView(this@onlineHero),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )

        val copy =
            LinearLayout(this@onlineHero).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactOnline()) 104 else 146),
                    dp(14),
                    dp(if (isCompactOnline()) 12 else 24),
                    dp(14),
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "VERIFIED TEAM UP"
                            } else {
                                "VERIFIED ONLINE"
                            }
                        LudoProofTheme.title(
                            this,
                            if (isCompactOnline()) 21f else 27f,
                            gold = true,
                        )
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    },
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "Red + Yellow vs Green + Blue"
                            } else {
                                "Create a room or join with a match code"
                            }
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 11f else 12.5f,
                            bright = true,
                        )
                        gravity = Gravity.START
                        setPadding(0, dp(4), 0, 0)
                    },
                )

                addView(
                    TextView(this@onlineHero).apply {
                        text =
                            if (gameMode == GameMode.TEAM_UP) {
                                "Remote EntroNex authority • exit forfeits your team"
                            } else {
                                "Remote EntroNex authority • exit abandons this match"
                            }
                        LudoProofTheme.body(
                            this,
                            if (isCompactOnline()) 9.5f else 10.5f,
                        )
                        gravity = Gravity.START
                        setPadding(0, dp(3), 0, 0)
                    },
                )
            }

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )
    }
