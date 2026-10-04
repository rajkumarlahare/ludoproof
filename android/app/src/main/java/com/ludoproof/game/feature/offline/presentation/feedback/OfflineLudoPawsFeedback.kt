package com.ludoproof.game.feature.offline.presentation.feedback

import android.content.Context
import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
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
    CAPTURE,
    SAFE,
    HOME,
    VICTORY,
    DEFEAT,
}

data class OfflineFeedbackDecision(
    val sound: OfflineFeedbackSound,
    val reactions: List<LudoPawsReaction>,
)

/**
 * Pure policy for presentation feedback after an already-committed local action.
 *
 * It only compares immutable snapshots and never participates in dice, move,
 * capture, turn, CPU, proof, or winner decisions.
 */
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
            reactions.maxByOrNull(
                LudoPawsReaction::priority,
            )
        val dedicatedSound =
            when (highest?.voiceCue) {
                VoiceCue.VICTORY -> OfflineFeedbackSound.VICTORY
                VoiceCue.DEFEAT -> OfflineFeedbackSound.DEFEAT
                VoiceCue.CAPTURE,
                VoiceCue.CAPTURED,
                -> OfflineFeedbackSound.CAPTURE

                VoiceCue.HOME -> OfflineFeedbackSound.HOME
                VoiceCue.SAFE -> OfflineFeedbackSound.SAFE
                VoiceCue.SIX -> OfflineFeedbackSound.SIX
                VoiceCue.THIRD_SIX,
                VoiceCue.FRUSTRATED,
                VoiceCue.IDLE,
                VoiceCue.NERVOUS,
                null,
                -> OfflineFeedbackSound.NONE
            }

        val sound =
            when {
                dedicatedSound != OfflineFeedbackSound.NONE -> dedicatedSound
                action == OfflineFeedbackAction.MOVE &&
                    tokenMovementCommitted -> OfflineFeedbackSound.MOVE
                else -> OfflineFeedbackSound.NONE
            }

        return OfflineFeedbackDecision(
            sound = sound,
            reactions = reactions,
        )
    }

    /**
     * Stable key for one already-committed local action. Roll and move are
     * intentionally distinct even when they share one fairness event index.
     */
    fun committedActionKey(
        current: MatchSnapshot?,
        action: OfflineFeedbackAction,
    ): String? {
        current ?: return null

        val eventIndex =
            when (action) {
                OfflineFeedbackAction.ROLL ->
                    current.pendingRoll
                        ?.eventIndex
                        ?: current.history
                            .lastOrNull()
                            ?.eventIndex
                        ?: (current.randomEventIndex - 1)
                            .takeIf { it >= 0 }

                OfflineFeedbackAction.MOVE ->
                    current.history
                        .lastOrNull {
                            it.moveTokenIndex != null
                        }
                        ?.eventIndex
                        ?: current.history
                            .lastOrNull()
                            ?.eventIndex
                        ?: current.pendingRoll
                            ?.eventIndex
            }
                ?: return null

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

        return current.players.any {
                currentPlayer ->
            val previousPlayer =
                previous.players.firstOrNull {
                    it.playerId == currentPlayer.playerId
                }
                    ?: return@any false
            previousPlayer.tokens != currentPlayer.tokens
        }
    }
}

/**
 * Android side-effect adapter for [OfflineLudoPawsFeedbackPolicy].
 *
 * The activity-owned ledger makes a successful local action idempotent at the
 * sound/haptic boundary, so duplicated callbacks cannot emit feedback twice.
 */
object OfflineLudoPawsFeedbackDispatcher {
    fun committed(
        context: Context,
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
        action: OfflineFeedbackAction,
    ) {
        val ledger =
            (context as? OfflineGameActivity)
                ?.feedbackLedger
                ?: return
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
        val safeCurrent =
            current
                ?: return
        val actionKey =
            OfflineLudoPawsFeedbackPolicy
                .committedActionKey(
                    current = safeCurrent,
                    action = action,
                )
                ?: return
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
            OfflineFeedbackSound.MOVE -> GameSoundFeedback.move(context)
            OfflineFeedbackSound.SIX -> GameSoundFeedback.six(context)
            OfflineFeedbackSound.CAPTURE -> GameSoundFeedback.capture(context)
            OfflineFeedbackSound.SAFE -> GameSoundFeedback.safe(context)
            OfflineFeedbackSound.HOME -> GameSoundFeedback.home(context)
            OfflineFeedbackSound.VICTORY -> GameSoundFeedback.victory(context)
            OfflineFeedbackSound.DEFEAT -> GameSoundFeedback.defeat(context)
        }

        LudoPawsHaptics.reaction(
            context = context,
            reactions = decision.reactions,
        )
    }
}
