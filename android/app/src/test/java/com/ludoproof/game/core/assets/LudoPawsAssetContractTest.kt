package com.ludoproof.game.core.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsAssetContractTest {
    @Test
    fun `production paths are deterministic`() {
        assertEquals(
            "ludo_paws/characters/dog/pawn.webp",
            LudoPawsAssetContract.pawnPath("dog"),
        )
        assertEquals(
            "ludo_paws/characters/dog/portrait.webp",
            LudoPawsAssetContract.portraitPath("dog"),
        )
        assertEquals(
            "ludo_paws/characters/dog/full_body.webp",
            LudoPawsAssetContract.fullBodyPath("dog"),
        )
        assertEquals(
            "ludo_paws/characters/dog/expressions/happy.webp",
            LudoPawsAssetContract.expressionPath(
                "dog",
                "happy",
            ),
        )
        assertEquals(
            "ludo_paws/audio/dog/capture_01.ogg",
            LudoPawsAssetContract.voicePath(
                "dog",
                "capture",
                1,
            ),
        )
    }

    @Test
    fun `unsafe ids and paths are rejected`() {
        assertTrue(LudoPawsAssetContract.isSafeId("goat"))
        assertFalse(LudoPawsAssetContract.isSafeId("../goat"))
        assertFalse(LudoPawsAssetContract.isSafeId("Goat"))
        assertTrue(
            LudoPawsAssetContract.isSafeRuntimeAssetPath(
                "ludo_paws/characters/cat/pawn.webp",
            ),
        )
        assertFalse(
            LudoPawsAssetContract.isSafeRuntimeAssetPath(
                "ludo_paws/../secrets.webp",
            ),
        )
        assertFalse(
            LudoPawsAssetContract.isSafeRuntimeAssetPath(
                "/ludo_paws/characters/cat/pawn.webp",
            ),
        )
    }

    @Test
    fun `downsampling follows max dimension with powers of two`() {
        assertEquals(
            1,
            LudoPawsAssetContract.calculatePowerOfTwoSampleSize(
                width = 256,
                height = 256,
                targetMaxDimensionPx = 256,
            ),
        )
        assertEquals(
            4,
            LudoPawsAssetContract.calculatePowerOfTwoSampleSize(
                width = 1024,
                height = 512,
                targetMaxDimensionPx = 256,
            ),
        )
        assertEquals(
            4,
            LudoPawsAssetContract.calculatePowerOfTwoSampleSize(
                width = 512,
                height = 1024,
                targetMaxDimensionPx = 256,
            ),
        )
    }

    @Test
    fun `decoded argb estimate is bounded and predictable`() {
        assertEquals(
            262_144L,
            LudoPawsAssetContract.estimatedArgb8888Bytes(
                width = 512,
                height = 512,
                sampleSize = 2,
            ),
        )
        assertEquals(
            3 * 1024 * 1024,
            LudoPawsAssetContract.SINGLE_BITMAP_MAX_DECODED_BYTES,
        )
        assertEquals(
            12 * 1024 * 1024,
            LudoPawsAssetContract.ACTIVE_CHARACTER_ART_MAX_DECODED_BYTES,
        )
    }
}
