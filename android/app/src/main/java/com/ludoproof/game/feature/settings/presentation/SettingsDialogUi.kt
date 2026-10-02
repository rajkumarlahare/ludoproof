package com.ludoproof.game.ui.dialogs

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory

internal fun showSettingsDialog(
    context: Context,
    onChanged: (() -> Unit)? = null,
) {
    val settingsStore =
        GameSettingsStore(
            context,
        )
    val cosmetics =
        CosmeticInventoryStore(
            context,
        )
    val profileLevel =
        ProfileProgression
            .levelProgress(
                ProfileStore(
                    context,
                )
                    .snapshot()
                    .totalXp,
            )
            .level

    val dialog =
        baseDialog(
            context,
        )
    val panel =
        dialogPanel(
            context,
            "SETTINGS",
            "GAMEPLAY & APPEARANCE",
            dialog,
        )

    fun refresh(
        change: () -> Unit,
    ) {
        change()
        onChanged
            ?.invoke()
        dialog.dismiss()
        showSettingsDialog(
            context,
            onChanged,
        )
    }

    fun addClickableRow(
        title: String,
        value: String,
        detail: String,
        action: () -> Unit,
    ) {
        val row =
            settingsRow(
                context,
                title,
                value,
                detail,
            ).apply {
                isClickable =
                    true
                isFocusable =
                    true
                foreground =
                    context.getDrawable(
                        android.R.drawable.list_selector_background,
                    )
                setOnClickListener {
                    action()
                }
            }

        panel.addView(
            row,
            fullWidthParams(
                context,
                topDp = 9,
            ),
        )
    }

    val settings =
        settingsStore
            .snapshot()

    addClickableRow(
        title = "Music",
        value =
            if (
                settings.musicEnabled
            ) {
                "ON"
            } else {
                "OFF"
            },
        detail =
            "Background music preference. Tap to toggle.",
    ) {
        refresh {
            settingsStore
                .setMusicEnabled(
                    !settings
                        .musicEnabled,
                )
        }
    }

    addClickableRow(
        title = "Sound",
        value =
            if (
                settings.soundEnabled
            ) {
                "ON"
            } else {
                "OFF"
            },
        detail =
            "Dice, move and interaction sound feedback.",
    ) {
        refresh {
            settingsStore
                .setSoundEnabled(
                    !settings
                        .soundEnabled,
                )
        }
    }

    addClickableRow(
        title = "Quick chat",
        value =
            if (
                settings.quickChatEnabled
            ) {
                "ON"
            } else {
                "OFF"
            },
        detail =
            "Shows quick emoji reactions during a match.",
    ) {
        refresh {
            settingsStore
                .setQuickChatEnabled(
                    !settings
                        .quickChatEnabled,
                )
        }
    }

    addClickableRow(
        title = "Game speed",
        value =
            settings
                .gameSpeed
                .label,
        detail =
            "Controls dice timing, CPU thinking and token movement animation speed.",
    ) {
        refresh {
            settingsStore
                .setGameSpeed(
                    settings
                        .gameSpeed
                        .next(),
                )
        }
    }

    fun nextOwnedCosmetic(
        category: CosmeticCategory,
    ) {
        val owned =
            StoreCosmeticCatalog
                .forCategory(
                    category,
                )
                .filter {
                    cosmetics
                        .isOwned(
                            it.id,
                        )
                }
        if (
            owned.isEmpty()
        ) {
            return
        }

        val current =
            cosmetics.selectedId(
                category,
            )
        val currentIndex =
            owned.indexOfFirst {
                it.id ==
                    current
            }
        val next =
            owned[
                if (
                    currentIndex <
                    0
                ) {
                    0
                } else {
                    (
                        currentIndex +
                            1
                        ) %
                        owned.size
                }
            ]

        cosmetics.acquireOrSelect(
            cosmetic =
                next,
            currentLevel =
                profileLevel,
        )
    }

    val selectedBoard =
        StoreCosmeticCatalog
            .find(
                cosmetics.selectedId(
                    CosmeticCategory.BOARD,
                ),
            )
    addClickableRow(
        title = "Board",
        value =
            selectedBoard
                ?.title
                ?.uppercase()
                ?: "CLASSIC",
        detail =
            "Tap to cycle through boards you already unlocked.",
    ) {
        refresh {
            nextOwnedCosmetic(
                CosmeticCategory.BOARD,
            )
        }
    }

    val selectedDice =
        StoreCosmeticCatalog
            .find(
                cosmetics.selectedId(
                    CosmeticCategory.DICE,
                ),
            )
    addClickableRow(
        title = "Dice",
        value =
            selectedDice
                ?.title
                ?.uppercase()
                ?: "CLASSIC",
        detail =
            "Tap to cycle through dice you already unlocked.",
    ) {
        refresh {
            nextOwnedCosmetic(
                CosmeticCategory.DICE,
            )
        }
    }

    panel.addView(
        trustStrip(
            context,
            "FAIRNESS IS UNCHANGED",
            "These options change presentation, audio and animation timing only. Dice outcomes and Ludo rules are not changed.",
        ),
        fullWidthParams(
            context,
            topDp = 12,
        ),
    )

    dialog.setContentView(
        panel,
    )
    sizeDialog(
        dialog,
        .92f,
    )
    dialog.show()
}
