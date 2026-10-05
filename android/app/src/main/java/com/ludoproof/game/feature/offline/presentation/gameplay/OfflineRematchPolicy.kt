package com.ludoproof.game.ui.offline.gameplay

import com.ludoproof.game.MatchSnapshot

data class OfflineRematchSpec(
    val playerCount: Int,
    val preferredColor: String,
)

/**
 * Derives a fresh local rematch from the match that just finished.
 *
 * The result intentionally comes from the authoritative finished snapshot rather
 * than setup-screen defaults so a resumed finished match can rematch with the
 * same seats and perspective even after preferences changed elsewhere.
 */
object OfflineRematchPolicy {
    fun fromFinishedState(
        state: MatchSnapshot,
    ): OfflineRematchSpec? {
        if (state.status != "FINISHED") return null
        if (state.players.size !in 2..4) return null
        val firstPlayer = state.players.firstOrNull() ?: return null

        return OfflineRematchSpec(
            playerCount = state.players.size,
            preferredColor = firstPlayer.color,
        )
    }
}
