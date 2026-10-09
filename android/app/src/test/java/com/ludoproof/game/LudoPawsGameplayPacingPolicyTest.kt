package com.ludoproof.game

import com.ludoproof.game.feature.settings.data.local.GameSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsGameplayPacingPolicyTest {
    @Test
    fun normalDiceResultGetsReadableHoldBeforeFollowUp() {
        assertEquals(
            700L,
            LudoPawsGameplayPacingPolicy
                .diceResultHoldMillis(GameSpeed.NORMAL),
        )
        assertEquals(
            100L,
            LudoPawsGameplayPacingPolicy
                .postRollAutoMoveDelayMillis(GameSpeed.NORMAL),
        )
    }

    @Test
    fun normalCaptureReturnHasTimeForContactPushAndSettle() {
        assertEquals(
            1_300L,
            LudoPawsGameplayPacingPolicy
                .captureReturnDurationMillis(GameSpeed.NORMAL),
        )
        assertTrue(
            LudoPawsGameplayPacingPolicy
                .captureReturnDurationMillis(GameSpeed.NORMAL) >
                1_000L,
        )
    }

    @Test
    fun fasterModeStillPreservesOrderedReadablePacing() {
        assertTrue(
            LudoPawsGameplayPacingPolicy
                .diceResultHoldMillis(GameSpeed.FAST) <
                LudoPawsGameplayPacingPolicy
                    .diceResultHoldMillis(GameSpeed.NORMAL),
        )
        assertTrue(
            LudoPawsGameplayPacingPolicy
                .diceResultHoldMillis(GameSpeed.NORMAL) <
                LudoPawsGameplayPacingPolicy
                    .diceResultHoldMillis(GameSpeed.SLOW),
        )
        assertTrue(
            LudoPawsGameplayPacingPolicy
                .captureReturnDurationMillis(GameSpeed.FAST) <
                LudoPawsGameplayPacingPolicy
                    .captureReturnDurationMillis(GameSpeed.NORMAL),
        )
        assertTrue(
            LudoPawsGameplayPacingPolicy
                .captureReturnDurationMillis(GameSpeed.NORMAL) <
                LudoPawsGameplayPacingPolicy
                    .captureReturnDurationMillis(GameSpeed.SLOW),
        )
    }

    @Test
    fun movementAudioUsesOneTickPerVisualStepAtEverySpeed() {
        assertEquals(
            360L,
            LudoPawsGameplayPacingPolicy.movementAudioStepDurationMillis(
                forwardDurationMillis = 3 * 360L + LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS,
                visualSteps = 3,
            ),
        )
        assertEquals(
            280L,
            LudoPawsGameplayPacingPolicy.movementAudioStepDurationMillis(
                forwardDurationMillis = 6 * 280L + LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS,
                visualSteps = 6,
            ),
        )
        assertEquals(
            210L,
            LudoPawsGameplayPacingPolicy.movementAudioStepDurationMillis(
                forwardDurationMillis = 6 * 210L + LudoPaws3DRenderCadencePolicy.FORWARD_LANDING_SETTLE_MILLIS,
                visualSteps = 6,
            ),
        )
    }

    @Test
    fun captureContactDelayMatchesTheFinalVisibleStepAtEverySpeed() {
        assertEquals(
            2_160L,
            LudoPawsGameplayPacingPolicy.captureContactDelayMillis(
                speed = GameSpeed.SLOW,
                visualSteps = 6,
            ),
        )
        assertEquals(
            1_680L,
            LudoPawsGameplayPacingPolicy.captureContactDelayMillis(
                speed = GameSpeed.NORMAL,
                visualSteps = 6,
            ),
        )
        assertEquals(
            1_260L,
            LudoPawsGameplayPacingPolicy.captureContactDelayMillis(
                speed = GameSpeed.FAST,
                visualSteps = 6,
            ),
        )
    }

    @Test
    fun extraRollGetsLongerBreathingRoom() {
        val normal =
            LudoPawsGameplayPacingPolicy
                .postMoveBreathMillis(
                    speed = GameSpeed.NORMAL,
                    extraTurn = false,
                )
        val extra =
            LudoPawsGameplayPacingPolicy
                .postMoveBreathMillis(
                    speed = GameSpeed.NORMAL,
                    extraTurn = true,
                )

        assertEquals(450L, normal)
        assertEquals(600L, extra)
        assertTrue(extra > normal)
    }
}
