package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PlayerSnapshot

enum class GameMomentType {
    TURN_STARTED,
    ROLL_STARTED,
    SIX_ROLLED,
    LOW_ROLL,
    TOKEN_LEFT_YARD,
    TOKEN_MOVED,
    CAPTURE_MADE,
    TOKEN_CAPTURED,
    SAFE_REACHED,
    HOME_REACHED,
    NO_LEGAL_MOVE,
    THIRD_SIX_FORFEIT,
    PLAYER_LEADING,
    IDLE_WAITING,
    MATCH_WIN,
    MATCH_LOSS,
    TEAM_WIN,
    TEAM_LOSS,
}

data class LudoPawsGameMoment(
    val matchId: String,
    val eventIndex: Int,
    val type: GameMomentType,
    val playerId: String,
    val seat: Int,
    val tokenIndex: Int? = null,
    val relatedPlayerId: String? = null,
    val value: Int? = null,
)

/**
 * Phase 7 presentation-only state-diff engine.
 *
 * The detector consumes immutable public MatchSnapshot values. It never rolls
 * dice, selects a move, changes turn order, mutates proof material, or writes
 * match state. Both offline and online screens can therefore feed the same
 * authoritative snapshot shape into this class.
 */
object LudoPawsGameMomentDetector {
    private val startOffsets =
        mapOf(
            "RED" to 0,
            "GREEN" to 13,
            "YELLOW" to 26,
            "BLUE" to 39,
        )

    private val safeGlobalCells =
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

    fun detect(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): List<LudoPawsGameMoment> {
        if (
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            return emptyList()
        }

        val moments =
            mutableListOf<LudoPawsGameMoment>()

        detectTurnChange(
            previous = previous,
            current = current,
            sink = moments,
        )
        detectRollLifecycle(
            previous = previous,
            current = current,
            sink = moments,
        )
        detectTokenChanges(
            previous = previous,
            current = current,
            sink = moments,
        )
        detectLeadChange(
            previous = previous,
            current = current,
            sink = moments,
        )
        detectFinish(
            previous = previous,
            current = current,
            sink = moments,
        )

        return moments.distinctBy {
            listOf(
                it.matchId,
                it.eventIndex.toString(),
                it.type.name,
                it.playerId,
                it.tokenIndex?.toString().orEmpty(),
                it.relatedPlayerId.orEmpty(),
            ).joinToString(":")
        }
    }

    fun idleMoment(
        current: MatchSnapshot?,
        nowMillis: Long,
        lastMeaningfulChangeAtMillis: Long,
        thresholdMillis: Long = 12_000L,
    ): LudoPawsGameMoment? {
        if (
            current == null ||
            current.status != "ACTIVE" ||
            thresholdMillis <= 0L ||
            nowMillis - lastMeaningfulChangeAtMillis < thresholdMillis
        ) {
            return null
        }

        val active =
            current.players
                .getOrNull(current.turnSeat)
                ?: return null

        return moment(
            current = current,
            type = GameMomentType.IDLE_WAITING,
            player = active,
            eventIndex = current.randomEventIndex,
        )
    }

    private fun detectTurnChange(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsGameMoment>,
    ) {
        if (
            current.status != "ACTIVE" ||
            previous.turnSeat == current.turnSeat
        ) {
            return
        }

        val active =
            current.players
                .getOrNull(current.turnSeat)
                ?: return
        sink +=
            moment(
                current = current,
                type = GameMomentType.TURN_STARTED,
                player = active,
                eventIndex = current.randomEventIndex,
            )
    }

