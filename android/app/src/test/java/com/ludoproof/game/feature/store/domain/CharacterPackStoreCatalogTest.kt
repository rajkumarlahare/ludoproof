package com.ludoproof.game.feature.store.domain

import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterPackStoreCatalogTest {
    @Test
    fun `starter paws is free content ready and linked to complete character content`() {
        val starter =
            requireNotNull(
                StoreCosmeticCatalog.find(
                    StoreCosmeticCatalog.DEFAULT_CHARACTER_PACK,
                ),
            )

        assertEquals(
            CosmeticCategory.CHARACTER_PACK,
            starter.category,
        )
        assertEquals(
            CosmeticUnlockKind.FREE,
            starter.unlockKind,
        )
        assertTrue(starter.contentAvailable)

        val pack =
            LudoPawsCharacterCatalog.pack(
                requireNotNull(starter.characterPackId),
            )
        assertNotNull(pack)
        val characters =
            LudoPawsCharacterCatalog.charactersForPack(
                requireNotNull(starter.characterPackId),
            )
        assertEquals(4, characters.size)
        characters.forEach { character ->
            assertNotNull(
                LudoPawsCharacterCatalog.voiceSet(
                    character.voiceSetId,
                ),
            )
            assertNotNull(
                LudoPawsCharacterCatalog.animationSet(
                    character.animationSetId,
                ),
            )
            assertTrue(
                character.fallbackDrawableName.startsWith("lp_"),
            )
        }
    }

    @Test
    fun `animal pack catalog supports every planned progression channel`() {
        val kinds =
            StoreCosmeticCatalog.characterPacks
                .map { it.unlockKind }
                .toSet()

        assertTrue(CosmeticUnlockKind.FREE in kinds)
        assertTrue(CosmeticUnlockKind.LEVEL in kinds)
        assertTrue(CosmeticUnlockKind.GEMS in kinds)
        assertTrue(CosmeticUnlockKind.REWARDED_ADS in kinds)
        assertTrue(CosmeticUnlockKind.EVENT in kinds)
    }

    @Test
    fun `future packs cannot spend currency or become selectable before content ships`() {
        val future =
            StoreCosmeticCatalog.characterPacks
                .filter {
                    it.id !=
                        StoreCosmeticCatalog.DEFAULT_CHARACTER_PACK
                }

        assertTrue(future.isNotEmpty())
        future.forEach { offer ->
            assertFalse(offer.contentAvailable)
            assertTrue(
                LudoPawsCharacterCatalog.pack(
                    requireNotNull(offer.characterPackId),
                ) == null,
            )
        }
    }

    @Test
    fun `event packs require a stable event key`() {
        StoreCosmeticCatalog.characterPacks
            .filter {
                it.unlockKind ==
                    CosmeticUnlockKind.EVENT
            }
            .forEach { offer ->
                assertFalse(
                    offer.eventKey.isNullOrBlank(),
                )
            }
    }
}
