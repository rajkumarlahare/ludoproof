package com.ludoproof.game.feature.characters.domain.catalog

import com.ludoproof.game.feature.characters.domain.model.AnimalPersonality
import com.ludoproof.game.feature.characters.domain.model.AnimalSpecies
import com.ludoproof.game.feature.characters.domain.model.CharacterSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsCharacterCatalogTest {
    @Test
    fun `starter pack contains the four Phase 2 characters in deterministic order`() {
        val pack =
            requireNotNull(
                LudoPawsCharacterCatalog
                    .pack(
                        LudoPawsCharacterCatalog.STARTER_PACK_ID,
                    ),
            )

        assertTrue(pack.starter)
        assertEquals(
            listOf(
                "duck",
                "squirrel",
                "hedgehog",
                "sheep",
            ),
            pack.characterIds,
        )
        assertEquals(
            pack.characterIds,
            LudoPawsCharacterCatalog
                .charactersForPack(pack.id)
                .map { it.id },
        )
    }

    @Test
    fun `starter characters carry species personality voice animation and fallback identity`() {
        val duck =
            requireNotNull(
                LudoPawsCharacterCatalog
                    .character("duck"),
            )
        val squirrel =
            requireNotNull(
                LudoPawsCharacterCatalog
                    .character("squirrel"),
            )
        val hedgehog =
            requireNotNull(
                LudoPawsCharacterCatalog
                    .character("hedgehog"),
            )
        val sheep =
            requireNotNull(
                LudoPawsCharacterCatalog
                    .character("sheep"),
            )

        assertEquals(AnimalSpecies.DUCK, duck.species)
        assertEquals(AnimalPersonality.CHEERFUL, duck.personality)
        assertEquals("duck_default", duck.voiceSetId)
        assertEquals("duck_default", duck.animationSetId)
        assertEquals("lp_starter_duck", duck.fallbackDrawableName)

        assertEquals(AnimalPersonality.MISCHIEVOUS, squirrel.personality)
        assertEquals(AnimalPersonality.SHY, hedgehog.personality)
        assertEquals(AnimalPersonality.GENTLE, sheep.personality)
    }

    @Test
    fun `every character resolves its matching voice and animation set`() {
        for (character in LudoPawsCharacterCatalog.characters) {
            val voiceSet =
                LudoPawsCharacterCatalog
                    .voiceSet(character.voiceSetId)
            val animationSet =
                LudoPawsCharacterCatalog
                    .animationSet(character.animationSetId)

            assertNotNull(voiceSet)
            assertNotNull(animationSet)
            assertEquals(
                character.species,
                voiceSet?.species,
            )
            assertTrue(
                voiceSet?.supportedCues?.isNotEmpty() == true,
            )
            assertTrue(
                animationSet?.supportedCues?.isNotEmpty() == true,
            )
        }
    }

    @Test
    fun `valid selection remains unchanged`() {
        val selection =
            CharacterSelection(
                packId = "starter_paws",
                characterId = "hedgehog",
            )

        assertTrue(
            LudoPawsCharacterCatalog
                .isValidSelection(selection),
        )
        assertEquals(
            selection,
            LudoPawsCharacterCatalog
                .resolveSelection(
                    selection.packId,
                    selection.characterId,
                ),
        )
    }

    @Test
    fun `corrupt pack or character falls back safely to starter duck`() {
        val fallback =
            LudoPawsCharacterCatalog
                .defaultSelection

        assertEquals(
            fallback,
            LudoPawsCharacterCatalog
                .resolveSelection(
                    "unknown_pack",
                    "duck",
                ),
        )
        assertEquals(
            fallback,
            LudoPawsCharacterCatalog
                .resolveSelection(
                    "starter_paws",
                    "unknown_character",
                ),
        )
        assertEquals(
            fallback,
            LudoPawsCharacterCatalog
                .resolveSelection(
                    null,
                    null,
                ),
        )
    }

    @Test
    fun `pack without explicit character resolves its first valid character`() {
        assertEquals(
            LudoPawsCharacterCatalog
                .defaultSelection,
            LudoPawsCharacterCatalog
                .resolveSelection(
                    "starter_paws",
                    null,
                ),
        )
    }

    @Test
    fun `character cannot be selected outside its pack`() {
        val invalid =
            CharacterSelection(
                packId = "missing_pack",
                characterId = "duck",
            )

        assertFalse(
            LudoPawsCharacterCatalog
                .isValidSelection(invalid),
        )
    }
}
