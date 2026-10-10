package com.ludoproof.game.ui.offline.gameplay

import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.ludoproof.game.*
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.ui.quickchat.QuickChatButtonView
import com.ludoproof.game.ui.quickchat.showQuickChatPopup
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*

internal fun OfflineGameActivity.playerRail(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        minimumHeight =
            dp(
                if (isCompactSetup()) {
                    70
                } else {
                    78
                },
            )
    }

internal fun OfflineGameActivity.renderPlayerRails(
    state: MatchSnapshot,
) {
    val top = topPlayerRail ?: return
    val bottom = bottomPlayerRail ?: return

    diceView?.stopRolling()
    diceView = null
    diceHost = null

    top.removeAllViews()
    bottom.removeAllViews()

    val presentationBlocked =
        SystemClock.uptimeMillis() < gameplayActionBlockedUntilMillis
    val presentationEvent =
        state.history
            .lastOrNull()
            ?.takeIf {
                presentationBlocked
            }
    val presentationPlayerId = presentationEvent?.playerId
    val activePlayer =
        state.players
            .firstOrNull {
                it.playerId == presentationPlayerId
            }
            ?: state.players.getOrNull(state.turnSeat)

    addPlayerSlot(
        top,
        state,
        OfflinePlayerLayout.Slot.TOP_LEFT,
        alignEnd = false,
        activePlayer = activePlayer,
    )
    addPlayerSlot(
        top,
        state,
        OfflinePlayerLayout.Slot.TOP_RIGHT,
        alignEnd = true,
        activePlayer = activePlayer,
    )
    addPlayerSlot(
        bottom,
        state,
        OfflinePlayerLayout.Slot.BOTTOM_LEFT,
        alignEnd = false,
        activePlayer = activePlayer,
    )
    addPlayerSlot(
        bottom,
        state,
        OfflinePlayerLayout.Slot.BOTTOM_RIGHT,
        alignEnd = true,
        activePlayer = activePlayer,
    )

    // A committed move advances the authoritative turn immediately, but the
    // previous player's resolved dice face still belongs to the animation that
    // is on screen. Keep that face visible and non-interactive until the pawn
    // has fully finished moving/capturing; the next player's dice is rendered
    // only after the presentation block releases.
    val presentationOutcome =
        presentationEvent?.effectiveOutcome
            ?: presentationEvent?.outcome
    if (presentationOutcome != null) {
        diceView?.showOutcome(
            outcome = presentationOutcome,
            animate = false,
        )
    }
}

private fun OfflineGameActivity.addPlayerSlot(
    rail: LinearLayout,
    state: MatchSnapshot,
    slot: OfflinePlayerLayout.Slot,
    alignEnd: Boolean,
    activePlayer: PlayerSnapshot?,
) {
    val player =
        state.players
            .firstOrNull {
                OfflinePlayerLayout
                    .slotForColor(
                        color = it.color,
                        preferredBottomLeftColor =
                            state.players
                                .firstOrNull()
                                ?.color
                                ?: "BLUE",
                    ) == slot
            }

    val host =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity =
                (
                    if (alignEnd) {
                        Gravity.END
                    } else {
                        Gravity.START
                    }
                    ) or Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }

    if (player != null) {
        val active =
            player.playerId == activePlayer?.playerId &&
                state.status == "ACTIVE"
        val characterId =
            activeCharacterIdsBySeat
                .getOrNull(player.seat)

        if (alignEnd && active) {
            host.addView(
                activeDiceControl(player),
            )
        }

        val playerWidth =
            dp(
                if (isCompactSetup()) {
                    104
                } else {
                    116
                },
            )
        val computerPlayer = engine.isComputerPlayer(player.playerId)
        val playerCard =
            LudoPawsPlayerCardView(this).apply {
                bind(
                    player = player,
                    characterId = characterId,
                    active = active,
                    computer = computerPlayer,
                    compact = isCompactSetup(),
                    portraitOnEnd = alignEnd,
                )
            }
        val quickChatEnabled =
            GameSettingsStore(this).snapshot().quickChatEnabled
        val hasComputerOpponent =
            state.players.any { engine.isComputerPlayer(it.playerId) }
        val humanPlayerCount =
            state.players.count { !engine.isComputerPlayer(it.playerId) }
        // Quick Chat is a local human-vs-computer affordance, never a Pass & Play control.
        val showQuickChat =
            isComputerMode &&
                hasComputerOpponent &&
                humanPlayerCount == 1 &&
                !computerPlayer &&
                quickChatEnabled
        val quickChatButton =
            QuickChatButtonView(this).apply {
                visibility = if (showQuickChat) View.VISIBLE else View.GONE
                isEnabled = showQuickChat && state.status == "ACTIVE"
                alpha = if (isEnabled) 1f else .58f
                contentDescription = "Quick Chat for ${player.displayName}"
                setOnClickListener {
                    if (
                        state.status != "ACTIVE" ||
                        !showQuickChat ||
                        computerPlayer ||
                        !GameSettingsStore(this@addPlayerSlot).snapshot().quickChatEnabled
                    ) {
                        return@setOnClickListener
                    }
                    showQuickChatPopup(this) { emoji ->
                        presentQuickReaction(
                            emoji = emoji,
                            senderPlayerId = player.playerId,
                        )
                    }
                }
            }
        val profileColumn =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                clipChildren = false
                clipToPadding = false
                addView(
                    playerCard,
                    LinearLayout.LayoutParams(
                        playerWidth,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    quickChatButton,
                    LinearLayout.LayoutParams(
                        dp(36),
                        dp(36),
                    ).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        topMargin = dp(1)
                    },
                )
            }
        host.addView(
            profileColumn,
            LinearLayout.LayoutParams(
                playerWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        if (!alignEnd && active) {
            host.addView(
                activeDiceControl(player),
            )
        }
    }

    rail.addView(
        host,
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        ),
    )
}

private fun OfflineGameActivity.activeDiceControl(
    player: PlayerSnapshot,
): FrameLayout {
    val control =
        FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            // The dice itself is the visual control. Keep this host transparent so
            // no extra frame surrounds or clips the face/shadow.
            background = null
            elevation = 0f
            clipChildren = false
            clipToPadding = false
            setOnClickListener {
                rollOffline()
            }

            val newDice =
                DiceView(
                    this@activeDiceControl,
                ).apply {
                    setPlayerTeamColor(player.color)
                }
            diceView = newDice
            addView(
                newDice,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }
    diceHost = control

    val cpuTurn =
        engine.isComputerPlayer(
            player.playerId,
        )
    control.contentDescription =
        if (cpuTurn) {
            player.displayName + " is thinking"
        } else {
            "Roll dice for " + player.displayName
        }
    control.isEnabled =
        !cpuTurn &&
            SystemClock.uptimeMillis() >= gameplayActionBlockedUntilMillis
    control.alpha = 1f
    control.layoutParams =
        LinearLayout.LayoutParams(
            dp(
                if (isCompactSetup()) {
                    54
                } else {
                    58
                },
            ),
            dp(
                if (isCompactSetup()) {
                    54
                } else {
                    58
                },
            ),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            setMargins(dp(1), 0, dp(1), 0)
        }

    return control
}
