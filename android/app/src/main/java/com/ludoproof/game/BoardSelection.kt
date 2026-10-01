package com.ludoproof.game

internal fun selectableTokenIndexes(state: MatchSnapshot?, playerId: String?): Set<Int> {
    if (state?.status != "ACTIVE" || playerId == null) return emptySet()
    val player = state.players.find { it.playerId == playerId } ?: return emptySet()
    val roll = state.pendingRoll ?: return emptySet()
    if (roll.status != "RESOLVED" || player.seat != state.turnSeat || roll.seat != player.seat) {
        return emptySet()
    }
    return roll.legalTokenIndexes.filterTo(mutableSetOf()) { it in player.tokens.indices }
}
