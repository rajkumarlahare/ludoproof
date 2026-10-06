package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import com.ludoproof.game.core.audio.LudoPawsSoundPool
import com.ludoproof.game.feature.characters.domain.audio.LudoPawsProceduralVocal

/**
 * Resolves stable raw-resource names before falling back to procedural audio.
 *
 * Vocal lookup has three tiers:
 * 1. exact situation override (`lp_vocal_dog_capture_01`),
 * 2. packaged species primitive (`lp_vocal_dog_yip`),
 * 3. generated copyright-safe emergency fallback.
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
        val packagedFallback =
            packagedFallbackResourceName(fallback)
        val resourceId =
            (orderedNames + packagedFallback)
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

    internal fun packagedFallbackResourceName(
        preset: LudoPawsProceduralVocal,
    ): String =
        when (preset) {
            LudoPawsProceduralVocal.DOG_YIP -> "lp_vocal_dog_yip"
            LudoPawsProceduralVocal.DOG_WHINE -> "lp_vocal_dog_whine"
            LudoPawsProceduralVocal.DOG_RUFF -> "lp_vocal_dog_ruff"
            LudoPawsProceduralVocal.GOAT_BLEAT -> "lp_vocal_goat_bleat"
            LudoPawsProceduralVocal.GOAT_SOFT_BLEAT -> "lp_vocal_goat_soft_bleat"
            LudoPawsProceduralVocal.DUCK_QUACK -> "lp_vocal_duck_quack"
            LudoPawsProceduralVocal.DUCK_SOFT_QUACK -> "lp_vocal_duck_soft_quack"
            LudoPawsProceduralVocal.CAT_CHIRP -> "lp_vocal_cat_chirp"
            LudoPawsProceduralVocal.CAT_MEW -> "lp_vocal_cat_mew"
            LudoPawsProceduralVocal.CAT_PURR -> "lp_vocal_cat_purr"
            LudoPawsProceduralVocal.CAT_HUFF -> "lp_vocal_cat_huff"
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
