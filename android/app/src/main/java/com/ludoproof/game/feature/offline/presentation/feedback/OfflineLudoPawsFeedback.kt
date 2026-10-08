package com.ludoproof.game.feature.offline.presentation.feedback

import android.content.Context
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.feature.characters.domain.reaction.GameMomentType
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFeedbackLedger
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.feature.settings.data.local.LudoPawsHaptics

enum class OfflineFeedbackAction {
    ROLL,
    MOVE,
}

enum class OfflineFeedbackSound {
    NONE,
    MOVE,
    SIX,
    YARD_EXIT,
    CAPTURE,
    SAFE,
    HOME_LANE,
    HOME,
    FRUSTRATED,
    THIRD_SIX,
    VICTORY,
    DEFEAT,
}

data class OfflineFeedbackDecision(
    val sound: OfflineFeedbackSound,
    val reactions: List<LudoPawsReaction>,
)

/** Pure policy for presentation feedback after an already-committed local action. */
object OfflineLudoPawsFeedbackPolicy {
    fun decide(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
        action: OfflineFeedbackAction,
    ): OfflineFeedbackDecision {
        val reactions =
            LudoPawsReactionEngine.derive(
                previous = previous,
                current = current,
            )
        return decide(
            action = action,
            reactions = reactions,
            tokenMovementCommitted =
                hasCommittedTokenMovement(
                    previous = previous,
                    current = current,
                ),
        )
    }

    fun decide(
        action: OfflineFeedbackAction,
        reactions: List<LudoPawsReaction>,
        tokenMovementCommitted: Boolean,
    ): OfflineFeedbackDecision {
        val highest =
            reactions.maxByOrNull(LudoPawsReaction::priority)
        val dedicatedSound =
            when (highest?.momentType) {
                GameMomentType.SIX_ROLLED -> OfflineFeedbackSound.SIX
                // All physical pawn-arrival sounds are emitted by the shared
                // 3D movement clock, never immediately at snapshot commit.
                GameMomentType.TOKEN_LEFT_YARD,
                GameMomentType.CAPTURE_MADE,
                GameMomentType.TOKEN_CAPTURED,
                GameMomentType.SAFE_REACHED,
                GameMomentType.HOME_LANE_ENTERED,
                GameMomentType.HOME_REACHED,
                GameMomentType.ONLY_LEGAL_MOVE,
                -> OfflineFeedbackSound.NONE
                GameMomentType.POOR_ROLL_STREAK,
                GameMomentType.EXACT_HOME_MISS,
                GameMomentType.NO_LEGAL_MOVE,
                -> OfflineFeedbackSound.FRUSTRATED
                GameMomentType.THIRD_SIX_FORFEIT -> OfflineFeedbackSound.THIRD_SIX
                GameMomentType.MATCH_WIN,
                GameMomentType.TEAM_WIN,
                -> OfflineFeedbackSound.VICTORY
                GameMomentType.MATCH_LOSS,
                GameMomentType.TEAM_LOSS,
                -> OfflineFeedbackSound.DEFEAT
                GameMomentType.TURN_STARTED,
                GameMomentType.ROLL_STARTED,
                GameMomentType.LOW_ROLL,
                GameMomentType.TOKEN_MOVED,
                GameMomentType.TOKEN_THREATENED,
                GameMomentType.PLAYER_LEADING,
                GameMomentType.IDLE_WAITING,
                null,
                -> OfflineFeedbackSound.NONE
            }

        val sound =
            when {
                dedicatedSound != OfflineFeedbackSound.NONE -> dedicatedSound
                // A committed movement is already rendered/audio-ticked per visual step.
                // Do not fall back to a second one-shot movement cue.
                action == OfflineFeedbackAction.MOVE && tokenMovementCommitted ->
                    OfflineFeedbackSound.NONE
                else -> OfflineFeedbackSound.NONE
            }

        return OfflineFeedbackDecision(
            sound = sound,
            reactions = reactions,
        )
    }

    fun committedActionKey(
        current: MatchSnapshot?,
        action: OfflineFeedbackAction,
    ): String? {
        current ?: return null
        val eventIndex =
            when (action) {
                OfflineFeedbackAction.ROLL ->
                    current.pendingRoll?.eventIndex
                        ?: current.history.lastOrNull()?.eventIndex
                        ?: (current.randomEventIndex - 1).takeIf { it >= 0 }
                OfflineFeedbackAction.MOVE ->
                    current.history.lastOrNull {
                        it.moveTokenIndex != null
                    }?.eventIndex
                        ?: current.history.lastOrNull()?.eventIndex
                        ?: current.pendingRoll?.eventIndex
            } ?: return null
        return "${action.name}:$eventIndex"
    }

    private fun hasCommittedTokenMovement(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): Boolean {
        if (
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            return false
        }
        return current.players.any { currentPlayer ->
            val previousPlayer =
                previous.players.firstOrNull {
                    it.playerId == currentPlayer.playerId
                } ?: return@any false
            previousPlayer.tokens != currentPlayer.tokens
        }
    }
}

/** Android side-effect adapter with exactly-once feedback gating. */
object OfflineLudoPawsFeedbackDispatcher {
    fun committed(
        context: Context,
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
        action: OfflineFeedbackAction,
    ) {
        val activity =
            context as? OfflineGameActivity
                ?: return
        val ledger = activity.feedbackLedger

        committed(
            context = context,
            ledger = ledger,
            previous = previous,
            current = current,
            action = action,
        )
    }

    fun committed(
        context: Context,
        ledger: LudoPawsFeedbackLedger,
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
        action: OfflineFeedbackAction,
    ) {
        val safeCurrent = current ?: return
        val actionKey =
            OfflineLudoPawsFeedbackPolicy
                .committedActionKey(
                    current = safeCurrent,
                    action = action,
                ) ?: return
        if (
            !ledger.once(
                matchId = safeCurrent.matchId,
                key = "OFFLINE:$actionKey",
            )
        ) {
            return
        }

        val decision =
            OfflineLudoPawsFeedbackPolicy.decide(
                previous = previous,
                current = safeCurrent,
                action = action,
            )
        when (decision.sound) {
            OfflineFeedbackSound.NONE -> Unit
            // Kept for enum compatibility; synchronized movement uses moveStep().
            OfflineFeedbackSound.MOVE -> Unit
            OfflineFeedbackSound.SIX -> GameSoundFeedback.six(context)
            OfflineFeedbackSound.YARD_EXIT -> GameSoundFeedback.yardExit(context)
            OfflineFeedbackSound.CAPTURE -> GameSoundFeedback.capture(context)
            OfflineFeedbackSound.SAFE -> GameSoundFeedback.safe(context)
            OfflineFeedbackSound.HOME_LANE -> GameSoundFeedback.homeLane(context)
            OfflineFeedbackSound.HOME -> GameSoundFeedback.home(context)
            OfflineFeedbackSound.FRUSTRATED -> GameSoundFeedback.frustrated(context)
            OfflineFeedbackSound.THIRD_SIX -> GameSoundFeedback.thirdSix(context)
            OfflineFeedbackSound.VICTORY -> GameSoundFeedback.victory(context)
            OfflineFeedbackSound.DEFEAT -> GameSoundFeedback.defeat(context)
        }

        LudoPawsHaptics.reaction(
            context = context,
            reactions = decision.reactions,
        )
    }

}
