package com.ludoproof.game

import com.ludoproof.game.core.audio.LudoPawsAudioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsAudioCatalogTest {
    @Test
    fun jumpAndLandingUseSeparateNumberedFamilies() {
        assertEquals(
            listOf(
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_01.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_02.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_03.wav",
            ),
            LudoPawsAudioCatalog.Sfx.MOVE_JUMP,
        )
        assertEquals(
            listOf(
                "audio/sfx/gameplay/movement/step/lp_sfx_step_01.wav",
                "audio/sfx/gameplay/movement/step/lp_sfx_step_02.wav",
                "audio/sfx/gameplay/movement/step/lp_sfx_step_03.wav",
            ),
            LudoPawsAudioCatalog.Sfx.MOVE_STEP,
        )
        assertTrue(
            LudoPawsAudioCatalog.Sfx.MOVE_JUMP.none {
                it in LudoPawsAudioCatalog.Sfx.MOVE_STEP
            },
        )
    }

    @Test
    fun everyStarterCharacterHasItsOwnReplaceableStepFamily() {
        val families =
            listOf(
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_DOG,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_GOAT,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_DUCK,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_CAT,
            )
        assertTrue(families.all { it.size == 3 })
        assertTrue(families.flatten().all { it.endsWith(".wav") })
        assertEquals(12, families.flatten().distinct().size)
    }

    @Test
    fun sixHasAnAuthoredFamilyRatherThanOnlyARawFallback() {
        assertEquals(
            listOf(
                "audio/sfx/gameplay/six/lp_sfx_six_01.wav",
                "audio/sfx/gameplay/six/lp_sfx_six_02.wav",
                "audio/sfx/gameplay/six/lp_sfx_six_03.wav",
            ),
            LudoPawsAudioCatalog.Sfx.SIX,
        )
    }

    @Test
    fun diceSettleAndExactHomeMissHaveOwnAuthoredFamilies() {
        assertEquals(
            listOf(
                "audio/sfx/gameplay/dice/settle/lp_sfx_dice_settle_01.wav",
                "audio/sfx/gameplay/dice/settle/lp_sfx_dice_settle_02.wav",
                "audio/sfx/gameplay/dice/settle/lp_sfx_dice_settle_03.wav",
            ),
            LudoPawsAudioCatalog.Sfx.DICE_SETTLE,
        )
        assertEquals(
            listOf(
                "audio/sfx/gameplay/exact_home_miss/lp_sfx_exact_home_miss_01.wav",
                "audio/sfx/gameplay/exact_home_miss/lp_sfx_exact_home_miss_02.wav",
                "audio/sfx/gameplay/exact_home_miss/lp_sfx_exact_home_miss_03.wav",
            ),
            LudoPawsAudioCatalog.Sfx.EXACT_HOME_MISS,
        )
    }

    @Test
    fun allCatalogPathsUseSupportedUncompressedAudioFormats() {
        val families =
            listOf(
                LudoPawsAudioCatalog.Sfx.MOVE_STEP,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_DOG,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_GOAT,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_DUCK,
                LudoPawsAudioCatalog.Sfx.MOVE_STEP_CAT,
                LudoPawsAudioCatalog.Sfx.MOVE_JUMP,
                LudoPawsAudioCatalog.Sfx.UI_CLICK,
                LudoPawsAudioCatalog.Sfx.DICE_ROLL,
                LudoPawsAudioCatalog.Sfx.DICE_SETTLE,
                LudoPawsAudioCatalog.Sfx.SIX,
                LudoPawsAudioCatalog.Sfx.YARD_EXIT,
                LudoPawsAudioCatalog.Sfx.CAPTURE,
                LudoPawsAudioCatalog.Sfx.SAFE_RELIEF,
                LudoPawsAudioCatalog.Sfx.HOME_LANE,
                LudoPawsAudioCatalog.Sfx.HOME,
                LudoPawsAudioCatalog.Sfx.FAIL,
                LudoPawsAudioCatalog.Sfx.EXACT_HOME_MISS,
                LudoPawsAudioCatalog.Sfx.THIRD_SIX,
                LudoPawsAudioCatalog.Sfx.VICTORY,
                LudoPawsAudioCatalog.Sfx.DEFEAT,
            ).flatten()
        assertTrue(families.all { it.endsWith(".wav") || it.endsWith(".ogg") })
        assertEquals(families.size, families.distinct().size)
    }
}
