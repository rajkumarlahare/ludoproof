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
    fun legacySingleLegalTokenIndexStillRequiresOneToken() {
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

    @Test
    fun autoMoveCollapsesMultiplePawnsWithTheSameDestination() {
        assertEquals(
            0,
            LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                tokens = listOf(20, 20, -1, -1),
                roll = 3,
                legalTokenIndexes = setOf(0, 1),
            ),
        )
    }

    @Test
    fun autoMoveCollapsesMultipleYardPawnsEnteringTheSameStartCell() {
        assertEquals(
            0,
            LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                tokens = listOf(-1, -1, 18, 30),
                roll = 6,
                legalTokenIndexes = setOf(0, 1),
            ),
        )
    }

    @Test
    fun autoMoveCollapsesAllFourPawnsWhenTheyShareOneDestination() {
        assertEquals(
            0,
            LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                tokens = listOf(14, 14, 14, 14),
                roll = 5,
                legalTokenIndexes = setOf(0, 1, 2, 3),
            ),
        )
    }

    @Test
    fun autoMoveRemainsManualWhenLegalPawnsHaveDifferentDestinations() {
        assertNull(
            LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                tokens = listOf(20, 25, -1, -1),
                roll = 3,
                legalTokenIndexes = setOf(0, 1),
            ),
        )
    }

    @Test
    fun autoMoveFailsClosedForAnInconsistentLegalTokenSet() {
        assertNull(
            LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                tokens = listOf(20, 57, -1, -1),
                roll = 3,
                legalTokenIndexes = setOf(0, 1),
            ),
        )
    }

    @Test
    fun autoMoveUsesCanonicalDestinationAcrossLegacyEntryEncoding() {
        assertEquals(
            0,
            LudoTurnAutomationPolicy.singleAutomaticTokenIndex(
                tokens = listOf(51, 52, -1, -1),
                roll = 5,
                legalTokenIndexes = setOf(0, 1),
            ),
        )
    }
}
