package com.ludoproof.game

object LocalSecretBinding {
    const val FORMAT_VERSION = 2

    fun pendingRollAad(
        matchId: String,
        clientCommitment: String,
    ): ByteArray =
        (
            "ludoproof:pending-roll:v2:" +
                matchId +
                ":" +
                clientCommitment
            ).toByteArray(
            Charsets.UTF_8,
        )

    fun playerSessionAad(
        matchId: String,
        playerId: String,
    ): ByteArray =
        (
            "ludoproof:player-session:v2:" +
                matchId +
                ":" +
                playerId
            ).toByteArray(
            Charsets.UTF_8,
        )
}
