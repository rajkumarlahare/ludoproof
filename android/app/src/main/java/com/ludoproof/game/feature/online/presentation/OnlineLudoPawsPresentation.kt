package com.ludoproof.game.feature.online

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.ludoproof.game.LudoPawsPlayerCardView
import com.ludoproof.game.LudoPawsReactiveBoardView
import com.ludoproof.game.MainActivity
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFeedbackLedger
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import com.ludoproof.game.feature.online.domain.OnlineLudoPawsCharacterPolicy
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.feature.settings.data.local.LudoPawsHaptics
import kotlin.math.roundToInt

/** Presentation bridge for the shared online Ludo Paws board. */
internal object OnlineLudoPawsPresentation {
    private data class Host(
        val board: LudoPawsReactiveBoardView,
        val railScroll: HorizontalScrollView,
        val rail: LinearLayout,
        val feedbackLedger: LudoPawsFeedbackLedger = LudoPawsFeedbackLedger(),
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
        renderRail(
            activity = activity,
            host = host,
            state = current,
            characterIdsBySeat = characterIdsBySeat,
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
        host.railScroll.visibility = View.GONE
        runCatching {
            activity.boardFrame.removeView(host.board)
            activity.matchStatusPanel.removeView(host.railScroll)
        }
        activity.boardFrame.tag = null
        activity.boardView.visibility = View.VISIBLE
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

        val rail =
            LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(activity, 2),
                    dp(activity, 2),
                    dp(activity, 2),
                    dp(activity, 2),
                )
            }
        val railScroll =
            HorizontalScrollView(activity).apply {
                isHorizontalScrollBarEnabled = false
                isFillViewport = true
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(
                    rail,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        activity.matchStatusPanel.addView(
            railScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(activity, 8)
            },
        )

        return Host(
            board = reactiveBoard,
            railScroll = railScroll,
            rail = rail,
        )
    }

    private fun renderRail(
        activity: MainActivity,
        host: Host,
        state: MatchSnapshot,
        characterIdsBySeat: List<String>,
    ) {
        host.railScroll.visibility = View.VISIBLE
        host.rail.removeAllViews()
        val activePlayerId =
            state.players
                .getOrNull(state.actingSeat ?: state.turnSeat)
                ?.playerId

        state.players
            .sortedBy { it.seat }
            .forEach { player ->
                val local = player.playerId == activity.playerId
                host.rail.addView(
                    LudoPawsPlayerCardView(activity).apply {
                        bind(
                            player = player,
                            characterId = characterIdsBySeat.getOrNull(player.seat),
                            active =
                                state.status == "ACTIVE" &&
                                    player.playerId == activePlayerId,
                            computer = false,
                            compact = true,
                            localPlayer = local,
                        )
                    },
                    LinearLayout.LayoutParams(
                        dp(activity, 148),
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        marginEnd = dp(activity, 6)
                    },
                )
            }
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
            GameSoundFeedback.roll(activity)
        }

        val reactions =
            host.feedbackLedger.filterReactions(
                LudoPawsReactionEngine.derive(
                    previous = previous,
                    current = current,
                ),
            )

        val dedicatedReactionSound =
            if (reactions.isNotEmpty()) {
                GameSoundFeedback.reaction(
                    context = activity,
                    reactions = reactions,
                    characterIdsBySeat = characterIdsBySeat,
                ).also {
                    LudoPawsHaptics.reaction(
                        context = activity,
                        reactions = reactions,
                    )
                }
            } else {
                false
            }

        if (
            !dedicatedReactionSound &&
            hasTokenMovement(
                previous = previous,
                current = current,
            )
        ) {
            val eventIndex =
                current.history.lastOrNull()?.eventIndex
                    ?: current.randomEventIndex
            if (
                host.feedbackLedger.once(
                    matchId = current.matchId,
                    key = "MOVE:$eventIndex",
                )
            ) {
                val seat = movementSeat(previous, current)
                GameSoundFeedback.move(
                    context = activity,
                    characterId = seat?.let(characterIdsBySeat::getOrNull),
                )
            }
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
