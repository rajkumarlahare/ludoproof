package com.ludoproof.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ludoproof.game.feature.offline.domain.ai.LudoPawsComputerMovePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFullMatchInstrumentedTest {
    @Test
    fun twoAndFourPlayerMatchesFinishThroughProductionEngineAndCpuPolicy() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        for (playerCount in listOf(2, 4)) {
            runCompleteMatch(
                context = context,
                playerCount = playerCount,
            )
        }
    }

    private fun runCompleteMatch(
        context: Context,
        playerCount: Int,
    ) {
        val session =
            LocalMatchSession(
                context = context,
                mode = GameMode.COMPUTER,
            )
        session.clear()

        try {
            var state =
                session.start(
                    playerCount = playerCount,
                    preferredColor = "RED",
                )
            var transitions = 0

            while (
                state.status == "ACTIVE" &&
                transitions < MAX_TRANSITIONS
            ) {
                state =
                    if (state.pendingRoll == null) {
                        session.roll()
                    } else {
                        val pending = requireNotNull(state.pendingRoll)
                        val outcome = requireNotNull(pending.outcome)
                        val active = state.players[state.turnSeat]
                        val tokenIndex =
                            LudoPawsComputerMovePolicy.chooseToken(
                                state = state,
                                player = active,
                                legalTokenIndexes = pending.legalTokenIndexes,
                                outcome = outcome,
                            )
                        assertNotNull(
                            "CPU policy must choose from every authoritative legal set",
                            tokenIndex,
                        )
                        session.move(requireNotNull(tokenIndex))
                    }

                // The live LocalMatchSession already runs this policy. Calling it
                // explicitly here makes a future accidental bypass visible in the
                // end-to-end contract as well.
                OfflineMatchInvariantPolicy.requireValid(state)
                transitions += 1
            }

            assertTrue(
                "$playerCount-player local match exceeded $MAX_TRANSITIONS transitions",
                transitions < MAX_TRANSITIONS,
            )
            assertEquals(
                "$playerCount-player local match must finish",
                "FINISHED",
                state.status,
            )

            val finishedPlayers =
                state.players.count {
                    it.tokens.all { position ->
                        position == LudoPathEncoding.HOME_POSITION
                    }
                }
            assertTrue(
                "$playerCount-player match must finish only after all but the last player are placed",
                finishedPlayers >= playerCount - 1,
            )

            val winnerId = state.winnerPlayerId
            assertNotNull(
                "$playerCount-player finished match must expose a winner",
                winnerId,
            )
            val winner =
                state.players.firstOrNull {
                    it.playerId == winnerId
                }
            assertNotNull(
                "$playerCount-player winner must still be seated",
                winner,
            )
            assertTrue(
                "$playerCount-player winner must have four exact-home tokens",
                requireNotNull(winner).tokens.all {
                    it == LudoPathEncoding.HOME_POSITION
                },
            )
        } finally {
            session.clear()
        }
    }

    private companion object {
        const val MAX_TRANSITIONS = 6_000
    }
}
