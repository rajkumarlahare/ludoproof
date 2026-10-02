package com.ludoproof.game.feature.settings.data.local

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object GameSoundFeedback {
    fun click(
        context: Context,
    ) {
        if (
            !GameSettingsStore(
                context,
            )
                .snapshot()
                .soundEnabled
        ) {
            return
        }

        runCatching {
            ToneGenerator(
                AudioManager.STREAM_MUSIC,
                42,
            ).apply {
                startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    55,
                )
                android.os.Handler(
                    android.os.Looper.getMainLooper(),
                ).postDelayed(
                    {
                        release()
                    },
                    90L,
                )
            }
        }
    }

    fun move(
        context: Context,
    ) {
        if (
            !GameSettingsStore(
                context,
            )
                .snapshot()
                .soundEnabled
        ) {
            return
        }

        runCatching {
            ToneGenerator(
                AudioManager.STREAM_MUSIC,
                35,
            ).apply {
                startTone(
                    ToneGenerator.TONE_PROP_ACK,
                    65,
                )
                android.os.Handler(
                    android.os.Looper.getMainLooper(),
                ).postDelayed(
                    {
                        release()
                    },
                    100L,
                )
            }
        }
    }
}
