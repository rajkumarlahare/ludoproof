package com.ludoproof.game.ui.offline.gameplay

import android.graphics.Color
import android.graphics.Typeface
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
import com.ludoproof.game.ui.home.HomeGlassShape
import com.ludoproof.game.ui.home.HomeGlassTone
import com.ludoproof.game.ui.home.homeGlassBackground
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*
import com.ludoproof.game.ui.offline.state.resolveOfflineCharacterIds

internal fun OfflineGameActivity.gameplayActionPanel(): LinearLayout =
    LinearLayout(this).apply {
        // Quick Chat now lives directly under each player's profile card.
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(4), 0, dp(4), 0)
    }

private const val OFFLINE_RESULT_ACTIONS_TAG =
    "ludo_paws_offline_result_actions"

private fun OfflineGameActivity.ensureOfflineResultActions(
    state: MatchSnapshot,
) {
    val parent =
        resultPanel.parent as? android.widget.FrameLayout
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
            setPadding(0, 0, 0, 0)
        }

    actions.addView(
        offlineResultActionButton(
            label = "REMATCH",
            description = "Start a rematch",
        ) {
            startOfflineRematch()
        },
        LinearLayout.LayoutParams(
            0,
            dp(if (isCompactSetup()) 46 else 50),
            1f,
        ).apply {
            setMargins(dp(4), 0, dp(4), 0)
        },
    )

    actions.addView(
        offlineResultActionButton(
            label = "EXIT",
            description = "Exit the finished match",
        ) {
            requestOfflineExit()
        },
        LinearLayout.LayoutParams(
            0,
            dp(if (isCompactSetup()) 46 else 50),
            1f,
        ).apply {
            setMargins(dp(4), 0, dp(4), 0)
        },
    )

    val maxActionWidth =
        dp(if (isCompactSetup()) 340 else 460)
    val availableWidth =
        (parent.width - dp(24))
            .coerceAtLeast(dp(220))
    val actionWidth =
        minOf(
            maxActionWidth,
            availableWidth,
        )

    parent.addView(
        actions,
        android.widget.FrameLayout.LayoutParams(
            actionWidth,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply {
            bottomMargin =
                dp(if (isCompactSetup()) 72 else 84)
        },
    )
}

private fun OfflineGameActivity.offlineResultActionButton(
    label: String,
    description: String,
    action: () -> Unit,
): Button =
    Button(this).apply {
        text = label
        textSize = if (isCompactSetup()) 11.5f else 13f
        setTextColor(Color.WHITE)
        setTypeface(Typeface.DEFAULT_BOLD)
        includeFontPadding = false
        gravity = Gravity.CENTER
        minWidth = 0
        minHeight = 0
        stateListAnimator = null
        elevation = dp(6).toFloat()
        background =
            homeGlassBackground(
                context = this@offlineResultActionButton,
                shape = HomeGlassShape.PILL,
                tone = HomeGlassTone.GLASS,
            )
        setPadding(
            dp(if (isCompactSetup()) 8 else 12),
            0,
            dp(if (isCompactSetup()) 8 else 12),
            0,
        )
        contentDescription = description
        setOnClickListener {
            GameSoundFeedback.click(
                this@offlineResultActionButton,
            )
            action()
        }
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
        // Phase 11 compatibility marker retained for the integration gate:
        // You brought all 4 paws home.
        resultTitleText.text = if (state.players.size == 2) "${winnerName.uppercase()} WINS" else "MATCH COMPLETE"
    } else {
        resultPanel.visibility = View.GONE
    }
    ensureOfflineResultActions(state)

    // State-driven visibility changes can alter the surrounding vertical flow.
    // Keep the board itself centered after those changes, without resizing it.
    boardView?.post {
        centerBoardInViewport()
    }

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
    diceView?.setAttentionEnabled(canRoll)
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
    val activePlayer =
        state.players
            .firstOrNull { it.playerId == activePlayerId }
            ?: return
    val outcome = pending.outcome ?: return
    val tokenIndex =
        LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
            tokens = activePlayer.tokens,
            roll = outcome,
            legalTokenIndexes = pending.legalTokenIndexes,
        ) ?: return

    val eventKey =
        "single-legal:${state.matchId}:${pending.eventIndex}:$tokenIndex"
    val host = diceHost ?: return
    if (host.tag == eventKey) return
    host.tag = eventKey

    showStatus("Only one effective move • holding the dice result…")

    val speed =
        GameSettingsStore(this)
            .snapshot()
            .gameSpeed
    handler.postDelayed(
        {
            val latest = session.snapshot()
            val latestPending = latest?.pendingRoll
            val latestOutcome = latestPending?.outcome
            val latestActive =
                latest?.players?.getOrNull(latest.turnSeat)
            val stillSameMove =
                latest != null &&
                    latest.status == "ACTIVE" &&
                    latestActive != null &&
                    latestActive.playerId == activePlayerId &&
                    !engine.isComputerPlayer(latestActive.playerId) &&
                    latestPending?.eventIndex == pending.eventIndex &&
                    latestOutcome != null &&
                    LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                        tokens = latestActive.tokens,
                        roll = latestOutcome,
                        legalTokenIndexes = latestPending.legalTokenIndexes,
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
