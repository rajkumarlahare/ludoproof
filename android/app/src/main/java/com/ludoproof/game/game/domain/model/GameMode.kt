package com.ludoproof.game

enum class MatchAuthority {
    REMOTE,
    LOCAL,
}

enum class GameMode(
    val wireValue: String,
    val displayName: String,
    val authority: MatchAuthority,
    val allowedPlayerCounts: Set<Int>,
    val ranked: Boolean,
    val teamBased: Boolean = false,
) {
    ONLINE(
        wireValue = "ONLINE",
        displayName = "Online",
        authority = MatchAuthority.REMOTE,
        allowedPlayerCounts = setOf(2, 4),
        ranked = true,
    ),
    TEAM_UP(
        wireValue = "TEAM_UP",
        displayName = "Team Up",
        authority = MatchAuthority.REMOTE,
        allowedPlayerCounts = setOf(4),
        // Team Up v1 deliberately stays off the individual leaderboard until
        // a dedicated two-winner team ledger is introduced server-side.
        ranked = false,
        teamBased = true,
    ),
    FRIENDS(
        wireValue = "FRIENDS",
        displayName = "Friends",
        authority = MatchAuthority.REMOTE,
        allowedPlayerCounts = setOf(2, 3, 4),
        ranked = false,
    ),
    COMPUTER(
        wireValue = "COMPUTER",
        displayName = "Computer",
        authority = MatchAuthority.LOCAL,
        allowedPlayerCounts = setOf(2, 3, 4),
        ranked = false,
    ),
    PASS_AND_PLAY(
        wireValue = "PASS_AND_PLAY",
        displayName = "Pass & Play",
        authority = MatchAuthority.LOCAL,
        allowedPlayerCounts = setOf(2, 3, 4),
        ranked = false,
    );

    val isRemote: Boolean
        get() =
            authority ==
                MatchAuthority.REMOTE

    val isLocal: Boolean
        get() =
            authority ==
                MatchAuthority.LOCAL

    fun supportsPlayerCount(
        playerCount: Int,
    ): Boolean =
        playerCount in
            allowedPlayerCounts

    companion object {
        fun fromWireValue(
            value: String?,
        ): GameMode? {
            val normalized =
                value
                    ?.trim()
                    ?.uppercase()
                    ?: return null

            if (
                normalized ==
                LEGACY_LOCAL
            ) {
                return PASS_AND_PLAY
            }

            return entries
                .firstOrNull {
                    it.wireValue ==
                        normalized
                }
        }

        private const val LEGACY_LOCAL =
            "LOCAL"
    }
}
