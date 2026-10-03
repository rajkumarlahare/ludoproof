package com.ludoproof.game.feature.team.data.local

import android.content.Context
import org.json.JSONObject
import java.util.UUID

data class TeamMatchmakingTicket(
    val requestId: String,
    val displayName: String,
    val startedAt: Long,
)

class TeamMatchmakingStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun save(ticket: TeamMatchmakingTicket) {
        require(runCatching { UUID.fromString(ticket.requestId) }.isSuccess)
        require(ticket.displayName.length in 2..24)
        require(ticket.startedAt > 0L)
        check(
            prefs.edit()
                .putString(
                    KEY_JSON,
                    JSONObject()
                        .put("requestId", ticket.requestId)
                        .put("displayName", ticket.displayName)
                        .put("startedAt", ticket.startedAt)
                        .toString(),
                )
                .commit(),
        ) {
            "Team matchmaking ticket could not be persisted"
        }
    }

    fun load(): TeamMatchmakingTicket? {
        val raw = prefs.getString(KEY_JSON, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val requestId = json.getString("requestId")
            val displayName = json.getString("displayName")
            val startedAt = json.getLong("startedAt")
            require(runCatching { UUID.fromString(requestId) }.isSuccess)
            require(displayName.length in 2..24)
            require(startedAt > 0L)
            TeamMatchmakingTicket(
                requestId = requestId,
                displayName = displayName,
                startedAt = startedAt,
            )
        }.getOrElse {
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "ludoproof_team_matchmaking"
        const val KEY_JSON = "team_search_ticket"
    }
}
