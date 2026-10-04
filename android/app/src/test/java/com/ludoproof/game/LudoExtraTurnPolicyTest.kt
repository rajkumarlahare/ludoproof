package com.ludoproof.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoExtraTurnPolicyTest {
    @Test
    fun reachingHomeGrantsExtraTurn() {
        assertTrue(
            LudoExtraTurnPolicy.grantsExtraTurn(
                roll = 1,
                captures = 0,
                destination = LudoPathEncoding.HOME_POSITION,
            ),
        )
    }

    @Test
    fun existingSixAndCaptureRulesStayUnchanged() {
        assertTrue(
            LudoExtraTurnPolicy.grantsExtraTurn(
                roll = 6,
                captures = 0,
                destination = 10,
            ),
        )
        assertTrue(
            LudoExtraTurnPolicy.grantsExtraTurn(
                roll = 2,
                captures = 1,
                destination = 10,
            ),
        )
        assertFalse(
            LudoExtraTurnPolicy.grantsExtraTurn(
                roll = 2,
                captures = 0,
                destination = 10,
            ),
        )
    }
}
