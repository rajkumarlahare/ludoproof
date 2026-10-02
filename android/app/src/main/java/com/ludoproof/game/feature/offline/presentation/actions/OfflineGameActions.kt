package com.ludoproof.game.feature.offline

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.PlayerSnapshot
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.ui.offline.gameplay.renderGame
import com.ludoproof.game.ui.offline.gameplay.showStatus

internal fun OfflineGameActivity.rollOffline() {
    val control =
        diceHost
            ?: return
    if (
        !control.isEnabled
    ) {
        return
    }

    val activeId =
        engine.activePlayerId()
    if (
        engine.isComputerPlayer(
            activeId,
        )
    ) {
        return
    }

    control.isEnabled =
        false
    control.alpha =
        .58f
    GameSoundFeedback.click(
        this,
    )
    diceView
        ?.startRolling()
    showStatus(
        "Rolling locally…",
    )

    val delay =
        GameSettingsStore(
            this,
        )
            .snapshot()
            .gameSpeed
            .rollDelayMs

    handler.postDelayed(
        {
            runCatching {
                engine.roll()
            }.onSuccess {
                    state ->
                renderGame(
                    state,
                )
            }.onFailure {
                    error ->
                diceView
                    ?.stopRolling()
                control.isEnabled =
                    true
                control.alpha =
                    1f
                showStatus(
                    error.message
                        ?: "Roll failed",
                )
            }
        },
        delay,
    )
}

internal fun OfflineGameActivity.scheduleComputerTurnIfNeeded(
    state: MatchSnapshot,
) {
    if (
        !isComputerMode ||
        state.status !=
        "ACTIVE"
    ) {
        computerActionRevision =
            null
        return
    }

    val active =
        state.players
            .getOrNull(
                state.turnSeat,
            )
            ?: return

    if (
        !engine.isComputerPlayer(
            active.playerId,
        )
    ) {
        computerActionRevision =
            null
        return
    }

    val actionKey =
        state.randomEventIndex *
            100 +
            state.turnSeat *
            10 +
            if (
                state.pendingRoll !=
                null
            ) {
                1
            } else {
                0
            }

    if (
        computerActionRevision ==
        actionKey
    ) {
        return
    }
    computerActionRevision =
        actionKey

    val speed =
        GameSettingsStore(
            this,
        )
            .snapshot()
            .gameSpeed

    if (
        state.pendingRoll ==
        null
    ) {
        diceView
            ?.startRolling()
        showStatus(
            active.displayName +
                " is rolling…",
        )
        handler.postDelayed(
            {
                val latest =
                    engine.snapshot()
                val latestActive =
                    latest
                        ?.players
                        ?.getOrNull(
                            latest.turnSeat,
                        )
                if (
                    latest ==
                    null ||
                    latest.status !=
                    "ACTIVE" ||
                    latest.pendingRoll !=
                    null ||
                    latestActive
                        ?.playerId !=
                    active.playerId
                ) {
                    return@postDelayed
                }

                runCatching {
                    engine.roll()
                }.onSuccess {
                        next ->
                    renderGame(
                        next,
                    )
                }.onFailure {
                        error ->
                    diceView
                        ?.stopRolling()
                    computerActionRevision =
                        null
                    showStatus(
                        error.message
                            ?: "Computer roll failed",
                    )
                }
            },
            speed.cpuThinkMs,
        )
        return
    }

    val pending =
        state.pendingRoll
            ?: return
    val outcome =
        pending.outcome
            ?: return
    val token =
        chooseComputerToken(
            state =
                state,
            player =
                active,
            legal =
                pending
                    .legalTokenIndexes,
            outcome =
                outcome,
        )

    if (
        token ==
        null
    ) {
        computerActionRevision =
            null
        return
    }

    showStatus(
        active.displayName +
            " is choosing a move…",
    )
    handler.postDelayed(
        {
            val latest =
                engine.snapshot()
            val latestActive =
                latest
                    ?.players
                    ?.getOrNull(
                        latest.turnSeat,
                    )
            if (
                latest ==
                null ||
                latest.status !=
                "ACTIVE" ||
                latestActive
                    ?.playerId !=
                active.playerId ||
                latest.pendingRoll
                    ?.legalTokenIndexes
                    ?.contains(
                        token,
                    ) !=
                true
            ) {
                return@postDelayed
            }

            runCatching {
                engine.move(
                    token,
                )
            }.onSuccess {
                    next ->
                GameSoundFeedback.move(
                    this,
                )
                renderGame(
                    next,
                )
            }.onFailure {
                    error ->
                computerActionRevision =
                    null
                showStatus(
                    error.message
                        ?: "Computer move failed",
                )
            }
        },
        (
            speed.cpuThinkMs /
                2
            )
            .coerceAtLeast(
                180L,
            ),
    )
}

