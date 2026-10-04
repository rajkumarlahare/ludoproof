package com.ludoproof.game.feature.online.domain

import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog

/**
 * Backward-compatible cosmetic identity contract shared by remote presentation.
 *
 * The wire field is intentionally optional for legacy clients. Missing, blank,
 * stale, or unsupported values fall back to the Starter Paws default. This
 * contract is presentation-only and must never participate in dice/proof state.
 */
object LudoPawsCharacterIdentityContract {
    const val WIRE_FIELD = "characterId"
    const val SCHEMA_VERSION = 1

    fun resolveRemote(characterId: String?): String =
        characterId
            ?.trim()
            ?.lowercase()
            ?.takeIf {
                LudoPawsCharacterCatalog.character(it) != null
            }
            ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
}
