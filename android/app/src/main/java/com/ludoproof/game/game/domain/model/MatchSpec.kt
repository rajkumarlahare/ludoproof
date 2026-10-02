package com.ludoproof.game

object ClassicRuleset {
    const val KEY =
        "CLASSIC_V1"

    // Keep the deployed identifier stable. Both local v4 and the
    // authoritative server already prove against this ruleset id.
    const val ID =
        "ludoproof-standard-v1"

    const val BOARD_TRACK_CELLS =
        52
    const val HOME_POSITION =
        57
    const val TOKENS_PER_PLAYER =
        4
}

data class MatchSpec(
    val mode: GameMode,
    val playerCount: Int,
    val rulesetId: String =
        ClassicRuleset.ID,
) {
    init {
        require(
            mode.supportsPlayerCount(
                playerCount,
            ),
        ) {
            mode.wireValue +
                " does not support " +
                playerCount +
                " players"
        }
        require(
            rulesetId ==
                ClassicRuleset.ID,
        ) {
            "Unsupported ruleset: " +
                rulesetId
        }
    }

    val authority:
        MatchAuthority
        get() =
            mode.authority

    val ranked: Boolean
        get() =
            mode.ranked

    companion object {
        fun classic(
            mode: GameMode,
            playerCount: Int,
        ): MatchSpec =
            MatchSpec(
                mode =
                    mode,
                playerCount =
                    playerCount,
                rulesetId =
                    ClassicRuleset.ID,
            )
    }
}
