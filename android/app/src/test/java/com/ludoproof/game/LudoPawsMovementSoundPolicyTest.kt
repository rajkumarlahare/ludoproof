package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Test

class LudoPawsMovementSoundPolicyTest {
    @Test
    fun yardExitUsesDedicatedCueWithoutStackingFirstStepSound() {
        assertEquals(
            listOf(LudoPawsMovementSoundCue.YARD_EXIT),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "BLUE",
                fromPosition = -1,
                step = 1,
            ),
        )
    }

    @Test
    fun ordinaryStepUsesOnlyMovementCue() {
        assertEquals(
            listOf(LudoPawsMovementSoundCue.STEP),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "BLUE",
                fromPosition = 2,
                step = 1,
            ),
        )
    }

    @Test
    fun safeStarAddsReliefToTheArrivalStep() {
        assertEquals(
            listOf(
                LudoPawsMovementSoundCue.STEP,
                LudoPawsMovementSoundCue.SAFE_RELIEF,
            ),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "RED",
                fromPosition = 7,
                step = 1,
            ),
        )
    }

    @Test
    fun yardExitDoesNotAlsoTriggerSafeReliefOnItsSafeStartCell() {
        assertEquals(
            listOf(LudoPawsMovementSoundCue.YARD_EXIT),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "RED",
                fromPosition = -1,
                step = 1,
            ),
        )
    }

    @Test
    fun homeLaneCuePlaysOnlyOnFirstHomeLaneCell() {
        assertEquals(
            listOf(
                LudoPawsMovementSoundCue.STEP,
                LudoPawsMovementSoundCue.HOME_LANE,
            ),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "RED",
                fromPosition = 50,
                step = 1,
            ),
        )
        assertEquals(
            listOf(LudoPawsMovementSoundCue.STEP),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "RED",
                fromPosition = 52,
                step = 1,
            ),
        )
    }

    @Test
    fun homeCuePlaysOnFinalCenterArrival() {
        assertEquals(
            listOf(
                LudoPawsMovementSoundCue.STEP,
                LudoPawsMovementSoundCue.HOME,
            ),
            LudoPawsMovementSoundPolicy.cuesForVisualStep(
                color = "RED",
                fromPosition = 56,
                step = 1,
            ),
        )
    }

    @Test
    fun everyConfiguredSafeGlobalCellIsRecognized() {
        val expectedByColor =
            mapOf(
                "RED" to setOf(0, 8, 13, 21, 26, 34, 39, 47),
                "GREEN" to setOf(0, 8, 13, 21, 26, 34, 39, 47),
                "YELLOW" to setOf(0, 8, 13, 21, 26, 34, 39, 47),
                "BLUE" to setOf(0, 8, 13, 21, 26, 34, 39, 47),
            )

        expectedByColor.forEach { (color, globalSafeCells) ->
            val start =
                when (color) {
                    "RED" -> 0
                    "GREEN" -> 13
                    "YELLOW" -> 26
                    "BLUE" -> 39
                    else -> error("unknown color")
                }
            globalSafeCells.forEach { global ->
                val position = (global - start + 52) % 52
                assertEquals(
                    listOf(
                        LudoPawsMovementSoundCue.STEP,
                        LudoPawsMovementSoundCue.SAFE_RELIEF,
                    ),
                    LudoPawsMovementSoundPolicy.cuesForVisualStep(
                        color = color,
                        fromPosition = position - 1,
                        step = 1,
                    ),
                )
            }
        }
    }
}
