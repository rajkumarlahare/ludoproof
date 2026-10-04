package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.HistoryEventSnapshot
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PlayerSnapshot
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue

data class LudoPawsReaction(
    val playerId: String,
    val seat: Int,
    val tokenIndex: Int? = null,
    val voiceCue: VoiceCue,
    val animationCue: AnimationCue,
    val priority: Int,
)

/**
 * Pure presentation-domain detector for Ludo Paws reactions.
 *
 * It compares two immutable MatchSnapshot values and never mutates gameplay,
 * proof, dice, legal-move, capture, winner, or persistence state.
 */
object LudoPawsReactionEngine {
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
    ): List<LudoPawsReaction> {
        if (
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            return emptyList()
        }

        val reactions =
            mutableListOf<LudoPawsReaction>()

        detectNewRollReactions(
            previous = previous,
            current = current,
            sink = reactions,
        )
        detectTokenReactions(
            previous = previous,
            current = current,
            sink = reactions,
        )
        detectMatchFinishReactions(
            previous = previous,
            current = current,
            sink = reactions,
        )

        return reactions
            .distinctBy {
                listOf(
                    it.playerId,
                    it.tokenIndex?.toString().orEmpty(),
                    it.voiceCue.name,
                ).joinToString(":")
            }
            .sortedByDescending(
                LudoPawsReaction::priority,
            )
    }

    private fun detectNewRollReactions(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsReaction>,
    ) {
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
                            reaction(
                                player = player,
                                voiceCue = VoiceCue.THIRD_SIX,
                                animationCue = AnimationCue.ANGRY,
                                priority = 92,
                            )

                    outcome == 6 ->
                        sink +=
                            reaction(
                                player = player,
                                voiceCue = VoiceCue.SIX,
                                animationCue = AnimationCue.EXCITED,
                                priority = 66,
                            )

                    event.moveTokenIndex == null &&
                        current.pendingRoll?.eventIndex !=
                        event.eventIndex ->
                        sink +=
                            reaction(
                                player = player,
                                voiceCue = VoiceCue.FRUSTRATED,
                                animationCue = AnimationCue.SAD,
                                priority = 48,
                            )
                }
            }
    }

    private fun detectTokenReactions(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsReaction>,
    ) {
        current.players.forEach {
                currentPlayer ->
            val previousPlayer =
                previous.players
                    .firstOrNull {
                        it.playerId ==
                            currentPlayer.playerId
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

                    if (
                        from >= 0 &&
                        to == -1
                    ) {
                        sink +=
                            reaction(
                                player = currentPlayer,
                                tokenIndex = tokenIndex,
                                voiceCue = VoiceCue.CAPTURED,
                                animationCue = AnimationCue.CAPTURED,
                                priority = 86,
                            )
                        return@forEach
                    }

                    if (to <= from) {
                        return@forEach
                    }

                    val moveEvent =
                        current.history
                            .asReversed()
                            .firstOrNull {
                                it.playerId ==
                                    currentPlayer.playerId &&
                                    it.moveTokenIndex ==
                                    tokenIndex
                            }

                    when {
                        moveEvent?.captures
                            ?.let {
                                it > 0
                            } == true ->
                            sink +=
                                reaction(
                                    player = currentPlayer,
                                    tokenIndex = tokenIndex,
                                    voiceCue = VoiceCue.CAPTURE,
                                    animationCue = AnimationCue.CAPTURE,
                                    priority = 88,
                                )

                        to == 57 ->
                            sink +=
                                reaction(
                                    player = currentPlayer,
                                    tokenIndex = tokenIndex,
                                    voiceCue = VoiceCue.HOME,
                                    animationCue = AnimationCue.HOME,
                                    priority = 82,
                                )

                        isSafePosition(
                            color = currentPlayer.color,
                            position = to,
                        ) ->
                            sink +=
                                reaction(
                                    player = currentPlayer,
                                    tokenIndex = tokenIndex,
                                    voiceCue = VoiceCue.SAFE,
                                    animationCue = AnimationCue.SAFE,
                                    priority = 58,
                                )
                    }
                }
        }
    }

    private fun detectMatchFinishReactions(
        previous: MatchSnapshot,
        current: MatchSnapshot,
        sink: MutableList<LudoPawsReaction>,
    ) {
        if (
            previous.status == "FINISHED" ||
            current.status != "FINISHED" ||
            current.winnerPlayerId.isNullOrBlank()
        ) {
            return
        }

        current.players.forEach {
                player ->
            if (
                player.playerId ==
                current.winnerPlayerId
            ) {
                sink +=
                    reaction(
                        player = player,
                        voiceCue = VoiceCue.VICTORY,
                        animationCue = AnimationCue.VICTORY,
                        priority = 100,
                    )
            } else {
                sink +=
                    reaction(
                        player = player,
                        voiceCue = VoiceCue.DEFEAT,
                        animationCue = AnimationCue.DEFEAT,
                        priority = 30,
                    )
            }
        }
    }

    private fun isThirdConsecutiveSix(
        current: MatchSnapshot,
        event: HistoryEventSnapshot,
    ): Boolean {
        val recent =
            current.history
                .filter {
                    it.eventIndex <=
                        event.eventIndex
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

    private fun MatchSnapshot.playerFor(
        event: HistoryEventSnapshot,
    ): PlayerSnapshot? =
        players.firstOrNull {
            it.playerId == event.playerId
        }

    private fun reaction(
        player: PlayerSnapshot,
        tokenIndex: Int? = null,
        voiceCue: VoiceCue,
        animationCue: AnimationCue,
        priority: Int,
    ): LudoPawsReaction =
        LudoPawsReaction(
            playerId = player.playerId,
            seat = player.seat,
            tokenIndex = tokenIndex,
            voiceCue = voiceCue,
            animationCue = animationCue,
            priority = priority,
        )
}
