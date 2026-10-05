package com.ludoproof.game.feature.offline

import com.ludoproof.game.LudoPathEncoding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineComputerMovePolicyTest {
    @Test
    fun `cpu scoring uses canonical track to home transition`() {
        assertEquals(
            LudoPathEncoding.FIRST_HOME_LANE_POSITION,
            computerMoveDestination(
                position = LudoPathEncoding.LAST_TRACK_POSITION,
                outcome = 1,
            ),
        )
        assertEquals(
            LudoPathEncoding.HOME_POSITION,
            computerMoveDestination(
                position = LudoPathEncoding.LAST_TRACK_POSITION,
                outcome = 6,
            ),
        )
    }

    @Test
    fun `cpu scoring respects yard and legacy entry semantics`() {
        assertNull(
            computerMoveDestination(
                position = -1,
                outcome = 5,
            ),
        )
        assertEquals(
            0,
            computerMoveDestination(
                position = -1,
                outcome = 6,
            ),
        )
        assertEquals(
            53,
            computerMoveDestination(
                position = LudoPathEncoding.RESERVED_LEGACY_ENTRY_POSITION,
                outcome = 1,
            ),
        )
    }
}
