package com.ludoproof.game

data class OfflineBindingPlayer(
    val playerId: String,
    val color: String,
    val tokens: List<Int>,
)

data class OfflineBindingHistory(
    val eventIndex: Int,
    val playerId: String,
    val clientCommitment: String?,
    val proofDigest: String?,
    val outcome: Int?,
    val moveTokenIndex: Int?,
    val captures: Int,
    val fairnessDigest: String? = null,
)

object OfflineLudoV4Binding {
    const val RULESET_ID =
        "ludoproof-standard-v1"

    val WORLD =
        LocalV4WorldConfig(
            cellsPerOutcome =
                16,
            timelineTicks =
                512,
            epochCount =
                8,
            probeCount =
                3,
        )

    fun deriveRoll(
        matchId: String,
        status: String,
        players: List<OfflineBindingPlayer>,
        turnSeat: Int,
        eventIndex: Int,
        consecutiveSixes: List<Int>,
        winnerPlayerId: String?,
        history: List<OfflineBindingHistory>,
    ): LocalV4Result {
        require(
            eventIndex >=
                0,
        )

        val activePlayer =
            players.getOrNull(
                turnSeat,
            )
                ?: error(
                    "offline turn seat is invalid",
                )

        val actorHash =
            EntroNexV4Local.sha256Hex(
                "ludoproof:actor:v1:" +
                    activePlayer
                        .playerId,
            )

        val authoritativeState =
            mapOf(
                "schemaVersion" to
                    1,
                "matchId" to
                    matchId,
                "status" to
                    status,
                "players" to
                    players.map {
                            player ->
                        mapOf(
                            "playerId" to
                                player.playerId,
                            "color" to
                                player.color,
                            "tokens" to
                                player.tokens,
                        )
                    },
                "turnSeat" to
                    turnSeat,
                "randomEventIndex" to
                    eventIndex,
                "consecutiveSixes" to
                    consecutiveSixes,
                "winnerPlayerId" to
                    winnerPlayerId,
                "history" to
                    history.map {
                            event ->
                        mapOf(
                            "eventIndex" to
                                event.eventIndex,
                            "playerId" to
                                event.playerId,
                            "clientCommitment" to
                                event.clientCommitment,
                            "proofDigest" to
                                event.proofDigest,
                            "outcome" to
                                event.outcome,
                            "moveTokenIndex" to
                                event.moveTokenIndex,
                            "captures" to
                                event.captures,
                            "fairnessDigest" to
                                event.fairnessDigest,
                        )
                    },
            )

        val previousStateHash =
            EntroNexV4Local.sha256Hex(
                "entronex:v4:game-state:" +
                    EntroNexV4Local.jcs(
                        authoritativeState,
                    ),
            )

        val rulesetHash =
            EntroNexV4Local.sha256Hex(
                "entronex:v4:game-ruleset:" +
                    EntroNexV4Local.jcs(
                        RULESET,
                    ),
            )

        val config =
            LocalV4Config(
                outcomes =
                    listOf(
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                    ),
                context =
                    LocalV4Context(
                        applicationId =
                            "ludoproof",
                        sessionId =
                            matchId,
                        eventId =
                            "roll:" +
                                eventIndex,
                        eventType =
                            "DICE_ROLL",
                        eventIndex =
                            eventIndex
                                .toLong(),
                        subjectHash =
                            actorHash,
                        previousStateHash =
                            previousStateHash,
                        metadataDigest =
                            rulesetHash,
                    ),
                world =
                    WORLD,
            )

        val serverSeed =
            EntroNexV4Local
                .randomSeedHex()
        val clientSeed =
            EntroNexV4Local
                .randomSeedHex()

        return EntroNexV4Local.resolve(
            roundId =
                EntroNexV4Local
                    .randomRoundId(),
            serverSeed =
                serverSeed,
            clientSeed =
                clientSeed,
            config =
                config,
        )
    }

    private val RULESET:
        Map<String, Any?> =
        mapOf(
            "id" to
                RULESET_ID,
            "boardTrackCells" to
                52,
            "homePosition" to
                57,
            "tokensPerPlayer" to
                4,
            "startOffsets" to
                mapOf(
                    "RED" to
                        0,
                    "GREEN" to
                        13,
                    "YELLOW" to
                        26,
                    "BLUE" to
                        39,
                ),
            "safeGlobalCells" to
                listOf(
                    0,
                    8,
                    13,
                    21,
                    26,
                    34,
                    39,
                    47,
                ),
            "leaveYardRequiresSix" to
                true,
            "exactRollToHome" to
                true,
            "extraTurnOnSix" to
                true,
            "extraTurnOnCapture" to
                true,
            "threeConsecutiveSixesForfeit" to
                true,
            "captureOnSafeCell" to
                false,
        )
}
