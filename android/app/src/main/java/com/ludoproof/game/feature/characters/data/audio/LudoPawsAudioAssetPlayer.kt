package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import android.content.res.AssetManager
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
    private val authoredAssetAvailability = mutableMapOf<String, Boolean>()
    private val resolvedAssetsByExpectedPath = mutableMapOf<String, List<String>>()
    private val nextSfxVariantByFamily = mutableMapOf<String, Int>()

    fun playVocal(
        context: Context,
        rawResourceNames: List<String>,
        variantIndex: Int,
        fallback: LudoPawsProceduralVocal,
        volume: Float,
        playbackRate: Float,
        priority: Int = 5,
    ) {
        val orderedNames =
            rotate(
                values = rawResourceNames,
                startIndex = variantIndex,
            )
        val vocalAssetPaths =
            orderedNames.flatMap { resourceName ->
                val characterId =
                    resourceName
                        .removePrefix("lp_vocal_")
                        .substringBefore('_')
                if (characterId !in SUPPORTED_VOCAL_CHARACTERS) {
                    emptyList()
                } else {
                    listOf(
                        "audio/sfx/voices/$characterId/$resourceName.wav",
                        "audio/sfx/voices/$characterId/$resourceName.ogg",
                    )
                }
            }
        val authoredVocalPath =
            availableAssetPaths(context, vocalAssetPaths).firstOrNull()
        if (
            authoredVocalPath != null &&
            LudoPawsSoundPool.playAsset(
                context = context,
                assetPath = authoredVocalPath,
                volume = volume,
                rate = playbackRate,
                priority = priority,
            )
        ) {
            return
        }

        val packagedFallback = packagedFallbackResourceName(fallback)
        val resourceId =
            (orderedNames + packagedFallback)
                .asSequence()
                .map { rawResourceId(context, it) }
                .firstOrNull { it != 0 }

        if (resourceId != null) {
            LudoPawsSoundPool.play(
                context = context,
                resourceId = resourceId,
                volume = volume,
                rate = playbackRate,
                priority = priority,
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
     * All available numbered takes are loaded before gameplay can request them.
     */
    @Synchronized
    fun preloadSfx(
        context: Context,
        rawResourceNames: List<String>,
        assetPaths: List<String> = emptyList(),
    ) {
        var loadedAuthoredAsset = false
        for (assetPath in availableAssetPaths(context, assetPaths)) {
            if (LudoPawsSoundPool.preloadAsset(context, assetPath)) {
                loadedAuthoredAsset = true
            }
        }
        if (loadedAuthoredAsset) return

        val resourceId =
            rawResourceNames
                .asSequence()
                .map { rawResourceId(context, it) }
                .firstOrNull { it != 0 }

        if (resourceId != null) {
            LudoPawsSoundPool.preload(context, resourceId)
        }
    }

    @Synchronized
    fun playAuthoredSfx(
        context: Context,
        assetPaths: List<String>,
        volume: Float,
        playbackRate: Float = 1f,
        priority: Int = 1,
    ): Boolean {
        val available = availableAssetPaths(context, assetPaths)
        if (available.isEmpty()) return false

        val familyKey = assetFamilyKey(assetPaths)
        val startIndex = (nextSfxVariantByFamily[familyKey] ?: 0).mod(available.size)
        for (offset in available.indices) {
            val index = (startIndex + offset) % available.size
            if (
                LudoPawsSoundPool.playAsset(
                    context = context,
                    assetPath = available[index],
                    volume = volume,
                    rate = playbackRate,
                    priority = priority,
                )
            ) {
                nextSfxVariantByFamily[familyKey] = (index + 1) % available.size
                return true
            }
        }
        return false
    }

    fun playSfx(
        context: Context,
        rawResourceNames: List<String>,
        fallback: LudoPawsProceduralAudio.Sfx,
        volume: Float,
        playbackRate: Float = 1f,
        priority: Int = 1,
        assetPaths: List<String> = emptyList(),
    ) {
        if (
            playAuthoredSfx(
                context = context,
                assetPaths = assetPaths,
                volume = volume,
                playbackRate = playbackRate,
                priority = priority,
            )
        ) {
            return
        }

        val resourceId =
            rawResourceNames
                .asSequence()
                .map { rawResourceId(context, it) }
                .firstOrNull { it != 0 }

        if (resourceId != null) {
            LudoPawsSoundPool.play(
                context = context,
                resourceId = resourceId,
                volume = volume,
                rate = playbackRate,
                priority = priority,
            )
            return
        }

        LudoPawsProceduralAudio.playSfx(
            preset = fallback,
            volume = volume,
            playbackRate = playbackRate,
        )
    }

    /**
     * Resolves canonical asset names and Windows duplicate names such as
     * `lp_sfx_capture_01 (1).wav`. Asset discovery is cached by expected path,
     * avoiding directory scans and regex matching on each movement tick.
     */
    @Synchronized
    private fun availableAssetPaths(
        context: Context,
        assetPaths: List<String>,
    ): List<String> {
        val assets = context.applicationContext.assets
        return assetPaths
            .distinct()
            .flatMap { expectedPath ->
                resolvedAssetsByExpectedPath[expectedPath]
                    ?: resolveAssetPaths(assets, expectedPath).also {
                        resolvedAssetsByExpectedPath[expectedPath] = it
                    }
            }
            .distinct()
    }

    private fun resolveAssetPaths(
        assets: AssetManager,
        expectedPath: String,
    ): List<String> {
        val directory = expectedPath.substringBeforeLast('/', "")
        val fileName = expectedPath.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.isBlank()) return emptyList()

        val stem = fileName.removeSuffix(".$extension")
        val filenamePattern =
            Regex(
                "^" + Regex.escape(stem) +
                    "(?: \\(\\d+\\))?\\." + Regex.escape(extension) + "$",
                RegexOption.IGNORE_CASE,
            )
        return runCatching {
            assets.list(directory)
                .orEmpty()
                .filter(filenamePattern::matches)
                .sortedWith(compareBy<String>({ duplicateFilenameIndex(it) }, { it }))
                .map { actualFileName ->
                    if (directory.isBlank()) actualFileName else "$directory/$actualFileName"
                }
                .filter { actualPath ->
                    authoredAssetAvailability.getOrPut(actualPath) {
                        runCatching { assets.openFd(actualPath).use { } }.isSuccess
                    }
                }
        }.getOrDefault(emptyList())
    }

    private fun duplicateFilenameIndex(fileName: String): Int =
        Regex(""" \\((\\d+)\\)(?=\\.[^.]+$)""")
            .find(fileName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0

    private fun assetFamilyKey(assetPaths: List<String>): String =
        assetPaths.firstOrNull()
            .orEmpty()
            .replace(Regex("""_\\d{2}\\.(wav|ogg)$"""), "_variant.$1")

    private val SUPPORTED_VOCAL_CHARACTERS =
        setOf("dog", "goat", "duck", "cat")

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

    @Synchronized
    private fun <T> rotate(
        values: List<T>,
        startIndex: Int,
    ): List<T> {
        if (values.size <= 1) return values
        val safe = startIndex.mod(values.size)
        return values.drop(safe) + values.take(safe)
    }
}
