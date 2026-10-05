package com.ludoproof.game

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameJsonV3GameplayTest {
    @Test
    fun classicV3ParsesOpeningBonusAndEffectiveOutcome() {
        val state =
            JSONObject()
                .put("schemaVersion", 1)
                .put("matchId", "LPV3JSON23")
                .put("status", "ACTIVE")
                .put("revision", 5)
                .put("hostPlayerId", "p0")
                .put("targetPlayerCount", 2)
                .put("matchMode", "ONLINE")
                .put("players", players())
                .put("turnSeat", 0)
                .put("randomEventIndex", 1)
                .put(
                    "pendingRoll",
                    JSONObject()
                        .put("status", "RESOLVED")
                        .put("seat", 0)
                        .put("eventIndex", 0)
                        .put("eventId", "roll:0")
                        .put("roundId", "round-0")
                        .put("serverCommitment", "a".repeat(64))
                        .put("clientCommitment", "b".repeat(64))
                        .put("revealDeadlineAt", 123456789L)
                        .put("proofDigest", "c".repeat(64))
                        .put("outcome", 6)
                        .put("randomOutcome", 2)
                        .put("openingRollApplied", true)
                        .put("legalTokenIndexes", JSONArray(listOf(0, 1, 2, 3))),
                )
                .put("winnerPlayerId", JSONObject.NULL)
                .put("rulesetId", "ludoproof-standard-v3")
                .put(
                    "history",
                    JSONArray().put(
                        JSONObject()
                            .put("eventIndex", 0)
                            .put("playerId", "p0")
                            .put("outcome", 2)
                            .put("randomOutcome", 2)
                            .put("effectiveOutcome", 6)
                            .put("openingRollApplied", true)
                            .put("captures", 0),
                    ),
                )

        val parsed =
            GameJson.envelope(
                JSONObject().put("state", state),
            ).state

        requireNotNull(parsed)
        assertEquals("ludoproof-standard-v3", parsed.rulesetId)
        assertEquals(6, parsed.pendingRoll?.outcome)
        assertEquals(2, parsed.pendingRoll?.randomOutcome)
        assertTrue(parsed.pendingRoll?.openingRollApplied == true)
        assertEquals(6, parsed.history.single().effectiveOutcome)
        assertEquals(2, parsed.history.single().randomOutcome)
    }

    @Test(expected = GameSchemaException::class)
    fun teamV3StillRejectsClassicV3Id() {
        val state =
            JSONObject()
                .put("schemaVersion", 1)
                .put("matchId", "LPV3TEAM23")
                .put("status", "ACTIVE")
                .put("revision", 1)
                .put("hostPlayerId", "p0")
                .put("targetPlayerCount", 4)
                .put("matchMode", "TEAM_UP")
                .put("teamAssignments", JSONArray(listOf("A", "B", "A", "B")))
                .put("actingSeat", 0)
                .put("winnerTeamId", JSONObject.NULL)
                .put("players", teamPlayers())
                .put("turnSeat", 0)
                .put("randomEventIndex", 0)
                .put("pendingRoll", JSONObject.NULL)
                .put("winnerPlayerId", JSONObject.NULL)
                .put("rulesetId", "ludoproof-standard-v3")
                .put("history", JSONArray())

        GameJson.envelope(JSONObject().put("state", state))
    }

    private fun players(): JSONArray =
        JSONArray().apply {
            repeat(2) { seat ->
                put(
                    JSONObject()
                        .put("playerId", "p$seat")
                        .put("displayName", "Player $seat")
                        .put("characterId", "duck")
                        .put("color", listOf("RED", "GREEN")[seat])
                        .put("seat", seat)
                        .put("tokens", JSONArray(listOf(-1, -1, -1, -1))),
                )
            }
        }

    private fun teamPlayers(): JSONArray =
        JSONArray().apply {
            val colors = listOf("RED", "GREEN", "YELLOW", "BLUE")
            val teams = listOf("A", "B", "A", "B")
            repeat(4) { seat ->
                put(
                    JSONObject()
                        .put("playerId", "p$seat")
                        .put("displayName", "Player $seat")
                        .put("characterId", "duck")
                        .put("color", colors[seat])
                        .put("seat", seat)
                        .put("teamId", teams[seat])
                        .put("tokens", JSONArray(listOf(-1, -1, -1, -1))),
                )
            }
        }
}
