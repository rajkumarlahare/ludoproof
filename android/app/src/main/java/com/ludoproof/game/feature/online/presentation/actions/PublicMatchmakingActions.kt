package com.ludoproof.game.feature.online

import android.view.View
import com.ludoproof.game.*
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import org.json.JSONObject
import java.util.UUID

internal fun MainActivity.selectPublicPlayerCount(
    playerCount: Int,
) {
    if (
        playerCount !=
            2 &&
        playerCount !=
            4
    ) {
        return
    }
    if (
        publicMatchmakingStore.load() !=
            null ||
        matchId !=
            null
    ) {
        return
    }

    selectedPublicPlayerCount =
        playerCount
    updatePublicPlayerCountButtons()
}

internal fun MainActivity.beginPublicMatchmaking() {
    if (
        matchId !=
            null &&
        currentState
            ?.status !=
        "FINISHED"
    ) {
        showStatus(
            "Finish the current online match before searching again.",
        )
        return
    }

    val existing =
        publicMatchmakingStore
            .load()
    if (
        existing !=
        null
    ) {
        selectedPublicPlayerCount =
            existing.playerCount
        setPublicSearchUi(
            searching = true,
            queuedPlayers = 1,
            targetPlayerCount =
                existing.playerCount,
        )
        pollPublicMatchmaking()
        return
    }

    val displayName =
        runCatching {
            playerName()
        }
            .getOrElse {
                showStatus(
                    it.message
                        ?: "Invalid player name",
                )
                return
            }

    runCatching {
        ProfileStore(
            this,
        ).updateDisplayName(
            displayName,
        )
    }

    val ticket =
        PublicMatchmakingTicket(
            requestId =
                UUID.randomUUID()
                    .toString(),
            displayName =
                displayName,
            playerCount =
                selectedPublicPlayerCount,
            startedAt =
                System.currentTimeMillis(),
        )

    runCatching {
        publicMatchmakingStore
            .save(
                ticket,
            )
    }
        .onFailure {
            showStatus(
                "Could not save matchmaking search.",
            )
            return
        }

    setPublicSearchUi(
        searching = true,
        queuedPlayers = 1,
        targetPlayerCount =
            ticket.playerCount,
    )
    showStatus(
        "Searching for a public match…",
    )

    val profileId =
        runCatching {
            LeaderboardIdentityStore(
                this,
            ).profileId()
        }
            .getOrNull()

    runMatchmakingRequest(
        action = {
            api.searchPublicMatch(
                displayName =
                    ticket.displayName,
                clientRequestId =
                    ticket.requestId,
                playerCount =
                    ticket.playerCount,
                profileId =
                    profileId,
            )
        },
        onSuccess = {
            applyPublicMatchmakingResponse(
                it,
            )
        },
    )
}

internal fun MainActivity.pollPublicMatchmaking() {
    val ticket =
        publicMatchmakingStore
            .load()
            ?: return
    if (
        matchId !=
            null &&
        currentState
            ?.status !=
        "FINISHED"
    ) {
        publicMatchmakingStore
            .clear()
        setPublicSearchUi(
            searching = false,
        )
        return
    }

    runMatchmakingRequest(
        action = {
            api.publicMatchStatus(
                clientRequestId =
                    ticket.requestId,
                playerCount =
                    ticket.playerCount,
            )
        },
        onSuccess = {
            applyPublicMatchmakingResponse(
                it,
            )
        },
        quietFailure = true,
    )
}

internal fun MainActivity.cancelPublicMatchmaking() {
    val ticket =
        publicMatchmakingStore
            .load()
            ?: run {
                setPublicSearchUi(
                    searching = false,
                )
                return
            }

    runMatchmakingRequest(
        action = {
            api.cancelPublicMatch(
                clientRequestId =
                    ticket.requestId,
                playerCount =
                    ticket.playerCount,
            )
        },
        onSuccess = {
                response ->
            when (
                response.optString(
                    "status",
                )
            ) {
                "MATCHED" -> {
                    showStatus(
                        "A match was found while cancelling. Opening it now…",
                    )
                    pollPublicMatchmaking()
                }

                else -> {
                    publicMatchmakingStore
                        .clear()
                    setPublicSearchUi(
                        searching = false,
                    )
                    showStatus(
                        "Matchmaking cancelled.",
                    )
                }
            }
        },
    )
}

