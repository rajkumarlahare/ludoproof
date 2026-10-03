package com.ludoproof.game

import org.json.JSONArray
import org.json.JSONObject

data class PlayerSnapshot(
    val playerId: String,
    val displayName: String,
    val color: String,
    val seat: Int,
    val tokens: List<Int>,
    val teamId: String? = null,
)

data class PendingRollSnapshot(
    val status: String,
    val seat: Int,
    val eventIndex: Int,
    val eventId: String?,
    val roundId: String?,
    val serverCommitment: String?,
    val clientCommitment: String?,
    val revealDeadlineAt: Long?,
    val proofDigest: String?,
    val outcome: Int?,
    val legalTokenIndexes: Set<Int>,
    val scheduledSeat: Int? = null,
)

data class HistoryEventSnapshot(
    val eventIndex: Int,
    val playerId: String,
    val roundId: String?,
    val proofDigest: String?,
    val outcome: Int?,
    val moveTokenIndex: Int?,
    val captures: Int,
    val serverCommitment: String? = null,
    val clientCommitment: String? = null,
    val previousStateHash: String? = null,
    val rulesetHash: String? = null,
    val fairnessProtocol: String? = null,
    val previousFairnessDigest: String? = null,
    val fairnessDigest: String? = null,
    val status: String? = null,
)

data class MatchSnapshot(
    val matchId: String,
    val status: String,
    val hostPlayerId: String,
    val targetPlayerCount: Int? = null,
    val matchMode: String = "ONLINE",
    val players: List<PlayerSnapshot>,
    val turnSeat: Int,
    val randomEventIndex: Int,
    val pendingRoll: PendingRollSnapshot?,
    val winnerPlayerId: String?,
    val rulesetId: String,
    val history: List<HistoryEventSnapshot>,
    val actingSeat: Int? = null,
    val teamAssignments: List<String> = emptyList(),
    val winnerTeamId: String? = null,
)

data class GameEnvelope(
    val playerId: String?,
    val state: MatchSnapshot?,
)

object GameJson {
    fun envelope(response: JSONObject): GameEnvelope {
        val stateObject =
            response.optJSONObject("state")
                ?: if (response.has("matchId") && response.has("players")) {
                    response
                } else {
                    null
                }

        return GameEnvelope(
            playerId =
                response.optString("playerId")
                    .takeIf { it.isNotBlank() },
            state = stateObject?.let(::match),
        )
    }

    private fun match(value: JSONObject): MatchSnapshot =
        MatchSnapshot(
            matchId = value.optString("matchId"),
            status = value.optString("status", "WAITING"),
            hostPlayerId = value.optString("hostPlayerId"),
            targetPlayerCount =
                if (
                    value.has("targetPlayerCount") &&
                    !value.isNull("targetPlayerCount")
                ) {
                    value.optInt("targetPlayerCount")
                        .takeIf { it in 2..4 }
                } else {
                    null
                },
            matchMode = value.optString("matchMode", "ONLINE"),
            players = value.optJSONArray("players").toPlayerList(),
            turnSeat = value.optInt("turnSeat", 0),
            randomEventIndex = value.optInt("randomEventIndex", 0),
            pendingRoll = value.optJSONObject("pendingRoll")?.let(::pendingRoll),
            winnerPlayerId =
                value.optString("winnerPlayerId")
                    .takeIf { it.isNotBlank() && it != "null" },
            rulesetId = value.optString("rulesetId"),
            history = value.optJSONArray("history").toHistoryList(),
            actingSeat =
                if (value.has("actingSeat") && !value.isNull("actingSeat")) {
                    value.optInt("actingSeat").takeIf { it in 0..3 }
                } else {
                    null
                },
            teamAssignments = value.optJSONArray("teamAssignments").toStringList(),
            winnerTeamId =
                value.optString("winnerTeamId")
                    .takeIf { it == "A" || it == "B" },
        )

