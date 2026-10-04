package com.ludoproof.game

/**
 * Classic turn-retention rules shared by local gameplay.
 *
 * A player keeps the turn after rolling six, capturing an opponent, or moving
 * one token exactly into the finished center position.
 */
object LudoExtraTurnPolicy {
    fun grantsExtraTurn(
        roll: Int,
        captures: Int,
        destination: Int,
    ): Boolean =
        roll == 6 ||
            captures > 0 ||
            destination == LudoPathEncoding.HOME_POSITION
}
