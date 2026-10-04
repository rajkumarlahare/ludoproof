package com.ludoproof.game.feature.settings.data.local

import android.content.Context

enum class GameSpeed(
    val label: String,
    val moveStepMs: Long,
    val rollDelayMs: Long,
    val cpuThinkMs: Long,
) {
    SLOW(
        label = "SLOW",
        moveStepMs = 320L,
        rollDelayMs = 620L,
        cpuThinkMs = 850L,
    ),
    NORMAL(
        label = "NORMAL",
        moveStepMs = 240L,
        rollDelayMs = 430L,
        cpuThinkMs = 600L,
    ),
    FAST(
        label = "FAST",
        moveStepMs = 180L,
        rollDelayMs = 260L,
        cpuThinkMs = 380L,
    );

    fun next(): GameSpeed =
        entries[
            (
                ordinal +
                    1
                ) %
                entries.size
        ]
}

data class GameSettings(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val animalVoicesEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotionEnabled: Boolean = false,
    val quickChatEnabled: Boolean = true,
    val gameSpeed: GameSpeed = GameSpeed.NORMAL,
)

class GameSettingsStore(
    context: Context,
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE,
            )

    fun snapshot(): GameSettings =
        GameSettings(
            musicEnabled =
                prefs.getBoolean(
                    KEY_MUSIC,
                    true,
                ),
            soundEnabled =
                prefs.getBoolean(
                    KEY_SOUND,
                    true,
                ),
            animalVoicesEnabled =
                prefs.getBoolean(
                    KEY_ANIMAL_VOICES,
                    true,
                ),
            hapticsEnabled =
                prefs.getBoolean(
                    KEY_HAPTICS,
                    true,
                ),
            reducedMotionEnabled =
                prefs.getBoolean(
                    KEY_REDUCED_MOTION,
                    false,
                ),
            quickChatEnabled =
                prefs.getBoolean(
                    KEY_QUICK_CHAT,
                    true,
                ),
            gameSpeed =
                runCatching {
                    GameSpeed.valueOf(
                        prefs.getString(
                            KEY_GAME_SPEED,
                            GameSpeed.NORMAL.name,
                        )
                            ?: GameSpeed.NORMAL.name,
                    )
                }.getOrDefault(
                    GameSpeed.NORMAL,
                ),
        )

    fun setMusicEnabled(
        enabled: Boolean,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_MUSIC,
                enabled,
            )
            .apply()
    }

    fun setSoundEnabled(
        enabled: Boolean,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_SOUND,
                enabled,
            )
            .apply()
    }

    fun setAnimalVoicesEnabled(
        enabled: Boolean,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_ANIMAL_VOICES,
                enabled,
            )
            .apply()
    }

    fun setHapticsEnabled(
        enabled: Boolean,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_HAPTICS,
                enabled,
            )
            .apply()
    }

    fun setReducedMotionEnabled(
        enabled: Boolean,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_REDUCED_MOTION,
                enabled,
            )
            .apply()
    }

    fun setQuickChatEnabled(
        enabled: Boolean,
    ) {
        prefs.edit()
            .putBoolean(
                KEY_QUICK_CHAT,
                enabled,
            )
            .apply()
    }

    fun setGameSpeed(
        speed: GameSpeed,
    ) {
        prefs.edit()
            .putString(
                KEY_GAME_SPEED,
                speed.name,
            )
            .apply()
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_game_settings"
        const val KEY_MUSIC =
            "music_enabled"
        const val KEY_SOUND =
            "sound_enabled"
        const val KEY_ANIMAL_VOICES =
            "animal_voices_enabled"
        const val KEY_HAPTICS =
            "haptics_enabled"
        const val KEY_REDUCED_MOTION =
            "reduced_motion_enabled"
        const val KEY_QUICK_CHAT =
            "quick_chat_enabled"
        const val KEY_GAME_SPEED =
            "game_speed"
    }
}
