package com.ludoproof.game

/**
 * Pure gameplay policy shared by local and remote presentation layers.
 *
 * This policy never invents a move when the player has a real choice. A move is
 * automatic only when the authoritative legal-token set contains one effective
 * move, either because one token is legal or because all legal tokens are
 * interchangeable. Opening-roll handling is also explicit so the guaranteed opening six
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

    /**
     * Returns one representative token when all legal tokens produce the same
     * effective destination. Token identity is not a meaningful choice in that
     * case, so the move can be automated without changing gameplay semantics.
     *
     * The legal-token set remains authoritative; this policy only collapses
     * interchangeable legal tokens for presentation/automation.
     */
    fun singleAutomaticTokenIndex(
        tokens: List<Int>,
        roll: Int,
        legalTokenIndexes: Set<Int>,
    ): Int? {
        require(roll in 1..6) {
            "dice outcome must be between 1 and 6"
        }

        if (legalTokenIndexes.isEmpty()) return null

        val destinationsByToken =
            legalTokenIndexes.associateWith { tokenIndex ->
                tokens
                    .getOrNull(tokenIndex)
                    ?.let { position ->
                        LudoPathEncoding.destinationForRoll(
                            position = position,
                            roll = roll,
                        )
                    }
            }

        // Fail closed if the snapshot is internally inconsistent. We never
        // auto-select from a partially invalid legal-token set.
        if (destinationsByToken.values.any { it == null }) {
            return null
        }

        val distinctDestinations =
            destinationsByToken.values
                .filterNotNull()
                .distinct()

        if (distinctDestinations.size != 1) return null

        return destinationsByToken.keys.minOrNull()
    }
}
