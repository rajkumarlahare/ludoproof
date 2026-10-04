package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PendingRollSnapshot
import com.ludoproof.game.PlayerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LudoPawsIdleReactionPolicyTest {
    @Test
    fun activeMatchBecomesIdleOnlyAfterThreshold() {
        val state = snapshot()

        assertEquals(
            12_000L,
            LudoPawsIdleReactionPolicy.delayUntilEligibleMillis(
                state = state,
                lastMeaningfulChangeAtMillis = 1_000L,
                nowMillis = 1_000L,
            ),
        )
        assertEquals(
            1L,
            LudoPawsIdleReactionPolicy.delayUntilEligibleMillis(
                state = state,
                lastMeaningfulChangeAtMillis = 1_000L,
                nowMillis = 12_999L,
            ),
        )
        assertEquals(
            0L,
            LudoPawsIdleReactionPolicy.delayUntilEligibleMillis(
                state = state,
                lastMeaningfulChangeAtMillis = 1_000L,
                nowMillis = 13_000L,
            ),
        )
    }

    @Test
    fun waitingAndFinishedMatchesNeverScheduleIdleReaction() {
        assertNull(
            LudoPawsIdleReactionPolicy.delayUntilEligibleMillis(
                state = snapshot(status = "WAITING"),
                lastMeaningfulChangeAtMillis = 0L,
                nowMillis = 50_000L,
            ),
        )
        assertNull(
            LudoPawsIdleReactionPolicy.delayUntilEligibleMillis(
                state = snapshot(status = "FINISHED"),
                lastMeaningfulChangeAtMillis = 0L,
                nowMillis = 50_000L,
            ),
        )
    }

    @Test
    fun identicalRefreshKeepsMeaningfulKeyStable() {
        val first = snapshot()
        val duplicate = first.copy()

        assertEquals(
            LudoPawsIdleReactionPolicy.meaningfulStateKey(first),
            LudoPawsIdleReactionPolicy.meaningfulStateKey(duplicate),
        )
    }

    @Test
    fun authoritativeRollOrTokenChangeResetsMeaningfulKey() {
        val base = snapshot()
        val rolled =
            base.copy(
                randomEventIndex = 3,
                pendingRoll =
                    PendingRollSnapshot(
                        status = "RESOLVED",
                        seat = 0,
                        eventIndex = 2,
                        eventId = null,
                        roundId = null,
                        serverCommitment = null,
                        clientCommitment = null,
                        revealDeadlineAt = null,
                        proofDigest = null,
                        outcome = 4,
                        legalTokenIndexes = setOf(0),
                    ),
            )
        val moved =
            base.copy(
                players =
                    listOf(
                        player("p1", 0, listOf(6, -1, -1, -1)),
                        player("p2", 1),
                    ),
            )

        val baseKey =
            LudoPawsIdleReactionPolicy.meaningfulStateKey(base)
        assertNotEquals(
            baseKey,
            LudoPawsIdleReactionPolicy.meaningfulStateKey(rolled),
        )
        assertNotEquals(
            baseKey,
            LudoPawsIdleReactionPolicy.meaningfulStateKey(moved),
        )
    }

    @Test
    fun teamUpIdleKeyUsesActingSeatNotTeamTurnOwner() {
        val state =
            snapshot(
                actingSeat = 2,
                players =
                    listOf(
                        player("p1", 0),
                        player("p2", 1),
                        player("p3", 2),
                        player("p4", 3),
                    ),
            )

        assertEquals(
            "idle-match:2:2:p3",
            LudoPawsIdleReactionPolicy.idleReactionKey(state),
        )
    }

    private fun snapshot(
        status: String = "ACTIVE",
        actingSeat: Int? = null,
        players: List<PlayerSnapshot> =
            listOf(
                player("p1", 0),
                player("p2", 1),
            ),
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "idle-match",
            status = status,
            hostPlayerId = "p1",
            players = players,
            turnSeat = 0,
            randomEventIndex = 2,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "classic",
            history = emptyList(),
            actingSeat = actingSeat,
        )

    private fun player(
        id: String,
        seat: Int,
        tokens: List<Int> = listOf(-1, -1, -1, -1),
    ): PlayerSnapshot =
        PlayerSnapshot(
            playerId = id,
            displayName = id,
            color =
                listOf(
                    "RED",
                    "GREEN",
                    "YELLOW",
                    "BLUE",
                )[seat],
            seat = seat,
            tokens = tokens,
        )
}
