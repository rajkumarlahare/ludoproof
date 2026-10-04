package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoPathEncodingTest {
    @Test
    fun `track exits directly into first home lane`() {
        assertEquals(52, LudoPathEncoding.destinationForRoll(50, 1))
        assertEquals(52, LudoPathEncoding.destinationForRoll(49, 2))
        assertEquals(57, LudoPathEncoding.destinationForRoll(50, 6))
        assertEquals(57, LudoPathEncoding.destinationForRoll(56, 1))
        assertNull(LudoPathEncoding.destinationForRoll(56, 2))
    }

    @Test
    fun `legacy position 51 is treated as first home lane`() {
        assertEquals(52, LudoPathEncoding.normalizeLegacyEntry(51))
        assertEquals(53, LudoPathEncoding.destinationForRoll(51, 1))
        assertEquals(1, LudoPathEncoding.visualStepCount(50, 52))
        assertEquals(6, LudoPathEncoding.visualStepCount(50, 57))
        assertEquals(52, LudoPathEncoding.positionAtVisualStep(50, 1))
        assertEquals(57, LudoPathEncoding.positionAtVisualStep(50, 6))
    }
}
