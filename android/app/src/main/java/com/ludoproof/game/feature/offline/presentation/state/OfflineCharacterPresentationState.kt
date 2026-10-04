package com.ludoproof.game.ui.offline.state

import com.ludoproof.game.GameMode
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.feature.characters.domain.selection.StarterPawsAssignmentPolicy
import com.ludoproof.game.feature.offline.data.local.ActiveOfflineCharacterSetup

/**
 * Presentation-only recovery policy for Starter Paws assignments.
 * Saved Ludo gameplay remains independent from cosmetic character metadata.
 */
object OfflineCharacterAssignmentPolicy {
    fun resolve(
        mode: GameMode,
        playerCount: Int,
        preferredCharacterId: String,
        active: ActiveOfflineCharacterSetup?,
    ): List<String> {
        require(mode.isLocal) {
            "Offline character assignment requires a local game mode"
        }

        val requested =
            active
                ?.takeIf {
                    it.mode == mode &&
                        it.playerCount == playerCount
                }
                ?.characterIds
                .orEmpty()

        return StarterPawsAssignmentPolicy.normalize(
            playerCount = playerCount,
            requestedCharacterIds = requested,
            preferredCharacterId = preferredCharacterId,
            computerMode = mode == GameMode.COMPUTER,
        )
    }
}

/**
 * Resolves one stable character list for the active local match.
 *
 * A resumed match may outlive the presentation-only character preference record
 * (for example after an app upgrade or preference repair). In that case we
 * deterministically rebuild Starter Paws assignments and persist the repaired
 * presentation record without touching OfflineGameEngine saved state.
 */
internal fun OfflineGameActivity.resolveOfflineCharacterIds(
    state: MatchSnapshot,
): List<String> {
    val playerCount =
        state.players.size
    if (
        activeCharacterMatchId == state.matchId &&
        activeCharacterIdsBySeat.size == playerCount
    ) {
        return activeCharacterIdsBySeat
    }

    val active =
        offlineCharacterSetupStore.loadActive()
    val preferredCharacterId =
        characterSelectionStore
            .load()
            .characterId
    val resolved =
        OfflineCharacterAssignmentPolicy.resolve(
            mode = gameMode,
            playerCount = playerCount,
            preferredCharacterId = preferredCharacterId,
            active = active,
        )

    val preferredColor =
        state.players
            .firstOrNull()
            ?.color
            ?: active?.preferredColor
            ?: "BLUE"

    if (
        active == null ||
        active.mode != gameMode ||
        active.playerCount != playerCount ||
        active.preferredColor != preferredColor ||
        active.characterIds != resolved
    ) {
        offlineCharacterSetupStore.saveActive(
            mode = gameMode,
            playerCount = playerCount,
            preferredColor = preferredColor,
            characterIds = resolved,
        )
    }

    activeCharacterMatchId =
        state.matchId
    activeCharacterIdsBySeat =
        resolved
    return resolved
}
