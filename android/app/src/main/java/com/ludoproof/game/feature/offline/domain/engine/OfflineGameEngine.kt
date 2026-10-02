package com.ludoproof.game

import android.content.Context
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.model.ProfileGameMode
import com.ludoproof.game.feature.profile.domain.model.ProfileMatchSource
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class OfflineGameEngine(
    context: Context,
    private val computerMode: Boolean = false,
) {
    private val profileStore =
        ProfileStore(
            context.applicationContext,
        )

    private val prefs =
        context.getSharedPreferences(
            if (computerMode) {
                PREFS_NAME_COMPUTER
            } else {
                PREFS_NAME
            },
            Context.MODE_PRIVATE,
        )

    private var state: LocalState? =
        loadState()

    fun hasSavedGame(): Boolean =
        state != null

    fun snapshot(): MatchSnapshot? =
        state?.toSnapshot()

    fun lastRandomnessAudit():
        OfflineRandomnessAudit? =
        state?.lastAudit

    fun activePlayerId(): String? =
        state
            ?.players
            ?.getOrNull(
                state?.turnSeat ?: -1,
            )
            ?.playerId

    fun humanPlayerId(): String? =
        state
            ?.players
            ?.firstOrNull()
            ?.playerId

    fun humanColor(): String? =
        state
            ?.players
            ?.firstOrNull()
            ?.color

    fun isComputerPlayer(
        playerId: String?,
    ): Boolean =
        computerMode &&
            playerId
                ?.startsWith(
                    "cpu-player-",
                ) ==
            true

    fun start(
        playerCount: Int,
        preferredColor: String = "RED",
    ): MatchSnapshot {
        require(
            playerCount in
                2..4,
        ) {
            "Offline game supports 2 to 4 players"
        }
        require(
            preferredColor in
                OfflinePlayerLayout.COLORS,
        ) {
            "Unsupported offline color"
        }

        val colorOrder =
            OfflinePlayerLayout
                .colorsFor(
                    playerCount,
                    preferredColor,
                )

        val humanName =
            profileStore
                .snapshot()
                .displayName
                .ifBlank {
                    "Player 1"
                }

        val players =
            (
                0 until
                    playerCount
                )
                .map {
                        seat ->
                    val isCpu =
                        computerMode &&
                            seat >
                            0
                    LocalPlayer(
                        playerId =
                            if (isCpu) {
                                "cpu-player-" +
                                    (
                                        seat +
                                            1
                                        )
                            } else {
                                "offline-player-" +
                                    (
                                        seat +
                                            1
                                        )
                            },
                        displayName =
                            when {
                                seat ==
                                    0 ->
                                    humanName

                                isCpu ->
                                    "CPU " +
                                        seat

                                else ->
                                    "Player " +
                                        (
                                            seat +
                                                1
                                            )
                            },
                        color =
                            colorOrder[
                                seat
                            ],
                        tokens =
                            mutableListOf(
                                -1,
                                -1,
                                -1,
                                -1,
                            ),
                    )
                }
                .toMutableList()

        state =
            LocalState(
                matchId =
                    "offline-" +
                        UUID.randomUUID()
                            .toString(),
                status =
                    "ACTIVE",
                players =
                    players,
                turnSeat =
                    0,
                eventIndex =
                    0,
                consecutiveSixes =
                    MutableList(
                        playerCount,
                    ) {
                        0
                    },
                pendingOutcome =
                    null,
                pendingLegal =
                    mutableSetOf(),
                pendingEventIndex =
                    null,
                pendingRoundId =
                    null,
                pendingClientCommitment =
                    null,
                pendingProofDigest =
                    null,
                winnerPlayerId =
                    null,
                history =
                    mutableListOf(),
                lastAudit =
                    null,
                revision =
                    1,
            )
        persist()
        return requireNotNull(
            snapshot(),
        )
    }

    fun roll(): MatchSnapshot {
        val current =
            requireActiveState()
        check(
            current.pendingOutcome ==
                null,
        ) {
            "Move a highlighted token before rolling again"
        }

        val seat =
            current.turnSeat
        val event =
            current.eventIndex

        val result =
            OfflineLudoV4Binding
                .deriveRoll(
                    matchId =
                        current.matchId,
                    status =
                        current.status,
                    players =
                        current.players
                            .map {
                                    player ->
                                OfflineBindingPlayer(
                                    playerId =
                                        player.playerId,
                                    color =
                                        player.color,
                                    tokens =
                                        player.tokens
                                            .toList(),
                                )
                            },
                    turnSeat =
                        current.turnSeat,
                    eventIndex =
                        event,
                    consecutiveSixes =
                        current.consecutiveSixes
                            .toList(),
                    winnerPlayerId =
                        current.winnerPlayerId,
                    history =
                        current.history
                            .map {
                                    history ->
                                OfflineBindingHistory(
                                    eventIndex =
                                        history.eventIndex,
                                    playerId =
                                        history.playerId,
                                    clientCommitment =
                                        history.clientCommitment,
                                    proofDigest =
                                        history.proofDigest,
                                    outcome =
                                        history.outcome,
                                    moveTokenIndex =
                                        history.moveTokenIndex,
                                    captures =
                                        history.captures,
                                    fairnessDigest =
                                        history.fairnessDigest,
                                )
                            },
                )

        check(
            result.outcome in
                1..6,
        ) {
            "Local EntroNex v4 outcome is outside the locked Ludo range"
        }
        check(
            EntroNexV4Local.verify(
                result,
            ),
        ) {
            "Local EntroNex v4 proof verification failed"
        }

        val audit =
            OfflineRandomnessAudit
                .fromResult(
                    eventIndex =
                        event,
                    result =
                        result,
                )
        check(
            audit.verify(),
        ) {
            "Local EntroNex v4 audit verification failed"
        }

        val outcome =
            result.outcome

        current.eventIndex +=
            1
        current.lastAudit =
            audit

        if (
            outcome ==
            6
        ) {
            current.consecutiveSixes[
                seat
            ] =
                current.consecutiveSixes[
                    seat
                ] +
                    1
        } else {
            current.consecutiveSixes[
                seat
            ] =
                0
        }

        val fairnessMaterial =
            OfflineFairnessChain
                .seal(
                    OfflineFairnessMaterial(
                        eventIndex =
                            event,
                        playerId =
                            current.players[
                                seat
                            ].playerId,
                        color =
                            current.players[
                                seat
                            ].color,
                        roundId =
                            result.roundId,
                        serverCommitment =
                            result.serverCommitment,
                        clientCommitment =
                            result.clientCommitment,
                        actorHash =
                            result.config
                                .context
                                .subjectHash,
                        previousStateHash =
                            result.config
                                .context
                                .previousStateHash,
                        rulesetHash =
                            result.config
                                .context
                                .metadataDigest,
                        proofDigest =
                            result.proofDigest,
                        outcome =
                            outcome,
                    ),
                    current.history
                        .asReversed()
                        .firstOrNull {
                            it.fairnessDigest !=
                                null
                        }
                        ?.fairnessDigest,
                )
        check(
            OfflineFairnessChain
                .verify(
                    fairnessMaterial,
                ),
        ) {
            "Local fairness receipt verification failed"
        }

        current.history +=
            LocalHistoryEvent(
                eventIndex =
                    event,
                playerId =
                    current.players[
                        seat
                    ].playerId,
                color =
                    current.players[
                        seat
                    ].color,
                roundId =
                    result.roundId,
                serverCommitment =
                    result.serverCommitment,
                clientCommitment =
                    result.clientCommitment,
                actorHash =
                    result.config
                        .context
                        .subjectHash,
                previousStateHash =
                    result.config
                        .context
                        .previousStateHash,
                rulesetHash =
                    result.config
                        .context
                        .metadataDigest,
                proofDigest =
                    result.proofDigest,
                fairnessProtocol =
                    fairnessMaterial
                        .fairnessProtocol,
                previousFairnessDigest =
                    fairnessMaterial
                        .previousFairnessDigest,
                fairnessDigest =
                    fairnessMaterial
                        .fairnessDigest,
                outcome =
                    outcome,
                moveTokenIndex =
                    null,
                captures =
                    0,
            )

        if (
            outcome ==
                6 &&
            current.consecutiveSixes[
                seat
            ] >=
                3
        ) {
            current.consecutiveSixes[
                seat
            ] =
                0
            clearPending(
                current,
            )
            advanceTurn(
                current,
            )
            current.revision +=
                1
            persist()
            return current
                .toSnapshot()
        }

        val legal =
            legalTokenIndexes(
                current.players[
                    seat
                ].tokens,
                outcome,
            )

        if (
            legal.isEmpty()
        ) {
            clearPending(
                current,
            )
            if (
                outcome !=
                6
            ) {
                advanceTurn(
                    current,
                )
            }
            current.revision +=
                1
            persist()
            return current
                .toSnapshot()
        }

        current.pendingOutcome =
            outcome
        current.pendingLegal =
            legal.toMutableSet()
        current.pendingEventIndex =
            event
        current.pendingRoundId =
            result.roundId
        current.pendingClientCommitment =
            result.clientCommitment
        current.pendingProofDigest =
            result.proofDigest
        current.revision +=
            1
        persist()
        return current
            .toSnapshot()
    }

    fun move(
        tokenIndex: Int,
    ): MatchSnapshot {
        val current =
            requireActiveState()
        val outcome =
            current.pendingOutcome
                ?: error(
                    "Roll the dice before moving",
                )
        val event =
            current.pendingEventIndex
                ?: error(
                    "Offline roll state is incomplete",
                )

        require(
            tokenIndex in
                current.pendingLegal,
        ) {
            "That token cannot use this roll"
        }

        val seat =
            current.turnSeat
        val player =
            current.players[
                seat
            ]
        val oldPosition =
            player.tokens[
                tokenIndex
            ]
        val destination =
            if (
                oldPosition ==
                -1
            ) {
                0
            } else {
                oldPosition +
                    outcome
            }
        player.tokens[
            tokenIndex
        ] =
            destination

        val captures =
            captureOpponents(
                current,
                seat,
                destination,
            )

        val historyIndex =
            current.history
                .indexOfLast {
                    it.eventIndex ==
                        event
                }
        if (
            historyIndex >=
            0
        ) {
            val previous =
                current.history[
                    historyIndex
                ]
            current.history[
                historyIndex
            ] =
                previous.copy(
                    moveTokenIndex =
                        tokenIndex,
                    captures =
                        captures,
                )
        }

        clearPending(
            current,
        )

        if (
            player.tokens
                .all {
                    it ==
                        HOME_POSITION
                }
        ) {
            current.status =
                "FINISHED"
            current.winnerPlayerId =
                player.playerId
            current.revision +=
                1
            persist()
            runCatching {
                profileStore
                    .recordCompletedMatch(
                        matchId =
                            current.matchId,
                        mode =
                            ProfileGameMode.CLASSIC,
                        source =
                            if (
                                computerMode
                            ) {
                                ProfileMatchSource.COMPUTER
                            } else {
                                ProfileMatchSource.LOCAL
                            },
                        won =
                            player.playerId ==
                                current.players
                                    .firstOrNull()
                                    ?.playerId,
                    )
            }
            return current
                .toSnapshot()
        }

        val extraTurn =
            outcome ==
                6 ||
                captures >
                0
        if (
            !extraTurn
        ) {
            advanceTurn(
                current,
            )
        }

        current.revision +=
            1
        persist()
        return current
            .toSnapshot()
    }

    fun clear() {
        state =
            null
        prefs.edit()
            .clear()
            .apply()
    }

    private fun requireActiveState():
        LocalState {
        val current =
            state
                ?: error(
                    "Start an offline game first",
                )
        check(
            current.status ==
                "ACTIVE",
        ) {
            "Offline game is already finished"
        }
        return current
    }

    private fun clearPending(
        current: LocalState,
    ) {
        current.pendingOutcome =
            null
        current.pendingLegal
            .clear()
        current.pendingEventIndex =
            null
        current.pendingRoundId =
            null
        current.pendingClientCommitment =
            null
        current.pendingProofDigest =
            null
    }

    private fun legalTokenIndexes(
        tokens: List<Int>,
        roll: Int,
    ): Set<Int> =
        buildSet {
            tokens
                .forEachIndexed {
                        index,
                        position ->
                    when {
                        position ==
                            HOME_POSITION ->
                            Unit
                        position ==
                            -1 &&
                            roll ==
                            6 ->
                            add(
                                index,
                            )
                        position in
                            0..56 &&
                            position +
                                roll <=
                            HOME_POSITION ->
                            add(
                                index,
                            )
                    }
                }
        }

    private fun captureOpponents(
        current: LocalState,
        movingSeat: Int,
        destination: Int,
    ): Int {
        if (
            destination !in
            0..51
        ) {
            return 0
        }

        val mover =
            current.players[
                movingSeat
            ]
        val global =
            globalCell(
                mover.color,
                destination,
            )
                ?: return 0

        if (
            global in
            SAFE_GLOBAL_CELLS
        ) {
            return 0
        }

        var captures =
            0
        current.players
            .forEachIndexed {
                    seat,
                    opponent ->
                if (
                    seat ==
                    movingSeat
                ) {
                    return@forEachIndexed
                }
                opponent.tokens =
                    opponent.tokens
                        .map {
                                position ->
                            if (
                                globalCell(
                                    opponent.color,
                                    position,
                                ) ==
                                global
                            ) {
                                captures +=
                                    1
                                -1
                            } else {
                                position
                            }
                        }
                        .toMutableList()
            }
        return captures
    }

    private fun globalCell(
        color: String,
        relativePosition: Int,
    ): Int? {
        if (
            relativePosition !in
            0..51
        ) {
            return null
        }
        val offset =
            START_OFFSETS[
                color
            ]
                ?: return null
        return (
            offset +
                relativePosition
            ) %
            52
    }

    private fun advanceTurn(
        current: LocalState,
    ) {
        current.turnSeat =
            (
                current.turnSeat +
                    1
                ) %
                current.players
                    .size
    }

    private fun persist() {
        val current =
            state
                ?: return
        val json =
            JSONObject()
                .put(
                    "matchId",
                    current.matchId,
                )
                .put(
                    "status",
                    current.status,
                )
                .put(
                    "turnSeat",
                    current.turnSeat,
                )
                .put(
                    "eventIndex",
                    current.eventIndex,
                )
                .put(
                    "revision",
                    current.revision,
                )
                .put(
                    "winnerPlayerId",
                    current.winnerPlayerId,
                )
                .put(
                    "players",
                    JSONArray().apply {
                        current.players
                            .forEach {
                                    player ->
                                put(
                                    JSONObject()
                                        .put(
                                            "playerId",
                                            player.playerId,
                                        )
                                        .put(
                                            "displayName",
                                            player.displayName,
                                        )
                                        .put(
                                            "color",
                                            player.color,
                                        )
                                        .put(
                                            "tokens",
                                            JSONArray(
                                                player.tokens,
                                            ),
                                        ),
                                )
                            }
                    },
                )
                .put(
                    "consecutiveSixes",
                    JSONArray(
                        current.consecutiveSixes,
                    ),
                )
                .put(
                    "history",
                    JSONArray().apply {
                        current.history
                            .takeLast(
                                MAX_HISTORY,
                            )
                            .forEach {
                                    event ->
                                put(
                                    JSONObject()
                                        .put(
                                            "eventIndex",
                                            event.eventIndex,
                                        )
                                        .put(
                                            "playerId",
                                            event.playerId,
                                        )
                                        .put(
                                            "color",
                                            event.color,
                                        )
                                        .put(
                                            "roundId",
                                            event.roundId,
                                        )
                                        .put(
                                            "serverCommitment",
                                            event.serverCommitment,
                                        )
                                        .put(
                                            "clientCommitment",
                                            event.clientCommitment,
                                        )
                                        .put(
                                            "actorHash",
                                            event.actorHash,
                                        )
                                        .put(
                                            "previousStateHash",
                                            event.previousStateHash,
                                        )
                                        .put(
                                            "rulesetHash",
                                            event.rulesetHash,
                                        )
                                        .put(
                                            "proofDigest",
                                            event.proofDigest,
                                        )
                                        .put(
                                            "fairnessProtocol",
                                            event.fairnessProtocol,
                                        )
                                        .put(
                                            "previousFairnessDigest",
                                            event.previousFairnessDigest,
                                        )
                                        .put(
                                            "fairnessDigest",
                                            event.fairnessDigest,
                                        )
                                        .put(
                                            "outcome",
                                            event.outcome,
                                        )
                                        .put(
                                            "moveTokenIndex",
                                            event.moveTokenIndex,
                                        )
                                        .put(
                                            "captures",
                                            event.captures,
                                        ),
                                )
                            }
                    },
                )
                .put(
                    "lastAudit",
                    current.lastAudit
                        ?.toJson(),
                )

        current.pendingOutcome
            ?.let {
                    outcome ->
                json.put(
                    "pending",
                    JSONObject()
                        .put(
                            "outcome",
                            outcome,
                        )
                        .put(
                            "eventIndex",
                            current.pendingEventIndex,
                        )
                        .put(
                            "roundId",
                            current.pendingRoundId,
                        )
                        .put(
                            "clientCommitment",
                            current.pendingClientCommitment,
                        )
                        .put(
                            "proofDigest",
                            current.pendingProofDigest,
                        )
                        .put(
                            "legal",
                            JSONArray(
                                current.pendingLegal
                                    .sorted(),
                            ),
                        ),
                )
            }

        prefs.edit()
            .putString(
                KEY_STATE,
                json.toString(),
            )
            .commit()
    }

    private fun loadState():
        LocalState? {
        val raw =
            prefs.getString(
                KEY_STATE,
                null,
            )
                ?: return null

        return runCatching {
            val json =
                JSONObject(
                    raw,
                )
            val playersJson =
                json.getJSONArray(
                    "players",
                )
            val players =
                buildList {
                    for (
                        index in
                        0 until
                            playersJson
                                .length()
                    ) {
                        val value =
                            playersJson
                                .getJSONObject(
                                    index,
                                )
                        val tokensJson =
                            value
                                .getJSONArray(
                                    "tokens",
                                )
                        val tokens =
                            MutableList(
                                tokensJson
                                    .length(),
                            ) {
                                    tokenIndex ->
                                tokensJson
                                    .getInt(
                                        tokenIndex,
                                    )
                            }
                        add(
                            LocalPlayer(
                                playerId =
                                    value.getString(
                                        "playerId",
                                    ),
                                displayName =
                                    value.getString(
                                        "displayName",
                                    ),
                                color =
                                    value.getString(
                                        "color",
                                    ),
                                tokens =
                                    tokens,
                            ),
                        )
                    }
                }
                    .toMutableList()

            val sixesJson =
                json.getJSONArray(
                    "consecutiveSixes",
                )
            val sixes =
                MutableList(
                    sixesJson
                        .length(),
                ) {
                        index ->
                    sixesJson
                        .getInt(
                            index,
                        )
                }

            val history =
                mutableListOf<
                    LocalHistoryEvent
                    >()
            val historyJson =
                json.optJSONArray(
                    "history",
                )
            if (
                historyJson !=
                null
            ) {
                for (
                    index in
                    0 until
                        historyJson
                            .length()
                ) {
                    val value =
                        historyJson
                            .getJSONObject(
                                index,
                            )
                    history +=
                        LocalHistoryEvent(
                            eventIndex =
                                value.getInt(
                                    "eventIndex",
                                ),
                            playerId =
                                value.getString(
                                    "playerId",
                                ),
                            color =
                                value
                                    .nullableString(
                                        "color",
                                    ),
                            roundId =
                                value
                                    .nullableString(
                                        "roundId",
                                    ),
                            serverCommitment =
                                value
                                    .nullableString(
                                        "serverCommitment",
                                    ),
                            clientCommitment =
                                value
                                    .nullableString(
                                        "clientCommitment",
                                    ),
                            actorHash =
                                value
                                    .nullableString(
                                        "actorHash",
                                    ),
                            previousStateHash =
                                value
                                    .nullableString(
                                        "previousStateHash",
                                    ),
                            rulesetHash =
                                value
                                    .nullableString(
                                        "rulesetHash",
                                    ),
                            proofDigest =
                                value
                                    .nullableString(
                                        "proofDigest",
                                    ),
                            fairnessProtocol =
                                value
                                    .nullableString(
                                        "fairnessProtocol",
                                    ),
                            previousFairnessDigest =
                                value
                                    .nullableString(
                                        "previousFairnessDigest",
                                    ),
                            fairnessDigest =
                                value
                                    .nullableString(
                                        "fairnessDigest",
                                    ),
                            outcome =
                                if (
                                    value.has(
                                        "outcome",
                                    ) &&
                                    !value.isNull(
                                        "outcome",
                                    )
                                ) {
                                    value.getInt(
                                        "outcome",
                                    )
                                } else {
                                    null
                                },
                            moveTokenIndex =
                                if (
                                    value.has(
                                        "moveTokenIndex",
                                    ) &&
                                    !value.isNull(
                                        "moveTokenIndex",
                                    )
                                ) {
                                    value.getInt(
                                        "moveTokenIndex",
                                    )
                                } else {
                                    null
                                },
                            captures =
                                value.optInt(
                                    "captures",
                                    0,
                                ),
                        )
                }
            }

            check(
                OfflineFairnessChain
                    .verifyHistory(
                        history.map {
                            it.toFairnessMaterial()
                        },
                    ),
            ) {
                "Offline fairness history chain verification failed"
            }

            val pending =
                json.optJSONObject(
                    "pending",
                )
            val restoredProofDigest =
                pending
                    ?.nullableString(
                        "proofDigest",
                    )
            val restorePending =
                pending !=
                    null &&
                    restoredProofDigest !=
                    null
            val legal =
                mutableSetOf<
                    Int
                    >()
            if (
                restorePending
            ) {
                pending
                    ?.optJSONArray(
                        "legal",
                    )
                    ?.let {
                            values ->
                        for (
                            index in
                            0 until
                                values.length()
                        ) {
                            legal +=
                                values.getInt(
                                    index,
                                )
                        }
                    }
            }

            LocalState(
                matchId =
                    json.optString(
                        "matchId",
                    )
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?: (
                            "offline-legacy-" +
                                UUID.randomUUID()
                                    .toString()
                            ),
                status =
                    json.optString(
                        "status",
                        "ACTIVE",
                    ),
                players =
                    players,
                turnSeat =
                    json.optInt(
                        "turnSeat",
                        0,
                    ),
                eventIndex =
                    json.optInt(
                        "eventIndex",
                        0,
                    ),
                consecutiveSixes =
                    sixes,
                pendingOutcome =
                    if (
                        restorePending &&
                        pending !=
                        null &&
                        pending.has(
                            "outcome",
                        )
                    ) {
                        pending.optInt(
                            "outcome",
                        )
                    } else {
                        null
                    },
                pendingLegal =
                    legal,
                pendingEventIndex =
                    if (
                        restorePending &&
                        pending !=
                        null &&
                        pending.has(
                            "eventIndex",
                        )
                    ) {
                        pending.optInt(
                            "eventIndex",
                        )
                    } else {
                        null
                    },
                pendingRoundId =
                    if (
                        restorePending
                    ) {
                        pending
                            ?.nullableString(
                                "roundId",
                            )
                    } else {
                        null
                    },
                pendingClientCommitment =
                    if (
                        restorePending
                    ) {
                        pending
                            ?.nullableString(
                                "clientCommitment",
                            )
                    } else {
                        null
                    },
                pendingProofDigest =
                    if (
                        restorePending
                    ) {
                        restoredProofDigest
                    } else {
                        null
                    },
                winnerPlayerId =
                    json
                        .nullableString(
                            "winnerPlayerId",
                        ),
                history =
                    history,
                lastAudit =
                    OfflineRandomnessAudit
                        .fromJson(
                            json.optJSONObject(
                                "lastAudit",
                            ),
                        ),
                revision =
                    json.optInt(
                        "revision",
                        1,
                    ),
            )
        }.getOrElse {
            prefs.edit()
                .clear()
                .apply()
            null
        }
    }

    private fun JSONObject.nullableString(
        name: String,
    ): String? =
        optString(
            name,
        )
            .takeIf {
                it.isNotBlank() &&
                    it !=
                    "null"
            }

    private fun LocalState.toSnapshot():
        MatchSnapshot =
        MatchSnapshot(
            matchId =
                matchId,
            status =
                status,
            hostPlayerId =
                players
                    .firstOrNull()
                    ?.playerId
                    .orEmpty(),
            players =
                players
                    .mapIndexed {
                            seat,
                            player ->
                        PlayerSnapshot(
                            playerId =
                                player.playerId,
                            displayName =
                                player.displayName,
                            color =
                                player.color,
                            seat =
                                seat,
                            tokens =
                                player.tokens
                                    .toList(),
                        )
                    },
            turnSeat =
                turnSeat,
            randomEventIndex =
                eventIndex,
            pendingRoll =
                pendingOutcome
                    ?.let {
                            outcome ->
                        PendingRollSnapshot(
                            status =
                                "RESOLVED",
                            seat =
                                turnSeat,
                            eventIndex =
                                pendingEventIndex
                                    ?: (
                                        eventIndex -
                                            1
                                        ),
                            eventId =
                                "roll:" +
                                    (
                                        pendingEventIndex
                                            ?: (
                                                eventIndex -
                                                    1
                                                )
                                        ),
                            roundId =
                                pendingRoundId,
                            serverCommitment =
                                lastAudit
                                    ?.takeIf {
                                        it.eventIndex ==
                                            pendingEventIndex
                                    }
                                    ?.serverCommitment,
                            clientCommitment =
                                pendingClientCommitment,
                            revealDeadlineAt =
                                null,
                            proofDigest =
                                pendingProofDigest,
                            outcome =
                                outcome,
                            legalTokenIndexes =
                                pendingLegal
                                    .toSet(),
                        )
                    },
            winnerPlayerId =
                winnerPlayerId,
            rulesetId =
                OfflineLudoV4Binding
                    .RULESET_ID,
            history =
                history.map {
                    it.toSnapshot()
                },
        )

    private data class LocalPlayer(
        val playerId: String,
        val displayName: String,
        val color: String,
        var tokens: MutableList<Int>,
    )

    private data class LocalHistoryEvent(
        val eventIndex: Int,
        val playerId: String,
        val color: String?,
        val roundId: String?,
        val serverCommitment: String?,
        val clientCommitment: String?,
        val actorHash: String?,
        val previousStateHash: String?,
        val rulesetHash: String?,
        val proofDigest: String?,
        val fairnessProtocol: String?,
        val previousFairnessDigest: String?,
        val fairnessDigest: String?,
        val outcome: Int?,
        val moveTokenIndex: Int?,
        val captures: Int,
    ) {
        fun toSnapshot():
            HistoryEventSnapshot =
            HistoryEventSnapshot(
                eventIndex =
                    eventIndex,
                playerId =
                    playerId,
                roundId =
                    roundId,
                proofDigest =
                    proofDigest,
                outcome =
                    outcome,
                moveTokenIndex =
                    moveTokenIndex,
                captures =
                    captures,
                serverCommitment =
                    serverCommitment,
                clientCommitment =
                    clientCommitment,
                previousStateHash =
                    previousStateHash,
                rulesetHash =
                    rulesetHash,
                fairnessProtocol =
                    fairnessProtocol,
                previousFairnessDigest =
                    previousFairnessDigest,
                fairnessDigest =
                    fairnessDigest,
                status =
                    "RESOLVED",
            )

        fun toFairnessMaterial():
            OfflineFairnessMaterial =
            OfflineFairnessMaterial(
                eventIndex =
                    eventIndex,
                playerId =
                    playerId,
                color =
                    color,
                roundId =
                    roundId,
                serverCommitment =
                    serverCommitment,
                clientCommitment =
                    clientCommitment,
                actorHash =
                    actorHash,
                previousStateHash =
                    previousStateHash,
                rulesetHash =
                    rulesetHash,
                proofDigest =
                    proofDigest,
                outcome =
                    outcome,
                fairnessProtocol =
                    fairnessProtocol,
                previousFairnessDigest =
                    previousFairnessDigest,
                fairnessDigest =
                    fairnessDigest,
            )
    }

    private data class LocalState(
        val matchId: String,
        var status: String,
        val players:
            MutableList<
                LocalPlayer
                >,
        var turnSeat: Int,
        var eventIndex: Int,
        val consecutiveSixes:
            MutableList<Int>,
        var pendingOutcome: Int?,
        var pendingLegal:
            MutableSet<Int>,
        var pendingEventIndex:
            Int?,
        var pendingRoundId:
            String?,
        var pendingClientCommitment:
            String?,
        var pendingProofDigest:
            String?,
        var winnerPlayerId:
            String?,
        val history:
            MutableList<
                LocalHistoryEvent
                >,
        var lastAudit:
            OfflineRandomnessAudit?,
        var revision: Int,
    )

    private companion object {
        const val PREFS_NAME =
            "ludoproof_offline_game"
        const val PREFS_NAME_COMPUTER =
            "ludoproof_computer_game"
        const val KEY_STATE =
            "state"
        const val HOME_POSITION =
            57
        const val MAX_HISTORY =
            100

        val START_OFFSETS =
            mapOf(
                "RED" to
                    0,
                "GREEN" to
                    13,
                "YELLOW" to
                    26,
                "BLUE" to
                    39,
            )

        val SAFE_GLOBAL_CELLS =
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
    }
}
