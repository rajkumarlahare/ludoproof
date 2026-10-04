package com.ludoproof.game.feature.characters.data.local

import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog

/**
 * Process-local mirror of the user's persisted cosmetic selection.
 *
 * Network clients use this only to attach presentation metadata to create/join
 * requests. It never participates in dice, move, proof, turn, or winner logic.
 */
object LudoPawsCharacterRuntime {
    @Volatile
    private var selectedCharacterId: String =
        LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID

    fun selectedCharacterId(): String =
        selectedCharacterId

    fun update(characterId: String?) {
        selectedCharacterId =
            characterId
                ?.takeIf {
                    LudoPawsCharacterCatalog.character(it) != null
                }
                ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
    }
}
