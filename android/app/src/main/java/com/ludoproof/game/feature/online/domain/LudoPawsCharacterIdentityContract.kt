package com.ludoproof.game.feature.online.domain

import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog

/**
 * Backward-compatible cosmetic identity contract shared by remote presentation.
 *
 * The wire field stays optional for legacy clients. Old starter ids are migrated
 * onto the canonical Dog/Goat/Duck/Cat roster; missing, blank, stale or
 * unsupported values fall back to the production default. This contract remains
 * presentation-only and never participates in dice/proof state.
 */
object LudoPawsCharacterIdentityContract {
    const val WIRE_FIELD = "characterId"
    const val SCHEMA_VERSION = 1

    fun resolveRemote(characterId: String?): String {
        val canonical =
            LudoPawsCharacterCatalog
                .canonicalCharacterId(
                    characterId
                        ?.trim()
                        ?.lowercase(),
                )
        return canonical
            ?.takeIf {
                LudoPawsCharacterCatalog.character(it) != null
            }
            ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
    }
}
