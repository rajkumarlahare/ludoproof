package com.ludoproof.game

import org.junit.Assert.assertEquals
import org.junit.Test

class MatchSpecV3Test {
    @Test
    fun localClassicSpecCanExplicitlyUseRulesetV3() {
        val spec =
            MatchSpec.classic(
                mode = GameMode.COMPUTER,
                playerCount = 2,
                rulesetId = OfflineLudoV3Binding.RULESET_ID,
            )

        assertEquals(ClassicRuleset.V3_ID, spec.rulesetId)
        assertEquals(OfflineLudoV3Binding.RULESET_ID, spec.rulesetId)
    }
}
