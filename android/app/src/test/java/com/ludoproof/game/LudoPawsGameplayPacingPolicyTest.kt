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
