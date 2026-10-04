package com.ludoproof.game.feature.characters.domain.catalog

import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.characters.domain.model.AnimalPack
import com.ludoproof.game.feature.characters.domain.model.AnimalPersonality
import com.ludoproof.game.feature.characters.domain.model.AnimalSpecies
import com.ludoproof.game.feature.characters.domain.model.AnimationSet
import com.ludoproof.game.feature.characters.domain.model.CharacterSelection
import com.ludoproof.game.feature.characters.domain.model.VoiceSet

object LudoPawsCharacterCatalog {
    const val STARTER_PACK_ID =
        "starter_paws"
    const val DEFAULT_CHARACTER_ID =
        "duck"

    private val voiceSetList =
        listOf(
            VoiceSet(
                id = "duck_default",
                species = AnimalSpecies.DUCK,
            ),
            VoiceSet(
                id = "squirrel_default",
                species = AnimalSpecies.SQUIRREL,
            ),
            VoiceSet(
                id = "hedgehog_default",
                species = AnimalSpecies.HEDGEHOG,
            ),
            VoiceSet(
                id = "sheep_default",
                species = AnimalSpecies.SHEEP,
            ),
        )

    private val animationSetList =
        listOf(
            AnimationSet("duck_default"),
            AnimationSet("squirrel_default"),
            AnimationSet("hedgehog_default"),
            AnimationSet("sheep_default"),
        )

    private val characterList =
        listOf(
            AnimalCharacter(
                id = "duck",
                displayName = "Ducky",
                species = AnimalSpecies.DUCK,
                personality = AnimalPersonality.CHEERFUL,
                voiceSetId = "duck_default",
                animationSetId = "duck_default",
                fallbackDrawableName = "lp_starter_duck",
            ),
            AnimalCharacter(
                id = "squirrel",
                displayName = "Nutty",
                species = AnimalSpecies.SQUIRREL,
                personality = AnimalPersonality.MISCHIEVOUS,
                voiceSetId = "squirrel_default",
                animationSetId = "squirrel_default",
                fallbackDrawableName = "lp_starter_squirrel",
            ),
            AnimalCharacter(
                id = "hedgehog",
                displayName = "Spike",
                species = AnimalSpecies.HEDGEHOG,
                personality = AnimalPersonality.SHY,
                voiceSetId = "hedgehog_default",
                animationSetId = "hedgehog_default",
                fallbackDrawableName = "lp_starter_hedgehog",
            ),
            AnimalCharacter(
                id = "sheep",
                displayName = "Woolly",
                species = AnimalSpecies.SHEEP,
                personality = AnimalPersonality.GENTLE,
                voiceSetId = "sheep_default",
                animationSetId = "sheep_default",
                fallbackDrawableName = "lp_starter_sheep",
            ),
        )

    private val packList =
        listOf(
            AnimalPack(
                id = STARTER_PACK_ID,
                displayName = "Starter Paws",
                starter = true,
                characterIds =
                    listOf(
                        "duck",
                        "squirrel",
                        "hedgehog",
                        "sheep",
                    ),
            ),
        )

    private val charactersById =
        characterList.associateBy(
            AnimalCharacter::id,
        )
    private val packsById =
        packList.associateBy(
            AnimalPack::id,
        )
    private val voiceSetsById =
        voiceSetList.associateBy(
            VoiceSet::id,
        )
    private val animationSetsById =
        animationSetList.associateBy(
            AnimationSet::id,
        )

    val defaultSelection: CharacterSelection =
        CharacterSelection(
            packId = STARTER_PACK_ID,
            characterId = DEFAULT_CHARACTER_ID,
        )

    init {
        validateCatalog()
    }

    val characters: List<AnimalCharacter>
        get() = characterList

    val packs: List<AnimalPack>
        get() = packList

    val voiceSets: List<VoiceSet>
        get() = voiceSetList

    val animationSets: List<AnimationSet>
        get() = animationSetList

    fun character(
        id: String,
    ): AnimalCharacter? =
        charactersById[id]

    fun pack(
        id: String,
    ): AnimalPack? =
        packsById[id]

    fun voiceSet(
        id: String,
    ): VoiceSet? =
        voiceSetsById[id]

    fun animationSet(
        id: String,
    ): AnimationSet? =
        animationSetsById[id]

    fun charactersForPack(
        packId: String,
    ): List<AnimalCharacter> =
        packsById[packId]
            ?.characterIds
            ?.mapNotNull(charactersById::get)
            .orEmpty()

    fun isValidSelection(
        selection: CharacterSelection,
    ): Boolean {
        val pack =
            packsById[selection.packId]
                ?: return false
        return selection.characterId in pack.characterIds &&
            charactersById.containsKey(
                selection.characterId,
            )
    }

    fun resolveSelection(
        packId: String?,
        characterId: String?,
    ): CharacterSelection {
        if (
            packId.isNullOrBlank() &&
            characterId.isNullOrBlank()
        ) {
            return defaultSelection
        }

        val pack =
            packId
                ?.let(packsById::get)
                ?: return defaultSelection

        if (characterId.isNullOrBlank()) {
            val firstCharacter =
                pack.characterIds
                    .firstOrNull()
                    ?: return defaultSelection
            return CharacterSelection(
                packId = pack.id,
                characterId = firstCharacter,
            )
        }

        val requested =
            CharacterSelection(
                packId = pack.id,
                characterId = characterId,
            )
        return if (
            isValidSelection(requested)
        ) {
            requested
        } else {
            defaultSelection
        }
    }

    private fun validateCatalog() {
        require(
            charactersById.size == characterList.size,
        ) {
            "Duplicate Ludo Paws character id"
        }
        require(
            packsById.size == packList.size,
        ) {
            "Duplicate Ludo Paws pack id"
        }
        require(
            voiceSetsById.size == voiceSetList.size,
        ) {
            "Duplicate Ludo Paws voice-set id"
        }
        require(
            animationSetsById.size == animationSetList.size,
        ) {
            "Duplicate Ludo Paws animation-set id"
        }
        require(
            packList.count(AnimalPack::starter) == 1,
        ) {
            "Exactly one starter character pack is required"
        }
        require(
            packsById[STARTER_PACK_ID]?.starter == true,
        ) {
            "Starter Paws must remain the starter pack"
        }
        require(
            charactersById.containsKey(DEFAULT_CHARACTER_ID),
        ) {
            "Default Ludo Paws character is missing"
        }

        for (pack in packList) {
            require(
                pack.characterIds.all(charactersById::containsKey),
            ) {
                "Pack ${pack.id} references an unknown character"
            }
        }

        for (character in characterList) {
            val voiceSet =
                voiceSetsById[character.voiceSetId]
            require(voiceSet != null) {
                "Character ${character.id} references an unknown voice set"
            }
            require(
                voiceSet.species == character.species,
            ) {
                "Voice set ${voiceSet.id} belongs to the wrong species"
            }
            require(
                animationSetsById.containsKey(
                    character.animationSetId,
                ),
            ) {
                "Character ${character.id} references an unknown animation set"
            }
        }

        require(
            isValidSelection(defaultSelection),
        ) {
            "Default Ludo Paws selection must be valid"
        }
    }
}
