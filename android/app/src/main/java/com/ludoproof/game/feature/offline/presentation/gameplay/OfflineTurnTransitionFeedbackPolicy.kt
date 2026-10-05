package com.ludoproof.game.ui.offline.gameplay

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot

/**
 * Presentation-only explanation for rolls that resolve without a token move.
 *
 * The policy compares the before/after snapshots from one fresh roll so an old
 * receipt is never replayed as a banner when a saved match is resumed.
 */
object OfflineTurnTransitionFeedbackPolicy {
    fun message(
        previous: MatchSnapshot?,
        current: MatchSnapshot,
    ): String? {
        if (previous == null) return null
        if (previous.matchId != current.matchId) return null
        if (previous.status != "ACTIVE" || current.status != "ACTIVE") return null
        if (previous.pendingRoll != null || current.pendingRoll != null) return null
        if (current.randomEventIndex != previous.randomEventIndex + 1) return null

        val event =
            current.history.lastOrNull()
                ?.takeIf {
                    it.eventIndex == previous.randomEventIndex &&
                        it.moveTokenIndex == null
                }
                ?: return null
        val roller =
            current.players.firstOrNull {
                it.playerId == event.playerId
            } ?: return null
        val active =
            current.players.getOrNull(current.turnSeat)
                ?: return null
        val outcome = event.effectiveOutcome ?: event.outcome ?: return null

        return when {
            roller.playerId != active.playerId &&
                isThirdSixForfeit(event, outcome) ->
                "${roller.displayName} rolled a third 6 • turn forfeited • ${active.displayName}'s turn"

            roller.playerId != active.playerId ->
                "${roller.displayName} rolled $outcome • no legal move • ${active.displayName}'s turn"

            outcome == 6 ->
                "${roller.displayName} rolled 6 • no legal move • roll again"

            else ->
                null
        }
    }

    private fun isThirdSixForfeit(
        event: HistoryEventSnapshot,
        outcome: Int,
    ): Boolean =
        outcome == 6 &&
            !event.openingRollApplied
}
