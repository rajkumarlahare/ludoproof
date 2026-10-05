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
enum class LudoPaws3DSpecies {
    DOG,
    GOAT,
    DUCK,
    CAT,
}

internal object LudoPaws3DCharacterPolicy {
    /**
     * The shared renderer currently asks for species by board color. Install a
     * render-thread-local color -> selected-character mapping before each frame
     * so production rendering follows the real seat selection without leaking
     * cosmetics into MatchSnapshot's authoritative gameplay fields.
     */
    private val renderSpeciesByColor =
        ThreadLocal.withInitial<Map<String, LudoPaws3DSpecies>> {
            emptyMap()
        }

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
        ) ?: fallbackSpeciesForColor(fallbackColor)

    fun bindRenderAssignments(
        snapshot: MatchSnapshot?,
        characterIdsBySeat: List<String>,
    ) {
        val mapping =
            snapshot
                ?.players
                ?.mapNotNull {
                    player ->
                    speciesForSeat(
                        characterIdsBySeat = characterIdsBySeat,
                        seat = player.seat,
                        fallbackColor = player.color,
                    )
                        ?.let {
                            player.color to it
                        }
                }
                ?.toMap()
                .orEmpty()
        renderSpeciesByColor.set(mapping)
    }

    fun clearRenderAssignments() {
        renderSpeciesByColor.remove()
    }

    /**
     * Renderer-facing lookup. Selected seat identity wins when installed for
     * the current render thread; color mapping is compatibility fallback only.
     */
    fun speciesForColor(
        color: String,
    ): LudoPaws3DSpecies? =
        renderSpeciesByColor
            .get()
            ?.get(color)
            ?: fallbackSpeciesForColor(color)

    private fun fallbackSpeciesForColor(
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
