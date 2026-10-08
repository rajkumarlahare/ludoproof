package com.ludoproof.game

import com.ludoproof.game.core.audio.LudoPawsAudioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsAudioCatalogTest {
    @Test
    fun jumpAssetsUseStableNumberedPaths() {
        assertEquals(
            listOf(
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_01.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_02.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_03.wav",
            ),
            LudoPawsAudioCatalog.Sfx.MOVE_JUMP,
        )
        assertTrue(
            LudoPawsAudioCatalog.Sfx.MOVE_JUMP
                .all { it.endsWith(".wav") },
        )
    }
}
