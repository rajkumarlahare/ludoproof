package com.ludoproof.game.feature.online

import com.ludoproof.game.MainActivity
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.model.ProfileGameMode
import com.ludoproof.game.feature.profile.domain.model.ProfileMatchSource

internal fun MainActivity.abandonRemoteSessionState() {
    val abandonedMatchId =
        matchId
    val wasActive =
        currentState
            ?.status ==
            "ACTIVE"

    if (
        wasActive &&
        !abandonedMatchId
            .isNullOrBlank()
    ) {
        ProfileStore(this)
            .recordCompletedMatch(
                matchId =
                    abandonedMatchId,
                mode =
                    ProfileGameMode.CLASSIC,
                source =
                    ProfileMatchSource.ONLINE,
                won =
                    false,
            )
    }

    publicMatchmakingStore.clear()
    secureSessionStore.clear()
    pendingRollStore.clear()
    cachedMatchStore.clear()

    matchId = null
    playerToken = null
    playerId = null
    currentState = null
    pendingSecret = null

    realtimeConnected = false
    realtimeClient.disconnect()
}
