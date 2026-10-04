package com.ludoproof.game.ui.dialogs

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.ludoproof.game.DiceView
import com.ludoproof.game.LudoBoardView
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.settings.data.local.GameMusicController
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory

internal fun showSettingsDialog(
    context: Context,
    onChanged: (() -> Unit)? = null,
) {
    val activity =
        context as? Activity
            ?: return
    if (
        activity.isFinishing ||
        activity.isDestroyed
    ) {
        return
    }

    val settingsStore =
        GameSettingsStore(context)
    val cosmetics =
        CosmeticInventoryStore(context)
    val profileLevel =
        ProfileProgression
            .levelProgress(
                ProfileStore(context)
                    .snapshot()
                    .totalXp,
            )
            .level

    val dialog =
        baseDialog(context)
    val panel =
        settingsCompactPanel(
            context = context,
            dialog = dialog,
        )

    fun notifyChanged(
        refreshCosmetics: Boolean = false,
    ) {
        if (refreshCosmetics) {
            reloadVisibleCosmetics(
                activity.window
                    .decorView,
            )
        }
        onChanged?.invoke()
    }

    var settings =
        settingsStore.snapshot()

    val musicControl =
        SettingsAudioIconView(
            context,
        ).apply {
            kind =
                SettingsAudioIconKind.MUSIC
            isOn =
                settings.musicEnabled
            contentDescription =
                if (isOn) {
                    "Music on"
                } else {
                    "Music muted"
                }
            setOnClickListener {
                val enabled =
                    !settingsStore
                        .snapshot()
                        .musicEnabled
                settingsStore
                    .setMusicEnabled(
                        enabled,
                    )
                GameMusicController
                    .onPreferenceChanged(
                        context,
                    )
                isOn = enabled
                contentDescription =
                    if (enabled) {
                        "Music on"
                    } else {
                        "Music muted"
                    }
                GameSoundFeedback.click(
                    context,
                )
                settings =
                    settingsStore
                        .snapshot()
                notifyChanged()
            }
        }
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Music",
            control = musicControl,
        ),
    )

    val soundControl =
        SettingsAudioIconView(
            context,
        ).apply {
            kind =
                SettingsAudioIconKind.SOUND
            isOn =
                settings.soundEnabled
            contentDescription =
                if (isOn) {
                    "Sound on"
                } else {
                    "Sound muted"
                }
            setOnClickListener {
                val enabled =
                    !settingsStore
                        .snapshot()
                        .soundEnabled
                settingsStore
                    .setSoundEnabled(
                        enabled,
                    )
                isOn = enabled
                contentDescription =
                    if (enabled) {
                        "Sound on"
                    } else {
                        "Sound muted"
                    }
                if (enabled) {
                    GameSoundFeedback.click(
                        context,
                    )
                }
                settings =
                    settingsStore
                        .snapshot()
                notifyChanged()
            }
        }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Game Sounds",
            control = soundControl,
        ),
    )

    val animalVoicesControl =
        SettingsBooleanControl(
            context = context,
            initialValue =
                settings.animalVoicesEnabled,
        ) { enabled ->
            settingsStore
                .setAnimalVoicesEnabled(
                    enabled,
                )
            settings =
                settingsStore
                    .snapshot()
            GameSoundFeedback.click(
                context,
            )
            notifyChanged()
        }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Animal Voices",
            control = animalVoicesControl,
        ),
    )

    val quickChatControl =
        SettingsBooleanControl(
            context = context,
            initialValue =
                settings.quickChatEnabled,
        ) { enabled ->
            settingsStore
                .setQuickChatEnabled(
                    enabled,
                )
            settings =
                settingsStore
                    .snapshot()
            GameSoundFeedback.click(
                context,
            )
            notifyChanged()
        }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Quick chat",
            control = quickChatControl,
        ),
    )

    val hapticsControl =
        SettingsBooleanControl(
            context = context,
            initialValue =
                settings.hapticsEnabled,
        ) { enabled ->
            settingsStore
                .setHapticsEnabled(
                    enabled,
                )
            settings =
                settingsStore
                    .snapshot()
            GameSoundFeedback.click(
                context,
            )
            notifyChanged()
        }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Haptics",
            control = hapticsControl,
        ),
    )

    val reducedMotionControl =
        SettingsBooleanControl(
            context = context,
            initialValue =
                settings.reducedMotionEnabled,
        ) { enabled ->
            settingsStore
                .setReducedMotionEnabled(
                    enabled,
                )
            settings =
                settingsStore
                    .snapshot()
            GameSoundFeedback.click(
                context,
            )
            notifyChanged()
        }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Reduced Motion",
            control = reducedMotionControl,
        ),
    )

    val speedField =
        settingsDropdownField(
            context,
            settings.gameSpeed.label,
        )
    speedField.setOnClickListener {
        val latest =
            settingsStore
                .snapshot()
        showSettingsChoiceDialog(
            context = context,
            title = "GAME SPEED",
            options =
                listOf(
                    "FAST",
                    "NORMAL",
                    "SLOW",
                ),
            selected =
                latest.gameSpeed.label,
        ) { selected ->
            val speed =
                com.ludoproof.game
                    .feature.settings.data.local
                    .GameSpeed.entries
                    .first {
                        it.label ==
                            selected
                    }
            settingsStore
                .setGameSpeed(
                    speed,
                )
            speedField.text =
                settingsDropdownLabel(
                    selected,
                )
            settings =
                settingsStore
                    .snapshot()
            GameSoundFeedback.click(
                context,
            )
            notifyChanged()
        }
    }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Game Speed",
            control = speedField,
        ),
    )

    val ownedBoards =
        StoreCosmeticCatalog
            .forCategory(
                CosmeticCategory.BOARD,
            )
            .filter {
                cosmetics.isOwned(
                    it.id,
                )
            }
    val selectedBoard =
        StoreCosmeticCatalog
            .find(
                cosmetics.selectedId(
                    CosmeticCategory.BOARD,
                ),
            )
    val boardField =
        settingsDropdownField(
            context,
            selectedBoard
                ?.title
                ?.uppercase()
                ?: "CLASSIC",
        )
    boardField.setOnClickListener {
        val currentTitle =
            StoreCosmeticCatalog
                .find(
                    cosmetics.selectedId(
                        CosmeticCategory.BOARD,
                    ),
                )
                ?.title
                ?.uppercase()
                ?: "CLASSIC"
        showSettingsChoiceDialog(
            context = context,
            title = "BOARDS",
            options =
                ownedBoards.map {
                    it.title.uppercase()
                },
            selected = currentTitle,
        ) { selected ->
            val cosmetic =
                ownedBoards
                    .firstOrNull {
                        it.title
                            .uppercase() ==
                            selected
                    }
                    ?: return@showSettingsChoiceDialog
            cosmetics.acquireOrSelect(
                cosmetic = cosmetic,
                currentLevel =
                    profileLevel,
            )
            val applied =
                StoreCosmeticCatalog
                    .find(
                        cosmetics.selectedId(
                            CosmeticCategory.BOARD,
                        ),
                    )
                    ?.title
                    ?.uppercase()
                    ?: "CLASSIC"
            boardField.text =
                settingsDropdownLabel(
                    applied,
                )
            GameSoundFeedback.click(
                context,
            )
            notifyChanged(
                refreshCosmetics =
                    true,
            )
        }
    }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Boards",
            control = boardField,
        ),
    )

    val ownedDice =
        StoreCosmeticCatalog
            .forCategory(
                CosmeticCategory.DICE,
            )
            .filter {
                cosmetics.isOwned(
                    it.id,
                )
            }
    val selectedDice =
        StoreCosmeticCatalog
            .find(
                cosmetics.selectedId(
                    CosmeticCategory.DICE,
                ),
            )
    val diceField =
        settingsDropdownField(
            context,
            selectedDice
                ?.title
                ?.uppercase()
                ?: "CLASSIC",
        )
    diceField.setOnClickListener {
        val currentTitle =
            StoreCosmeticCatalog
                .find(
                    cosmetics.selectedId(
                        CosmeticCategory.DICE,
                    ),
                )
                ?.title
                ?.uppercase()
                ?: "CLASSIC"
        showSettingsChoiceDialog(
            context = context,
            title = "DICE",
            options =
                ownedDice.map {
                    it.title.uppercase()
                },
            selected = currentTitle,
        ) { selected ->
            val cosmetic =
                ownedDice
                    .firstOrNull {
                        it.title
                            .uppercase() ==
                            selected
                    }
                    ?: return@showSettingsChoiceDialog
            cosmetics.acquireOrSelect(
                cosmetic = cosmetic,
                currentLevel =
                    profileLevel,
            )
            val applied =
                StoreCosmeticCatalog
                    .find(
                        cosmetics.selectedId(
                            CosmeticCategory.DICE,
                        ),
                    )
                    ?.title
                    ?.uppercase()
                    ?: "CLASSIC"
            diceField.text =
                settingsDropdownLabel(
                    applied,
                )
            GameSoundFeedback.click(
                context,
            )
            notifyChanged(
                refreshCosmetics =
                    true,
            )
        }
    }
    panel.addView(
        settingsDivider(context),
    )
    panel.addView(
        settingsCompactRow(
            context = context,
            label = "Dice",
            control = diceField,
        ),
    )

    panel.addView(
        settingsPrivacyLink(
            context,
        ).apply {
            setOnClickListener {
                GameSoundFeedback.click(
                    context,
                )
                showPrivacyPolicyDialog(
                    context,
                )
            }
        },
    )

    dialog.setContentView(
        panel,
    )
    sizeDialog(
        dialog,
        .91f,
    )
    dialog.show()
}

private fun reloadVisibleCosmetics(
    view: View,
) {
    when (view) {
        is LudoBoardView ->
            view.reloadStyle()
        is DiceView ->
            view.reloadStyle()
    }

    if (view is ViewGroup) {
        for (
            index in
            0 until
                view.childCount
        ) {
            reloadVisibleCosmetics(
                view.getChildAt(
                    index,
                ),
            )
        }
    }
}
