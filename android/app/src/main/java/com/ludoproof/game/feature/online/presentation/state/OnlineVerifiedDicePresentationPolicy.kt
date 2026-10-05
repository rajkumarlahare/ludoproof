package com.ludoproof.game

/**
 * Fail-closed presentation contract for remote verified dice.
 *
 * A pending roll is one authoritative event in progress. While it exists, the
 * UI must never fill missing fields from an older history event: doing that can
 * visually pair the previous roll/proof with the new event index. History is
 * used only after there is no pending roll.
 */
internal data class OnlineVerifiedDicePresentation(
    val outcome: Int,
    val proofDigest: String,
    val eventIndex: Int?,
    val openingRollApplied: Boolean,
    val eventKey: String,
)

internal object OnlineVerifiedDicePresentationPolicy {
    fun resolve(state: MatchSnapshot): OnlineVerifiedDicePresentation? {
        val pending = state.pendingRoll
        if (pending != null) {
            if (pending.status != "RESOLVED") return null

            val outcome = pending.outcome?.takeIf { it in 1..6 } ?: return null
            val proofDigest = pending.proofDigest?.takeIf(String::isNotBlank) ?: return null
            val eventIndex = pending.eventIndex.takeIf { it >= 0 }
            return presentation(
                state = state,
                outcome = outcome,
                proofDigest = proofDigest,
                eventIndex = eventIndex,
                openingRollApplied = pending.openingRollApplied,
            )
        }

        val latest = state.history.lastOrNull() ?: return null
        val outcome =
            (latest.effectiveOutcome ?: latest.outcome)
                ?.takeIf { it in 1..6 }
                ?: return null
        val proofDigest = latest.proofDigest?.takeIf(String::isNotBlank) ?: return null
        val eventIndex = latest.eventIndex.takeIf { it >= 0 }
        return presentation(
            state = state,
            outcome = outcome,
            proofDigest = proofDigest,
            eventIndex = eventIndex,
            openingRollApplied = latest.openingRollApplied,
        )
    }

    private fun presentation(
        state: MatchSnapshot,
        outcome: Int,
        proofDigest: String,
        eventIndex: Int?,
        openingRollApplied: Boolean,
    ): OnlineVerifiedDicePresentation {
        val identity = eventIndex?.toString() ?: "proof:$proofDigest"
        return OnlineVerifiedDicePresentation(
            outcome = outcome,
            proofDigest = proofDigest,
            eventIndex = eventIndex,
            openingRollApplied = openingRollApplied,
            eventKey = "${state.matchId}:$identity",
        )
    }
}
