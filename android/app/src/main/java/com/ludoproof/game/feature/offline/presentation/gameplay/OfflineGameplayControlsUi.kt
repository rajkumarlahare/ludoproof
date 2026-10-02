package com.ludoproof.game.ui.offline.gameplay

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.ludoproof.game.ui.offline.setup.*

internal fun OfflineGameActivity.gameplayActionPanel():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(4),
            dp(2),
            dp(4),
            dp(4),
        )

        statusText =
            TextView(
                this@gameplayActionPanel,
            ).apply {
                text =
                    "Tap the dice beside the active player."
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 10f else 11f,
                    centered = true,
                )
                setPadding(
                    dp(4),
                    dp(3),
                    dp(4),
                    dp(5),
                )
            }
        addView(
            requireNotNull(
                statusText,
            ),
        )

        addView(
            gameplayTools(),
        )
    }

internal fun OfflineGameActivity.gameplayTools():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER

        fun addTool(
            label: String,
            positive: Boolean,
            action: () -> Unit,
        ) {
            addView(
                Button(
                    this@gameplayTools,
                ).apply {
                    text =
                        label
                    textSize =
                        if (isCompactSetup()) 9.5f else 10.5f
                    if (positive) {
                        LudoProofTheme
                            .positive(this)
                    } else {
                        LudoProofTheme
                            .secondary(this)
                    }
                    minHeight = 0
                    setPadding(
                        dp(4),
                        dp(4),
                        dp(4),
                        dp(4),
                    )
                    setOnClickListener {
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactSetup()) 42 else 46),
                    1f,
                ).apply {
                    setMargins(
                        dp(3),
                        0,
                        dp(3),
                        0,
                    )
                },
            )
        }

        addTool(
            "HISTORY",
            false,
        ) {
            ArcadeDialogs
                .showProofHistory(
                    this@gameplayTools,
                    "OFFLINE HISTORY",
                    offlineHistory(
                        engine.snapshot(),
                    ),
                )
        }
        addTool(
            "ENGINE MAP",
            false,
        ) {
            ArcadeDialogs
                .showNaturalWorldAudit(
                    this@gameplayTools,
                    engine
                        .lastRandomnessAudit(),
                )
        }
        addTool(
            "NEW GAME",
            true,
        ) {
            engine.clear()
            showSetup()
        }
    }

internal fun OfflineGameActivity.gameplaySectionParams(
    topMarginDp: Int,
) =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(topMarginDp)
    }

internal fun OfflineGameActivity.gameplayToolParams() =
    LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f,
    ).apply {
        setMargins(
            dp(5),
            dp(12),
            dp(5),
            0,
        )
    }

internal fun OfflineGameActivity.renderGame(
    state: MatchSnapshot,
) {
    val active =
        state.players
            .getOrNull(
                state.turnSeat,
            )
    val winner =
        state.players
            .find {
                it.playerId ==
                    state.winnerPlayerId
            }

    if (
        state.status ==
        "FINISHED"
    ) {
        val winnerName =
            winner
                ?.displayName
                ?: "Player"
        resultPanel.visibility =
            View.VISIBLE
        resultTitleText.text =
            "WINNER • " +
                winnerName
        resultSubtitleText.text =
            "Local v4 result • history and engine map remain available"
    } else {
        resultPanel.visibility =
            View.GONE
    }

    boardView?.bind(
        state,
        engine.activePlayerId(),
    )

    renderPlayerRails(
        state,
    )

    val pending =
        state.pendingRoll
    val latest =
        state.history
            .lastOrNull()
    val outcome =
        pending?.outcome
            ?: latest?.outcome

    if (
        outcome != null
    ) {
        diceView?.showOutcome(
            outcome,
        )
    }

    val canRoll =
        state.status ==
            "ACTIVE" &&
            pending ==
            null

    diceHost?.isEnabled =
        canRoll
    diceHost?.alpha =
        if (canRoll) {
            1f
        } else {
            .58f
        }

    when {
        state.status ==
            "FINISHED" ->
            showStatus(
                "Game complete • review history or start a new game.",
            )

        pending !=
            null ->
            showStatus(
                (
                    active
                        ?.displayName
                        ?: "Player"
                    ) +
                    " • dice " +
                    pending.outcome +
                    " • move a highlighted token",
            )

        else ->
            showStatus(
                (
                    active
                        ?.displayName
                        ?: "Player"
                    ) +
                    " turn • tap the dice beside the profile",
            )
    }
}
