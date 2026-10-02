package com.ludoproof.game

class RemoteMatchSession(
    override val mode: GameMode,
    private val snapshotProvider:
        () -> MatchSnapshot?,
    private val playerIdProvider:
        () -> String?,
) : MatchSession {
    init {
        require(
            mode.isRemote,
        ) {
            "RemoteMatchSession requires a remote game mode"
        }
    }

    override val rulesetId:
        String
        get() =
            snapshot()
                ?.rulesetId
                ?: ClassicRuleset.ID

    override fun snapshot():
        MatchSnapshot? =
        snapshotProvider()

    override fun localPlayerId():
        String? =
        playerIdProvider()
}
