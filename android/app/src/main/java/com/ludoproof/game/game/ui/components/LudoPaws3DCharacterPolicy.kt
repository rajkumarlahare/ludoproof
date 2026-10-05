package com.ludoproof.game

/**
 * Production 3D pawn identity for the first board-runtime rollout.
 *
 * This mapping is presentation-only. Ludo color remains authoritative gameplay
 * state while the code-generated 3D animals replace the temporary 2D pawn art.
 * Character/store identity migration is intentionally a later step so this
 * rollout cannot affect dice, legal moves, proof state, persistence, or server
 * compatibility.
 */
internal enum class LudoPaws3DSpecies {
    DOG,
    GOAT,
    DUCK,
    CAT,
}

internal object LudoPaws3DCharacterPolicy {
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
