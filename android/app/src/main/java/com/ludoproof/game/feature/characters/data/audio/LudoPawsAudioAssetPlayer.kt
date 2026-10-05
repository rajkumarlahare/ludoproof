package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import com.ludoproof.game.core.audio.LudoPawsSoundPool
import com.ludoproof.game.feature.characters.domain.audio.LudoPawsProceduralVocal

/**
 * Resolves stable raw-resource names before falling back to procedural audio.
 *
 * This is the only Android resource lookup boundary for character/game sounds.
 * A designer can therefore add or replace an .ogg/.wav under res/raw using the
 * documented name without touching Kotlin gameplay or reaction code.
 */
internal object LudoPawsAudioAssetPlayer {
    fun playVocal(
        context: Context,
        rawResourceNames: List<String>,
        variantIndex: Int,
        fallback: LudoPawsProceduralVocal,
        volume: Float,
        playbackRate: Float,
    ) {
        val orderedNames =
            rotate(
                values = rawResourceNames,
                startIndex = variantIndex,
            )
        val resourceId =
            orderedNames
                .asSequence()
                .map {
                    rawResourceId(
                        context = context,
                        resourceName = it,
                    )
                }
                .firstOrNull { it != 0 }

        if (resourceId != null) {
            LudoPawsSoundPool.play(
                context = context,
                resourceId = resourceId,
                volume = volume,
                rate = playbackRate,
            )
            return
        }

        LudoPawsProceduralAudio.playVocal(
            preset = fallback,
            volume = volume,
            playbackRate = playbackRate,
        )
    }

    fun playSfx(
        context: Context,
        rawResourceNames: List<String>,
        fallback: LudoPawsProceduralAudio.Sfx,
        volume: Float,
        playbackRate: Float = 1f,
    ) {
        val resourceId =
            rawResourceNames
                .asSequence()
                .map {
                    rawResourceId(
                        context = context,
                        resourceName = it,
                    )
                }
                .firstOrNull { it != 0 }

        if (resourceId != null) {
            LudoPawsSoundPool.play(
                context = context,
                resourceId = resourceId,
                volume = volume,
                rate = playbackRate,
            )
            return
        }

        LudoPawsProceduralAudio.playSfx(
            preset = fallback,
            volume = volume,
            playbackRate = playbackRate,
        )
    }

    @Suppress("DEPRECATION")
    private fun rawResourceId(
        context: Context,
        resourceName: String,
    ): Int =
        context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName,
        )

    private fun <T> rotate(
        values: List<T>,
        startIndex: Int,
    ): List<T> {
        if (values.size <= 1) return values
        val safe = startIndex.mod(values.size)
        return values.drop(safe) + values.take(safe)
    }
}
