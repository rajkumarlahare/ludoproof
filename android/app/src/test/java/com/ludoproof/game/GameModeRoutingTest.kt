package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameModeRoutingTest {
    @Test
    fun onlineRoutesToOnlineActivity() {
        assertEquals(
            GameModeDestination.ONLINE_ACTIVITY,
            GameModeRouting.destination(
                GameMode.ONLINE,
            ),
        )
    }

    @Test
    fun teamStaysOnRemoteEntryWhileFriendsRoutesToFriendsHub() {
        assertEquals(
            GameModeDestination.REMOTE_MODE_ENTRY,
            GameModeRouting.destination(
                GameMode.TEAM_UP,
            ),
        )
        assertEquals(
            GameModeDestination.FRIENDS_ACTIVITY,
            GameModeRouting.destination(
                GameMode.FRIENDS,
            ),
        )
    }

    @Test
    fun computerAndPassAndPlayRouteToOfflineEngine() {
        assertEquals(
            GameModeDestination.OFFLINE_ACTIVITY,
            GameModeRouting.destination(
                GameMode.COMPUTER,
            ),
        )
        assertEquals(
            GameModeDestination.OFFLINE_ACTIVITY,
            GameModeRouting.destination(
                GameMode.PASS_AND_PLAY,
            ),
        )
    }
}