internal fun MainActivity.applyPublicMatchmakingResponse(
    response: JSONObject,
) {
    when (
        response.optString(
            "status",
        )
    ) {
        "MATCHED" -> {
            publicMatchmakingStore
                .clear()
            setPublicSearchUi(
                searching = false,
            )
            captureSession(
                response,
            )
            applyResponse(
                response,
                announce = false,
            )
            connectRealtimeIfPossible()

            val count =
                response.optInt(
                    "targetPlayerCount",
                    currentState
                        ?.players
                        ?.size
                        ?: 0,
                )
            showStatus(
                if (
                    count >
                    0
                ) {
                    "Match found • $count players • live sync connected when available."
                } else {
                    "Match found • live sync connected when available."
                },
            )
        }

        "SEARCHING" -> {
            val target =
                response.optInt(
                    "targetPlayerCount",
                    selectedPublicPlayerCount,
                )
            val queued =
                response.optInt(
                    "queuedPlayers",
                    1,
                )
                .coerceIn(
                    1,
                    target,
                )
            setPublicSearchUi(
                searching = true,
                queuedPlayers =
                    queued,
                targetPlayerCount =
                    target,
            )
        }

        "IDLE" -> {
            publicMatchmakingStore
                .clear()
            setPublicSearchUi(
                searching = false,
            )
            showStatus(
                "Search expired. Tap Find Match to search again.",
            )
        }

        else -> {
            showStatus(
                "Unexpected matchmaking response.",
            )
        }
    }
}

internal fun MainActivity.restorePublicMatchmakingUi() {
    if (
        !::findMatchButton
            .isInitialized
    ) {
        return
    }

    val ticket =
        publicMatchmakingStore
            .load()
    if (
        matchId !=
            null &&
        currentState
            ?.status !=
        "FINISHED"
    ) {
        if (
            ticket !=
            null
        ) {
            publicMatchmakingStore
                .clear()
        }
        setPublicSearchUi(
            searching = false,
        )
        return
    }

    if (
        ticket ==
        null
    ) {
        setPublicSearchUi(
            searching = false,
        )
        return
    }

    selectedPublicPlayerCount =
        ticket.playerCount
    nameInput.setText(
        ticket.displayName,
    )
    setPublicSearchUi(
        searching = true,
        queuedPlayers = 1,
        targetPlayerCount =
            ticket.playerCount,
    )
}

internal fun MainActivity.connectRealtimeIfPossible() {
    if (
        !canRenderUi() ||
        !isOnline
    ) {
        return
    }

    val code =
        matchId
            ?: return
    val token =
        playerToken
            ?: return

    if (
        currentState
            ?.status ==
        "FINISHED"
    ) {
        realtimeClient.disconnect()
        realtimeConnected =
            false
        updateConnectionLabel()
        return
    }

    realtimeClient.connect(
        matchId =
            code,
        playerToken =
            token,
    )
}

