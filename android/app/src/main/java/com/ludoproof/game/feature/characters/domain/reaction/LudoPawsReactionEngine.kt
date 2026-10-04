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

/**
 * Presentation reaction adapter for the shared game-moment detector.
 *
 * [derive] is pure and is used by unit tests/domain consumers. [detect] is the
 * playback entry point used by the UI: it sends derived reactions through the
 * Phase 8 director so reconnect/re-render duplicates, cooldown spam, priority,
 * queueing, and interruption policy are handled before animation/voice output.
 */
object LudoPawsReactionEngine {
    private val playbackDirector =
        LudoPawsReactionDirector()

    fun detect(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): List<LudoPawsReaction> {
        val raw =
            derive(
                previous = previous,
                current = current,
            )
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
    ): List<LudoPawsReaction> =
        LudoPawsGameMomentDetector
            .detect(
                previous = previous,
                current = current,
            )
            .mapNotNull(::toReaction)
            .distinctBy(
                LudoPawsReaction::reactionKey,
            )
            .sortedByDescending(
                LudoPawsReaction::priority,
            )

    internal fun resetPlaybackStateForTests() {
        playbackDirector.clear()
    }

    private fun toReaction(
        moment: LudoPawsGameMoment,
    ): LudoPawsReaction? =
        when (moment.type) {
            GameMomentType.SIX_ROLLED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.SIX,
                    animationCue = AnimationCue.EXCITED,
                    priority = 66,
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

            GameMomentType.HOME_REACHED ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.HOME,
                    animationCue = AnimationCue.HOME,
                    priority = 82,
                )

            GameMomentType.NO_LEGAL_MOVE ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.FRUSTRATED,
                    animationCue = AnimationCue.SAD,
                    priority = 48,
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

            GameMomentType.PLAYER_LEADING ->
                reaction(
                    moment = moment,
                    voiceCue = VoiceCue.NERVOUS,
                    animationCue = AnimationCue.NERVOUS,
                    priority = 24,
                )

            GameMomentType.TURN_STARTED,
            GameMomentType.ROLL_STARTED,
            GameMomentType.LOW_ROLL,
            GameMomentType.TOKEN_LEFT_YARD,
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
                listOf(
                    moment.matchId,
                    moment.eventIndex.toString(),
                    moment.type.name,
                    moment.playerId,
                    moment.tokenIndex
                        ?.toString()
                        .orEmpty(),
                ).joinToString(":"),
        )

    private fun monotonicMillis(): Long =
        System.nanoTime() /
            1_000_000L
}
