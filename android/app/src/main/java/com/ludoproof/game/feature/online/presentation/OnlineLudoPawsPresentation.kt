package com.ludoproof.game.feature.online

import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.ludoproof.game.LudoPawsPlayerCardView
import com.ludoproof.game.LudoPawsReactiveBoardView
import com.ludoproof.game.MainActivity
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.OfflinePlayerLayout
import com.ludoproof.game.PlayerSnapshot
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFeedbackLedger
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import com.ludoproof.game.feature.online.domain.OnlineLudoPawsCharacterPolicy
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.feature.settings.data.local.LudoPawsHaptics
import com.ludoproof.game.ui.quickchat.QuickChatButtonView
import com.ludoproof.game.ui.quickchat.animateQuickChatReaction
import com.ludoproof.game.ui.quickchat.showQuickChatPopup
import kotlin.math.roundToInt

/** Presentation bridge for the shared online Ludo Paws board. */
internal object OnlineLudoPawsPresentation {
    private data class Host(
        val board: LudoPawsReactiveBoardView,
        val topRail: LinearLayout,
        val bottomRail: LinearLayout,
        val feedbackLedger: LudoPawsFeedbackLedger = LudoPawsFeedbackLedger(),
        var quickChatReactionView: View? = null,
        var lastQuickChatAtMs: Long = 0L,
    )

    fun render(
        activity: MainActivity,
        previous: MatchSnapshot?,
        current: MatchSnapshot,
    ) {
        val host =
            host(activity)
                ?: createHost(activity).also {
                    activity.boardFrame.tag = it
                }

        val characterIdsBySeat =
            OnlineLudoPawsCharacterPolicy.characterIdsBySeat(current)
        val localColor =
            current.players.firstOrNull {
                it.playerId == activity.playerId
            }?.color

        activity.boardView.visibility = View.GONE
        host.board.visibility = View.VISIBLE
        host.board.bind(
            state = current,
            playerId = activity.playerId,
            perspectiveColor = localColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        renderRails(
            activity = activity,
            host = host,
            state = current,
            characterIdsBySeat = characterIdsBySeat,
            perspectiveColor = localColor,
        )
        dispatchFeedback(
            activity = activity,
            host = host,
            previous = previous,
            current = current,
            characterIdsBySeat = characterIdsBySeat,
        )
    }

    fun clear(activity: MainActivity) {
        val host = host(activity) ?: return
        host.feedbackLedger.clear()
        host.board.bind(
            state = null,
            playerId = null,
            characterIdsBySeat = emptyList(),
        )
        host.board.visibility = View.GONE
        host.topRail.visibility = View.GONE
        host.bottomRail.visibility = View.GONE
        runCatching {
            activity.boardFrame.removeView(host.board)
            activity.matchStatusPanel.removeView(host.topRail)
            activity.actionPanel.removeView(host.bottomRail)
        }
        activity.boardFrame.tag = null
        activity.boardView.visibility = View.VISIBLE
    }

    fun presentRemoteQuickChat(
        activity: MainActivity,
        senderPlayerId: String,
        displayName: String,
        emoji: String,
    ) {
        if (senderPlayerId == activity.playerId) return
        if (emoji !in com.ludoproof.game.ui.quickchat.QuickChatEmojiCatalog.EMOJIS) return
        if (!GameSettingsStore(activity).snapshot().quickChatEnabled) return
        val state = activity.currentState ?: return
        if (state.status != "ACTIVE") return
        if (state.players.none { it.playerId == senderPlayerId }) return
        val host = host(activity) ?: return
        host.quickChatReactionView =
            animateQuickChatReaction(
                parent = host.board,
                previous = host.quickChatReactionView,
                emoji = emoji,
                displayName = displayName,
            )
    }

    private fun host(activity: MainActivity): Host? =
        activity.boardFrame.tag as? Host

    private fun createHost(activity: MainActivity): Host {
        val reactiveBoard =
            LudoPawsReactiveBoardView(activity).apply {
                onTokenSelected = { tokenIndex ->
                    activity.moveToken(tokenIndex)
                }
            }
        activity.boardFrame.addView(
            reactiveBoard,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ),
        )

        val topRail = playerRail(activity)
        activity.matchStatusPanel.addView(
            topRail,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(activity, 4)
            },
        )

