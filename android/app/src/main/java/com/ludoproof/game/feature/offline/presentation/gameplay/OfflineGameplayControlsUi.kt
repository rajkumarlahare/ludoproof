package com.ludoproof.game.ui.offline.gameplay

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.*
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.feature.offline.presentation.feedback.OfflineFeedbackAction
import com.ludoproof.game.feature.offline.presentation.feedback.OfflineLudoPawsFeedbackDispatcher
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*
import com.ludoproof.game.ui.offline.state.resolveOfflineCharacterIds

internal fun OfflineGameActivity.gameplayActionPanel(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(2), dp(4), dp(4))

        statusText =
            TextView(this@gameplayActionPanel).apply {
                text = "Tap the dice beside the active player."
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 10f else 11f,
                    centered = true,
                )
                setPadding(dp(4), dp(3), dp(4), dp(5))
            }
        addView(requireNotNull(statusText))

        if (
            GameSettingsStore(this@gameplayActionPanel)
                .snapshot()
                .quickChatEnabled
        ) {
            addView(quickChatBar())
        }

        addView(gameplayTools())
    }

internal fun OfflineGameActivity.quickChatBar(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(2), dp(4), dp(2), dp(7))

        listOf("👍", "😄", "👏", "😮").forEach { emoji ->
            addView(
                Button(this@quickChatBar).apply {
                    text = emoji
                    textSize = if (isCompactSetup()) 18f else 20f
                    minWidth = 0
                    minHeight = 0
                    LudoProofTheme.secondary(this)
                    setOnClickListener {
                        GameSoundFeedback.click(this@quickChatBar)
                        val name =
                            engine.snapshot()
                                ?.players
                                ?.firstOrNull()
                                ?.displayName
                                ?: "Player"
                        showStatus("$name  $emoji")
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactSetup()) 42 else 46),
                    1f,
                ).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                },
            )
        }
    }

internal fun OfflineGameActivity.gameplayTools(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER

        fun addTool(
            label: String,
            positive: Boolean,
            action: () -> Unit,
        ) {
            addView(
                Button(this@gameplayTools).apply {
                    text = label
                    textSize = if (isCompactSetup()) 9.5f else 10.5f
                    if (positive) {
                        LudoProofTheme.positive(this)
                    } else {
                        LudoProofTheme.secondary(this)
                    }
                    minHeight = 0
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    setOnClickListener { action() }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactSetup()) 42 else 46),
                    1f,
                ).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                },
            )
        }

        addTool("HISTORY", false) {
            ArcadeDialogs.showProofHistory(
                this@gameplayTools,
                "OFFLINE HISTORY",
                offlineHistory(engine.snapshot()),
            )
        }
        addTool("ENGINE MAP", false) {
            ArcadeDialogs.showNaturalWorldAudit(
                this@gameplayTools,
                engine.lastRandomnessAudit(),
            )
        }
        addTool("NEW GAME", true) {
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
        topMargin = dp(topMarginDp)
    }

internal fun OfflineGameActivity.gameplayToolParams() =
    LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f,
    ).apply {
        setMargins(dp(5), dp(12), dp(5), 0)
    }

