package com.ludoproof.game.core.audio

/**
 * Designer-facing paths for authored audio kept outside Android res/raw.
 *
 * The repository-level /audio tree is copied into APK assets at build time.
 * Each family lists up to three numbered takes. Runtime selection rotates
 * deterministically through the takes that actually exist, then falls back to
 * raw resources and finally to the generated sound.
 */
object LudoPawsAudioCatalog {
    object Sfx {
        val MOVE_STEP =
            listOf(
                "audio/sfx/gameplay/movement/step/lp_sfx_step_01.wav",
                "audio/sfx/gameplay/movement/step/lp_sfx_step_02.wav",
                "audio/sfx/gameplay/movement/step/lp_sfx_step_03.wav",
            )

        val MOVE_STEP_DOG =
            listOf(
                "audio/sfx/gameplay/movement/step/dog/lp_sfx_step_dog_01.wav",
                "audio/sfx/gameplay/movement/step/dog/lp_sfx_step_dog_02.wav",
                "audio/sfx/gameplay/movement/step/dog/lp_sfx_step_dog_03.wav",
            )

        val MOVE_STEP_GOAT =
            listOf(
                "audio/sfx/gameplay/movement/step/goat/lp_sfx_step_goat_01.wav",
                "audio/sfx/gameplay/movement/step/goat/lp_sfx_step_goat_02.wav",
                "audio/sfx/gameplay/movement/step/goat/lp_sfx_step_goat_03.wav",
            )

        val MOVE_STEP_DUCK =
            listOf(
                "audio/sfx/gameplay/movement/step/duck/lp_sfx_step_duck_01.wav",
                "audio/sfx/gameplay/movement/step/duck/lp_sfx_step_duck_02.wav",
                "audio/sfx/gameplay/movement/step/duck/lp_sfx_step_duck_03.wav",
                // The surviving Duck take was moved to the shared step directory.
                // Keep it assigned to Duck; do not expose this clip to other species.
                "audio/sfx/gameplay/movement/step/lp_sfx_step_duck_01.wav",
            )

        val MOVE_STEP_CAT =
            listOf(
                "audio/sfx/gameplay/movement/step/cat/lp_sfx_step_cat_01.wav",
                "audio/sfx/gameplay/movement/step/cat/lp_sfx_step_cat_02.wav",
                "audio/sfx/gameplay/movement/step/cat/lp_sfx_step_cat_03.wav",
            )

        val MOVE_JUMP =
            listOf(
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_01.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_02.wav",
                "audio/sfx/gameplay/movement/jump/lp_sfx_jump_03.wav",
            )

        val UI_CLICK =
            listOf(
                "audio/sfx/ui/click/lp_sfx_ui_click_01.wav",
                "audio/sfx/ui/click/lp_sfx_ui_click_02.wav",
                "audio/sfx/ui/click/lp_sfx_ui_click_03.wav",
            )

        val DICE_ROLL =
            listOf(
                "audio/sfx/gameplay/dice/lp_sfx_dice_roll_01.wav",
                "audio/sfx/gameplay/dice/lp_sfx_dice_roll_02.wav",
                "audio/sfx/gameplay/dice/lp_sfx_dice_roll_03.wav",
            )

        val DICE_SETTLE =
            listOf(
                "audio/sfx/gameplay/dice/settle/lp_sfx_dice_settle_01.wav",
                "audio/sfx/gameplay/dice/settle/lp_sfx_dice_settle_02.wav",
                "audio/sfx/gameplay/dice/settle/lp_sfx_dice_settle_03.wav",
            )

        val SIX =
            listOf(
                "audio/sfx/gameplay/six/lp_sfx_six_01.wav",
                "audio/sfx/gameplay/six/lp_sfx_six_02.wav",
                "audio/sfx/gameplay/six/lp_sfx_six_03.wav",
            )

        val YARD_EXIT =
            listOf(
                "audio/sfx/gameplay/yard_exit/lp_sfx_yard_exit_01.wav",
                "audio/sfx/gameplay/yard_exit/lp_sfx_yard_exit_02.wav",
                "audio/sfx/gameplay/yard_exit/lp_sfx_yard_exit_03.wav",
            )

        val CAPTURE =
            listOf(
                "audio/sfx/gameplay/capture/lp_sfx_capture_01.wav",
                "audio/sfx/gameplay/capture/lp_sfx_capture_02.wav",
                "audio/sfx/gameplay/capture/lp_sfx_capture_03.wav",
            )

        val SAFE_RELIEF =
            listOf(
                "audio/sfx/gameplay/safe/lp_sfx_safe_relief_01.wav",
                "audio/sfx/gameplay/safe/lp_sfx_safe_relief_02.wav",
                "audio/sfx/gameplay/safe/lp_sfx_safe_relief_03.wav",
            )

        val HOME_LANE =
            listOf(
                "audio/sfx/gameplay/home_lane/lp_sfx_home_lane_01.wav",
                "audio/sfx/gameplay/home_lane/lp_sfx_home_lane_02.wav",
                "audio/sfx/gameplay/home_lane/lp_sfx_home_lane_03.wav",
            )

        val HOME =
            listOf(
                "audio/sfx/gameplay/home/lp_sfx_home_01.wav",
                "audio/sfx/gameplay/home/lp_sfx_home_02.wav",
                "audio/sfx/gameplay/home/lp_sfx_home_03.wav",
            )

        val EXACT_HOME_MISS =
            listOf(
                "audio/sfx/gameplay/exact_home_miss/lp_sfx_exact_home_miss_01.wav",
                "audio/sfx/gameplay/exact_home_miss/lp_sfx_exact_home_miss_02.wav",
                "audio/sfx/gameplay/exact_home_miss/lp_sfx_exact_home_miss_03.wav",
            )

        val FAIL =
            listOf(
                "audio/sfx/gameplay/fail/lp_sfx_fail_01.wav",
                "audio/sfx/gameplay/fail/lp_sfx_fail_02.wav",
                "audio/sfx/gameplay/fail/lp_sfx_fail_03.wav",
            )

        val THIRD_SIX =
            listOf(
                "audio/sfx/gameplay/third_six/lp_sfx_third_six_01.wav",
                "audio/sfx/gameplay/third_six/lp_sfx_third_six_02.wav",
                "audio/sfx/gameplay/third_six/lp_sfx_third_six_03.wav",
            )

        val VICTORY =
            listOf(
                "audio/sfx/gameplay/victory/lp_sfx_victory_01.wav",
                "audio/sfx/gameplay/victory/lp_sfx_victory_02.wav",
                "audio/sfx/gameplay/victory/lp_sfx_victory_03.wav",
            )

        val DEFEAT =
            listOf(
                "audio/sfx/gameplay/defeat/lp_sfx_defeat_01.wav",
                "audio/sfx/gameplay/defeat/lp_sfx_defeat_02.wav",
                "audio/sfx/gameplay/defeat/lp_sfx_defeat_03.wav",
            )
    }
}
