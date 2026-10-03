package com.ludoproof.game.core.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsAssetContractTest {
    @Test
    fun `starter paths are deterministic`() {
        assertEquals(
            "ludo_paws/characters/duck/pawn.webp",
            LudoPawsAssetContract.pawnPath("duck"),
        )
        assertEquals(
            "ludo_paws/characters/duck/portrait.webp",
            LudoPawsAssetContract.portraitPath("duck"),
        )
        assertEquals(
            "ludo_paws/characters/duck/full_body.webp",
            LudoPawsAssetContract.fullBodyPath("duck"),
        )
        assertEquals(
            "ludo_paws/characters/duck/expressions/happy.webp",
            LudoPawsAssetContract.expressionPath(
                "duck",
                "happy",
            ),
        )
        assertEquals(
            "ludo_paws/audio/duck/capture_01.ogg",
            LudoPawsAssetContract.voicePath(
                "duck",
                "capture",
                1,
            ),
        )
    }

    @Test
    fun `unsafe ids and paths are rejected`() {
        assertTrue(
            LudoPawsAssetContract.isSafeId("hedgehog"),
        )
        assertFalse(
            LudoPawsAssetContract.isSafeId("../hedgehog"),
        )
        assertFalse(
            LudoPawsAssetContract.isSafeId("Hedgehog"),
        )
        assertTrue(
            LudoPawsAssetContract.isSafeRuntimeAssetPath(
                "ludo_paws/characters/sheep/pawn.webp",
            ),
        )
        assertFalse(
            LudoPawsAssetContract.isSafeRuntimeAssetPath(
                "ludo_paws/../secrets.webp",
            ),
        )
        assertFalse(
            LudoPawsAssetContract.isSafeRuntimeAssetPath(
                "/ludo_paws/characters/sheep/pawn.webp",
            ),
        )
    }

    @Test
    fun `downsampling follows max dimension with powers of two`() {
        assertEquals(
            1,
            LudoPawsAssetContract
                .calculatePowerOfTwoSampleSize(
                    width = 256,
                    height = 256,
                    targetMaxDimensionPx = 256,
                ),
        )
        assertEquals(
            4,
            LudoPawsAssetContract
                .calculatePowerOfTwoSampleSize(
                    width = 1024,
                    height = 512,
                    targetMaxDimensionPx = 256,
                ),
        )
        assertEquals(
            4,
            LudoPawsAssetContract
                .calculatePowerOfTwoSampleSize(
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
            LudoPawsAssetContract
                .estimatedArgb8888Bytes(
                    width = 512,
                    height = 512,
                    sampleSize = 2,
                ),
        )
        assertEquals(
            3 * 1024 * 1024,
            LudoPawsAssetContract
                .SINGLE_BITMAP_MAX_DECODED_BYTES,
        )
        assertEquals(
            12 * 1024 * 1024,
            LudoPawsAssetContract
                .ACTIVE_CHARACTER_ART_MAX_DECODED_BYTES,
        )
    }
}
