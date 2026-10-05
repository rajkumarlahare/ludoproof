package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoTurnAutomationPolicyTest {
    @Test
    fun guaranteesOpeningSixOnlyBeforeTheOpeningRollIsConsumed() {
        val yard = listOf(-1, -1, -1, -1)

        assertTrue(
            LudoTurnAutomationPolicy.shouldGuaranteeOpeningSix(
                tokens = yard,
                openingRollConsumed = false,
            ),
        )
        assertFalse(
            LudoTurnAutomationPolicy.shouldGuaranteeOpeningSix(
                tokens = yard,
                openingRollConsumed = true,
            ),
        )
        assertFalse(
            LudoTurnAutomationPolicy.shouldGuaranteeOpeningSix(
                tokens = listOf(0, -1, -1, -1),
                openingRollConsumed = false,
            ),
        )
    }

    @Test
    fun openingGuaranteeTurnsAnyVerifiedOutcomeIntoSix() {
        assertEquals(
            6,
            LudoTurnAutomationPolicy.effectiveOutcome(
                verifiedOutcome = 2,
                guaranteeOpeningSix = true,
            ),
        )
        assertEquals(
            4,
            LudoTurnAutomationPolicy.effectiveOutcome(
                verifiedOutcome = 4,
                guaranteeOpeningSix = false,
            ),
        )
    }

    @Test
    fun autoMoveExistsOnlyWhenExactlyOneLegalTokenExists() {
        assertNull(
            LudoTurnAutomationPolicy.singleLegalTokenIndex(emptySet()),
        )
        assertEquals(
            2,
            LudoTurnAutomationPolicy.singleLegalTokenIndex(setOf(2)),
        )
        assertNull(
            LudoTurnAutomationPolicy.singleLegalTokenIndex(setOf(0, 2)),
        )
    }
}
