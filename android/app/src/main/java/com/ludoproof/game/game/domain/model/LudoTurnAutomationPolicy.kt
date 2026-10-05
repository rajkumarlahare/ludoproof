package com.ludoproof.game

/**
 * Pure gameplay policy shared by local and remote presentation layers.
 *
 * This policy never invents a move when the player has a real choice. A move is
 * automatic only when the authoritative legal-token set contains exactly one
 * token. Opening-roll handling is also explicit so the guaranteed opening six
 * can be bound into a versioned ruleset without changing normal dice behavior.
 */
object LudoTurnAutomationPolicy {
    const val OPENING_ROLL_OUTCOME = 6

    fun shouldGuaranteeOpeningSix(
        tokens: List<Int>,
        openingRollConsumed: Boolean,
    ): Boolean =
        !openingRollConsumed &&
            tokens.size == 4 &&
            tokens.all { it == -1 }

    fun effectiveOutcome(
        verifiedOutcome: Int,
        guaranteeOpeningSix: Boolean,
    ): Int {
        require(verifiedOutcome in 1..6) {
            "verified dice outcome must be between 1 and 6"
        }
        return if (guaranteeOpeningSix) {
            OPENING_ROLL_OUTCOME
        } else {
            verifiedOutcome
        }
    }

    fun singleLegalTokenIndex(
        legalTokenIndexes: Set<Int>,
    ): Int? =
        legalTokenIndexes
            .singleOrNull()
}
