package com.ludoproof.game.feature.leaderboard.data.local

import android.content.Context
import java.util.Locale
import java.util.UUID

class LeaderboardIdentityStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    @Synchronized
    fun profileId(): String {
        val existing =
            prefs.getString(
                KEY_PROFILE_ID,
                null,
            )
                ?.trim()
                ?.lowercase(
                    Locale.ROOT,
                )
        if (
            existing != null &&
            PROFILE_ID.matches(
                existing,
            )
        ) {
            return existing
        }

        val generated =
            UUID.randomUUID()
                .toString()
                .lowercase(
                    Locale.ROOT,
                )
        check(
            prefs.edit()
                .putString(
                    KEY_PROFILE_ID,
                    generated,
                )
                .commit(),
        ) {
            "Leaderboard identity could not be persisted"
        }
        return generated
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_leaderboard_identity"
        const val KEY_PROFILE_ID =
            "profile_id_v1"
        val PROFILE_ID =
            Regex(
                "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            )
    }
}
