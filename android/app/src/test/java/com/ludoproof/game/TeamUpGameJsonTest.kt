package com.ludoproof.game

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TeamUpGameJsonTest {
    @Test
    fun parsesTeamAssignmentsActingSeatAndWinner() {
        val state =
            JSONObject()
                .put("matchId", "LPTEAM2345")
                .put("status", "FINISHED")
                .put("hostPlayerId", "p_red")
                .put("targetPlayerCount", 4)
                .put("matchMode", "TEAM_UP")
                .put("turnSeat", 0)
                .put("actingSeat", 2)
                .put("randomEventIndex", 3)
                .put("winnerPlayerId", JSONObject.NULL)
                .put("winnerTeamId", "A")
                .put("rulesetId", "ludoproof-team-v1")
                .put(
                    "teamAssignments",
                    JSONArray(listOf("A", "B", "A", "B")),
                )
                .put(
                    "players",
                    JSONArray(
                        listOf(
                            player("p_red", "RED", 0, "A"),
                            player("p_green", "GREEN", 1, "B"),
                            player("p_yellow", "YELLOW", 2, "A"),
                            player("p_blue", "BLUE", 3, "B"),
                        ),
                    ),
                )
                .put("history", JSONArray())

        val snapshot =
            GameJson.envelope(
                JSONObject().put("state", state),
            ).state!!

        assertEquals("TEAM_UP", snapshot.matchMode)
        assertEquals(2, snapshot.actingSeat)
        assertEquals(listOf("A", "B", "A", "B"), snapshot.teamAssignments)
        assertEquals("A", snapshot.winnerTeamId)
        assertNull(snapshot.winnerPlayerId)
        assertEquals("A", snapshot.players[0].teamId)
        assertEquals("B", snapshot.players[1].teamId)
    }

    private fun player(
        id: String,
        color: String,
        seat: Int,
        teamId: String,
    ): JSONObject =
        JSONObject()
            .put("playerId", id)
            .put("displayName", id)
            .put("color", color)
            .put("seat", seat)
            .put("teamId", teamId)
            .put("tokens", JSONArray(listOf(-1, -1, -1, -1)))
}
