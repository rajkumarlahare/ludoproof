package com.ludoproof.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class OfflineGameEngine(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
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
                COLORS,
        ) {
            "Unsupported offline color"
        }

        val colorOrder =
            listOf(
                preferredColor,
            ) +
                COLORS.filter {
                    it !=
                        preferredColor
                }

        val players =
            (
                0 until
                    playerCount
                )
                .map {
                        seat ->
                    LocalPlayer(
                        playerId =
                            "offline-player-" +
                                (
                                    seat +
                                        1
                                    ),
                        displayName =
                            "Player " +
                                (
                                    seat +
                                        1
                                    ),
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

        current.history +=
            LocalHistoryEvent(
                eventIndex =
                    event,
                playerId =
                    current.players[
                        seat
                    ].playerId,
                roundId =
                    result.roundId,
                clientCommitment =
                    result.clientCommitment,
                proofDigest =
                    result.proofDigest,
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
                                            "roundId",
                                            event.roundId,
                                        )
                                        .put(
                                            "clientCommitment",
                                            event.clientCommitment,
                                        )
                                        .put(
                                            "proofDigest",
                                            event.proofDigest,
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
                            roundId =
                                value
                                    .nullableString(
                                        "roundId",
                                    ),
                            clientCommitment =
                                value
                                    .nullableString(
                                        "clientCommitment",
                                    ),
                            proofDigest =
                                value
                                    .nullableString(
                                        "proofDigest",
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

            val pending =
                json.optJSONObject(
                    "pending",
                )
            val legal =
                mutableSetOf<
                    Int
                    >()
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
                    pending
                        ?.nullableString(
                            "roundId",
                        ),
                pendingClientCommitment =
                    pending
                        ?.nullableString(
                            "clientCommitment",
                        ),
                pendingProofDigest =
                    pending
                        ?.nullableString(
                            "proofDigest",
                        ),
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
        val roundId: String?,
        val clientCommitment: String?,
        val proofDigest: String?,
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
        const val KEY_STATE =
            "state"
        const val HOME_POSITION =
            57
        const val MAX_HISTORY =
            100

        val COLORS =
            listOf(
                "RED",
                "GREEN",
                "YELLOW",
                "BLUE",
            )

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
