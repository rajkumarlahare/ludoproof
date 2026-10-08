package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import com.ludoproof.game.core.audio.LudoPawsSoundPool
import com.ludoproof.game.feature.characters.domain.audio.LudoPawsProceduralVocal

/**
 * Resolves authored assets before raw-resource and procedural fallbacks.
 *
 * Vocal lookup has three tiers:
 * 1. exact situation override,
 * 2. packaged species primitive,
 * 3. generated copyright-safe emergency fallback.
 *
 * Physical SFX lookup has two authored tiers:
 * 1. repository-owned /audio asset paths,
 * 2. stable res/raw resource names,
 * followed by the generated situation-specific fallback.
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

    /**
     * Prewarms the same authored/raw SFX resolution chain used by playSfx.
     *
     * Only the first available authored asset is loaded, avoiding unnecessary
     * SoundPool memory use when future numbered variants are added.
     */
    fun preloadSfx(
        context: Context,
        rawResourceNames: List<String>,
        assetPaths: List<String> = emptyList(),
    ) {
        for (assetPath in assetPaths) {
            if (
                LudoPawsSoundPool.preloadAsset(
                    context = context,
                    assetPath = assetPath,
                )
            ) {
                return
            }
        }

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
            LudoPawsSoundPool.preload(
                context = context,
                resourceId = resourceId,
            )
        }
    }

    fun playSfx(
        context: Context,
        rawResourceNames: List<String>,
        fallback: LudoPawsProceduralAudio.Sfx,
        volume: Float,
        playbackRate: Float = 1f,
        assetPaths: List<String> = emptyList(),
    ) {
        for (assetPath in assetPaths) {
            if (
                LudoPawsSoundPool.playAsset(
                    context = context,
                    assetPath = assetPath,
                    volume = volume,
                    rate = playbackRate,
                )
            ) {
                return
            }
        }

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
