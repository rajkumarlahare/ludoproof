package com.ludoproof.game.ui.offline.gameplay

import android.os.SystemClock
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
        gravity = Gravity.CENTER
        setPadding(dp(4), 0, dp(4), 0)

        statusText =
            TextView(this@gameplayActionPanel).apply {
                text = "Tap the dice."
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 9.5f else 10.5f,
                    centered = true,
                    bright = true,
                )
                maxLines = 1
                setPadding(dp(4), dp(2), dp(4), dp(3))
            }
        addView(requireNotNull(statusText))

        if (
            GameSettingsStore(this@gameplayActionPanel)
                .snapshot()
                .quickChatEnabled
        ) {
            addView(quickChatBar())
        }
    }

internal fun OfflineGameActivity.quickChatBar(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(2), dp(1), dp(2), dp(2))

        listOf("👍", "😄", "👏", "😮").forEach { emoji ->
            addView(
                Button(this@quickChatBar).apply {
                    text = emoji
                    textSize = if (isCompactSetup()) 16f else 18f
                    minWidth = 0
                    minHeight = 0
                    LudoProofTheme.secondary(this)
                    setPadding(0, 0, 0, 0)
                    setOnClickListener {
                        presentQuickReaction(emoji)
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactSetup()) 34 else 38),
                    1f,
                ).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                },
            )
        }
    }

private const val OFFLINE_RESULT_ACTIONS_TAG =
    "ludo_paws_offline_result_actions"

private fun OfflineGameActivity.ensureOfflineResultActions(
    state: MatchSnapshot,
) {
    val parent =
        resultPanel.parent as? LinearLayout
            ?: return
    val existing =
        parent.findViewWithTag<View>(
            OFFLINE_RESULT_ACTIONS_TAG,
        )

    if (state.status != "FINISHED") {
        existing?.visibility = View.GONE
        return
    }

    if (existing != null) {
        existing.visibility = View.VISIBLE
        return
    }

    val actions =
        LinearLayout(this).apply {
            tag = OFFLINE_RESULT_ACTIONS_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), 0, dp(4), 0)

            addView(
                Button(this@ensureOfflineResultActions).apply {
                    text = "REMATCH"
                    textSize = if (isCompactSetup()) 11f else 12f
                    minHeight = 0
                    LudoProofTheme.positive(this)
                    setOnClickListener {
                        GameSoundFeedback.click(
                            this@ensureOfflineResultActions,
                        )
                        startOfflineRematch()
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactSetup()) 44 else 48),
                    1f,
                ).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                },
            )

            addView(
                Button(this@ensureOfflineResultActions).apply {
                    text = "CHANGE SETUP"
                    textSize = if (isCompactSetup()) 10f else 11f
                    minHeight = 0
                    LudoProofTheme.secondary(this)
                    setOnClickListener {
                        GameSoundFeedback.click(
                            this@ensureOfflineResultActions,
                        )
                        session.clear()
                        showSetup()
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompactSetup()) 44 else 48),
                    1f,
                ).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                },
            )
        }

    parent.addView(
        actions,
        parent.indexOfChild(resultPanel) + 1,
        gameplaySectionParams(
            if (isCompactSetup()) 4 else 6,
        ).apply {
            leftMargin = dp(8)
            rightMargin = dp(8)
        },
    )
}

