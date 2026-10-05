package com.ludoproof.game

import android.content.Context

class LocalMatchSession(
    context: Context,
    override val mode: GameMode,
) : MatchSession {
    init {
        require(
            mode.isLocal,
        ) {
            "LocalMatchSession requires a local game mode"
        }
    }

    val engine =
        OfflineGameEngine(
            context =
                context.applicationContext,
            mode =
                mode,
        )

    override val rulesetId:
        String =
        OfflineLudoV3Binding.RULESET_ID

    override fun snapshot():
        MatchSnapshot? =
        engine.snapshot()

    override fun localPlayerId():
        String? =
        engine.humanPlayerId()

    fun hasSavedGame():
        Boolean =
        engine.hasSavedGame()

    fun start(
        playerCount: Int,
        preferredColor: String,
    ): MatchSnapshot {
        MatchSpec.classic(
            mode =
                mode,
            playerCount =
                playerCount,
        )
        return engine.start(
            playerCount =
                playerCount,
            preferredColor =
                preferredColor,
        )
    }

    fun roll():
        MatchSnapshot =
        engine.roll()

    fun move(
        tokenIndex: Int,
    ): MatchSnapshot =
        engine.move(
            tokenIndex,
        )

    fun clear() {
        engine.clear()
    }
}
