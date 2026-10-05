package com.ludoproof.game.ui.offline.state

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot

/**
 * Explains authoritative engine transitions that complete without a token move.
 *
 * OfflineGameEngine can advance the turn immediately after a non-six roll with
 * no legal destination, or after the third consecutive natural six. By the time
 * the UI receives that snapshot the active dice already belongs to the next
 * player, so this pure presentation policy preserves the result as readable HUD
 * copy without delaying or reinterpreting gameplay.
 */
object OfflineTurnTransitionPresentationPolicy {
    fun automaticTurnMessage(
        state: MatchSnapshot,
    ): String? {
        if (state.status != "ACTIVE") return null

        val active =
            state.players.getOrNull(state.turnSeat)
                ?: return null
        val latest =
            state.history.lastOrNull()
                ?: return null

        // If the same player still owns the turn, the dice face remains beside
        // that player and normal pending/roll UI already explains the state.
        if (latest.playerId == active.playerId) return null

        // A recorded move is a normal turn completion, not an automatic pass.
        if (latest.moveTokenIndex != null) return null

        val outcome =
            latest.effectiveOutcome
                ?: latest.outcome
                ?: return null
        val roller =
            state.players.firstOrNull {
                it.playerId == latest.playerId
            } ?: return null

        return if (
            isThirdConsecutiveNaturalSix(
                history = state.history,
                playerId = latest.playerId,
            )
        ) {
            "${roller.displayName} rolled a third 6 • turn forfeited • Next: ${active.displayName}"
        } else {
            "${roller.displayName} rolled $outcome • no legal move • Next: ${active.displayName}"
        }
    }

    private fun isThirdConsecutiveNaturalSix(
        history: List<HistoryEventSnapshot>,
        playerId: String,
    ): Boolean {
        val recentForPlayer =
            history
                .asReversed()
                .takeWhile { it.playerId == playerId }
                .take(3)

        return recentForPlayer.size == 3 &&
            recentForPlayer.all { event ->
                !event.openingRollApplied &&
                    (event.effectiveOutcome ?: event.outcome) == 6
            }
    }
}
