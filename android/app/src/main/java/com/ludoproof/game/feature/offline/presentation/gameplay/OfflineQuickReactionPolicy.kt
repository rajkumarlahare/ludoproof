package com.ludoproof.game.ui.offline.gameplay

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.ui.quickchat.QuickChatEmojiCatalog

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
    private val supported = QuickChatEmojiCatalog.EMOJIS.toSet()

    fun resolve(
        state: MatchSnapshot?,
        emoji: String,
        nowMs: Long,
        lastShownAtMs: Long?,
        senderPlayerId: String? = null,
    ): OfflineQuickReactionPresentation? {
        state ?: return null
        if (state.status != "ACTIVE") return null
        if (emoji !in supported) return null
        if (lastShownAtMs != null && nowMs - lastShownAtMs < COOLDOWN_MS) return null

        val sender =
            if (senderPlayerId == null) {
                state.players.getOrNull(state.turnSeat)
            } else {
                state.players.firstOrNull { it.playerId == senderPlayerId }
            } ?: return null
        return OfflineQuickReactionPresentation(
            playerId = sender.playerId,
            displayName = sender.displayName,
            seat = sender.seat,
            emoji = emoji,
            durationMs = 1250L,
            riseDp = 52,
        )
    }
}
