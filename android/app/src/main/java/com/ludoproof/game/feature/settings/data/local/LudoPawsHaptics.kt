package com.ludoproof.game.feature.settings.data.local

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction

/** Small presentation-only haptics layer used by character reactions. */
object LudoPawsHaptics {
    fun reaction(
        context: Context,
        reactions: List<LudoPawsReaction>,
    ) {
        if (
            reactions.isEmpty() ||
            !GameSettingsStore(context)
                .snapshot()
                .hapticsEnabled
        ) {
            return
        }

        val cue =
            reactions
                .maxByOrNull(
                    LudoPawsReaction::priority,
                )
                ?.voiceCue
                ?: return
        val effect =
            when (cue) {
                VoiceCue.CAPTURE,
                VoiceCue.CAPTURED,
                VoiceCue.THIRD_SIX,
                -> VibrationEffect.createOneShot(
                    72L,
                    150,
                )

                VoiceCue.HOME,
                VoiceCue.VICTORY,
                -> VibrationEffect.createWaveform(
                    longArrayOf(
                        0L,
                        38L,
                        44L,
                        64L,
                    ),
                    intArrayOf(
                        0,
                        90,
                        0,
                        150,
                    ),
                    -1,
                )

                VoiceCue.SIX,
                VoiceCue.SAFE,
                -> VibrationEffect.createOneShot(
                    34L,
                    85,
                )

                VoiceCue.DEFEAT,
                VoiceCue.FRUSTRATED,
                -> VibrationEffect.createOneShot(
                    42L,
                    70,
                )

                VoiceCue.IDLE,
                VoiceCue.NERVOUS,
                -> return
            }

        runCatching {
            vibrator(context)
                ?.takeIf(
                    Vibrator::hasVibrator,
                )
                ?.vibrate(effect)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrator(
        context: Context,
    ): Vibrator? =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            context.getSystemService(
                VibratorManager::class.java,
            )
                ?.defaultVibrator
        } else {
            context.getSystemService(
                Context.VIBRATOR_SERVICE,
            ) as? Vibrator
        }
}