internal fun MainActivity.setPublicSearchUi(
    searching: Boolean,
    queuedPlayers: Int = 0,
    targetPlayerCount: Int =
        selectedPublicPlayerCount,
) {
    if (
        !::findMatchButton
            .isInitialized
    ) {
        return
    }

    updatePublicPlayerCountButtons()

    findMatchButton.visibility =
        if (
            searching
        ) {
            View.GONE
        } else {
            View.VISIBLE
        }
    cancelMatchmakingButton.visibility =
        if (
            searching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    matchmakingStatusText.visibility =
        if (
            searching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    matchmakingSlotsText.visibility =
        if (
            searching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    matchmakingTimerText.visibility =
        if (
            searching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

    if (
        searching
    ) {
        matchmakingStatusText.text =
            "SEARCHING FOR PLAYERS…  " +
                queuedPlayers +
                "/" +
                targetPlayerCount

        matchmakingSlotsText.text =
            buildString {
                for (
                    seat in
                    1..targetPlayerCount
                ) {
                    if (
                        seat >
                        1
                    ) {
                        append(
                            "\n",
                        )
                    }

                    if (
                        seat ==
                        1
                    ) {
                        append(
                            "YOU   •   READY ✓",
                        )
                    } else {
                        append(
                            "PLAYER ",
                        )
                        append(
                            seat,
                        )
                        append(
                            "   •   ",
                        )
                        append(
                            if (
                                seat <=
                                queuedPlayers
                            ) {
                                "FOUND ✓"
                            } else {
                                "SEARCHING…"
                            },
                        )
                    }
                }
            }

        val startedAt =
            publicMatchmakingStore
                .load()
                ?.startedAt
                ?: System.currentTimeMillis()
        val elapsedSeconds =
            (
                (
                    System.currentTimeMillis() -
                        startedAt
                    ) /
                    1_000L
                )
                .coerceAtLeast(
                    0L,
                )
        val minutes =
            elapsedSeconds /
                60L
        val seconds =
            elapsedSeconds %
                60L
        matchmakingTimerText.text =
            "SEARCH  " +
                String.format(
                    java.util.Locale.US,
                    "%02d:%02d",
                    minutes,
                    seconds,
                )
    }

    val interactive =
        !searching &&
            isOnline &&
            (
                matchId ==
                    null ||
                currentState
                    ?.status ==
                "FINISHED"
            )

    nameInput.isEnabled =
        interactive
    twoPlayerButton.isEnabled =
        interactive
    fourPlayerButton.isEnabled =
        interactive
    findMatchButton.isEnabled =
        interactive
    cancelMatchmakingButton.isEnabled =
        searching &&
            isOnline

    matchInput.isEnabled =
        interactive
    createButton.isEnabled =
        interactive
    joinButton.isEnabled =
        interactive
}

internal fun MainActivity.updatePublicPlayerCountButtons() {
    if (
        !::twoPlayerButton
            .isInitialized
    ) {
        return
    }

    val twoSelected =
        selectedPublicPlayerCount ==
            2

    twoPlayerButton.text =
        if (
            twoSelected
        ) {
            "✓  2 PLAYERS"
        } else {
            "2 PLAYERS"
        }
    fourPlayerButton.text =
        if (
            twoSelected
        ) {
            "4 PLAYERS"
        } else {
            "✓  4 PLAYERS"
        }

    if (
        twoSelected
    ) {
        LudoProofTheme.positive(
            twoPlayerButton,
        )
        LudoProofTheme.secondary(
            fourPlayerButton,
        )
    } else {
        LudoProofTheme.secondary(
            twoPlayerButton,
        )
        LudoProofTheme.positive(
            fourPlayerButton,
        )
    }
}

private fun MainActivity.runMatchmakingRequest(
    action: () -> JSONObject,
    onSuccess: (
        JSONObject,
    ) -> Unit,
    quietFailure: Boolean = false,
) {
    if (
        matchmakingRequestInFlight ||
        !canRenderUi() ||
        executor.isShutdown
    ) {
        return
    }

    if (
        !isOnline
    ) {
        if (
            !quietFailure
        ) {
            showStatus(
                "Internet connection is required for public matchmaking.",
            )
        }
        return
    }

    matchmakingRequestInFlight =
        true

    val submitted =
        runCatching {
            executor.execute {
                try {
                    val response =
                        action()
                    mainHandler.post {
                        matchmakingRequestInFlight =
                            false
                        if (
                            canRenderUi()
                        ) {
                            onSuccess(
                                response,
                            )
                        }
                    }
                } catch (
                    error: Exception,
                ) {
                    mainHandler.post {
                        matchmakingRequestInFlight =
                            false
                        if (
                            !canRenderUi()
                        ) {
                            return@post
                        }

                        if (
                            !quietFailure
                        ) {
                            showStatus(
                                "Matchmaking error: " +
                                    (
                                        error.message
                                            ?: error
                                                .toString()
                                        ),
                            )
                        }
                    }
                }
            }
        }
            .isSuccess

    if (!submitted) {
        matchmakingRequestInFlight =
            false
        if (
            !quietFailure &&
            canRenderUi()
        ) {
            showStatus(
                "Matchmaking request was cancelled because the screen is closing.",
            )
        }
    }
}
