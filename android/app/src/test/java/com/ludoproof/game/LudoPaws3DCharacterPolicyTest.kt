package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoPaws3DCharacterPolicyTest {
    @Test
    fun `canonical selected characters resolve to matching production species`() {
        assertEquals(
            LudoPaws3DSpecies.DOG,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("dog"),
        )
        assertEquals(
            LudoPaws3DSpecies.GOAT,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("goat"),
        )
        assertEquals(
            LudoPaws3DSpecies.DUCK,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("duck"),
        )
        assertEquals(
            LudoPaws3DSpecies.CAT,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("cat"),
        )
    }

    @Test
    fun `legacy persisted character ids resolve to canonical production species`() {
        assertEquals(
            LudoPaws3DSpecies.DOG,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("squirrel"),
        )
        assertEquals(
            LudoPaws3DSpecies.GOAT,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("hedgehog"),
        )
        assertEquals(
            LudoPaws3DSpecies.CAT,
            LudoPaws3DCharacterPolicy.speciesForCharacterId("sheep"),
        )
    }

    @Test
    fun `seat assignment wins over ludo color for production rendering`() {
        assertEquals(
            LudoPaws3DSpecies.CAT,
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = listOf("cat", "dog"),
                seat = 0,
                fallbackColor = "RED",
            ),
        )
        assertEquals(
            LudoPaws3DSpecies.DOG,
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = listOf("cat", "dog"),
                seat = 1,
                fallbackColor = "GREEN",
            ),
        )
    }

    @Test
    fun `missing seat identity falls back deterministically to authoritative ludo color`() {
        assertEquals(
            LudoPaws3DSpecies.DOG,
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = emptyList(),
                seat = 0,
                fallbackColor = "RED",
            ),
        )
        assertEquals(
            LudoPaws3DSpecies.GOAT,
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = emptyList(),
                seat = 1,
                fallbackColor = "GREEN",
            ),
        )
    }

    @Test
    fun `unknown identity and color never invent a production pawn`() {
        assertNull(
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = listOf("dragon"),
                seat = 0,
                fallbackColor = "PURPLE",
            ),
        )
        assertNull(
            LudoPaws3DCharacterPolicy.speciesForCharacterId(""),
        )
    }
}
