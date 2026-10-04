package com.ludoproof.game.ui.offline.setup

import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.GameMode
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.characters.domain.selection.StarterPawsAssignmentPolicy
import com.ludoproof.game.feature.offline.data.local.OfflineCharacterSetupSnapshot

internal fun OfflineGameActivity.initializeCharacterSetup() {
    val preferred =
        characterSelectionStore
            .load()
    val saved =
        offlineCharacterSetupStore
            .loadSetup(
                mode = gameMode,
                playerCount = selectedPlayers,
                preferredCharacterId = preferred.characterId,
            )
    selectedCharacterSlot =
        saved.selectedSlot
    selectedCharacterIds =
        saved.characterIds
}

internal fun OfflineGameActivity.updateSelectedPlayerCount(
    playerCount: Int,
) {
    require(gameMode.supportsPlayerCount(playerCount)) {
        "Unsupported local player count: $playerCount"
    }
    selectedPlayers =
        playerCount
    selectedCharacterSlot =
        if (isComputerMode) {
            0
        } else {
            selectedCharacterSlot.coerceIn(
                0,
                playerCount - 1,
            )
        }
    selectedCharacterIds =
        StarterPawsAssignmentPolicy
            .normalize(
                playerCount = playerCount,
                requestedCharacterIds = selectedCharacterIds,
                preferredCharacterId =
                    characterSelectionStore
                        .load()
                        .characterId,
                computerMode = isComputerMode,
            )
    persistCharacterSetup()
    refreshSetupSelections()
}

internal fun OfflineGameActivity.selectCharacterSlot(
    slot: Int,
) {
    if (isComputerMode) {
        selectedCharacterSlot =
            0
    } else if (slot in 0 until selectedPlayers) {
        selectedCharacterSlot =
            slot
    }
    persistCharacterSetup()
    refreshCharacterSelectionUi()
}

internal fun OfflineGameActivity.selectCharacterForActiveSlot(
    characterId: String,
) {
    selectedCharacterIds =
        StarterPawsAssignmentPolicy
            .select(
                playerCount = selectedPlayers,
                currentCharacterIds = selectedCharacterIds,
                selectedSlot = selectedCharacterSlot,
                requestedCharacterId = characterId,
                computerMode = isComputerMode,
            )

    selectedCharacterIds
        .firstOrNull()
        ?.let { playerOneCharacterId ->
            characterSelectionStore.select(
                packId =
                    LudoPawsCharacterCatalog.STARTER_PACK_ID,
                characterId =
                    playerOneCharacterId,
            )
        }

    persistCharacterSetup()
    refreshCharacterSelectionUi()
}

internal fun OfflineGameActivity.persistCharacterSetup() {
    val repaired =
        offlineCharacterSetupStore
            .saveSetup(
                mode = gameMode,
                snapshot =
                    OfflineCharacterSetupSnapshot(
                        playerCount = selectedPlayers,
                        selectedSlot = selectedCharacterSlot,
                        characterIds = selectedCharacterIds,
                    ),
            )
    selectedCharacterSlot =
        repaired.selectedSlot
    selectedCharacterIds =
        repaired.characterIds
}

internal fun OfflineGameActivity.persistActiveCharacterSetup() {
    persistCharacterSetup()
    offlineCharacterSetupStore
        .saveActive(
            mode = gameMode,
            playerCount = selectedPlayers,
            preferredColor = selectedColor,
            characterIds = selectedCharacterIds,
        )
}

