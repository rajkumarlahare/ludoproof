package com.ludoproof.game

import org.junit.Assert.assertArrayEquals
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
    fun `yellow belly uses the classic board yellow while other team belly colors stay unchanged`() {
        assertArrayEquals(
            floatArrayOf(0.98f, 0.08f, 0.11f, 1f),
            requireNotNull(LudoPawsTeamIdentityPolicy.resolve("RED")).bellyGlColor,
            0.001f,
        )
        assertArrayEquals(
            floatArrayOf(0.03f, 0.78f, 0.30f, 1f),
            requireNotNull(LudoPawsTeamIdentityPolicy.resolve("GREEN")).bellyGlColor,
            0.001f,
        )
        assertArrayEquals(
            floatArrayOf(0.12f, 0.62f, 0.98f, 1f),
            requireNotNull(LudoPawsTeamIdentityPolicy.resolve("BLUE")).bellyGlColor,
            0.001f,
        )

        val yellow = requireNotNull(LudoPawsTeamIdentityPolicy.resolve("YELLOW"))
        assertArrayEquals(
            floatArrayOf(1f, 216f / 255f, 27f / 255f, 1f),
            yellow.bellyGlColor,
            0.001f,
        )
        assertArrayEquals(yellow.glColor, yellow.bellyGlColor, 0.001f)
    }

    @Test
    fun `team colors preserve the board's classic palette`() {
        assertEquals(0xFFF1252F.toInt(), LudoPawsTeamIdentityPolicy.resolve("RED")?.argb)
        assertEquals(0xFF00A950.toInt(), LudoPawsTeamIdentityPolicy.resolve("GREEN")?.argb)
        assertEquals(0xFFFFD81B.toInt(), LudoPawsTeamIdentityPolicy.resolve("YELLOW")?.argb)
        assertEquals(0xFF3097D7.toInt(), LudoPawsTeamIdentityPolicy.resolve("BLUE")?.argb)
    }
}
