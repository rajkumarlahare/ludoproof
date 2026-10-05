package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoPaws3DCharacterPolicyTest {
    @Test
    fun `production species stay bound to authoritative ludo colors`() {
        assertEquals(
            LudoPaws3DSpecies.DOG,
            LudoPaws3DCharacterPolicy.speciesForColor("RED"),
        )
        assertEquals(
            LudoPaws3DSpecies.GOAT,
            LudoPaws3DCharacterPolicy.speciesForColor("GREEN"),
        )
        assertEquals(
            LudoPaws3DSpecies.DUCK,
            LudoPaws3DCharacterPolicy.speciesForColor("YELLOW"),
        )
        assertEquals(
            LudoPaws3DSpecies.CAT,
            LudoPaws3DCharacterPolicy.speciesForColor("BLUE"),
        )
    }

    @Test
    fun `unknown colors never invent a production pawn`() {
        assertNull(
            LudoPaws3DCharacterPolicy.speciesForColor("PURPLE"),
        )
        assertNull(
            LudoPaws3DCharacterPolicy.speciesForColor(""),
        )
    }
}
