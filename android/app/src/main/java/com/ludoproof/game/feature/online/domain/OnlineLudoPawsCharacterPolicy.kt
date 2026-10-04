package com.ludoproof.game.feature.online.domain

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog

/**
 * Resolves server-supplied cosmetic identity into the seat-indexed shape used by
 * the shared Ludo Paws board. Unknown/missing values fall back safely so an old
 * room or old client can still render after the rollout.
 */
object OnlineLudoPawsCharacterPolicy {
    fun characterIdsBySeat(
        state: MatchSnapshot?,
    ): List<String> {
        if (state == null || state.players.isEmpty()) {
            return emptyList()
        }

        val highestSeat =
            state.players
                .maxOfOrNull { it.seat }
                ?.coerceAtLeast(0)
                ?: return emptyList()
        val size =
            maxOf(
                state.players.size,
                highestSeat + 1,
            )

        return MutableList(
            size,
        ) {
            LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
        }.also {
                result ->
            state.players.forEach {
                    player ->
                if (player.seat !in result.indices) {
                    return@forEach
                }
                result[player.seat] =
                    LudoPawsCharacterIdentityContract
                        .resolveRemote(player.characterId)
            }
        }
    }
}
