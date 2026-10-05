package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue

data class LudoPawsReaction(
    val playerId: String,
    val seat: Int,
    val tokenIndex: Int? = null,
    val voiceCue: VoiceCue,
    val animationCue: AnimationCue,
    val priority: Int,
    val matchId: String = "",
    val eventIndex: Int = -1,
    val momentType: GameMomentType? = null,
    val reactionKey: String = "",
)

/** Presentation adapter from authoritative snapshot moments to character intent. */
object LudoPawsReactionEngine {
    private val playbackDirector = LudoPawsReactionDirector()

    fun detect(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): List<LudoPawsReaction> {
        val raw = derive(previous, current)
        return playbackDirector
            .submit(
                reactions = raw,
                nowMillis = monotonicMillis(),
            )
            .reactions
    }

    fun derive(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): List<LudoPawsReaction> {
        val mapped =
            LudoPawsGameMomentDetector
                .detect(
                    previous = previous,
                    current = current,
                )
                .mapNotNull(::toReaction)

        return alignCapturePair(mapped)
            .distinctBy(LudoPawsReaction::reactionKey)
            .sortedByDescending(LudoPawsReaction::priority)
    }

    fun deriveIdle(
        current: MatchSnapshot?,
        nowMillis: Long,
        lastMeaningfulChangeAtMillis: Long,
        thresholdMillis: Long = 24_000L,
    ): List<LudoPawsReaction> =
        LudoPawsGameMomentDetector
            .idleMoment(
                current = current,
                nowMillis = nowMillis,
                lastMeaningfulChangeAtMillis = lastMeaningfulChangeAtMillis,
                thresholdMillis = thresholdMillis,
            )
            ?.let(::toReaction)
            ?.let(::listOf)
            .orEmpty()

    internal fun resetPlaybackStateForTests() {
        playbackDirector.clear()
    }

    private fun alignCapturePair(
        reactions: List<LudoPawsReaction>,
    ): List<LudoPawsReaction> {
        val captureMade =
            reactions.filter {
                it.momentType == GameMomentType.CAPTURE_MADE
            }
        if (captureMade.size != 1) return reactions

        val attacker = captureMade.single()
        return reactions.map { reaction ->
            if (
                reaction.momentType == GameMomentType.TOKEN_CAPTURED &&
                reaction.matchId == attacker.matchId
            ) {
                reaction.copy(
                    eventIndex = attacker.eventIndex,
                    reactionKey =
                        stableReactionKey(
                            matchId = reaction.matchId,
                            eventIndex = attacker.eventIndex,
                            type = requireNotNull(reaction.momentType),
                            playerId = reaction.playerId,
                            tokenIndex = reaction.tokenIndex,
                        ),
                )
            } else {
                reaction
            }
        }
    }

    private fun toReaction(
        moment: LudoPawsGameMoment,
    ): LudoPawsReaction? =
        when (moment.type) {
            GameMomentType.TURN_STARTED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.SILENT,
                    animationCue = AnimationCue.IDLE,
                    priority = 12,
                )

            GameMomentType.SIX_ROLLED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.SIX,
                    animationCue = AnimationCue.EXCITED,
                    priority = 66,
                )

            GameMomentType.TOKEN_LEFT_YARD ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.YARD_EXIT,
                    animationCue = AnimationCue.HAPPY,
                    priority = 44,
                )

            GameMomentType.ONLY_LEGAL_MOVE ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.SILENT,
                    animationCue = AnimationCue.HAPPY,
                    priority = 14,
                )

            GameMomentType.THIRD_SIX_FORFEIT ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.THIRD_SIX,
                    animationCue = AnimationCue.ANGRY,
                    priority = 92,
                )

            GameMomentType.CAPTURE_MADE ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.CAPTURE,
                    animationCue = AnimationCue.CAPTURE,
                    priority = 88,
                )

            GameMomentType.TOKEN_CAPTURED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.CAPTURED,
                    animationCue = AnimationCue.CAPTURED,
                    priority = 86,
                )

            GameMomentType.SAFE_REACHED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.SAFE,
                    animationCue = AnimationCue.SAFE,
                    priority = 58,
                )

            GameMomentType.HOME_LANE_ENTERED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.HOME_LANE,
                    animationCue = AnimationCue.EXCITED,
                    priority = 62,
                )

            GameMomentType.HOME_REACHED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.HOME,
                    animationCue = AnimationCue.HOME,
                    priority = 82,
                )

            GameMomentType.POOR_ROLL_STREAK ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.FRUSTRATED,
                    animationCue = AnimationCue.SAD,
                    priority = 52,
                )

            GameMomentType.EXACT_HOME_MISS ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.FRUSTRATED,
                    animationCue = AnimationCue.SAD,
                    priority = 50,
                )

            GameMomentType.NO_LEGAL_MOVE ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.FRUSTRATED,
                    animationCue = AnimationCue.SAD,
                    priority = 46,
                )

            GameMomentType.TOKEN_THREATENED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.NERVOUS,
                    animationCue = AnimationCue.NERVOUS,
                    priority = 40,
                )

            GameMomentType.PLAYER_LEADING ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.PROUD,
                    animationCue = AnimationCue.HAPPY,
                    priority = 26,
                )

            GameMomentType.MATCH_WIN,
            GameMomentType.TEAM_WIN,
            ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.VICTORY,
                    animationCue = AnimationCue.VICTORY,
                    priority = 100,
                )

            GameMomentType.MATCH_LOSS,
            GameMomentType.TEAM_LOSS,
            ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.DEFEAT,
                    animationCue = AnimationCue.DEFEAT,
                    priority = 30,
                )

            GameMomentType.IDLE_WAITING ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.IDLE,
                    animationCue = AnimationCue.IDLE,
                    priority = 20,
                )

            GameMomentType.ROLL_STARTED,
            GameMomentType.LOW_ROLL,
            GameMomentType.TOKEN_MOVED,
            -> null
        }

    private fun reaction(
        moment: LudoPawsGameMoment,
        voiceCue: VoiceCue,
        animationCue: AnimationCue,
        priority: Int,
    ): LudoPawsReaction =
        LudoPawsReaction(
            playerId = moment.playerId,
            seat = moment.seat,
            tokenIndex = moment.tokenIndex,
            voiceCue = voiceCue,
            animationCue = animationCue,
            priority = priority,
            matchId = moment.matchId,
            eventIndex = moment.eventIndex,
            momentType = moment.type,
            reactionKey =
                stableReactionKey(
                    matchId = moment.matchId,
                    eventIndex = moment.eventIndex,
                    type = moment.type,
                    playerId = moment.playerId,
                    tokenIndex = moment.tokenIndex,
                ),
        )

    private fun stableReactionKey(
        matchId: String,
        eventIndex: Int,
        type: GameMomentType,
        playerId: String,
        tokenIndex: Int?,
    ): String =
        listOf(
            matchId,
            eventIndex.toString(),
            type.name,
            playerId,
            tokenIndex?.toString().orEmpty(),
        ).joinToString(":")

    private fun monotonicMillis(): Long =
        System.nanoTime() / 1_000_000L
}
