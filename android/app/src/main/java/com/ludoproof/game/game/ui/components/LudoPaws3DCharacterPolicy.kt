package com.ludoproof.game

import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog

/**
 * Production 3D pawn identity resolver.
 *
 * Character selection/store identity is presentation-only and is resolved by
 * seat. Ludo color stays authoritative gameplay state and is used only as a
 * deterministic compatibility fallback when an older match/install has no
 * character identity yet.
 */
internal enum class LudoPaws3DSpecies {
    DOG,
    GOAT,
    DUCK,
    CAT,
}

internal object LudoPaws3DCharacterPolicy {
    fun speciesForCharacterId(
        characterId: String?,
    ): LudoPaws3DSpecies? =
        when (
            LudoPawsCharacterCatalog
                .canonicalCharacterId(characterId)
        ) {
            "dog" -> LudoPaws3DSpecies.DOG
            "goat" -> LudoPaws3DSpecies.GOAT
            "duck" -> LudoPaws3DSpecies.DUCK
            "cat" -> LudoPaws3DSpecies.CAT
            else -> null
        }

    fun speciesForSeat(
        characterIdsBySeat: List<String>,
        seat: Int,
        fallbackColor: String,
    ): LudoPaws3DSpecies? =
        speciesForCharacterId(
            characterIdsBySeat.getOrNull(seat),
        ) ?: speciesForColor(fallbackColor)

    /** Compatibility only for legacy states that have no characterId. */
    fun speciesForColor(
        color: String,
    ): LudoPaws3DSpecies? =
        when (color) {
            "RED" -> LudoPaws3DSpecies.DOG
            "GREEN" -> LudoPaws3DSpecies.GOAT
            "YELLOW" -> LudoPaws3DSpecies.DUCK
            "BLUE" -> LudoPaws3DSpecies.CAT
            else -> null
        }
}
