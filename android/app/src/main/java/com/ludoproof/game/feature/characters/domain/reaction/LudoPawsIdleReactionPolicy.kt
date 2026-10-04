package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.MatchSnapshot

/**
 * Pure timing/key policy for idle character reactions.
 *
 * The UI owns the clock. This policy only says whether authoritative visible
 * state meaningfully changed and when one low-frequency idle reaction becomes
 * eligible. It never changes gameplay, turn ownership, proof state or timers.
 */
object LudoPawsIdleReactionPolicy {
    const val IDLE_THRESHOLD_MILLIS = 12_000L

    fun meaningfulStateKey(
        state: MatchSnapshot?,
    ): String? {
        state ?: return null

        return buildString {
            append(state.matchId)
            append('|')
            append(state.status)
            append('|')
            append(state.turnSeat)
            append('|')
            append(state.actingSeat ?: -1)
            append('|')
            append(state.randomEventIndex)
            append('|')
            append(state.pendingRoll?.eventIndex ?: -1)
            append('|')
            append(state.pendingRoll?.status.orEmpty())
            append('|')
            append(state.pendingRoll?.outcome ?: -1)
            append('|')
            state.players
                .sortedBy { it.seat }
                .forEach { player ->
                    append(player.playerId)
                    append(':')
                    append(player.tokens.joinToString(","))
                    append(';')
                }
            append('|')
            append(state.winnerPlayerId.orEmpty())
            append('|')
            append(state.winnerTeamId.orEmpty())
        }
    }

    fun idleReactionKey(
        state: MatchSnapshot?,
    ): String? {
        if (
            state == null ||
            state.status != "ACTIVE"
        ) {
            return null
        }
        val activeSeat =
            state.actingSeat
                ?: state.turnSeat
        val player =
            state.players
                .getOrNull(activeSeat)
                ?: return null

        return listOf(
            state.matchId,
            state.randomEventIndex.toString(),
            activeSeat.toString(),
            player.playerId,
        ).joinToString(":")
    }

    fun delayUntilEligibleMillis(
        state: MatchSnapshot?,
        lastMeaningfulChangeAtMillis: Long,
        nowMillis: Long,
        thresholdMillis: Long = IDLE_THRESHOLD_MILLIS,
    ): Long? {
        if (
            state == null ||
            state.status != "ACTIVE" ||
            thresholdMillis <= 0L
        ) {
            return null
        }

        val elapsed =
            (nowMillis - lastMeaningfulChangeAtMillis)
                .coerceAtLeast(0L)
        return (
            thresholdMillis - elapsed
            )
            .coerceAtLeast(0L)
    }
}
