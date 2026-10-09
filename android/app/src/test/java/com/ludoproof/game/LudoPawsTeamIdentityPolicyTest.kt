package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoPawsTeamIdentityPolicyTest {
    @Test
    fun `resolves every canonical board color to its stable team sigil`() {
        assertEquals(LudoPawsTeamSigil.DIAMOND, LudoPawsTeamIdentityPolicy.resolve("RED")?.sigil)
        assertEquals(LudoPawsTeamSigil.LEAF, LudoPawsTeamIdentityPolicy.resolve("GREEN")?.sigil)
        assertEquals(LudoPawsTeamSigil.WAVE, LudoPawsTeamIdentityPolicy.resolve("BLUE")?.sigil)
        assertEquals(LudoPawsTeamSigil.SUN, LudoPawsTeamIdentityPolicy.resolve("YELLOW")?.sigil)
    }

    @Test
    fun `identity color lookup is case insensitive and rejects unsupported values`() {
        assertEquals(LudoPawsTeamColor.GREEN, LudoPawsTeamIdentityPolicy.resolve(" green "))
        assertNull(LudoPawsTeamIdentityPolicy.resolve(null))
        assertNull(LudoPawsTeamIdentityPolicy.resolve(""))
        assertNull(LudoPawsTeamIdentityPolicy.resolve("PURPLE"))
    }

    @Test
    fun `team identity stays independent from the selected pet species`() {
        // RED + Cat and GREEN + Dog must not inherit their species' previous collar colors.
        assertEquals(
            LudoPawsTeamColor.RED,
            LudoPawsTeamIdentityPolicy.resolve("RED"),
        )
        assertEquals(
            LudoPaws3DSpecies.CAT,
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = listOf("cat"),
                seat = 0,
                fallbackColor = "RED",
            ),
        )
        assertEquals(
            LudoPawsTeamColor.GREEN,
            LudoPawsTeamIdentityPolicy.resolve("GREEN"),
        )
        assertEquals(
            LudoPaws3DSpecies.DOG,
            LudoPaws3DCharacterPolicy.speciesForSeat(
                characterIdsBySeat = listOf("dog"),
                seat = 0,
                fallbackColor = "GREEN",
            ),
        )
        assertNotEquals(
            LudoPawsTeamIdentityPolicy.resolve("RED")?.sigil,
            LudoPawsTeamIdentityPolicy.resolve("GREEN")?.sigil,
        )
    }

    @Test
    fun `team colors preserve the board's classic palette`() {
        assertEquals(0xFFF1252F.toInt(), LudoPawsTeamIdentityPolicy.resolve("RED")?.argb)
        assertEquals(0xFF00A950.toInt(), LudoPawsTeamIdentityPolicy.resolve("GREEN")?.argb)
        assertEquals(0xFFFFD81B.toInt(), LudoPawsTeamIdentityPolicy.resolve("YELLOW")?.argb)
        assertEquals(0xFF3097D7.toInt(), LudoPawsTeamIdentityPolicy.resolve("BLUE")?.argb)
    }
}