        val bottomRail = playerRail(activity)
        activity.actionPanel.addView(
            bottomRail,
            0,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(activity, 4)
            },
        )

        return Host(
            board = reactiveBoard,
            topRail = topRail,
            bottomRail = bottomRail,
        )
    }

    private fun playerRail(activity: MainActivity): LinearLayout =
        LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            minimumHeight = dp(activity, 62)
        }

    private fun renderRails(
        activity: MainActivity,
        host: Host,
        state: MatchSnapshot,
        characterIdsBySeat: List<String>,
        perspectiveColor: String?,
    ) {
        host.topRail.visibility = View.VISIBLE
        host.bottomRail.visibility = View.VISIBLE
        host.topRail.removeAllViews()
        host.bottomRail.removeAllViews()

        val activePlayer =
            state.players
                .getOrNull(state.actingSeat ?: state.turnSeat)
        val preferredBottomLeftColor =
            perspectiveColor
                ?: state.players.firstOrNull()?.color
                ?: "BLUE"

        addPlayerSlot(
            activity = activity,
            quickChatHost = host,
            rail = host.topRail,
            state = state,
            slot = OfflinePlayerLayout.Slot.TOP_LEFT,
            alignEnd = false,
            activePlayer = activePlayer,
            preferredBottomLeftColor = preferredBottomLeftColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        addPlayerSlot(
            activity = activity,
            quickChatHost = host,
            rail = host.topRail,
            state = state,
            slot = OfflinePlayerLayout.Slot.TOP_RIGHT,
            alignEnd = true,
            activePlayer = activePlayer,
            preferredBottomLeftColor = preferredBottomLeftColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        addPlayerSlot(
            activity = activity,
            quickChatHost = host,
            rail = host.bottomRail,
            state = state,
            slot = OfflinePlayerLayout.Slot.BOTTOM_LEFT,
            alignEnd = false,
            activePlayer = activePlayer,
            preferredBottomLeftColor = preferredBottomLeftColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        addPlayerSlot(
            activity = activity,
            quickChatHost = host,
            rail = host.bottomRail,
            state = state,
            slot = OfflinePlayerLayout.Slot.BOTTOM_RIGHT,
            alignEnd = true,
            activePlayer = activePlayer,
            preferredBottomLeftColor = preferredBottomLeftColor,
            characterIdsBySeat = characterIdsBySeat,
        )
    }

    private fun addPlayerSlot(
        activity: MainActivity,
        quickChatHost: Host,
        rail: LinearLayout,
        state: MatchSnapshot,
        slot: OfflinePlayerLayout.Slot,
        alignEnd: Boolean,
        activePlayer: PlayerSnapshot?,
        preferredBottomLeftColor: String,
        characterIdsBySeat: List<String>,
    ) {
        val player =
            state.players.firstOrNull {
                OfflinePlayerLayout.slotForColor(
                    color = it.color,
                    preferredBottomLeftColor = preferredBottomLeftColor,
                ) == slot
            }

        val slotHost =
            LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity =
                    (if (alignEnd) Gravity.END else Gravity.START) or
                        Gravity.CENTER_VERTICAL
                clipChildren = false
                clipToPadding = false
            }

        if (player != null) {
            val active =
                state.status == "ACTIVE" &&
                    player.playerId == activePlayer?.playerId
            val localPlayer = player.playerId == activity.playerId
            val playerWidth = dp(activity, 112)
            val playerCard =
                LudoPawsPlayerCardView(activity).apply {
                    bind(
                        player = player,
                        characterId = characterIdsBySeat.getOrNull(player.seat),
                        active = active,
                        computer = false,
                        compact = true,
                        localPlayer = localPlayer,
                        portraitOnEnd = alignEnd,
                    )
                }
            val quickChatEnabled =
                GameSettingsStore(activity).snapshot().quickChatEnabled
            val showQuickChat =
                quickChatEnabled && localPlayer && state.status == "ACTIVE"
            val quickChatButton =
                QuickChatButtonView(activity).apply {
                    // Never render disabled chat controls under remote player profiles.
                    visibility = if (showQuickChat) View.VISIBLE else View.GONE
                    isEnabled = showQuickChat
                    alpha = if (isEnabled) 1f else .58f
                    contentDescription = "Quick Chat for ${player.displayName}"
                    setOnClickListener {
                        if (
                            !localPlayer ||
                            GameSettingsStore(activity).snapshot().quickChatEnabled.not()
                        ) {
                            return@setOnClickListener
                        }
                        showQuickChatPopup(this) { emoji ->
                            val current = activity.currentState ?: return@showQuickChatPopup
                            if (current.status != "ACTIVE") return@showQuickChatPopup
                            val sender = current.players.firstOrNull {
                                it.playerId == player.playerId &&
                                    it.playerId == activity.playerId
                            } ?: return@showQuickChatPopup
                            val now = SystemClock.elapsedRealtime()
                            if (
                                quickChatHost.lastQuickChatAtMs != 0L &&
                                now - quickChatHost.lastQuickChatAtMs < 700L
                            ) {
                                return@showQuickChatPopup
                            }
                            if (!activity.realtimeClient.sendQuickChat(emoji)) {
                                activity.statusText.text = "Quick Chat needs a live connection."
                                return@showQuickChatPopup
                            }
                            quickChatHost.lastQuickChatAtMs = now
                            quickChatHost.quickChatReactionView =
                                animateQuickChatReaction(
                                    parent = quickChatHost.board,
                                    previous = quickChatHost.quickChatReactionView,
                                    emoji = emoji,
                                    displayName = sender.displayName,
                                )
                            GameSoundFeedback.click(activity)
                        }
                    }
                }
            val profileColumn =
                LinearLayout(activity).apply {
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
                            dp(activity, 36),
                            dp(activity, 36),
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                            topMargin = dp(activity, 1)
                        },
                    )
                }
            slotHost.addView(
                profileColumn,
                LinearLayout.LayoutParams(
                    playerWidth,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        rail.addView(
            slotHost,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
    }

    private fun dispatchFeedback(
        activity: MainActivity,
        host: Host,
        previous: MatchSnapshot?,
        current: MatchSnapshot,
        characterIdsBySeat: List<String>,
    ) {
        if (
            previous == null ||
            previous.matchId != current.matchId
        ) {
            return
        }

        val pending = current.pendingRoll
        val previousPending = previous.pendingRoll
        if (
            pending != null &&
            (
                previousPending == null ||
                    previousPending.eventIndex != pending.eventIndex
                ) &&
            host.feedbackLedger.once(
                matchId = current.matchId,
                key = "ROLL:${pending.eventIndex}",
            )
        ) {
            GameSoundFeedback.diceSettle(activity)
        }

        val reactions =
            host.feedbackLedger.filterReactions(
                LudoPawsReactionEngine.derive(
                    previous = previous,
                    current = current,
                ),
            )

        if (reactions.isNotEmpty()) {
            GameSoundFeedback.reaction(
                context = activity,
                reactions = reactions,
                characterIdsBySeat = characterIdsBySeat,
            )
            LudoPawsHaptics.reaction(
                context = activity,
                reactions = reactions,
            )
        }
    }

    private fun hasTokenMovement(
        previous: MatchSnapshot,
        current: MatchSnapshot,
    ): Boolean =
        current.players.any { player ->
            val before =
                previous.players.firstOrNull {
                    it.playerId == player.playerId
                } ?: return@any false
            before.tokens != player.tokens
        }

    private fun movementSeat(
        previous: MatchSnapshot,
        current: MatchSnapshot,
    ): Int? =
        current.players.firstOrNull { player ->
            val before =
                previous.players.firstOrNull {
                    it.playerId == player.playerId
                } ?: return@firstOrNull false
            player.tokens.indices.any { index ->
                val from = before.tokens.getOrNull(index) ?: -1
                val to = player.tokens.getOrNull(index) ?: -1
                to > from
            }
        }?.seat

    private fun dp(
        activity: MainActivity,
        value: Int,
    ): Int =
        (value * activity.resources.displayMetrics.density).roundToInt()
}
