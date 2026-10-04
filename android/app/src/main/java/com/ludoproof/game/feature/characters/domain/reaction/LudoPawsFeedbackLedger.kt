package com.ludoproof.game.feature.characters.domain.reaction

/**
 * Bounded presentation-only ledger for sounds/haptics that must fire at most
 * once for a stable authoritative/local event while one match is active.
 *
 * The ledger is deliberately outside gameplay/proof code. It never changes
 * dice, moves, captures, turn ownership, or any authoritative state.
 */
class LudoPawsFeedbackLedger(
    private val maxKeys: Int = 512,
) {
    private var activeMatchId: String? = null
    private val consumedKeys = LinkedHashSet<String>()

    init {
        require(maxKeys >= 16) {
            "feedback ledger must retain at least 16 keys"
        }
    }

    fun once(
        matchId: String,
        key: String,
    ): Boolean {
        val safeMatchId = matchId.trim()
        val safeKey = key.trim()
        if (safeMatchId.isEmpty() || safeKey.isEmpty()) {
            return false
        }

        if (activeMatchId != safeMatchId) {
            activeMatchId = safeMatchId
            consumedKeys.clear()
        }

        val accepted =
            consumedKeys.add(
                "$safeMatchId:$safeKey",
            )
        if (!accepted) {
            return false
        }

        trim()
        return true
    }

    fun filterReactions(
        reactions: List<LudoPawsReaction>,
    ): List<LudoPawsReaction> =
        reactions.filter {
                reaction ->
            val key =
                reaction.reactionKey
                    .takeIf(String::isNotBlank)
                    ?: fallbackReactionKey(reaction)
            once(
                matchId = reaction.matchId,
                key = "REACTION:$key",
            )
        }

    fun clear() {
        activeMatchId = null
        consumedKeys.clear()
    }

    internal fun retainedKeyCountForTests(): Int =
        consumedKeys.size

    private fun trim() {
        while (consumedKeys.size > maxKeys) {
            val iterator = consumedKeys.iterator()
            if (!iterator.hasNext()) {
                return
            }
            iterator.next()
            iterator.remove()
        }
    }

    private fun fallbackReactionKey(
        reaction: LudoPawsReaction,
    ): String =
        listOf(
            reaction.eventIndex.toString(),
            reaction.momentType?.name
                ?: reaction.voiceCue.name,
            reaction.playerId,
            reaction.tokenIndex
                ?.toString()
                .orEmpty(),
        ).joinToString(":")
}
