package com.ludoproof.game.feature.online.domain

import com.ludoproof.game.MatchSnapshot
import com.ludoproof.game.PlayerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineLudoPawsCharacterPolicyTest {
    @Test
    fun `maps validated remote characters by seat`() {
        val state =
            match(
                listOf(
                    player(
                        id = "p2",
                        seat = 2,
                        characterId = "hedgehog",
                    ),
                    player(
                        id = "p0",
                        seat = 0,
                        characterId = "sheep",
                    ),
                ),
            )

        assertEquals(
            listOf(
                "sheep",
                "duck",
                "hedgehog",
            ),
            OnlineLudoPawsCharacterPolicy
                .characterIdsBySeat(state),
        )
    }

    @Test
    fun `missing or unknown character falls back to starter default`() {
        val state =
            match(
                listOf(
                    player(
                        id = "p0",
                        seat = 0,
                        characterId = null,
                    ),
                    player(
                        id = "p1",
                        seat = 1,
                        characterId = "dragon",
                    ),
                ),
            )

        assertEquals(
            listOf("duck", "duck"),
            OnlineLudoPawsCharacterPolicy
                .characterIdsBySeat(state),
        )
    }

    @Test
    fun `empty snapshot produces no cosmetic seats`() {
        assertEquals(
            emptyList<String>(),
            OnlineLudoPawsCharacterPolicy
                .characterIdsBySeat(
                    match(emptyList()),
                ),
        )
    }

    private fun match(
        players: List<PlayerSnapshot>,
    ): MatchSnapshot =
        MatchSnapshot(
            matchId = "LPPAWS1234",
            status = "ACTIVE",
            hostPlayerId =
                players.firstOrNull()
                    ?.playerId
                    .orEmpty(),
            players = players,
            turnSeat = 0,
            randomEventIndex = 0,
            pendingRoll = null,
            winnerPlayerId = null,
            rulesetId = "ludoproof-standard-v1",
            history = emptyList(),
        )

    private fun player(
        id: String,
        seat: Int,
        characterId: String?,
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
                )[seat.coerceIn(0, 3)],
            seat = seat,
            tokens = listOf(-1, -1, -1, -1),
            characterId = characterId,
        )
}
