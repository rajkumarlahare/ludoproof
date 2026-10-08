package com.ludoproof.game.core.audio

/**
 * Designer-facing paths for authored audio kept outside Android res/raw.
 *
 * The repository-level /audio tree is copied into the APK's assets at build
 * time. Adding a numbered variant here is optional; the first existing path
 * is used, while the existing raw-resource name remains the runtime fallback.
 */
object LudoPawsAudioCatalog {
    object Sfx {
        val MOVE_JUMP =
            listOf(
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_01.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_02.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_03.wav",
            )
    }
}
