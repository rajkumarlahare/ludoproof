package com.ludoproof.game

import com.ludoproof.game.feature.characters.data.audio.LudoPawsAudioAssetPlayer
import com.ludoproof.game.feature.characters.domain.audio.LudoPawsProceduralVocal
import org.junit.Assert.assertEquals
import org.junit.Test

class LudoPawsPackagedAudioFallbackTest {
    @Test
    fun packagedFallbackNamesRemainStableAndDesignerReplaceable() {
        val expected =
            mapOf(
                LudoPawsProceduralVocal.DOG_YIP to "lp_vocal_dog_yip",
                LudoPawsProceduralVocal.DOG_WHINE to "lp_vocal_dog_whine",
                LudoPawsProceduralVocal.DOG_RUFF to "lp_vocal_dog_ruff",
                LudoPawsProceduralVocal.GOAT_BLEAT to "lp_vocal_goat_bleat",
                LudoPawsProceduralVocal.GOAT_SOFT_BLEAT to "lp_vocal_goat_soft_bleat",
                LudoPawsProceduralVocal.DUCK_QUACK to "lp_vocal_duck_quack",
                LudoPawsProceduralVocal.DUCK_SOFT_QUACK to "lp_vocal_duck_soft_quack",
                LudoPawsProceduralVocal.CAT_CHIRP to "lp_vocal_cat_chirp",
                LudoPawsProceduralVocal.CAT_MEW to "lp_vocal_cat_mew",
                LudoPawsProceduralVocal.CAT_PURR to "lp_vocal_cat_purr",
                LudoPawsProceduralVocal.CAT_HUFF to "lp_vocal_cat_huff",
            )

        assertEquals(LudoPawsProceduralVocal.entries.toSet(), expected.keys)
        expected.forEach { (preset, resourceName) ->
            assertEquals(
                resourceName,
                LudoPawsAudioAssetPlayer.packagedFallbackResourceName(preset),
            )
        }
    }
}