    private fun pendingRoll(value: JSONObject): PendingRollSnapshot =
        PendingRollSnapshot(
            status = value.optString("status"),
            seat = value.optInt("seat", -1),
            eventIndex = value.optInt("eventIndex", -1),
            eventId =
                value.optString("eventId")
                    .takeIf { it.isNotBlank() && it != "null" },
            roundId =
                value.optString("roundId")
                    .takeIf { it.isNotBlank() && it != "null" },
            serverCommitment =
                value.optString("serverCommitment")
                    .takeIf { it.isNotBlank() && it != "null" },
            clientCommitment =
                value.optString("clientCommitment")
                    .takeIf { it.isNotBlank() && it != "null" },
            revealDeadlineAt =
                if (value.has("revealDeadlineAt") && !value.isNull("revealDeadlineAt")) {
                    value.optLong("revealDeadlineAt")
                } else {
                    null
                },
            proofDigest =
                value.optString("proofDigest")
                    .takeIf { it.isNotBlank() && it != "null" },
            outcome =
                if (value.has("outcome") && !value.isNull("outcome")) {
                    value.optInt("outcome")
                } else {
                    null
                },
            legalTokenIndexes = value.optJSONArray("legalTokenIndexes").toIntSet(),
            scheduledSeat =
                if (value.has("scheduledSeat") && !value.isNull("scheduledSeat")) {
                    value.optInt("scheduledSeat").takeIf { it in 0..3 }
                } else {
                    null
                },
        )

    private fun JSONArray?.toPlayerList(): List<PlayerSnapshot> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val player = optJSONObject(index) ?: continue
                add(
                    PlayerSnapshot(
                        playerId = player.optString("playerId"),
                        displayName = player.optString("displayName"),
                        color = player.optString("color"),
                        seat = player.optInt("seat", index),
                        tokens = player.optJSONArray("tokens").toIntList(),
                        teamId =
                            player.optString("teamId")
                                .takeIf { it == "A" || it == "B" },
                    ),
                )
            }
        }
    }

    private fun JSONArray?.toHistoryList(): List<HistoryEventSnapshot> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val event = optJSONObject(index) ?: continue
                add(
                    HistoryEventSnapshot(
                        eventIndex = event.optInt("eventIndex", -1),
                        playerId = event.optString("playerId"),
                        roundId =
                            event.optString("roundId")
                                .takeIf { it.isNotBlank() && it != "null" },
                        proofDigest =
                            event.optString("proofDigest")
                                .takeIf { it.isNotBlank() && it != "null" },
                        outcome =
                            if (event.has("outcome") && !event.isNull("outcome")) {
                                event.optInt("outcome")
                            } else {
                                null
                            },
                        moveTokenIndex =
                            if (event.has("moveTokenIndex") && !event.isNull("moveTokenIndex")) {
                                event.optInt("moveTokenIndex")
                            } else {
                                null
                            },
                        captures = event.optInt("captures", 0),
                        serverCommitment =
                            event.optString("serverCommitment")
                                .takeIf { it.isNotBlank() && it != "null" },
                        clientCommitment =
                            event.optString("clientCommitment")
                                .takeIf { it.isNotBlank() && it != "null" },
                        previousStateHash =
                            event.optString("previousStateHash")
                                .takeIf { it.isNotBlank() && it != "null" },
                        rulesetHash =
                            event.optString("rulesetHash")
                                .takeIf { it.isNotBlank() && it != "null" },
                        fairnessProtocol =
                            event.optString("fairnessProtocol")
                                .takeIf { it.isNotBlank() && it != "null" },
                        previousFairnessDigest =
                            event.optString("previousFairnessDigest")
                                .takeIf { it.isNotBlank() && it != "null" },
                        fairnessDigest =
                            event.optString("fairnessDigest")
                                .takeIf { it.isNotBlank() && it != "null" },
                        status =
                            event.optString("status")
                                .takeIf { it.isNotBlank() && it != "null" },
                    ),
                )
            }
        }
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) add(optInt(index))
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index)
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    private fun JSONArray?.toIntSet(): Set<Int> = toIntList().toSet()
}
