package com.ludoproof.game.ui.offline.gameplay

import com.ludoproof.game.MatchSnapshot

data class OfflineQuickReactionPresentation(
    val playerId: String,
    val displayName: String,
    val seat: Int,
    val emoji: String,
    val durationMs: Long,
    val riseDp: Int,
)

object OfflineQuickReactionPolicy {
    const val COOLDOWN_MS = 700L
    private val supported = setOf("👍", "😄", "👏", "😮")

    fun resolve(
        state: MatchSnapshot?,
        emoji: String,
        nowMs: Long,
        lastShownAtMs: Long?,
    ): OfflineQuickReactionPresentation? {
        state ?: return null
        if (state.status != "ACTIVE") return null
        if (emoji !in supported) return null
        if (lastShownAtMs != null && nowMs - lastShownAtMs < COOLDOWN_MS) return null

        val active = state.players.getOrNull(state.turnSeat) ?: return null
        return OfflineQuickReactionPresentation(
            playerId = active.playerId,
            displayName = active.displayName,
            seat = active.seat,
            emoji = emoji,
            durationMs = 1250L,
            riseDp = 52,
        )
    }
}
