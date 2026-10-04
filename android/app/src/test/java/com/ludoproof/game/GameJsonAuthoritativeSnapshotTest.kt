package com.ludoproof.game

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameJsonAuthoritativeSnapshotTest {
    @Test
    fun waitingSnapshotAllowsExplicitNullTurnSeat() {
        val state = classicState(status = "WAITING", playerCount = 1)

        val parsed = GameJson.envelope(JSONObject().put("state", state)).state

        requireNotNull(parsed)
        assertEquals("WAITING", parsed.status)
        assertEquals(1, parsed.players.size)
        assertNull(parsed.pendingRoll)
    }

    @Test(expected = GameSchemaException::class)
    fun activeSnapshotRejectsMissingTurnSeatInsteadOfDefaultingToZero() {
        val state = classicState(status = "ACTIVE", playerCount = 2)
        state.remove("turnSeat")

        GameJson.envelope(JSONObject().put("state", state))
    }

    @Test(expected = GameSchemaException::class)
    fun snapshotRejectsMalformedTokenCount() {
        val state = classicState(status = "ACTIVE", playerCount = 2)
        state.getJSONArray("players")
            .getJSONObject(0)
            .put("tokens", JSONArray().put(-1).put(-1).put(-1))

        GameJson.envelope(JSONObject().put("state", state))
    }

    @Test(expected = GameSchemaException::class)
    fun snapshotRejectsTokenPositionOutsideBoardContract() {
        val state = classicState(status = "ACTIVE", playerCount = 2)
        state.getJSONArray("players")
            .getJSONObject(1)
            .put("tokens", JSONArray().put(-1).put(58).put(-1).put(-1))

        GameJson.envelope(JSONObject().put("state", state))
    }

    @Test(expected = GameSchemaException::class)
    fun snapshotRejectsPendingRollSeatOutsideCurrentPlayers() {
        val state = classicState(status = "ACTIVE", playerCount = 2)
            .put("randomEventIndex", 1)
            .put(
                "pendingRoll",
                pendingRoll(
                    status = "COMMITTED",
                    seat = 3,
                    eventIndex = 0,
                ),
            )

        GameJson.envelope(JSONObject().put("state", state))
    }

    @Test(expected = GameSchemaException::class)
    fun snapshotRejectsNonMonotonicHistory() {
        val state = classicState(status = "ACTIVE", playerCount = 2)
            .put("randomEventIndex", 3)
            .put(
                "history",
                JSONArray()
                    .put(historyEvent(eventIndex = 1, playerId = "p0"))
                    .put(historyEvent(eventIndex = 1, playerId = "p1")),
            )

        GameJson.envelope(JSONObject().put("state", state))
    }

    @Test(expected = GameSchemaException::class)
    fun teamSnapshotRejectsClassicRulesetBinding() {
        val state = teamState()
            .put("rulesetId", "ludoproof-standard-v1")

        GameJson.envelope(JSONObject().put("state", state))
    }

    @Test
    fun validTeamSnapshotKeepsCanonicalAssignments() {
        val parsed =
            GameJson.envelope(
                JSONObject().put("state", teamState()),
            ).state

        requireNotNull(parsed)
        assertEquals("TEAM_UP", parsed.matchMode)
        assertEquals("ludoproof-team-v1", parsed.rulesetId)
        assertEquals(listOf("A", "B", "A", "B"), parsed.teamAssignments)
        assertEquals(listOf("A", "B", "A", "B"), parsed.players.map { it.teamId })
        assertEquals(0, parsed.actingSeat)
    }

    @Test(expected = GameSchemaException::class)
    fun numericFieldsRejectStringCoercion() {
        val state = classicState(status = "ACTIVE", playerCount = 2)
            .put("turnSeat", "0")

        GameJson.envelope(JSONObject().put("state", state))
    }

    private fun classicState(
        status: String,
        playerCount: Int,
    ): JSONObject =
        JSONObject()
            .put("schemaVersion", 1)
            .put("matchId", "LPABCDEFGH")
            .put("status", status)
            .put("revision", 3)
            .put("hostPlayerId", "p0")
            .put("targetPlayerCount", JSONObject.NULL)
            .put("matchMode", "ONLINE")
            .put("players", players(playerCount, team = false))
            .put("turnSeat", if (status == "WAITING") JSONObject.NULL else 0)
            .put("randomEventIndex", 0)
            .put("pendingRoll", JSONObject.NULL)
            .put("winnerPlayerId", JSONObject.NULL)
            .put("rulesetId", "ludoproof-standard-v1")
            .put("history", JSONArray())

    private fun teamState(): JSONObject =
        JSONObject()
            .put("schemaVersion", 1)
            .put("matchId", "LPTEAMAB23")
            .put("status", "ACTIVE")
            .put("revision", 7)
            .put("hostPlayerId", "p0")
            .put("targetPlayerCount", 4)
            .put("matchMode", "TEAM_UP")
            .put("teamAssignments", JSONArray(listOf("A", "B", "A", "B")))
            .put("actingSeat", 0)
            .put("winnerTeamId", JSONObject.NULL)
            .put("players", players(4, team = true))
            .put("turnSeat", 0)
            .put("randomEventIndex", 0)
            .put("pendingRoll", JSONObject.NULL)
            .put("winnerPlayerId", JSONObject.NULL)
            .put("rulesetId", "ludoproof-team-v1")
            .put("history", JSONArray())

    private fun players(
        count: Int,
        team: Boolean,
    ): JSONArray =
        JSONArray().apply {
            repeat(count) { seat ->
                put(
                    JSONObject()
                        .put("playerId", "p$seat")
                        .put("displayName", "Player $seat")
                        .put("characterId", "duck")
                        .put("color", listOf("RED", "GREEN", "YELLOW", "BLUE")[seat])
                        .put("seat", seat)
                        .put("tokens", JSONArray().put(-1).put(-1).put(-1).put(-1))
                        .apply {
                            if (team) {
                                put("teamId", listOf("A", "B", "A", "B")[seat])
                            }
                        },
                )
            }
        }

    private fun pendingRoll(
        status: String,
        seat: Int,
        eventIndex: Int,
    ): JSONObject =
        JSONObject()
            .put("status", status)
            .put("seat", seat)
            .put("eventIndex", eventIndex)
            .put("eventId", "roll:$eventIndex")
            .put("roundId", "round-1")
            .put("serverCommitment", "a".repeat(64))
            .put("clientCommitment", "b".repeat(64))
            .put("revealDeadlineAt", 123456789L)
            .put("proofDigest", JSONObject.NULL)
            .put("outcome", JSONObject.NULL)
            .put("legalTokenIndexes", JSONObject.NULL)

    private fun historyEvent(
        eventIndex: Int,
        playerId: String,
    ): JSONObject =
        JSONObject()
            .put("eventIndex", eventIndex)
            .put("playerId", playerId)
            .put("captures", 0)
}
