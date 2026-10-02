package com.ludoproof.game.feature.profile.data.local

import android.content.Context
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.profile.domain.model.ProfileGameMode
import com.ludoproof.game.feature.profile.domain.model.ProfileMatchSource
import com.ludoproof.game.feature.profile.domain.model.ProfileModeStats
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class ProfileStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun snapshot():
        ProfileSnapshot =
        load()
            .toSnapshot()

    @Synchronized
    fun updateDisplayName(
        value: String,
    ): ProfileSnapshot {
        val normalized =
            value
                .trim()
                .replace(
                    Regex("\\s+"),
                    " ",
                )
                .take(
                    MAX_NAME_LENGTH,
                )
                .ifBlank {
                    "Guest User"
                }

        val state =
            load()
        state.displayName =
            normalized
        save(
            state,
        )
        return state
            .toSnapshot()
    }

    @Synchronized
    fun recordCompletedMatch(
        matchId: String,
        mode: ProfileGameMode,
        source: ProfileMatchSource,
        won: Boolean,
    ): Boolean {
        if (
            matchId.isBlank()
        ) {
            return false
        }

        val state =
            load()
        if (
            matchId in
                state.recordedMatchIds
        ) {
            return false
        }

        state.recordedMatchIds +=
            matchId
        while (
            state.recordedMatchIds
                .size >
                MAX_RECORDED_MATCHES
        ) {
            val oldest =
                state.recordedMatchIds
                    .firstOrNull()
                    ?: break
            state.recordedMatchIds
                .remove(
                    oldest,
                )
        }

        state.totalGames +=
            1
        if (won) {
            state.totalWins +=
                1
            state.currentWinStreak +=
                1
            state.bestWinStreak =
                maxOf(
                    state.bestWinStreak,
                    state.currentWinStreak,
                )
        } else {
            state.currentWinStreak =
                0
        }

        when (source) {
            ProfileMatchSource.ONLINE ->
                state.onlineGames +=
                    1
            ProfileMatchSource.LOCAL ->
                state.localGames +=
                    1
            ProfileMatchSource.COMPUTER ->
                Unit
        }

        val currentStats =
            state.modeStats[
                mode
            ] ?: ProfileModeStats()
        state.modeStats[
            mode
        ] =
            currentStats.copy(
                games =
                    currentStats.games +
                        1,
                wins =
                    currentStats.wins +
                        if (won) {
                            1
                        } else {
                            0
                        },
            )

        state.totalXp +=
            ProfileProgression
                .xpAward(
                    won,
                )

        val today =
            LocalDate
                .now()
                .toEpochDay()
        if (
            state.lastPlayedDay !=
                today
        ) {
            state.currentDayStreak =
                if (
                    state.lastPlayedDay ==
                        today -
                            1
                ) {
                    state.currentDayStreak +
                        1
                } else {
                    1
                }
            state.bestDayStreak =
                maxOf(
                    state.bestDayStreak,
                    state.currentDayStreak,
                )
            state.lastPlayedDay =
                today
        }

        save(
            state,
        )
        return true
    }

    @Synchronized
    fun addPurchase(
        productId: String,
    ): Boolean {
        val normalized =
            productId
                .trim()
        if (
            normalized.isBlank()
        ) {
            return false
        }

        val state =
            load()
        if (
            !state.purchasedProducts
                .add(
                    normalized,
                )
        ) {
            return false
        }

        save(
            state,
        )
        return true
    }

    private fun load():
        MutableProfileState {
        val raw =
            prefs.getString(
                KEY_PROFILE_JSON,
                null,
            ) ?: return MutableProfileState()

        return runCatching {
            val json =
                JSONObject(
                    raw,
                )
            val modes =
                json.optJSONObject(
                    "modes",
                )

            MutableProfileState(
                displayName =
                    json.optString(
                        "displayName",
                        "Guest User",
                    ),
                totalGames =
                    json.optInt(
                        "totalGames",
                        0,
                    ),
                totalWins =
                    json.optInt(
                        "totalWins",
                        0,
                    ),
                currentDayStreak =
                    json.optInt(
                        "currentDayStreak",
                        0,
                    ),
                bestDayStreak =
                    json.optInt(
                        "bestDayStreak",
                        0,
                    ),
                currentWinStreak =
                    json.optInt(
                        "currentWinStreak",
                        0,
                    ),
                bestWinStreak =
                    json.optInt(
                        "bestWinStreak",
                        0,
                    ),
                totalXp =
                    json.optInt(
                        "totalXp",
                        0,
                    ),
                onlineGames =
                    json.optInt(
                        "onlineGames",
                        0,
                    ),
                localGames =
                    json.optInt(
                        "localGames",
                        0,
                    ),
                lastPlayedDay =
                    if (
                        json.has(
                            "lastPlayedDay",
                        )
                    ) {
                        json.optLong(
                            "lastPlayedDay",
                            Long.MIN_VALUE,
                        )
                            .takeIf {
                                it !=
                                    Long.MIN_VALUE
                            }
                    } else {
                        null
                    },
                modeStats =
                    ProfileGameMode
                        .entries
                        .associateWith {
                                mode ->
                            val value =
                                modes
                                    ?.optJSONObject(
                                        mode.name,
                                    )
                            ProfileModeStats(
                                games =
                                    value
                                        ?.optInt(
                                            "games",
                                            0,
                                        )
                                        ?: 0,
                                wins =
                                    value
                                        ?.optInt(
                                            "wins",
                                            0,
                                        )
                                        ?: 0,
                            )
                        }
                        .toMutableMap(),
                purchasedProducts =
                    json.optJSONArray(
                        "purchases",
                    )
                        .toStringSet(),
                recordedMatchIds =
                    json.optJSONArray(
                        "recordedMatchIds",
                    )
                        .toLinkedStringSet(),
            )
        }.getOrElse {
            MutableProfileState()
        }
    }

    private fun save(
        state: MutableProfileState,
    ) {
        val modes =
            JSONObject()
        state.modeStats
            .forEach {
                    (
                        mode,
                        stats,
                    ) ->
                modes.put(
                    mode.name,
                    JSONObject()
                        .put(
                            "games",
                            stats.games,
                        )
                        .put(
                            "wins",
                            stats.wins,
                        ),
                )
            }

        val json =
            JSONObject()
                .put(
                    "displayName",
                    state.displayName,
                )
                .put(
                    "totalGames",
                    state.totalGames,
                )
                .put(
                    "totalWins",
                    state.totalWins,
                )
                .put(
                    "currentDayStreak",
                    state.currentDayStreak,
                )
                .put(
                    "bestDayStreak",
                    state.bestDayStreak,
                )
                .put(
                    "currentWinStreak",
                    state.currentWinStreak,
                )
                .put(
                    "bestWinStreak",
                    state.bestWinStreak,
                )
                .put(
                    "totalXp",
                    state.totalXp,
                )
                .put(
                    "onlineGames",
                    state.onlineGames,
                )
                .put(
                    "localGames",
                    state.localGames,
                )
                .put(
                    "lastPlayedDay",
                    state.lastPlayedDay,
                )
                .put(
                    "modes",
                    modes,
                )
                .put(
                    "purchases",
                    JSONArray(
                        state.purchasedProducts
                            .toList(),
                    ),
                )
                .put(
                    "recordedMatchIds",
                    JSONArray(
                        state.recordedMatchIds
                            .toList(),
                    ),
                )

        check(
            prefs.edit()
                .putString(
                    KEY_PROFILE_JSON,
                    json.toString(),
                )
                .commit(),
        ) {
            "Profile stats could not be persisted"
        }
    }

    private fun JSONArray?
        .toStringSet():
        MutableSet<String> =
        linkedSetOf<String>()
            .also {
                    result ->
                if (
                    this ==
                    null
                ) {
                    return@also
                }
                for (
                    index in
                    0 until
                        length()
                ) {
                    optString(
                        index,
                    )
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.let(
                            result::add,
                        )
                }
            }

    private fun JSONArray?
        .toLinkedStringSet():
        LinkedHashSet<String> =
        LinkedHashSet(
            toStringSet(),
        )

    private data class MutableProfileState(
        var displayName: String = "Guest User",
        var totalGames: Int = 0,
        var totalWins: Int = 0,
        var currentDayStreak: Int = 0,
        var bestDayStreak: Int = 0,
        var currentWinStreak: Int = 0,
        var bestWinStreak: Int = 0,
        var totalXp: Int = 0,
        var onlineGames: Int = 0,
        var localGames: Int = 0,
        var lastPlayedDay: Long? = null,
        val modeStats:
            MutableMap<
                ProfileGameMode,
                ProfileModeStats,
                > =
            ProfileGameMode
                .entries
                .associateWith {
                    ProfileModeStats()
                }
                .toMutableMap(),
        val purchasedProducts:
            MutableSet<String> =
            linkedSetOf(),
        val recordedMatchIds:
            LinkedHashSet<String> =
            linkedSetOf(),
    ) {
        fun toSnapshot():
            ProfileSnapshot =
            ProfileSnapshot(
                displayName =
                    displayName,
                totalGames =
                    totalGames,
                totalWins =
                    totalWins,
                currentDayStreak =
                    currentDayStreak,
                bestDayStreak =
                    bestDayStreak,
                currentWinStreak =
                    currentWinStreak,
                bestWinStreak =
                    bestWinStreak,
                totalXp =
                    totalXp,
                onlineGames =
                    onlineGames,
                localGames =
                    localGames,
                classic =
                    modeStats[
                        ProfileGameMode.CLASSIC
                    ] ?: ProfileModeStats(),
                rush =
                    modeStats[
                        ProfileGameMode.RUSH
                    ] ?: ProfileModeStats(),
                snakeLadder =
                    modeStats[
                        ProfileGameMode.SNAKE_LADDER
                    ] ?: ProfileModeStats(),
                purchasedProducts =
                    purchasedProducts
                        .toList(),
            )
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_profile"
        const val KEY_PROFILE_JSON =
            "profile_json_v1"
        const val MAX_RECORDED_MATCHES =
            250
        const val MAX_NAME_LENGTH =
            24
    }
}
