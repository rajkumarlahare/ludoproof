package com.ludoproof.game

/**
 * Presentation-only explanation for remote rolls that resolve without a token move.
 *
 * The policy compares one fresh authoritative before/after transition. This keeps
 * polling/realtime refreshes idempotent and never replays an old history receipt
 * after process restore or a duplicate state sync.
 */
internal object OnlineTurnTransitionFeedbackPolicy {
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
        val activeSeat = current.actingSeat ?: current.turnSeat
        val active = current.players.getOrNull(activeSeat) ?: return null
        val outcome = event.effectiveOutcome ?: event.outcome ?: return null

        return when {
            roller.playerId != active.playerId &&
                isThirdSixForfeit(
                    history = current.history,
                    playerId = roller.playerId,
                ) ->
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
