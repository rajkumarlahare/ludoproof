package com.ludoproof.game

import com.ludoproof.game.feature.settings.data.local.GameSpeed
import kotlin.math.max

/**
 * Central presentation pacing for readable Ludo Paws play.
 *
 * The authoritative rules and committed match state never depend on these
 * values. They only decide how long the UI waits before exposing the next
 * action while dice, movement, capture and settle animations remain visible.
 */
object LudoPawsGameplayPacingPolicy {
    const val DICE_SETTLE_DURATION_MILLIS = 450L

    fun diceResultHoldMillis(speed: GameSpeed): Long =
        when (speed) {
            GameSpeed.SLOW -> 850L
            GameSpeed.NORMAL -> 700L
            GameSpeed.FAST -> 520L
        }

    /** Small tail after the common result hold before a forced single move. */
    fun postRollAutoMoveDelayMillis(speed: GameSpeed): Long =
        when (speed) {
            GameSpeed.SLOW -> 150L
            GameSpeed.NORMAL -> 100L
            GameSpeed.FAST -> 80L
        }

    fun captureReturnDurationMillis(speed: GameSpeed): Long =
        when (speed) {
            GameSpeed.SLOW -> 1_500L
            GameSpeed.NORMAL -> 1_300L
            GameSpeed.FAST -> 1_050L
        }

    fun postMoveBreathMillis(
        speed: GameSpeed,
        extraTurn: Boolean,
    ): Long {
        val base =
            when (speed) {
                GameSpeed.SLOW -> 560L
                GameSpeed.NORMAL -> 450L
                GameSpeed.FAST -> 330L
            }
        val extraRollHold =
            when (speed) {
                GameSpeed.SLOW -> 720L
                GameSpeed.NORMAL -> 600L
                GameSpeed.FAST -> 450L
            }
        return if (extraTurn) max(base, extraRollHold) else base
    }

    fun interactionDelayAfterMoveMillis(
        previous: MatchSnapshot?,
        current: MatchSnapshot,
        speed: GameSpeed,
    ): Long {
        if (
            previous == null ||
            previous.matchId != current.matchId
        ) {
            return postMoveBreathMillis(
                speed = speed,
                extraTurn = false,
            )
        }

        val motions =
            LudoPawsPawnAnimationPolicy.plans(
                previous = previous,
                current = current,
            )
        val forward =
            motions.firstOrNull {
                it.kind == LudoPawsPawnMotionKind.FORWARD
            }
        val forwardDuration =
            forward
                ?.let {
                    max(
                        speed.moveStepMs,
                        it.visualSteps.toLong() * speed.moveStepMs,
                    )
                }
                ?: 0L
        val hasCapture =
            motions.any {
                it.kind == LudoPawsPawnMotionKind.CAPTURE_RETURN
            }
        val extraTurn =
            previous.status == "ACTIVE" &&
                current.status == "ACTIVE" &&
                previous.turnSeat == current.turnSeat
        val captureDuration =
            if (hasCapture) {
                captureReturnDurationMillis(speed)
            } else {
                0L
            }

        return forwardDuration +
            captureDuration +
            postMoveBreathMillis(
                speed = speed,
                extraTurn = extraTurn,
            )
    }
}
