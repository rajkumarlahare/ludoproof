package com.ludoproof.game.feature.profile.domain

import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileProgressionTest {
    @Test
    fun levelProgressStartsAtLevelOne() {
        val value =
            ProfileProgression
                .levelProgress(
                    0,
                )

        assertEquals(
            1,
            value.level,
        )
        assertEquals(
            0,
            value.xpIntoLevel,
        )
        assertEquals(
            100,
            value.xpForNextLevel,
        )
    }

    @Test
    fun levelProgressCarriesXpForward() {
        val value =
            ProfileProgression
                .levelProgress(
                    120,
                )

        assertEquals(
            2,
            value.level,
        )
        assertEquals(
            20,
            value.xpIntoLevel,
        )
        assertEquals(
            125,
            value.xpForNextLevel,
        )
    }

    @Test
    fun badgesReflectRealStats() {
        val badges =
            ProfileProgression
                .badges(
                    ProfileSnapshot(
                        totalGames = 3,
                        totalWins = 1,
                        bestWinStreak = 1,
                        onlineGames = 0,
                    ),
                )

        assertTrue(
            badges.first {
                it.id ==
                    "first-win"
            }.unlocked,
        )
        assertTrue(
            badges.first {
                it.id ==
                    "ludo-novice"
            }.unlocked,
        )
        assertFalse(
            badges.first {
                it.id ==
                    "strategist"
            }.unlocked,
        )
    }
}