internal fun OfflineGameActivity.renderGame(
    state: MatchSnapshot,
) {
    val active = state.players.getOrNull(state.turnSeat)
    val winner =
        state.players.find {
            it.playerId == state.winnerPlayerId
        }

    if (state.status == "FINISHED") {
        val winnerName = winner?.displayName ?: "Player"
        resultPanel.visibility = View.VISIBLE
        resultTitleText.text = "WINNER • $winnerName"
        resultSubtitleText.text =
            if (
                isComputerMode &&
                winner?.playerId == engine.humanPlayerId()
            ) {
                "You brought all 4 paws home."
            } else {
                "All 4 paws reached home."
            }
    } else {
        resultPanel.visibility = View.GONE
    }

    val activePlayerId = active?.playerId
    val interactionPlayerId =
        if (
            isComputerMode &&
            engine.isComputerPlayer(activePlayerId)
        ) {
            null
        } else {
            activePlayerId
        }
    val characterIdsBySeat = resolveOfflineCharacterIds(state)

    boardView?.bind(
        state = state,
        playerId = interactionPlayerId,
        perspectiveColor = state.players.firstOrNull()?.color,
        characterIdsBySeat = characterIdsBySeat,
    )

    renderPlayerRails(state)

    val pending = state.pendingRoll
    val latest = state.history.lastOrNull()
    val outcome =
        pending?.outcome
            ?: latest?.effectiveOutcome
            ?: latest?.outcome

    if (outcome != null) {
        diceView?.showOutcome(outcome)
    }

    val computerTurn =
        isComputerMode &&
            engine.isComputerPlayer(activePlayerId)
    val canRoll =
        state.status == "ACTIVE" &&
            pending == null &&
            !computerTurn
    val activeHomeCount =
        active?.tokens
            ?.count {
                it == LudoPathEncoding.HOME_POSITION
            }
            ?: 0

    turnText?.text =
        when {
            state.status == "FINISHED" ->
                "MATCH COMPLETE"

            computerTurn ->
                (active?.displayName ?: "CPU") + " • CPU TURN"

            else ->
                (active?.displayName ?: "Player") + " • TURN"
        }

    infoText?.text =
        when {
            state.status == "FINISHED" ->
                (winner?.displayName ?: "Player") +
                    " wins • 4/4 home"

            pending != null -> {
                val legalCount =
                    pending.legalTokenIndexes.size
                val moveLabel =
                    if (legalCount == 1) {
                        "move"
                    } else {
                        "moves"
                    }
                "Dice ${pending.outcome} • $legalCount legal $moveLabel • $activeHomeCount/4 home"
            }

            computerTurn ->
                "Choosing the best move • $activeHomeCount/4 home"

            else ->
                "Roll the dice • $activeHomeCount/4 home"
        }

    diceHost?.isEnabled = canRoll
    // Disabled means non-interactive only. Never fade the dice itself: after a
    // resolved roll the player still needs to read that face while moving.
    diceHost?.alpha = 1f

    when {
        state.status == "FINISHED" ->
            showStatus("Game complete • review history or start a new game.")

        computerTurn && pending != null ->
            showStatus(
                (active?.displayName ?: "CPU") +
                    " • dice ${pending.outcome} • choosing a move…",
            )

        computerTurn ->
            showStatus((active?.displayName ?: "CPU") + " is thinking…")

        pending != null ->
            showStatus(
                (active?.displayName ?: "Player") +
                    " • dice ${pending.outcome} • move a highlighted token",
            )

        else ->
            showStatus(
                (active?.displayName ?: "Player") +
                    " turn • tap the dice beside the profile",
            )
    }

    scheduleSingleLegalHumanMove(
        state = state,
        activePlayerId = activePlayerId,
    )
    scheduleComputerTurnIfNeeded(state)
}

private fun OfflineGameActivity.scheduleSingleLegalHumanMove(
    state: MatchSnapshot,
    activePlayerId: String?,
) {
    if (state.status != "ACTIVE") return
    if (engine.isComputerPlayer(activePlayerId)) return

    val pending = state.pendingRoll ?: return
    val tokenIndex =
        LudoTurnAutomationPolicy.singleLegalTokenIndex(
            pending.legalTokenIndexes,
        ) ?: return

    val eventKey =
        "single-legal:${state.matchId}:${pending.eventIndex}:$tokenIndex"
    val host = diceHost ?: return
    if (host.tag == eventKey) return
    host.tag = eventKey

    showStatus(
        (state.players.getOrNull(state.turnSeat)?.displayName ?: "Player") +
            " • only one move • moving automatically…",
    )

    handler.postDelayed(
        {
            val latest = session.snapshot()
            val latestPending = latest?.pendingRoll
            val latestActive =
                latest?.players?.getOrNull(latest.turnSeat)
            val stillSameMove =
                latest != null &&
                    latest.status == "ACTIVE" &&
                    latestActive?.playerId == activePlayerId &&
                    !engine.isComputerPlayer(latestActive?.playerId) &&
                    latestPending?.eventIndex == pending.eventIndex &&
                    LudoTurnAutomationPolicy.singleLegalTokenIndex(
                        latestPending.legalTokenIndexes,
                    ) == tokenIndex

            if (!stillSameMove) {
                if (host.tag == eventKey) {
                    host.tag = null
                }
                return@postDelayed
            }

            runCatching {
                session.move(tokenIndex)
            }.onSuccess { next ->
                OfflineLudoPawsFeedbackDispatcher.committed(
                    context = this,
                    previous = latest,
                    current = next,
                    action = OfflineFeedbackAction.MOVE,
                )
                renderGame(next)
            }.onFailure { error ->
                if (host.tag == eventKey) {
                    host.tag = null
                }
                showStatus(
                    error.message ?: "Automatic move failed",
                )
            }
        },
        320L,
    )
}