private fun OfflineGameActivity.startOfflineRematch() {
    val finished =
        session.snapshot()
            ?: return
    val spec =
        OfflineRematchPolicy
            .fromFinishedState(finished)
            ?: run {
                showStatus("Rematch is available after the match ends.")
                return
            }
    val characterIds =
        resolveOfflineCharacterIds(finished)

    selectedPlayers = spec.playerCount
    selectedColor = spec.preferredColor
    selectedCharacterSlot =
        if (isComputerMode) {
            0
        } else {
            selectedCharacterSlot.coerceIn(
                0,
                spec.playerCount - 1,
            )
        }
    selectedCharacterIds = characterIds
    persistActiveCharacterSetup()

    showGame(
        session.start(
            playerCount = spec.playerCount,
            preferredColor = spec.preferredColor,
        ),
    )
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

internal fun OfflineGameActivity.renderGame(
    state: MatchSnapshot,
    presentationDelayMillis: Long = 0L,
) {
    if (presentationDelayMillis > 0L) {
        gameplayActionBlockedUntilMillis =
            maxOf(
                gameplayActionBlockedUntilMillis,
                SystemClock.uptimeMillis() + presentationDelayMillis,
            )
    }
    val blockedRemainingMillis =
        (gameplayActionBlockedUntilMillis - SystemClock.uptimeMillis())
            .coerceAtLeast(0L)

    val active = state.players.getOrNull(state.turnSeat)
    val winner =
        state.players.find {
            it.playerId == state.winnerPlayerId
        }

    if (state.status == "FINISHED") {
        val winnerName = winner?.displayName ?: "Player"
        resultPanel.visibility = View.VISIBLE
        resultTitleText.text = "${winnerName.uppercase()} WINS"
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
    ensureOfflineResultActions(state)

    val activePlayerId = active?.playerId
    val interactionPlayerId =
        if (
            blockedRemainingMillis > 0L ||
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
    val latestForActive =
        latest?.takeIf {
            it.playerId == activePlayerId
        }
    val presentedEventIndex =
        pending?.eventIndex
            ?: latestForActive?.eventIndex
    val outcome =
        pending?.outcome
            ?: latestForActive?.effectiveOutcome
            ?: latestForActive?.outcome

    if (
        outcome != null &&
        presentedEventIndex != null
    ) {
        val eventKey =
            "${state.matchId}:$presentedEventIndex"
        val shouldAnimate =
            presentedDiceEventKey != eventKey
        diceView?.showOutcome(
            outcome = outcome,
            animate = shouldAnimate,
        )
        presentedDiceEventKey = eventKey
    } else {
        presentedDiceEventKey = null
    }

    val computerTurn =
        isComputerMode &&
            engine.isComputerPlayer(activePlayerId)
    val canRoll =
        state.status == "ACTIVE" &&
            pending == null &&
            !computerTurn &&
            blockedRemainingMillis == 0L
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
                (active?.displayName ?: "CPU") + " • THINKING"

            isComputerMode && activePlayerId == engine.humanPlayerId() ->
                "YOUR TURN"

            else ->
                (active?.displayName ?: "Player") + " • TURN"
        }

    infoText?.text =
        when {
            state.status == "FINISHED" ->
                "4/4 PAWS HOME"

            pending != null -> {
                val legalCount = pending.legalTokenIndexes.size
                val choiceLabel =
                    if (legalCount == 1) "CHOICE" else "CHOICES"
                "DICE ${pending.outcome} • $legalCount $choiceLabel • $activeHomeCount/4 HOME"
            }

            else ->
                "$activeHomeCount/4 PAWS HOME"
        }

    diceHost?.isEnabled = canRoll
    // Disabled means non-interactive only. Never fade the dice itself: after a
    // resolved roll the player still needs to read that face while moving.
    diceHost?.alpha = 1f

    when {
        blockedRemainingMillis > 0L ->
            showStatus("Finishing the current paw action…")

        state.status == "FINISHED" ->
            showStatus("Rematch or change setup.")

        computerTurn && pending != null ->
            showStatus("Computer is choosing a paw…")

        computerTurn ->
            showStatus("Computer is rolling…")

        pending != null ->
            showStatus("Choose a glowing paw.")

        else ->
            showStatus("Tap the dice beside the active character.")
    }

    if (blockedRemainingMillis > 0L) {
        schedulePresentationUnlock(
            state = state,
            delayMillis = blockedRemainingMillis,
        )
        return
    }

    scheduleSingleLegalHumanMove(
        state = state,
        activePlayerId = activePlayerId,
    )
    scheduleComputerTurnIfNeeded(state)
}

internal fun OfflineGameActivity.renderCommittedMove(
    previous: MatchSnapshot?,
    current: MatchSnapshot,
) {
    val speed =
        GameSettingsStore(this)
            .snapshot()
            .gameSpeed
    val motions =
        LudoPawsPawnAnimationPolicy.plans(
            previous = previous,
            current = current,
        )
    val captured =
        motions.any {
            it.kind == LudoPawsPawnMotionKind.CAPTURE_RETURN
        }
    val delay =
        LudoPawsGameplayPacingPolicy
            .interactionDelayAfterMoveMillis(
                previous = previous,
                current = current,
                speed = speed,
            )

    renderGame(
        state = current,
        presentationDelayMillis = delay,
    )
    showStatus(
        if (captured) {
            "Capture • contact, push and return…"
        } else {
            "Paw is moving…"
        },
    )
}

private fun OfflineGameActivity.schedulePresentationUnlock(
    state: MatchSnapshot,
    delayMillis: Long,
) {
    val matchId = state.matchId
    val turnSeat = state.turnSeat
    val pendingEventIndex = state.pendingRoll?.eventIndex
    val latestEventIndex = state.history.lastOrNull()?.eventIndex

    handler.postDelayed(
        {
            val latest = session.snapshot() ?: return@postDelayed
            val stillSamePresentation =
                latest.matchId == matchId &&
                    latest.turnSeat == turnSeat &&
                    latest.pendingRoll?.eventIndex == pendingEventIndex &&
                    latest.history.lastOrNull()?.eventIndex == latestEventIndex
            if (!stillSamePresentation) return@postDelayed

            if (
                SystemClock.uptimeMillis() <
                gameplayActionBlockedUntilMillis
            ) {
                renderGame(latest)
                return@postDelayed
            }

            gameplayActionBlockedUntilMillis = 0L
            renderGame(latest)
        },
        delayMillis.coerceAtLeast(1L),
    )
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

    showStatus("Only one move • holding the dice result…")

    val speed =
        GameSettingsStore(this)
            .snapshot()
            .gameSpeed
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
                renderCommittedMove(
                    previous = latest,
                    current = next,
                )
            }.onFailure { error ->
                if (host.tag == eventKey) {
                    host.tag = null
                }
                showStatus(
                    error.message ?: "Automatic move failed",
                )
            }
        },
        LudoPawsGameplayPacingPolicy
            .postRollAutoMoveDelayMillis(speed),
    )
}
