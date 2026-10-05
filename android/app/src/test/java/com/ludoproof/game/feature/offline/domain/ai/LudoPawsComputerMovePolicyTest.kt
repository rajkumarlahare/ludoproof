package com.ludoproof.game.feature.offline.domain.ai

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PlayerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsComputerMovePolicyTest {
    @Test
    fun `canonical destination handles yard exit and exact home`() {
        assertEquals(
            0,
            LudoPawsComputerMovePolicy.destinationForRoll(-1, 6),
        )
        assertEquals(
            57,
            LudoPawsComputerMovePolicy.destinationForRoll(56, 1),
        )
        assertNull(
            LudoPawsComputerMovePolicy.destinationForRoll(56, 2),
        )
    }

    @Test
    fun `exact home beats ordinary progress`() {
        val cpu =
            player(
                id = "cpu",
                color = "RED",
                seat = 0,
                tokens = listOf(56, 20, -1, -1),
            )
        val state =
            state(
                cpu,
                player("human", "GREEN", 1),
            )

        assertEquals(
            0,
            LudoPawsComputerMovePolicy.chooseToken(
                state = state,
                player = cpu,
                legalTokenIndexes = setOf(0, 1),
                outcome = 1,
            ),
        )
    }

    @Test
    fun `capture outranks plain forward progress`() {
        val cpu =
            player(
                id = "cpu",
                color = "RED",
                seat = 0,
                tokens = listOf(7, 20, -1, -1),
            )
        // GREEN relative position 49 maps to global cell 10. RED token 0 lands
        // on global 10 after rolling 3, so this candidate captures it.
        val opponent =
            player(
                id = "human",
                color = "GREEN",
                seat = 1,
                tokens = listOf(49, -1, -1, -1),
            )
        val state = state(cpu, opponent)

        val evaluation =
            LudoPawsComputerMovePolicy.evaluate(
                state = state,
                player = cpu,
                tokenIndex = 0,
                outcome = 3,
            )
        assertEquals(1, evaluation?.captures)
        assertEquals(
            0,
            LudoPawsComputerMovePolicy.chooseToken(
                state = state,
                player = cpu,
                legalTokenIndexes = setOf(0, 1),
                outcome = 3,
            ),
        )
    }

    @Test
    fun `safe landing outranks nearby exposed progress`() {
        val cpu =
            player(
                id = "cpu",
                color = "RED",
                seat = 0,
                tokens = listOf(6, 10, -1, -1),
            )
        val state =
            state(
                cpu,
                player("human", "GREEN", 1),
            )

        val safe =
            LudoPawsComputerMovePolicy.evaluate(
                state = state,
                player = cpu,
                tokenIndex = 0,
                outcome = 2,
            )
        assertTrue(safe?.destinationSafe == true)
        assertEquals(8, safe?.destination)
        assertEquals(
            0,
            LudoPawsComputerMovePolicy.chooseToken(
                state = state,
                player = cpu,
                legalTokenIndexes = setOf(0, 1),
                outcome = 2,
            ),
        )
    }

    @Test
    fun `rescuing threatened token can beat raw progress`() {
        val cpu =
            player(
                id = "cpu",
                color = "RED",
                seat = 0,
                tokens = listOf(5, 40, -1, -1),
            )
        // GREEN at relative 42 can reach RED global cell 5 with a roll of 2.
        // Rolling 6 moves RED token 0 to global 11, outside that 1..6 threat.
        val opponent =
            player(
                id = "human",
                color = "GREEN",
                seat = 1,
                tokens = listOf(42, -1, -1, -1),
            )
        val state = state(cpu, opponent)

        val rescue =
            LudoPawsComputerMovePolicy.evaluate(
                state = state,
                player = cpu,
                tokenIndex = 0,
                outcome = 6,
            )
        assertTrue(rescue?.rescuedFromThreat == true)
        assertEquals(
            0,
            LudoPawsComputerMovePolicy.chooseToken(
                state = state,
                player = cpu,
                legalTokenIndexes = setOf(0, 1),
                outcome = 6,
            ),
        )
    }

    @Test
    fun `risk penalty avoids reachable capture square when alternative is clear`() {
        val cpu =
            player(
                id = "cpu",
                color = "RED",
                seat = 0,
                tokens = listOf(4, 8, -1, -1),
            )
        // GREEN relative 40 can reach globals 2..7 on rolls 1..6. RED token 0
        // would land on global 5; token 1 lands on global 9 and is clear.
        val opponent =
            player(
                id = "human",
                color = "GREEN",
                seat = 1,
                tokens = listOf(40, -1, -1, -1),
            )
        val state = state(cpu, opponent)

        val risky =
            LudoPawsComputerMovePolicy.evaluate(
                state = state,
                player = cpu,
                tokenIndex = 0,
                outcome = 1,
            )
        val clear =
            LudoPawsComputerMovePolicy.evaluate(
                state = state,
                player = cpu,
                tokenIndex = 1,
                outcome = 1,
            )
        assertTrue((risky?.threatRoutes ?: 0) > 0)
        assertEquals(0, clear?.threatRoutes)
        assertFalse(clear?.destinationSafe == true)
        assertEquals(
            1,
            LudoPawsComputerMovePolicy.chooseToken(
                state = state,
                player = cpu,
                legalTokenIndexes = setOf(0, 1),
                outcome = 1,
            ),
        )
    }

    @Test
    fun `equal candidates use stable lower token index`() {
        val cpu =
            player(
                id = "cpu",
                color = "RED",
                seat = 0,
                tokens = listOf(20, 20, -1, -1),
            )
        val state =
            state(
                cpu,
                player("human", "GREEN", 1),
            )

        assertEquals(
            0,
            LudoPawsComputerMovePolicy.chooseToken(
                state = state,
                player = cpu,
                legalTokenIndexes = setOf(0, 1),
                outcome = 2,
            ),
        )
    }

    private fun state(
        vararg players: PlayerSnapshot,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "cpu-policy-match",
            status = "ACTIVE",
            hostPlayerId = players.first().playerId,
            matchMode = "ONLINE",
            players = players.toList(),
            turnSeat = 0,
            randomEventIndex = 0,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "ludoproof-standard-v3",
            history = emptyList(),
        )

    private fun player(
        id: String,
        color: String,
        seat: Int,
        tokens: List<Int> = listOf(-1, -1, -1, -1),
    ): PlayerSnapshot =
        PlayerSnapshot(
            playerId = id,
            displayName = id,
            color = color,
            seat = seat,
            tokens = tokens,
        )
}
