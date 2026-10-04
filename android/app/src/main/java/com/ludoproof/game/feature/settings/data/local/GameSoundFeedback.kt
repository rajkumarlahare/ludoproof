package com.ludoproof.game.feature.settings.data.local

import android.content.Context
import com.ludoproof.game.R
import com.ludoproof.game.core.audio.LudoPawsSoundPool
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction

/** Short game/UI sound facade. Long-form music and animal voices are separate. */
object GameSoundFeedback {
    fun click(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_click,
            volume = .52f,
        )
    }

    fun move(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_move,
            volume = .44f,
        )
    }

    fun roll(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_roll,
            volume = .50f,
        )
    }

    fun six(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_safe,
            volume = .45f,
            rate = 1.18f,
        )
    }

    fun capture(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_capture,
            volume = .62f,
        )
    }

    fun safe(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_safe,
            volume = .48f,
        )
    }

    fun home(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_home,
            volume = .56f,
        )
    }

    fun victory(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_victory,
            volume = .62f,
        )
    }

    fun defeat(
        context: Context,
    ) {
        play(
            context = context,
            resourceId = R.raw.lp_sfx_defeat,
            volume = .46f,
        )
    }

    /** Plays at most one game SFX for a reaction batch; animal voice is separate. */
    fun reaction(
        context: Context,
        reactions: List<LudoPawsReaction>,
    ) {
        val cue =
            reactions
                .maxByOrNull(
                    LudoPawsReaction::priority,
                )
                ?.voiceCue
                ?: return
        when (cue) {
            VoiceCue.CAPTURE,
            VoiceCue.CAPTURED,
            -> capture(context)

            VoiceCue.SAFE -> safe(context)
            VoiceCue.HOME -> home(context)
            VoiceCue.VICTORY -> victory(context)
            VoiceCue.DEFEAT -> defeat(context)
            VoiceCue.SIX -> six(context)

            VoiceCue.THIRD_SIX,
            VoiceCue.FRUSTRATED,
            VoiceCue.IDLE,
            VoiceCue.NERVOUS,
            -> Unit
        }
    }

    private fun play(
        context: Context,
        resourceId: Int,
        volume: Float,
        rate: Float = 1f,
    ) {
        if (
            !GameSettingsStore(context)
                .snapshot()
                .soundEnabled
        ) {
            return
        }
        LudoPawsSoundPool.play(
            context = context,
            resourceId = resourceId,
            volume = volume,
            rate = rate,
        )
    }
}
