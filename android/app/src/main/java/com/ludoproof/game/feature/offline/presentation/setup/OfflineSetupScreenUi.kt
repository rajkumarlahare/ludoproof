package com.ludoproof.game.ui.offline.setup

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.gameplay.*

internal fun OfflineGameActivity.showSetup() {
    prepareOfflineUiTransition()

    val (root, host) =
        LudoProofTheme.arcadeRoot(this)

    val scroll =
        ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

    val contentHost =
        FrameLayout(this)
    scroll.addView(
        contentHost,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    val horizontalPaddingDp =
        LudoProofTheme
            .pageHorizontalPaddingDp(this)
    val availableWidth =
        (
            resources.displayMetrics.widthPixels -
                dp(horizontalPaddingDp * 2)
            ).coerceAtLeast(1)
    val contentWidth =
        minOf(
            availableWidth,
            dp(
                LudoProofTheme
                    .pageMaxContentWidthDp(this),
            ),
        )

    val content =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                0,
                dp(if (isCompactSetup()) 10 else 12),
                0,
                dp(24),
            )
        }
    contentHost.addView(
        content,
        FrameLayout.LayoutParams(
            contentWidth,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        ),
    )

    host.addView(
        scroll,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )

    content.addView(
        backHeader(
            if (isComputerMode) {
                "VS COMPUTER"
            } else {
                "PASS & PLAY"
            },
        ),
    )

    // Character identity is the main Ludo Paws choice, so it leads the setup.
    // Classic rules are the only local ruleset and no longer need a form panel.
    content.addView(
        characterPanel(),
        setupSectionParams(
            if (isCompactSetup()) 12 else 14,
        ),
    )

    content.addView(
        playersPanel(),
        setupSectionParams(
            if (isCompactSetup()) 10 else 12,
        ),
    )

    content.addView(
        colorPanel(),
        setupSectionParams(
            if (isCompactSetup()) 10 else 12,
        ),
    )

    refreshSetupSelections()

    content.addView(
        Button(this).apply {
            text = "PLAY"
            textSize = if (isCompactSetup()) 18f else 20f
            LudoProofTheme.primary(this)
            setOnClickListener {
                persistActiveCharacterSetup()
                showGame(
                    session.start(
                        playerCount = selectedPlayers,
                        preferredColor = selectedColor,
                    ),
                )
            }
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(if (isCompactSetup()) 58 else 62),
        ).apply {
            setMargins(
                if (isCompactSetup()) dp(10) else dp(42),
                dp(if (isCompactSetup()) 16 else 18),
                if (isCompactSetup()) dp(10) else dp(42),
                0,
            )
        },
    )

    setContentView(root)
}

internal fun OfflineGameActivity.setupHero(): FrameLayout =
    FrameLayout(this).apply {
        val heroHeight =
            dp(
                if (isCompactSetup()) {
                    162
                } else {
                    184
                },
            )

        addView(
            OfflineSetupArtView(
                this@setupHero,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )

        val copy =
            LinearLayout(
                this@setupHero,
            ).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(if (isCompactSetup()) 98 else 138),
                    dp(14),
                    dp(if (isCompactSetup()) 12 else 26),
                    dp(14),
                )
            }

        copy.addView(
            TextView(this@setupHero).apply {
                text = "LUDO PAWS"
                LudoProofTheme.title(
                    this,
                    if (isCompactSetup()) 23f else 28f,
                    gold = true,
                )
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            },
        )

        copy.addView(
            TextView(this@setupHero).apply {
                text =
                    if (isComputerMode) {
                        "Pick a paw and challenge the computer"
                    } else {
                        "Pick your paws and play together"
                    }
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 12f else 13f,
                    bright = true,
                )
                gravity = Gravity.START
                setPadding(0, dp(4), 0, 0)
            },
        )

        copy.addView(
            TextView(this@setupHero).apply {
                text = "Classic board • four paws race home"
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 10.5f else 11.5f,
                )
                gravity = Gravity.START
                setPadding(0, dp(3), 0, 0)
            },
        )

        addView(
            copy,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                heroHeight,
            ),
        )
    }

internal fun OfflineGameActivity.gameTypePanel(): LinearLayout =
    selectionPanel(
        "CLASSIC LUDO",
        "Four paws race from the yard to home.",
    ).apply {
        val row =
            LinearLayout(
                this@gameTypePanel,
            ).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

        row.addView(
            choiceTile(
                "▦",
                "CLASSIC",
                "Standard Ludo rules",
                true,
                true,
            ) {},
            setupTileParams(
                if (isCompactSetup()) {
                    118
                } else {
                    128
                },
                marginDp = 4,
            ),
        )
        addView(row)

        addView(
            TextView(
                this@gameTypePanel,
            ).apply {
                text = "Board and dice looks can be changed in Settings."
                LudoProofTheme.body(
                    this,
                    10.5f,
                    centered = true,
                    bright = true,
                )
                setPadding(dp(8), dp(10), dp(8), 0)
            },
        )
    }
