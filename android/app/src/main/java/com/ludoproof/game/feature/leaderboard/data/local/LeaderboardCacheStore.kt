package com.ludoproof.game.feature.leaderboard.data.local

import android.content.Context
import org.json.JSONObject

class LeaderboardCacheStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun save(
        payload: JSONObject,
    ) {
        val raw =
            payload.toString()
        if (
            raw.length >
            MAX_CACHE_CHARS
        ) {
            return
        }

        prefs.edit()
            .putString(
                KEY_PAYLOAD,
                raw,
            )
            .putLong(
                KEY_SAVED_AT,
                System.currentTimeMillis(),
            )
            .apply()
    }

    fun load():
        CachedLeaderboard? {
        val raw =
            prefs.getString(
                KEY_PAYLOAD,
                null,
            )
                ?: return null
        val savedAt =
            prefs.getLong(
                KEY_SAVED_AT,
                0L,
            )

        return runCatching {
            CachedLeaderboard(
                payload =
                    JSONObject(
                        raw,
                    ),
                savedAt =
                    savedAt,
            )
        }.getOrNull()
    }

    data class CachedLeaderboard(
        val payload: JSONObject,
        val savedAt: Long,
    )

    private companion object {
        const val PREFS_NAME =
            "ludoproof_leaderboard_cache"
        const val KEY_PAYLOAD =
            "payload_v1"
        const val KEY_SAVED_AT =
            "saved_at_v1"
        const val MAX_CACHE_CHARS =
            96_000
    }
}
