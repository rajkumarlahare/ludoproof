package com.ludoproof.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

class OfflineGameEngine(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )
    private val random = SecureRandom()
    private var state: LocalState? =
        loadState()

    fun hasSavedGame(): Boolean =
        state != null

    fun snapshot(): MatchSnapshot? =
        state?.toSnapshot()

    fun activePlayerId(): String? =
        state
            ?.players
            ?.getOrNull(
                state?.turnSeat ?: -1,
            )
            ?.playerId

    fun start(playerCount: Int): MatchSnapshot {
        require(playerCount in 2..4) {
            "Offline game supports 2 to 4 players"
        }

        val players =
            (0 until playerCount)
                .map { seat ->
                    LocalPlayer(
                        playerId =
                            "offline-player-" +
                                (seat + 1),
                        displayName =
                            "Player " +
                                (seat + 1),
                        color = COLORS[seat],
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
                status = "ACTIVE",
                players = players,
                turnSeat = 0,
                eventIndex = 0,
                consecutiveSixes =
                    MutableList(playerCount) { 0 },
                pendingOutcome = null,
                pendingLegal =
                    mutableSetOf(),
                pendingEventIndex = null,
                winnerPlayerId = null,
                history =
                    mutableListOf(),
                revision = 1,
            )
        persist()
        return requireNotNull(snapshot())
    }

    fun roll(): MatchSnapshot {
        val current =
            requireActiveState()
        check(current.pendingOutcome == null) {
            "Move a highlighted token before rolling again"
        }

        val seat =
            current.turnSeat
        val outcome =
            random.nextInt(6) + 1
        val event =
            current.eventIndex
        current.eventIndex += 1

        if (outcome == 6) {
            current.consecutiveSixes[seat] =
                current.consecutiveSixes[seat] + 1
        } else {
            current.consecutiveSixes[seat] = 0
        }

        current.history +=
            HistoryEventSnapshot(
                eventIndex = event,
                playerId =
                    current.players[seat].playerId,
                roundId = null,
                proofDigest = null,
                outcome = outcome,
                moveTokenIndex = null,
                captures = 0,
            )

        if (
            outcome == 6 &&
            current.consecutiveSixes[seat] >= 3
        ) {
            current.consecutiveSixes[seat] = 0
            advanceTurn(current)
            current.revision += 1
            persist()
            return current.toSnapshot()
        }

        val legal =
            legalTokenIndexes(
                current.players[seat].tokens,
                outcome,
            )

        if (legal.isEmpty()) {
            if (outcome != 6) {
                advanceTurn(current)
            }
            current.revision += 1
            persist()
            return current.toSnapshot()
        }

        current.pendingOutcome = outcome
        current.pendingLegal =
            legal.toMutableSet()
        current.pendingEventIndex = event
        current.revision += 1
        persist()
        return current.toSnapshot()
    }

    fun move(tokenIndex: Int): MatchSnapshot {
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
            tokenIndex in current.pendingLegal,
        ) {
            "That token cannot use this roll"
        }

        val seat =
            current.turnSeat
        val player =
            current.players[seat]
        val oldPosition =
            player.tokens[tokenIndex]
        val destination =
            if (oldPosition == -1) {
                0
            } else {
                oldPosition + outcome
            }
        player.tokens[tokenIndex] =
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
                    it.eventIndex == event
                }
        if (historyIndex >= 0) {
            val previous =
                current.history[historyIndex]
            current.history[historyIndex] =
                previous.copy(
                    moveTokenIndex =
                        tokenIndex,
                    captures = captures,
                )
        }

        current.pendingOutcome = null
        current.pendingLegal.clear()
        current.pendingEventIndex = null

        if (
            player.tokens.all {
                it == HOME_POSITION
            }
        ) {
            current.status = "FINISHED"
            current.winnerPlayerId =
                player.playerId
            current.revision += 1
            persist()
            return current.toSnapshot()
        }

        val extraTurn =
            outcome == 6 ||
                captures > 0
        if (!extraTurn) {
            advanceTurn(current)
        }

        current.revision += 1
        persist()
        return current.toSnapshot()
    }

    fun clear() {
        state = null
        prefs.edit().clear().apply()
    }

    private fun requireActiveState(): LocalState {
        val current =
            state
                ?: error(
                    "Start an offline game first",
                )
        check(current.status == "ACTIVE") {
            "Offline game is already finished"
        }
        return current
    }

    private fun legalTokenIndexes(
        tokens: List<Int>,
        roll: Int,
    ): Set<Int> =
        buildSet {
            tokens.forEachIndexed {
                    index,
                    position,
                ->
                when {
                    position == HOME_POSITION ->
                        Unit
                    position == -1 &&
                        roll == 6 ->
                        add(index)
                    position in 0..56 &&
                        position + roll <=
                        HOME_POSITION ->
                        add(index)
                }
            }
        }

    private fun captureOpponents(
        current: LocalState,
        movingSeat: Int,
        destination: Int,
    ): Int {
        if (destination !in 0..51) {
            return 0
        }

        val mover =
            current.players[movingSeat]
        val global =
            globalCell(
                mover.color,
                destination,
            ) ?: return 0

        if (global in SAFE_GLOBAL_CELLS) {
            return 0
        }

        var captures = 0
        current.players.forEachIndexed {
                seat,
                opponent,
            ->
            if (seat == movingSeat) {
                return@forEachIndexed
            }
            opponent.tokens =
                opponent.tokens
                    .map { position ->
                        if (
                            globalCell(
                                opponent.color,
                                position,
                            ) == global
                        ) {
                            captures += 1
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
        if (relativePosition !in 0..51) {
            return null
        }
        val offset =
            START_OFFSETS[color]
                ?: return null
        return (
            offset +
                relativePosition
            ) % 52
    }

    private fun advanceTurn(
        current: LocalState,
    ) {
        current.turnSeat =
            (
                current.turnSeat + 1
                ) %
                current.players.size
    }

    private fun persist() {
        val current =
            state ?: return
        val json =
            JSONObject()
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

    private fun loadState(): LocalState? {
        val raw =
            prefs.getString(
                KEY_STATE,
                null,
            ) ?: return null

        return runCatching {
            val json =
                JSONObject(raw)
            val playersJson =
                json.getJSONArray(
                    "players",
                )
            val players =
                buildList {
                    for (
                        index in
                        0 until
                            playersJson.length()
                    ) {
                        val value =
                            playersJson
                                .getJSONObject(
                                    index,
                                )
                        val tokensJson =
                            value.getJSONArray(
                                "tokens",
                            )
                        val tokens =
                            MutableList(
                                tokensJson.length(),
                            ) {
                                    tokenIndex ->
                                tokensJson.getInt(
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
                                tokens = tokens,
                            ),
                        )
                    }
                }.toMutableList()

            val sixesJson =
                json.getJSONArray(
                    "consecutiveSixes",
                )
            val sixes =
                MutableList(
                    sixesJson.length(),
                ) {
                        index ->
                    sixesJson.getInt(
                        index,
                    )
                }

            val history =
                mutableListOf<
                    HistoryEventSnapshot
                    >()
            val historyJson =
                json.optJSONArray(
                    "history",
                )
            if (historyJson != null) {
                for (
                    index in
                    0 until
                        historyJson.length()
                ) {
                    val value =
                        historyJson
                            .getJSONObject(
                                index,
                            )
                    history +=
                        HistoryEventSnapshot(
                            eventIndex =
                                value.getInt(
                                    "eventIndex",
                                ),
                            playerId =
                                value.getString(
                                    "playerId",
                                ),
                            roundId = null,
                            proofDigest = null,
                            outcome =
                                value.optInt(
                                    "outcome",
                                ),
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
                mutableSetOf<Int>()
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
                status =
                    json.optString(
                        "status",
                        "ACTIVE",
                    ),
                players = players,
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
                    pending
                        ?.optInt(
                            "outcome",
                        ),
                pendingLegal =
                    legal,
                pendingEventIndex =
                    pending
                        ?.optInt(
                            "eventIndex",
                        ),
                winnerPlayerId =
                    json.optString(
                        "winnerPlayerId",
                    )
                        .takeIf {
                            it.isNotBlank() &&
                                it != "null"
                        },
                history =
                    history,
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

    private fun LocalState.toSnapshot():
        MatchSnapshot =
        MatchSnapshot(
            matchId =
                "OFFLINE",
            status =
                status,
            hostPlayerId =
                players.firstOrNull()
                    ?.playerId
                    .orEmpty(),
            players =
                players.mapIndexed {
                        seat,
                        player,
                    ->
                    PlayerSnapshot(
                        playerId =
                            player.playerId,
                        displayName =
                            player.displayName,
                        color =
                            player.color,
                        seat = seat,
                        tokens =
                            player.tokens.toList(),
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
                                "offline:" +
                                    (
                                        pendingEventIndex
                                            ?: (
                                                eventIndex -
                                                    1
                                                )
                                        ),
                            roundId = null,
                            serverCommitment = null,
                            clientCommitment = null,
                            revealDeadlineAt = null,
                            proofDigest = null,
                            outcome = outcome,
                            legalTokenIndexes =
                                pendingLegal
                                    .toSet(),
                        )
                    },
            winnerPlayerId =
                winnerPlayerId,
            rulesetId =
                "ludoproof-standard-v1-offline",
            history =
                history.toList(),
        )

    private data class LocalPlayer(
        val playerId: String,
        val displayName: String,
        val color: String,
        var tokens: MutableList<Int>,
    )

    private data class LocalState(
        var status: String,
        val players: MutableList<LocalPlayer>,
        var turnSeat: Int,
        var eventIndex: Int,
        val consecutiveSixes: MutableList<Int>,
        var pendingOutcome: Int?,
        var pendingLegal: MutableSet<Int>,
        var pendingEventIndex: Int?,
        var winnerPlayerId: String?,
        val history:
            MutableList<HistoryEventSnapshot>,
        var revision: Int,
    )

    private companion object {
        const val PREFS_NAME =
            "ludoproof_offline_game"
        const val KEY_STATE =
            "state"
        const val HOME_POSITION = 57
        const val MAX_HISTORY = 100

        val COLORS =
            listOf(
                "RED",
                "GREEN",
                "YELLOW",
                "BLUE",
            )

        val START_OFFSETS =
            mapOf(
                "RED" to 0,
                "GREEN" to 13,
                "YELLOW" to 26,
                "BLUE" to 39,
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
