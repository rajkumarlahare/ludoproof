package com.ludoproof.game.feature.settings.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSettingsContractTest {
    @Test
    fun gameSpeedOffersExplicitFastNormalSlowChoices() {
        assertEquals(
            listOf("FAST", "NORMAL", "SLOW"),
            listOf(
                GameSpeed.FAST.label,
                GameSpeed.NORMAL.label,
                GameSpeed.SLOW.label,
            ),
        )
    }

    @Test
    fun fasterSpeedUsesShorterPresentationTiming() {
        assertEquals(
            180L,
            GameSpeed.FAST.moveStepMs,
        )
        assertEquals(
            240L,
            GameSpeed.NORMAL.moveStepMs,
        )
        assertEquals(
            320L,
            GameSpeed.SLOW.moveStepMs,
        )
        assertTrue(
            GameSpeed.FAST.moveStepMs <
                GameSpeed.NORMAL.moveStepMs,
        )
        assertTrue(
            GameSpeed.NORMAL.moveStepMs <
                GameSpeed.SLOW.moveStepMs,
        )
        assertTrue(
            GameSpeed.FAST.rollDelayMs <
                GameSpeed.NORMAL.rollDelayMs,
        )
        assertTrue(
            GameSpeed.NORMAL.rollDelayMs <
                GameSpeed.SLOW.rollDelayMs,
        )
        assertTrue(
            GameSpeed.FAST.cpuThinkMs <
                GameSpeed.NORMAL.cpuThinkMs,
        )
        assertTrue(
            GameSpeed.NORMAL.cpuThinkMs <
                GameSpeed.SLOW.cpuThinkMs,
        )
    }

    @Test
    fun defaultSettingsRemainCompatible() {
        val defaults = GameSettings()
        assertTrue(defaults.musicEnabled)
        assertTrue(defaults.soundEnabled)
        assertTrue(defaults.quickChatEnabled)
        assertEquals(
            GameSpeed.NORMAL,
            defaults.gameSpeed,
        )
    }
}
