package com.ludoproof.game.feature.online

import android.view.View
import com.ludoproof.game.*
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.ui.online.stylePublicPlayerCountButton
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

    runMatchmakingRequest(
        action = {
            val credential =
                ensureLeaderboardCredential()
            api.searchPublicMatch(
                profileToken =
                    credential.profileToken,
                displayName =
                    ticket.displayName,
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
            val credential =
                ensureLeaderboardCredential()
            api.publicMatchStatus(
                profileToken =
                    credential.profileToken,
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
            val credential =
                ensureLeaderboardCredential()
            api.cancelPublicMatch(
                profileToken =
                    credential.profileToken,
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
            val count =
                response.optInt(
                    "targetPlayerCount",
                    selectedPublicPlayerCount,
                )
                .coerceIn(
                    2,
                    4,
                )

            if (
                isMatchmakingUiReady()
            ) {
                matchmakingOpponentRail
                    .updateState(
                        queuedPlayers =
                            count,
                        targetPlayerCount =
                            count,
                        searching =
                            true,
                    )
                matchmakingStatusText.text =
                    "MATCH FOUND • STARTING…"
            }

            publicMatchmakingStore
                .clear()
            captureSession(
                response,
            )
            applyResponse(
                response,
                announce = false,
            )
            connectRealtimeIfPossible()

            showStatus(
                "Match found • " +
                    count +
                    " real players • game started.",
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
                "Search expired. Choose players and tap PLAY to search again.",
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
        !isMatchmakingUiReady()
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
        !isOnline ||
        realtimeConnected
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
        !isMatchmakingUiReady()
    ) {
        return
    }

    updatePublicPlayerCountButtons()

    matchmakingSetupPanel.visibility =
        if (
            searching
        ) {
            View.GONE
        } else {
            View.VISIBLE
        }
    matchmakingSearchPanel.visibility =
        if (
            searching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }

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
    matchmakingTimerText.visibility =
        if (
            searching
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    matchmakingSlotsText.visibility =
        View.GONE

    val safeTarget =
        if (
            targetPlayerCount ==
            4
        ) {
            4
        } else {
            2
        }
    val safeQueued =
        if (
            searching
        ) {
            queuedPlayers.coerceIn(
                1,
                safeTarget,
            )
        } else {
            1
        }

    matchmakingOpponentRail
        .updateState(
            queuedPlayers =
                safeQueued,
            targetPlayerCount =
                safeTarget,
            searching =
                searching,
        )

    if (
        searching
    ) {
        val opponentsFound =
            (
                safeQueued -
                    1
                )
                .coerceAtLeast(
                    0,
                )
        val opponentsNeeded =
            safeTarget -
                1
        val remaining =
            (
                opponentsNeeded -
                    opponentsFound
                )
                .coerceAtLeast(
                    0,
                )

        matchmakingStatusText.text =
            when {
                remaining <=
                    0 ->
                    "MATCH FOUND • STARTING…"

                opponentsFound ==
                    0 ->
                    "SEARCHING FOR " +
                        opponentsNeeded +
                        if (
                            opponentsNeeded ==
                            1
                        ) {
                            " PLAYER…"
                        } else {
                            " PLAYERS…"
                        }

                else ->
                    opponentsFound
                        .toString() +
                        "/" +
                        opponentsNeeded +
                        " PLAYERS FOUND • " +
                        remaining +
                        " MORE…"
            }

        matchmakingSlotsText.text =
            "You ready • " +
                opponentsFound +
                "/" +
                opponentsNeeded +
                " opponents found"

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
        !isPlayerCountUiReady()
    ) {
        return
    }

    val twoSelected =
        selectedPublicPlayerCount ==
            2

    stylePublicPlayerCountButton(
        twoPlayerButton,
        selected =
            twoSelected,
    )
    stylePublicPlayerCountButton(
        fourPlayerButton,
        selected =
            !twoSelected,
    )
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
            if (
                isMatchmakingUiReady() &&
                publicMatchmakingStore
                    .load() !=
                null
            ) {
                matchmakingStatusText.text =
                    "WAITING FOR INTERNET…"
            }
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
                            if (
                                isMatchmakingUiReady() &&
                                publicMatchmakingStore
                                    .load() !=
                                null
                            ) {
                                matchmakingStatusText.text =
                                    "SEARCH CONNECTION ERROR • RETRYING…"
                            }
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
