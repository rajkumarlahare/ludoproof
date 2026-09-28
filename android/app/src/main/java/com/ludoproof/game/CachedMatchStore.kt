package com.ludoproof.game

import android.content.Context
import org.json.JSONObject

class CachedMatchStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun save(
        playerId: String?,
        state: JSONObject?,
    ) {
        if (state == null) return
        val safe =
            JSONObject()
                .put(
                    "playerId",
                    playerId,
                )
                .put(
                    "state",
                    state,
                )
        prefs.edit()
            .putString(KEY_JSON, safe.toString())
            .commit()
    }

    fun load(): JSONObject? =
        prefs.getString(KEY_JSON, null)
            ?.let {
                runCatching {
                    JSONObject(it)
                }.getOrNull()
            }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_cached_match"
        const val KEY_JSON =
            "safe_match_envelope"
    }
}
