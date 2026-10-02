package com.ludoproof.game.feature.leaderboard.domain.model

import org.json.JSONObject

data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val games: Int,
    val wins: Int,
    val points: Int,
    val isViewer: Boolean = false,
)

data class LeaderboardSnapshot(
    val entries:
        List<LeaderboardEntry>,
    val viewer:
        LeaderboardEntry?,
    val finishPoints: Int,
    val winBonus: Int,
) {
    companion object {
        fun fromJson(
            payload: JSONObject,
        ): LeaderboardSnapshot {
            val scoring =
                payload.optJSONObject(
                    "scoring",
                )
            val array =
                payload.optJSONArray(
                    "entries",
                )
            val entries =
                buildList {
                    if (
                        array !=
                        null
                    ) {
                        for (
                            index in
                            0 until
                                array.length()
                        ) {
                            val item =
                                array
                                    .optJSONObject(
                                        index,
                                    )
                                    ?: continue
                            add(
                                item.toEntry(),
                            )
                        }
                    }
                }

            val viewer =
                payload
                    .optJSONObject(
                        "viewer",
                    )
                    ?.toEntry(
                        isViewerOverride =
                            true,
                    )

            return LeaderboardSnapshot(
                entries =
                    entries,
                viewer =
                    viewer,
                finishPoints =
                    scoring
                        ?.optInt(
                            "finish",
                            20,
                        )
                        ?: 20,
                winBonus =
                    scoring
                        ?.optInt(
                            "winBonus",
                            30,
                        )
                        ?: 30,
            )
        }

        private fun JSONObject.toEntry(
            isViewerOverride:
                Boolean? = null,
        ): LeaderboardEntry =
            LeaderboardEntry(
                rank =
                    optInt(
                        "rank",
                        0,
                    )
                        .coerceAtLeast(
                            0,
                        ),
                displayName =
                    optString(
                        "displayName",
                        "Player",
                    )
                        .ifBlank {
                            "Player"
                        },
                games =
                    optInt(
                        "games",
                        0,
                    )
                        .coerceAtLeast(
                            0,
                        ),
                wins =
                    optInt(
                        "wins",
                        0,
                    )
                        .coerceAtLeast(
                            0,
                        ),
                points =
                    optInt(
                        "points",
                        0,
                    )
                        .coerceAtLeast(
                            0,
                        ),
                isViewer =
                    isViewerOverride
                        ?: optBoolean(
                            "isViewer",
                            false,
                        ),
            )
    }
}