    private fun detectRollLifecycle(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsGameMoment>,
    ) {
        val pending =
            current.pendingRoll
        val previousPending =
            previous.pendingRoll

        if (
            pending != null &&
            (
                previousPending == null ||
                    previousPending.eventIndex != pending.eventIndex ||
                    previousPending.status != pending.status
                )
        ) {
            val player =
                current.players
                    .getOrNull(pending.seat)
            if (player != null) {
                sink +=
                    moment(
                        current = current,
                        type = GameMomentType.ROLL_STARTED,
                        player = player,
                        eventIndex = pending.eventIndex,
                        value = pending.outcome,
                    )
            }
        }

        val previousIndexes =
            previous.history
                .mapTo(mutableSetOf()) {
                    it.eventIndex
                }

        current.history
            .asSequence()
            .filter {
                it.eventIndex !in previousIndexes &&
                    it.outcome != null
            }
            .forEach {
                    event ->
                val player =
                    current.playerFor(event)
                        ?: return@forEach
                val outcome =
                    event.outcome
                        ?: return@forEach
                val thirdSix =
                    outcome == 6 &&
                        isThirdConsecutiveSix(
                            current = current,
                            event = event,
                        )

                when {
                    thirdSix ->
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.THIRD_SIX_FORFEIT,
                                player = player,
                                eventIndex = event.eventIndex,
                                value = outcome,
                            )

                    outcome == 6 ->
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.SIX_ROLLED,
                                player = player,
                                eventIndex = event.eventIndex,
                                value = outcome,
                            )

                    outcome <= 2 ->
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.LOW_ROLL,
                                player = player,
                                eventIndex = event.eventIndex,
                                value = outcome,
                            )
                }

                val pendingForEvent =
                    current.pendingRoll
                        ?.takeIf {
                            it.eventIndex == event.eventIndex
                        }
                if (
                    !thirdSix &&
                    event.moveTokenIndex == null &&
                    pendingForEvent == null
                ) {
                    sink +=
                        moment(
                            current = current,
                            type = GameMomentType.NO_LEGAL_MOVE,
                            player = player,
                            eventIndex = event.eventIndex,
                            value = outcome,
                        )
                }
            }
    }

    private fun detectTokenChanges(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsGameMoment>,
    ) {
        current.players.forEach {
                currentPlayer ->
            val previousPlayer =
                previous.players
                    .firstOrNull {
                        it.playerId == currentPlayer.playerId
                    }
                    ?: return@forEach

            currentPlayer.tokens
                .indices
                .forEach {
                        tokenIndex ->
                    val from =
                        previousPlayer.tokens
                            .getOrNull(tokenIndex)
                            ?: return@forEach
                    val to =
                        currentPlayer.tokens
                            .getOrNull(tokenIndex)
                            ?: return@forEach
                    if (from == to) {
                        return@forEach
                    }

                    val moveEvent =
                        current.latestMoveEvent(
                            playerId = currentPlayer.playerId,
                            tokenIndex = tokenIndex,
                        )
                    val eventIndex =
                        moveEvent?.eventIndex
                            ?: current.randomEventIndex

                    if (
                        from >= 0 &&
                        to == -1
                    ) {
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.TOKEN_CAPTURED,
                                player = currentPlayer,
                                eventIndex = eventIndex,
                                tokenIndex = tokenIndex,
                            )
                        return@forEach
                    }

                    if (to <= from) {
                        return@forEach
                    }

                    sink +=
                        moment(
                            current = current,
                            type =
                                if (from == -1 && to == 0) {
                                    GameMomentType.TOKEN_LEFT_YARD
                                } else {
                                    GameMomentType.TOKEN_MOVED
                                },
                            player = currentPlayer,
                            eventIndex = eventIndex,
                            tokenIndex = tokenIndex,
                            value = to,
                        )

                    if (
                        moveEvent?.captures
                            ?.let { it > 0 } == true
                    ) {
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.CAPTURE_MADE,
                                player = currentPlayer,
                                eventIndex = moveEvent.eventIndex,
                                tokenIndex = tokenIndex,
                                value = moveEvent.captures,
                            )
                    }

                    if (to == 57) {
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.HOME_REACHED,
                                player = currentPlayer,
                                eventIndex = eventIndex,
                                tokenIndex = tokenIndex,
                            )
                    } else if (
                        isSafePosition(
                            color = currentPlayer.color,
                            position = to,
                        )
                    ) {
                        sink +=
                            moment(
                                current = current,
                                type = GameMomentType.SAFE_REACHED,
                                player = currentPlayer,
                                eventIndex = eventIndex,
                                tokenIndex = tokenIndex,
                            )
                    }
                }
        }
    }

    private fun detectLeadChange(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsGameMoment>,
    ) {
        val previousLeader =
            uniqueLeader(previous)
        val currentLeader =
            uniqueLeader(current)
                ?: return

        if (
            previousLeader?.playerId == currentLeader.playerId ||
            leadMargin(current, currentLeader) < 8
        ) {
            return
        }

        sink +=
            moment(
                current = current,
                type = GameMomentType.PLAYER_LEADING,
                player = currentLeader,
                eventIndex = current.randomEventIndex,
                value = progressScore(currentLeader),
            )
    }

    private fun detectFinish(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsGameMoment>,
    ) {
        if (
            previous.status == "FINISHED" ||
            current.status != "FINISHED"
        ) {
            return
        }

        val eventIndex =
            current.history
                .lastOrNull()
                ?.eventIndex
                ?: current.randomEventIndex

        if (!current.winnerTeamId.isNullOrBlank()) {
            current.players.forEach {
                    player ->
                sink +=
                    moment(
                        current = current,
                        type =
                            if (player.teamId == current.winnerTeamId) {
                                GameMomentType.TEAM_WIN
                            } else {
                                GameMomentType.TEAM_LOSS
                            },
                        player = player,
                        eventIndex = eventIndex,
                    )
            }
            return
        }

        val winnerId =
            current.winnerPlayerId
                ?: return
        current.players.forEach {
                player ->
            sink +=
                moment(
                    current = current,
                    type =
                        if (player.playerId == winnerId) {
                            GameMomentType.MATCH_WIN
                        } else {
                            GameMomentType.MATCH_LOSS
                        },
                    player = player,
                    eventIndex = eventIndex,
                )
        }
    }

    private fun isThirdConsecutiveSix(
        current: MatchSnapshot,
        event: HistoryEventSnapshot,
    ): Boolean {
        val recent =
            current.history
                .filter {
                    it.eventIndex <= event.eventIndex
                }
                .takeLast(3)

        return recent.size == 3 &&
            recent.all {
                it.playerId == event.playerId &&
                    it.outcome == 6
            }
    }

    private fun isSafePosition(
        color: String,
        position: Int,
    ): Boolean {
        if (position !in 0..51) {
            return false
        }
        val start =
            startOffsets[color]
                ?: return false
        return (
            (start + position) % 52
            ) in safeGlobalCells
    }

    private fun uniqueLeader(
        state: MatchSnapshot,
    ): PlayerSnapshot? {
        val ranked =
            state.players
                .map {
                    it to progressScore(it)
                }
                .sortedByDescending {
                    it.second
                }
        val first =
            ranked.firstOrNull()
                ?: return null
        val second =
            ranked.getOrNull(1)
        if (
            second != null &&
            first.second == second.second
        ) {
            return null
        }
        return first.first
    }

    private fun leadMargin(
        state: MatchSnapshot,
        leader: PlayerSnapshot,
    ): Int {
        val leaderScore =
            progressScore(leader)
        val next =
            state.players
                .asSequence()
                .filter {
                    it.playerId != leader.playerId
                }
                .map(::progressScore)
                .maxOrNull()
                ?: 0
        return leaderScore - next
    }

    private fun progressScore(
        player: PlayerSnapshot,
    ): Int =
        player.tokens.sumOf {
                position ->
            when (position) {
                -1 -> 0
                in 0..51 -> position + 1
                in 52..56 -> 60 + (position - 52) * 8
                57 -> 100
                else -> 0
            }
        }

    private fun MatchSnapshot.playerFor(
        event: HistoryEventSnapshot,
    ): PlayerSnapshot? =
        players.firstOrNull {
            it.playerId == event.playerId
        }

    private fun MatchSnapshot.latestMoveEvent(
        playerId: String,
        tokenIndex: Int,
    ): HistoryEventSnapshot? =
        history
            .asReversed()
            .firstOrNull {
                it.playerId == playerId &&
                    it.moveTokenIndex == tokenIndex
            }

    private fun moment(
        current: MatchSnapshot,
        type: GameMomentType,
        player: PlayerSnapshot,
        eventIndex: Int,
        tokenIndex: Int? = null,
        relatedPlayerId: String? = null,
        value: Int? = null,
    ): LudoPawsGameMoment =
        LudoPawsGameMoment(
            matchId = current.matchId,
            eventIndex = eventIndex,
            type = type,
            playerId = player.playerId,
            seat = player.seat,
            tokenIndex = tokenIndex,
            relatedPlayerId = relatedPlayerId,
            value = value,
        )
}
