package com.ludoproof.game.feature.settings.data.local

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.ludoproof.game.R

object GameMusicController {
    private var appForeground = false
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val focusListener =
        AudioManager.OnAudioFocusChangeListener { change ->
            synchronized(this) {
                when (change) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasAudioFocus = true
                        player?.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                        if (
                            appForeground &&
                            player?.isPlaying == false
                        ) {
                            runCatching {
                                player?.start()
                            }
                        }
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                        player?.setVolume(DUCK_VOLUME, DUCK_VOLUME)

                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        hasAudioFocus = false
                        runCatching {
                            player?.pause()
                        }
                    }
                }
            }
        }

    @Synchronized
    fun onAppForeground(
        context: Context,
    ) {
        appForeground = true
        sync(context.applicationContext)
    }

    @Synchronized
    fun onAppBackground() {
        appForeground = false
        runCatching {
            player?.pause()
        }
        abandonFocus()
    }

    @Synchronized
    fun onPreferenceChanged(
        context: Context,
    ) {
        sync(context.applicationContext)
    }

    @Synchronized
    fun release() {
        abandonFocus()
        runCatching {
            player?.release()
        }
        player = null
        audioManager = null
    }

    private fun sync(
        context: Context,
    ) {
        val enabled =
            GameSettingsStore(context)
                .snapshot()
                .musicEnabled

        if (
            !enabled ||
            !appForeground
        ) {
            runCatching {
                player?.pause()
            }
            if (!enabled) {
                runCatching {
                    player?.seekTo(0)
                }
            }
            abandonFocus()
            return
        }

        ensurePlayer(context)
        if (player == null) {
            return
        }

        if (!requestFocus(context)) {
            return
        }

        player?.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
        if (player?.isPlaying == false) {
            runCatching {
                player?.start()
            }
        }
    }

    private fun ensurePlayer(
        context: Context,
    ) {
        if (player != null) {
            return
        }

        player =
            MediaPlayer.create(
                context,
                R.raw.ludoproof_theme,
            )
                ?.apply {
                    isLooping = true
                    setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                }
    }

    private fun requestFocus(
        context: Context,
    ): Boolean {
        if (hasAudioFocus) {
            return true
        }

        val manager =
            audioManager
                ?: (
                    context.getSystemService(
                        Context.AUDIO_SERVICE,
                    ) as? AudioManager
                    )
                    ?.also {
                        audioManager = it
                    }
                ?: return false

        val request =
            focusRequest
                ?: AudioFocusRequest
                    .Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setOnAudioFocusChangeListener(focusListener)
                    .setWillPauseWhenDucked(false)
                    .build()
                    .also {
                        focusRequest = it
                    }

        hasAudioFocus =
            manager.requestAudioFocus(request) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonFocus() {
        val manager = audioManager
        val request = focusRequest
        if (
            manager != null &&
            request != null &&
            hasAudioFocus
        ) {
            runCatching {
                manager.abandonAudioFocusRequest(request)
            }
        }
        hasAudioFocus = false
    }

    private const val MUSIC_VOLUME = .16f
    private const val DUCK_VOLUME = .05f
}
