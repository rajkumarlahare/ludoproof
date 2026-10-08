package com.ludoproof.game.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes

/**
 * Shared low-latency clip renderer for short Ludo Paws game/animal sounds.
 *
 * Samples are loaded lazily and a small first-play queue prevents a freshly
 * loaded clip from being silently lost. This object is presentation-only and
 * owns no gameplay or proof state.
 *
 * Authored audio can live in the repository-level /audio tree. Gradle mirrors
 * that tree into APK assets and this class loads those clips through the same
 * bounded SoundPool used by res/raw fallbacks.
 */
object LudoPawsSoundPool {
    private data class Sample(
        val soundId: Int,
        var ready: Boolean = false,
    )

    private data class PendingPlay(
        val volume: Float,
        val rate: Float,
    )

    private var pool: SoundPool? = null
    private val samplesByKey = mutableMapOf<String, Sample>()
    private val sourceKeyBySoundId = mutableMapOf<Int, String>()
    private val readySoundIds = mutableSetOf<Int>()
    private val pendingByKey = mutableMapOf<String, MutableList<PendingPlay>>()

    @Synchronized
    fun play(
        context: Context,
        @RawRes resourceId: Int,
        volume: Float = 1f,
        rate: Float = 1f,
    ): Boolean =
        playSource(
            context = context,
            key = "res:$resourceId",
            loader = { soundPool ->
                soundPool.load(
                    context.applicationContext,
                    resourceId,
                    1,
                )
            },
            volume = volume,
            rate = rate,
        )

    @Synchronized
    fun playAsset(
        context: Context,
        assetPath: String,
        volume: Float = 1f,
        rate: Float = 1f,
    ): Boolean =
        playSource(
            context = context,
            key = "asset:$assetPath",
            loader = { soundPool ->
                context.applicationContext.assets
                    .openFd(assetPath)
                    .use { descriptor ->
                        soundPool.load(
                            descriptor,
                            1,
                        )
                    }
            },
            volume = volume,
            rate = rate,
        )

    /**
     * Starts loading an authored asset without scheduling playback.
     *
     * Calling this before the first interactive move removes the one-time
     * SoundPool load latency from the movement audio clock. A successful return
     * means the clip was already cached or the asynchronous load was accepted.
     */
    @Synchronized
    fun preloadAsset(
        context: Context,
        assetPath: String,
    ): Boolean =
        preloadSource(
            context = context,
            key = "asset:$assetPath",
            loader = { soundPool ->
                context.applicationContext.assets
                    .openFd(assetPath)
                    .use { descriptor ->
                        soundPool.load(
                            descriptor,
                            1,
                        )
                    }
            },
        )

    /**
     * Starts loading a packaged raw-resource clip without scheduling playback.
     */
    @Synchronized
    fun preload(
        context: Context,
        @RawRes resourceId: Int,
    ): Boolean =
        preloadSource(
            context = context,
            key = "res:$resourceId",
            loader = { soundPool ->
                soundPool.load(
                    context.applicationContext,
                    resourceId,
                    1,
                )
            },
        )

    @Synchronized
    fun release() {
        runCatching {
            pool?.release()
        }
        pool = null
        samplesByKey.clear()
        sourceKeyBySoundId.clear()
        readySoundIds.clear()
        pendingByKey.clear()
    }

    private fun preloadSource(
        context: Context,
        key: String,
        loader: (SoundPool) -> Int,
    ): Boolean {
        val soundPool =
            ensurePool()
                ?: return false

        if (samplesByKey.containsKey(key)) {
            return true
        }

        return load(
            soundPool = soundPool,
            sourceKey = key,
            loader = loader,
        ) != null
    }

    private fun playSource(
        context: Context,
        key: String,
        loader: (SoundPool) -> Int,
        volume: Float,
        rate: Float,
    ): Boolean {
        val soundPool =
            ensurePool()
                ?: return false
        val sample =
            samplesByKey[key]
                ?: load(
                    soundPool = soundPool,
                    sourceKey = key,
                    loader = loader,
                ) ?: return false

        val safeVolume =
            volume.coerceIn(
                0f,
                1f,
            )
        val safeRate =
            rate.coerceIn(
                MIN_RATE,
                MAX_RATE,
            )

        if (
            sample.ready ||
            sample.soundId in readySoundIds
        ) {
            sample.ready = true
            playNow(
                soundPool = soundPool,
                soundId = sample.soundId,
                volume = safeVolume,
                rate = safeRate,
            )
        } else {
            pendingByKey
                .getOrPut(key) {
                    mutableListOf()
                }
                .apply {
                    if (size < MAX_PENDING_PER_SAMPLE) {
                        add(
                            PendingPlay(
                                volume = safeVolume,
                                rate = safeRate,
                            ),
                        )
                    }
                }
        }
        return true
    }

    private fun ensurePool(): SoundPool? {
        pool?.let {
            return it
        }

        val created =
            runCatching {
                SoundPool
                    .Builder()
                    .setMaxStreams(MAX_STREAMS)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(
                                AudioAttributes.USAGE_GAME,
                            )
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION,
                            )
                            .build(),
                    )
                    .build()
            }
                .getOrNull()
                ?: return null

        created.setOnLoadCompleteListener { soundPool, soundId, status ->
            synchronized(this) {
                if (
                    soundPool !== pool ||
                    status != 0
                ) {
                    return@synchronized
                }
                readySoundIds += soundId
                val sourceKey =
                    sourceKeyBySoundId[soundId]
                        ?: return@synchronized
                val sample =
                    samplesByKey[sourceKey]
                        ?: return@synchronized
                sample.ready = true
                flushPending(
                    soundPool = soundPool,
                    sourceKey = sourceKey,
                    sample = sample,
                )
            }
        }

        pool = created
        return created
    }

    private fun load(
        soundPool: SoundPool,
        sourceKey: String,
        loader: (SoundPool) -> Int,
    ): Sample? {
        val soundId =
            runCatching {
                loader(soundPool)
            }
                .getOrNull()
                ?.takeIf {
                    it > 0
                }
                ?: return null

        val sample =
            Sample(
                soundId = soundId,
                ready = soundId in readySoundIds,
            )
        samplesByKey[sourceKey] = sample
        sourceKeyBySoundId[soundId] = sourceKey
        if (sample.ready) {
            flushPending(
                soundPool = soundPool,
                sourceKey = sourceKey,
                sample = sample,
            )
        }
        return sample
    }

    private fun flushPending(
        soundPool: SoundPool,
        sourceKey: String,
        sample: Sample,
    ) {
        val pending =
            pendingByKey
                .remove(sourceKey)
                .orEmpty()
        pending.forEach { play ->
            playNow(
                soundPool = soundPool,
                soundId = sample.soundId,
                volume = play.volume,
                rate = play.rate,
            )
        }
    }

    private fun playNow(
        soundPool: SoundPool,
        soundId: Int,
        volume: Float,
        rate: Float,
    ) {
        runCatching {
            soundPool.play(
                soundId,
                volume,
                volume,
                1,
                0,
                rate,
            )
        }
    }

    private const val MAX_STREAMS = 8
    private const val MAX_PENDING_PER_SAMPLE = 3
    private const val MIN_RATE = .65f
    private const val MAX_RATE = 1.45f
}
