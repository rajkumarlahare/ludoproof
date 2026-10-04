package com.ludoproof.game.feature.characters.domain.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterPawsAssignmentPolicyTest {
    @Test
    fun `computer mode keeps human preference first and assigns unique cpus`() {
        val assigned =
            StarterPawsAssignmentPolicy.normalize(
                playerCount = 4,
                requestedCharacterIds = emptyList(),
                preferredCharacterId = "sheep",
                computerMode = true,
            )

        assertEquals(
            listOf(
                "sheep",
                "duck",
                "squirrel",
                "hedgehog",
            ),
            assigned,
        )
        assertEquals(4, assigned.distinct().size)
    }

    @Test
    fun `pass and play defaults to unique starter characters`() {
        val assigned =
            StarterPawsAssignmentPolicy.normalize(
                playerCount = 4,
                requestedCharacterIds = emptyList(),
                preferredCharacterId = "duck",
                computerMode = false,
            )

        assertEquals(
            listOf(
                "duck",
                "squirrel",
                "hedgehog",
                "sheep",
            ),
            assigned,
        )
        assertEquals(4, assigned.distinct().size)
    }

    @Test
    fun `selecting an animal owned by another local player swaps slots`() {
        val result =
            StarterPawsAssignmentPolicy.select(
                playerCount = 4,
                currentCharacterIds =
                    listOf(
                        "duck",
                        "squirrel",
                        "hedgehog",
                        "sheep",
                    ),
                selectedSlot = 2,
                requestedCharacterId = "duck",
                computerMode = false,
            )

        assertEquals(
            listOf(
                "hedgehog",
                "squirrel",
                "duck",
                "sheep",
            ),
            result,
        )
        assertEquals(4, result.distinct().size)
    }

    @Test
    fun `invalid character request repairs to safe starter assignment`() {
        val result =
            StarterPawsAssignmentPolicy.select(
                playerCount = 3,
                currentCharacterIds =
                    listOf(
                        "duck",
                        "squirrel",
                        "hedgehog",
                    ),
                selectedSlot = 1,
                requestedCharacterId = "not_a_character",
                computerMode = false,
            )

        assertEquals(
            listOf(
                "duck",
                "squirrel",
                "hedgehog",
            ),
            result,
        )
        assertTrue(
            result.all(
                StarterPawsAssignmentPolicy.starterCharacterIds::contains,
            ),
        )
    }

    @Test
    fun `changing player count remains deterministic and unique`() {
        val twoPlayers =
            StarterPawsAssignmentPolicy.normalize(
                playerCount = 2,
                requestedCharacterIds =
                    listOf(
                        "sheep",
                        "duck",
                        "squirrel",
                        "hedgehog",
                    ),
                preferredCharacterId = "sheep",
                computerMode = false,
            )
        val fourPlayers =
            StarterPawsAssignmentPolicy.normalize(
                playerCount = 4,
                requestedCharacterIds = twoPlayers,
                preferredCharacterId = "sheep",
                computerMode = false,
            )

        assertEquals(
            listOf("sheep", "duck"),
            twoPlayers,
        )
        assertEquals(4, fourPlayers.size)
        assertEquals(4, fourPlayers.distinct().size)
        assertEquals("sheep", fourPlayers.first())
    }

    @Test
    fun `computer selection can only change the human slot`() {
        val current =
            listOf(
                "duck",
                "squirrel",
                "hedgehog",
                "sheep",
            )
        val ignoredCpuChange =
            StarterPawsAssignmentPolicy.select(
                playerCount = 4,
                currentCharacterIds = current,
                selectedSlot = 2,
                requestedCharacterId = "sheep",
                computerMode = true,
            )
        val humanChange =
            StarterPawsAssignmentPolicy.select(
                playerCount = 4,
                currentCharacterIds = current,
                selectedSlot = 0,
                requestedCharacterId = "sheep",
                computerMode = true,
            )

        assertEquals(current, ignoredCpuChange)
        assertEquals("sheep", humanChange.first())
        assertEquals(4, humanChange.distinct().size)
    }

    @Test
    fun `only two to four local players are accepted`() {
        assertThrows(IllegalArgumentException::class.java) {
            StarterPawsAssignmentPolicy.normalize(
                playerCount = 1,
                requestedCharacterIds = emptyList(),
                preferredCharacterId = "duck",
                computerMode = false,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            StarterPawsAssignmentPolicy.normalize(
                playerCount = 5,
                requestedCharacterIds = emptyList(),
                preferredCharacterId = "duck",
                computerMode = false,
            )
        }
        assertFalse(
            StarterPawsAssignmentPolicy.starterCharacterIds.isEmpty(),
        )
    }
}
