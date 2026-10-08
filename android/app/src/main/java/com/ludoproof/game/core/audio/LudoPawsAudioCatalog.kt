package com.ludoproof.game.core.audio

/**
 * Designer-facing paths for authored audio kept outside Android res/raw.
 *
 * The repository-level /audio tree is copied into the APK's assets at build
 * time. Each family lists up to three numbered variants; the first existing
 * file is used, while the existing raw-resource name remains the runtime
 * fallback.
 */
object LudoPawsAudioCatalog {
    object Sfx {
        val MOVE_JUMP =
            family(
                "audio/sfx/gameplay/movement/jump",
                "lp_sfx_jump",
            )

        val UI_CLICK =
            family(
                "audio/sfx/ui/click",
                "lp_sfx_ui_click",
            )

        val DICE_ROLL =
            family(
                "audio/sfx/gameplay/dice",
                "lp_sfx_dice_roll",
            )

        val YARD_EXIT =
            family(
                "audio/sfx/gameplay/yard_exit",
                "lp_sfx_yard_exit",
            )

        val CAPTURE =
            family(
                "audio/sfx/gameplay/capture",
                "lp_sfx_capture",
            )

        val SAFE_RELIEF =
            family(
                "audio/sfx/gameplay/safe",
                "lp_sfx_safe_relief",
            )

        val HOME_LANE =
            family(
                "audio/sfx/gameplay/home_lane",
                "lp_sfx_home_lane",
            )

        val HOME =
            family(
                "audio/sfx/gameplay/home",
                "lp_sfx_home",
            )

        val FAIL =
            family(
                "audio/sfx/gameplay/fail",
                "lp_sfx_fail",
            )

        val THIRD_SIX =
            family(
                "audio/sfx/gameplay/third_six",
                "lp_sfx_third_six",
            )

        val VICTORY =
            family(
                "audio/sfx/gameplay/victory",
                "lp_sfx_victory",
            )

        val DEFEAT =
            family(
                "audio/sfx/gameplay/defeat",
                "lp_sfx_defeat",
            )

        private fun family(
            directory: String,
            basename: String,
        ): List<String> =
            listOf(
                "$directory/$basename" + "_01.wav",
                "$directory/$basename" + "_02.wav",
                "$directory/$basename" + "_03.wav",
            )
    }
}