internal fun OfflineGameActivity.characterPanel(): LinearLayout =
    selectionPanel(
        "CHOOSE YOUR PAW",
        if (isComputerMode) {
            "Choose your animal. Computer players are assigned automatically."
        } else {
            "Choose a different animal for each player."
        },
    ).apply {
        val slotsRow =
            LinearLayout(
                this@characterPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }
        val slotMap =
            linkedMapOf<Int, Button>()
        val visibleSlots =
            if (isComputerMode) {
                listOf(0)
            } else {
                (0 until selectedPlayers).toList()
            }

        visibleSlots.forEach { slot ->
            val button =
                tileButton(
                    if (isComputerMode) {
                        "YOU"
                    } else {
                        "P${slot + 1}"
                    },
                ) {
                    selectCharacterSlot(slot)
                }
            slotMap[slot] =
                button
            slotsRow.addView(
                button,
                setupTileParams(
                    if (isCompactSetup()) 52 else 56,
                    marginDp = 4,
                ),
            )
        }
        characterSlotButtons =
            slotMap
        addView(slotsRow)

        val scroll =
            HorizontalScrollView(
                this@characterPanel,
            ).apply {
                isHorizontalScrollBarEnabled =
                    false
                isFillViewport =
                    false
                overScrollMode =
                    HorizontalScrollView.OVER_SCROLL_NEVER
            }
        val cardsRow =
            LinearLayout(
                this@characterPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
                setPadding(
                    dp(4),
                    dp(10),
                    dp(4),
                    dp(4),
                )
            }
        val cardMap =
            linkedMapOf<String, LinearLayout>()

        LudoPawsCharacterCatalog
            .charactersForPack(
                LudoPawsCharacterCatalog.STARTER_PACK_ID,
            )
            .forEach { character ->
                val card =
                    characterChoiceCard(character)
                cardMap[character.id] =
                    card
                cardsRow.addView(
                    card,
                    LinearLayout.LayoutParams(
                        dp(if (isCompactSetup()) 96 else 108),
                        dp(if (isCompactSetup()) 126 else 136),
                    ).apply {
                        setMargins(
                            dp(4),
                            0,
                            dp(4),
                            0,
                        )
                    },
                )
            }
        characterCards =
            cardMap
        scroll.addView(cardsRow)
        addView(scroll)

        characterSummaryText =
            TextView(
                this@characterPanel,
            ).apply {
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 10.5f else 11.5f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(6),
                    dp(8),
                    dp(6),
                    0,
                )
            }
                .also(::addView)

        refreshCharacterSelectionUi()
    }

private fun OfflineGameActivity.characterChoiceCard(
    character: AnimalCharacter,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(6),
            dp(7),
            dp(6),
            dp(7),
        )

        val drawableId =
            resources.getIdentifier(
                character.fallbackDrawableName,
                "drawable",
                packageName,
            )
        addView(
            ImageView(
                this@characterChoiceCard,
            ).apply {
                if (drawableId != 0) {
                    setImageResource(drawableId)
                }
                contentDescription =
                    "${character.displayName} ${character.species.name.lowercase()}"
                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            },
            LinearLayout.LayoutParams(
                dp(if (isCompactSetup()) 54 else 62),
                dp(if (isCompactSetup()) 54 else 62),
            ),
        )

        addView(
            TextView(
                this@characterChoiceCard,
            ).apply {
                text =
                    character.displayName
                LudoProofTheme.body(
                    this,
                    if (isCompactSetup()) 12f else 13f,
                    centered = true,
                    bright = true,
                )
            },
        )

        addView(
            TextView(
                this@characterChoiceCard,
            ).apply {
                text =
                    character.personality.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercase)
                textSize =
                    if (isCompactSetup()) 9.5f else 10f
                setTextColor(
                    0xFFBCD5FF.toInt(),
                )
                gravity =
                    Gravity.CENTER
            },
        )

        setOnClickListener {
            selectCharacterForActiveSlot(
                character.id,
            )
        }
    }

internal fun OfflineGameActivity.refreshCharacterSelectionUi() {
    if (selectedCharacterIds.size != selectedPlayers) {
        selectedCharacterIds =
            StarterPawsAssignmentPolicy.normalize(
                playerCount = selectedPlayers,
                requestedCharacterIds = selectedCharacterIds,
                preferredCharacterId =
                    characterSelectionStore
                        .load()
                        .characterId,
                computerMode = isComputerMode,
            )
    }

    characterSlotButtons.forEach { (slot, button) ->
        if (slot == selectedCharacterSlot) {
            LudoProofTheme.selectedTile(button)
        } else {
            LudoProofTheme.normalTile(button)
        }
        val characterName =
            selectedCharacterIds
                .getOrNull(slot)
                ?.let(LudoPawsCharacterCatalog::character)
                ?.displayName
                .orEmpty()
        button.text =
            when {
                isComputerMode -> "YOU\n$characterName"
                else -> "P${slot + 1}\n$characterName"
            }
        button.setTextColor(
            if (slot == selectedCharacterSlot) {
                0xFF052A68.toInt()
            } else {
                Color.WHITE
            },
        )
    }

    val activeCharacterId =
        selectedCharacterIds
            .getOrNull(selectedCharacterSlot)
    characterCards.forEach { (characterId, card) ->
        if (characterId == activeCharacterId) {
            LudoProofTheme.selectedTile(card)
        } else {
            LudoProofTheme.normalTile(card)
        }
    }

    characterSummaryText?.text =
        selectedCharacterIds
            .mapIndexed { index, characterId ->
                val characterName =
                    LudoPawsCharacterCatalog
                        .character(characterId)
                        ?.displayName
                        ?: characterId
                val owner =
                    if (isComputerMode) {
                        if (index == 0) {
                            "YOU"
                        } else {
                            "CPU $index"
                        }
                    } else {
                        "P${index + 1}"
                    }
                "$owner $characterName"
            }
            .joinToString("  •  ")
}
