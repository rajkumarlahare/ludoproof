package com.ludoproof.game.ui.offline.state

import com.ludoproof.game.GameMode
import com.ludoproof.game.feature.offline.data.local.ActiveOfflineCharacterSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCharacterAssignmentPolicyTest {
    @Test
    fun `matching pass and play assignment is preserved`() {
        val active =
            ActiveOfflineCharacterSetup(
                mode = GameMode.PASS_AND_PLAY,
                playerCount = 4,
                preferredColor = "BLUE",
                characterIds =
                    listOf(
                        "duck",
                        "squirrel",
                        "hedgehog",
                        "sheep",
                    ),
            )

        val resolved =
            OfflineCharacterAssignmentPolicy.resolve(
                mode = GameMode.PASS_AND_PLAY,
                playerCount = 4,
                preferredCharacterId = "duck",
                active = active,
            )

        assertEquals(
            active.characterIds,
            resolved,
        )
    }

    @Test
    fun `mismatched saved presentation state is repaired deterministically`() {
        val wrongMode =
            ActiveOfflineCharacterSetup(
                mode = GameMode.COMPUTER,
                playerCount = 2,
                preferredColor = "BLUE",
                characterIds =
                    listOf(
                        "duck",
                        "squirrel",
                    ),
            )

        val resolved =
            OfflineCharacterAssignmentPolicy.resolve(
                mode = GameMode.PASS_AND_PLAY,
                playerCount = 3,
                preferredCharacterId = "hedgehog",
                active = wrongMode,
            )

        assertEquals(
            3,
            resolved.size,
        )
        assertEquals(
            "hedgehog",
            resolved.first(),
        )
        assertEquals(
            resolved.size,
            resolved.toSet().size,
        )
    }

    @Test
    fun `computer resume keeps human preference and unique cpu characters`() {
        val resolved =
            OfflineCharacterAssignmentPolicy.resolve(
                mode = GameMode.COMPUTER,
                playerCount = 4,
                preferredCharacterId = "sheep",
                active = null,
            )

        assertEquals(
            "sheep",
            resolved.first(),
        )
        assertEquals(
            4,
            resolved.size,
        )
        assertEquals(
            resolved.size,
            resolved.toSet().size,
        )
        assertTrue(
            "duck" in resolved,
        )
    }
}