private fun chooseComputerToken(
    state: MatchSnapshot,
    player: PlayerSnapshot,
    legal: Set<Int>,
    outcome: Int,
): Int? =
    legal.maxByOrNull {
            index ->
        computerMoveScore(
            state =
                state,
            player =
                player,
            tokenIndex =
                index,
            outcome =
                outcome,
        )
    }

private fun computerMoveScore(
    state: MatchSnapshot,
    player: PlayerSnapshot,
    tokenIndex: Int,
    outcome: Int,
): Int {
    val position =
        player.tokens
            .getOrNull(
                tokenIndex,
            )
            ?: -1
    val destination =
        if (
            position ==
            -1
        ) {
            0
        } else {
            position +
                outcome
        }

    if (
        destination ==
        57
    ) {
        return 100_000
    }

    var score =
        destination *
            20

    if (
        position ==
        -1
    ) {
        score +=
            4_000
    }

    if (
        destination in
        0..51
    ) {
        val offsets =
            mapOf(
                "RED" to 0,
                "GREEN" to 13,
                "YELLOW" to 26,
                "BLUE" to 39,
            )
        val safe =
            setOf(
                0,
                8,
                13,
                21,
                26,
                34,
                39,
                47,
            )
        val offset =
            offsets[
                player.color
            ]
                ?: 0
        val global =
            (
                offset +
                    destination
                ) %
                52

        if (
            global !in
            safe
        ) {
            val captures =
                state.players
                    .asSequence()
                    .filter {
                        it.playerId !=
                            player.playerId
                    }
                    .flatMap {
                            opponent ->
                        opponent.tokens
                            .asSequence()
                            .mapNotNull {
                                    opponentPosition ->
                                if (
                                    opponentPosition !in
                                    0..51
                                ) {
                                    null
                                } else {
                                    val opponentOffset =
                                        offsets[
                                            opponent.color
                                        ]
                                            ?: 0
                                    (
                                        opponentOffset +
                                            opponentPosition
                                        ) %
                                        52
                                }
                            }
                    }
                    .count {
                        it ==
                            global
                    }

            score +=
                captures *
                    10_000
        }
    }

    score +=
        (
            3 -
                tokenIndex
            )
    return score
}

internal fun OfflineGameActivity.offlineHistory(
    state: MatchSnapshot?,
): String {
    if (
        state ==
        null ||
        state.history.isEmpty()
    ) {
        return "No offline rolls yet."
    }
    return buildString {
        append(
            "Recent offline rolls\n\n",
        )
        state.history
            .takeLast(
                16,
            )
            .reversed()
            .forEach {
                    event ->
                append("#")
                append(
                    event.eventIndex,
                )
                append(
                    "  dice=",
                )
                append(
                    event.outcome
                        ?: "?",
                )
                if (
                    event.proofDigest !=
                    null
                ) {
                    append(
                        "  localV4=",
                    )
                    append(
                        event.proofDigest
                            .take(
                                8,
                            ),
                    )
                    append("…")
                }
                if (
                    event.moveTokenIndex !=
                    null
                ) {
                    append(
                        "  token=",
                    )
                    append(
                        event.moveTokenIndex +
                            1,
                    )
                }
                if (
                    event.captures >
                    0
                ) {
                    append(
                        "  captures=",
                    )
                    append(
                        event.captures,
                    )
                }
                append("\n")
            }
    }.trimEnd()
}

internal fun OfflineGameActivity.showStatus(
    value: String,
) {
    statusText
        ?.text =
        value
}
