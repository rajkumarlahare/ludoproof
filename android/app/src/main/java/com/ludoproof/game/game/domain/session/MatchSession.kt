package com.ludoproof.game

interface MatchSession {
    val mode: GameMode
    val rulesetId: String

    fun snapshot():
        MatchSnapshot?

    fun localPlayerId():
        String?
}
