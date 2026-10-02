package com.ludoproof.game.feature.profile.domain

import com.ludoproof.game.feature.profile.domain.model.ProfileBadge
import com.ludoproof.game.feature.profile.domain.model.ProfileLevelProgress
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot

object ProfileProgression {
    fun xpAward(
        won: Boolean,
    ): Int =
        20 +
            if (won) {
                30
            } else {
                0
            }

    fun levelProgress(
        totalXp: Int,
    ): ProfileLevelProgress {
        var remaining =
            totalXp.coerceAtLeast(0)
        var level =
            1
        var requirement =
            xpRequirement(level)

        while (
            remaining >=
                requirement &&
            level <
                100
        ) {
            remaining -=
                requirement
            level +=
                1
            requirement =
                xpRequirement(level)
        }

        return ProfileLevelProgress(
            level =
                level,
            xpIntoLevel =
                remaining,
            xpForNextLevel =
                requirement,
            fraction =
                if (requirement > 0) {
                    (
                        remaining
                            .toFloat() /
                            requirement
                                .toFloat()
                        )
                        .coerceIn(
                            0f,
                            1f,
                        )
                } else {
                    1f
                },
        )
    }

    fun badges(
        profile: ProfileSnapshot,
    ): List<ProfileBadge> =
        listOf(
            ProfileBadge(
                id = "first-win",
                title = "First Win",
                symbol = "★",
                unlocked =
                    profile.totalWins >=
                        1,
                requirement =
                    "Win your first game",
            ),
            ProfileBadge(
                id = "strategist",
                title = "Strategist",
                symbol = "◆",
                unlocked =
                    profile.totalWins >=
                        5,
                requirement =
                    "Win 5 games",
            ),
            ProfileBadge(
                id = "unstoppable",
                title = "Unstoppable",
                symbol = "✦",
                unlocked =
                    profile.bestWinStreak >=
                        3,
                requirement =
                    "Win 3 games in a row",
            ),
            ProfileBadge(
                id = "friendship",
                title = "Friendship",
                symbol = "♢",
                unlocked =
                    profile.onlineGames >=
                        3,
                requirement =
                    "Complete 3 online games",
            ),
            ProfileBadge(
                id = "social-gamer",
                title = "Social Gamer",
                symbol = "♛",
                unlocked =
                    profile.onlineGames >=
                        10,
                requirement =
                    "Complete 10 online games",
            ),
            ProfileBadge(
                id = "ludo-novice",
                title = "Ludo Novice",
                symbol = "✪",
                unlocked =
                    profile.totalGames >=
                        1,
                requirement =
                    "Complete a Ludo game",
            ),
        )

    private fun xpRequirement(
        level: Int,
    ): Int =
        100 +
            (
                level -
                    1
                ) *
            25
}
