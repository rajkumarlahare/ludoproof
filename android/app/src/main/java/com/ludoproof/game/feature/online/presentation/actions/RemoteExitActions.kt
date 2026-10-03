package com.ludoproof.game.feature.online

import com.ludoproof.game.GameApiException
import com.ludoproof.game.MainActivity
import com.ludoproof.game.RemoteForfeitApi
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.model.ProfileGameMode
import com.ludoproof.game.feature.profile.domain.model.ProfileMatchSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

private val remoteForfeitExecutor =
    Executors
        .newSingleThreadExecutor {
                work ->
            Thread(
                work,
                "ludoproof-remote-forfeit",
            ).apply {
                isDaemon =
                    true
            }
        }

private val inFlightForfeits =
    ConcurrentHashMap
        .newKeySet<String>()

internal fun MainActivity.abandonRemoteSessionState() {
    val abandonedMatchId =
        matchId
    val abandonedPlayerToken =
        playerToken
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

    if (
        wasActive &&
        !abandonedMatchId
            .isNullOrBlank() &&
        !abandonedPlayerToken
            .isNullOrBlank()
    ) {
        enqueueAuthoritativeForfeit(
            matchId =
                abandonedMatchId,
            playerToken =
                abandonedPlayerToken,
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

private fun enqueueAuthoritativeForfeit(
    matchId: String,
    playerToken: String,
) {
    val key =
        matchId
            .trim()
            .uppercase()
    if (
        !inFlightForfeits.add(
            key,
        )
    ) {
        return
    }

    remoteForfeitExecutor.execute {
        try {
            repeat(
                3,
            ) {
                    attempt ->
                try {
                    RemoteForfeitApi
                        .forfeit(
                            matchId =
                                matchId,
                            playerToken =
                                playerToken,
                        )
                    return@execute
                } catch (
                    error:
                    GameApiException,
                ) {
                    if (
                        error.code in
                        TERMINAL_FORFEIT_CODES
                    ) {
                        return@execute
                    }
                } catch (_: Exception) {
                    // Retry transient transport failures below.
                }

                if (
                    attempt <
                    2
                ) {
                    try {
                        Thread.sleep(
                            700L *
                                (
                                    attempt +
                                        1
                                    ),
                        )
                    } catch (_: InterruptedException) {
                        Thread.currentThread()
                            .interrupt()
                        return@execute
                    }
                }
            }
        } finally {
            inFlightForfeits.remove(
                key,
            )
        }
    }
}

private val TERMINAL_FORFEIT_CODES =
    setOf(
        "INVALID_MATCH_STATUS",
        "MATCH_NOT_FOUND",
        "PLAYER_FORFEITED",
        "INVALID_PLAYER_TOKEN",
        "AUTH_REQUIRED",
    )
