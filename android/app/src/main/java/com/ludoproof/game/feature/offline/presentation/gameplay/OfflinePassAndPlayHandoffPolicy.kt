package com.ludoproof.game.ui.offline.gameplay

import com.ludoproof.game.GameMode
import com.ludoproof.game.MatchSnapshot

data class OfflinePassAndPlayHandoff(
    val playerId: String,
    val displayName: String,
    val color: String,
    val seat: Int,
)

/**
 * Presentation-only policy for same-device Pass & Play turn handoff.
 *
 * A handoff is emitted only for a fresh ACTIVE -> ACTIVE transition where the
 * authoritative turn seat actually changed. Initial render, resume, rematch
 * start, extra turns, Computer mode, and finished matches intentionally do not
 * create a handoff prompt.
 */
object OfflinePassAndPlayHandoffPolicy {
    fun resolve(
        mode: GameMode,
        previous: MatchSnapshot?,
        current: MatchSnapshot,
    ): OfflinePassAndPlayHandoff? {
        if (mode != GameMode.PASS_AND_PLAY) return null
        if (previous == null) return null
        if (previous.matchId != current.matchId) return null
        if (previous.status != "ACTIVE" || current.status != "ACTIVE") return null
        if (previous.turnSeat == current.turnSeat) return null
        if (current.pendingRoll != null) return null

        val next =
            current.players.getOrNull(current.turnSeat)
                ?: return null

        return OfflinePassAndPlayHandoff(
            playerId = next.playerId,
            displayName = next.displayName,
            color = next.color,
            seat = next.seat,
        )
    }
}
