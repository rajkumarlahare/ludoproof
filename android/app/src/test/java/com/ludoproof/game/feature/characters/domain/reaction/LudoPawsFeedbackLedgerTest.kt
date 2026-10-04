package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoPawsFeedbackLedgerTest {
    @Test
    fun stableSideEffectKeyIsAcceptedExactlyOncePerMatch() {
        val ledger = LudoPawsFeedbackLedger()

        assertTrue(
            ledger.once(
                matchId = "match-a",
                key = "MOVE:7",
            ),
        )
        assertFalse(
            ledger.once(
                matchId = "match-a",
                key = "MOVE:7",
            ),
        )
    }

    @Test
    fun capturePairPlaysOnceAndReplayIsFullySuppressed() {
        val ledger = LudoPawsFeedbackLedger()
        val captureMade =
            reaction(
                playerId = "attacker",
                seat = 0,
                cue = VoiceCue.CAPTURE,
                type = GameMomentType.CAPTURE_MADE,
                key = "match-a:12:CAPTURE_MADE:attacker:0",
            )
        val captured =
            reaction(
                playerId = "victim",
                seat = 1,
                cue = VoiceCue.CAPTURED,
                type = GameMomentType.TOKEN_CAPTURED,
                key = "match-a:12:TOKEN_CAPTURED:victim:2",
            )
        val event = listOf(captureMade, captured)

        assertEquals(event, ledger.filterReactions(event))
        assertTrue(ledger.filterReactions(event).isEmpty())
    }

    @Test
    fun newMatchGetsFreshExactlyOnceNamespace() {
        val ledger = LudoPawsFeedbackLedger()

        assertTrue(ledger.once("match-a", "ROLL:0"))
        assertFalse(ledger.once("match-a", "ROLL:0"))
        assertTrue(ledger.once("match-b", "ROLL:0"))
        assertFalse(ledger.once("match-b", "ROLL:0"))
    }

    @Test
    fun retainedKeysStayBoundedWithoutDroppingRecentReplayProtection() {
        val ledger = LudoPawsFeedbackLedger(maxKeys = 16)

        repeat(24) { index ->
            assertTrue(
                ledger.once(
                    matchId = "match-a",
                    key = "MOVE:$index",
                ),
            )
        }

        assertEquals(16, ledger.retainedKeyCountForTests())
        assertFalse(ledger.once("match-a", "MOVE:23"))
    }

    private fun reaction(
        playerId: String,
        seat: Int,
        cue: VoiceCue,
        type: GameMomentType,
        key: String,
    ): LudoPawsReaction =
        LudoPawsReaction(
            playerId = playerId,
            seat = seat,
            tokenIndex = if (seat == 0) 0 else 2,
            voiceCue = cue,
            animationCue =
                if (cue == VoiceCue.CAPTURE) {
                    AnimationCue.CAPTURE
                } else {
                    AnimationCue.CAPTURED
                },
            priority = if (cue == VoiceCue.CAPTURE) 88 else 86,
            matchId = "match-a",
            eventIndex = 12,
            momentType = type,
            reactionKey = key,
        )
}
