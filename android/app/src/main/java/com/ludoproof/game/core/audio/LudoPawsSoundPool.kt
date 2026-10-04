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
    private var appContext: Context? = null
    private val samplesByResource = mutableMapOf<Int, Sample>()
    private val resourceBySoundId = mutableMapOf<Int, Int>()
    private val readySoundIds = mutableSetOf<Int>()
    private val pendingByResource = mutableMapOf<Int, MutableList<PendingPlay>>()

    @Synchronized
    fun play(
        context: Context,
        @RawRes resourceId: Int,
        volume: Float = 1f,
        rate: Float = 1f,
    ): Boolean {
        val soundPool =
            ensurePool(context.applicationContext)
                ?: return false
        val sample =
            samplesByResource[resourceId]
                ?: load(
                    soundPool = soundPool,
                    context = context.applicationContext,
                    resourceId = resourceId,
                )
                ?: return false

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
            pendingByResource
                .getOrPut(resourceId) {
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

    @Synchronized
    fun release() {
        runCatching {
            pool?.release()
        }
        pool = null
        appContext = null
        samplesByResource.clear()
        resourceBySoundId.clear()
        readySoundIds.clear()
        pendingByResource.clear()
    }

    private fun ensurePool(
        context: Context,
    ): SoundPool? {
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

        created.setOnLoadCompleteListener {
                soundPool,
                soundId,
                status,
            ->
            synchronized(this) {
                if (
                    soundPool !== pool ||
                    status != 0
                ) {
                    return@synchronized
                }
                readySoundIds += soundId
                val resourceId =
                    resourceBySoundId[soundId]
                        ?: return@synchronized
                val sample =
                    samplesByResource[resourceId]
                        ?: return@synchronized
                sample.ready = true
                flushPending(
                    soundPool = soundPool,
                    resourceId = resourceId,
                    sample = sample,
                )
            }
        }

        appContext = context
        pool = created
        return created
    }

    private fun load(
        soundPool: SoundPool,
        context: Context,
        @RawRes resourceId: Int,
    ): Sample? {
        val soundId =
            runCatching {
                soundPool.load(
                    context,
                    resourceId,
                    1,
                )
            }
                .getOrNull()
                ?.takeIf {
                    it > 0
                }
                ?: return null

        val sample =
            Sample(
                soundId = soundId,
                ready =
                    soundId in readySoundIds,
            )
        samplesByResource[resourceId] = sample
        resourceBySoundId[soundId] = resourceId
        if (sample.ready) {
            flushPending(
                soundPool = soundPool,
                resourceId = resourceId,
                sample = sample,
            )
        }
        return sample
    }

    private fun flushPending(
        soundPool: SoundPool,
        @RawRes resourceId: Int,
        sample: Sample,
    ) {
        val pending =
            pendingByResource
                .remove(resourceId)
                .orEmpty()
        pending.forEach {
                play ->
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
