package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.VoiceCue

/**
 * Stateful presentation scheduler for Ludo Paws reactions.
 *
 * This class never touches authoritative game state. It only decides which
 * already-derived presentation reactions are allowed to play, in what order,
 * and whether a high-priority reaction may interrupt a lower-priority one.
 */
class LudoPawsReactionDirector(
    private val playbackWindowMs: Long = 900L,
    private val maxConsumedKeys: Int = 512,
    private val maxQueuedBatches: Int = 12,
) {
    private data class ReactionBatch(
        val matchId: String,
        val eventIndex: Int,
        val reactions: List<LudoPawsReaction>,
    ) {
        val priority: Int =
            reactions.maxOfOrNull(LudoPawsReaction::priority)
                ?: Int.MIN_VALUE
    }

    data class Decision(
        val reactions: List<LudoPawsReaction> = emptyList(),
        val interrupted: Boolean = false,
        val queuedBatchCount: Int = 0,
    )

    private var activeMatchId: String? = null
    private var activeUntilMillis: Long = Long.MIN_VALUE
    private var activePriority: Int = Int.MIN_VALUE
    private val consumedKeys = LinkedHashSet<String>()
    private val queuedBatches = mutableListOf<ReactionBatch>()
    private val lastCueAtMillis = mutableMapOf<String, Long>()

    fun submit(
        reactions: List<LudoPawsReaction>,
        nowMillis: Long,
    ): Decision {
        val incomingMatchId =
            reactions
                .firstOrNull()
                ?.matchId
                ?.takeIf(String::isNotBlank)
        if (
            incomingMatchId != null &&
            incomingMatchId != activeMatchId
        ) {
            resetForMatch(incomingMatchId)
        }

        expireActiveIfNeeded(nowMillis)

        val accepted =
            reactions
                .asSequence()
                .sortedWith(
                    compareByDescending<LudoPawsReaction> {
                        it.priority
                    }.thenBy {
                        it.eventIndex
                    }.thenBy {
                        it.playerId
                    }.thenBy {
                        it.tokenIndex ?: -1
                    },
                )
                .filter(::rememberOnce)
                .filter {
                    cooldownAllows(
                        reaction = it,
                        nowMillis = nowMillis,
                    )
                }
                .toList()

        enqueue(
            batchesFor(accepted),
        )

        val active =
            nowMillis < activeUntilMillis &&
                activePriority != Int.MIN_VALUE
        val bestQueued =
            queuedBatches.firstOrNull()

        if (
            active &&
            bestQueued != null &&
            shouldInterrupt(
                incomingPriority = bestQueued.priority,
                activePriority = activePriority,
            )
        ) {
            queuedBatches.removeAt(0)
            start(
                batch = bestQueued,
                nowMillis = nowMillis,
            )
            return Decision(
                reactions = bestQueued.reactions,
                interrupted = true,
                queuedBatchCount = queuedBatches.size,
            )
        }

        if (active) {
            return Decision(
                queuedBatchCount = queuedBatches.size,
            )
        }

        val next =
            queuedBatches.firstOrNull()
                ?: return Decision()
        queuedBatches.removeAt(0)
        start(
            batch = next,
            nowMillis = nowMillis,
        )
        return Decision(
            reactions = next.reactions,
            queuedBatchCount = queuedBatches.size,
        )
    }

    fun clear() {
        activeMatchId = null
        activeUntilMillis = Long.MIN_VALUE
        activePriority = Int.MIN_VALUE
        consumedKeys.clear()
        queuedBatches.clear()
        lastCueAtMillis.clear()
    }

    private fun resetForMatch(
        matchId: String,
    ) {
        activeMatchId = matchId
        activeUntilMillis = Long.MIN_VALUE
        activePriority = Int.MIN_VALUE
        consumedKeys.clear()
        queuedBatches.clear()
        lastCueAtMillis.clear()
    }

    private fun expireActiveIfNeeded(
        nowMillis: Long,
    ) {
        if (nowMillis >= activeUntilMillis) {
            activeUntilMillis = Long.MIN_VALUE
            activePriority = Int.MIN_VALUE
        }
    }

    private fun rememberOnce(
        reaction: LudoPawsReaction,
    ): Boolean {
        val key =
            reaction.stableKey()
        if (!consumedKeys.add(key)) {
            return false
        }
        while (consumedKeys.size > maxConsumedKeys) {
            val iterator = consumedKeys.iterator()
            if (!iterator.hasNext()) {
                break
            }
            iterator.next()
            iterator.remove()
        }
        return true
    }

    private fun cooldownAllows(
        reaction: LudoPawsReaction,
        nowMillis: Long,
    ): Boolean {
        val cooldown =
            cooldownMillis(reaction.voiceCue)
        if (cooldown <= 0L) {
            return true
        }
        val key =
            reaction.playerId +
                ":" +
                reaction.voiceCue.name
        val previous =
            lastCueAtMillis[key]
        if (
            previous != null &&
            nowMillis - previous < cooldown
        ) {
            return false
        }
        lastCueAtMillis[key] = nowMillis
        return true
    }

    private fun cooldownMillis(
        cue: VoiceCue,
    ): Long =
        when (cue) {
            VoiceCue.IDLE -> 12_000L
            VoiceCue.NERVOUS -> 8_000L
            VoiceCue.FRUSTRATED -> 3_500L
            VoiceCue.SIX -> 750L
            VoiceCue.SAFE -> 900L
            VoiceCue.CAPTURE,
            VoiceCue.CAPTURED,
            VoiceCue.HOME,
            VoiceCue.THIRD_SIX,
            VoiceCue.VICTORY,
            VoiceCue.DEFEAT,
            -> 0L
        }

    private fun batchesFor(
        reactions: List<LudoPawsReaction>,
    ): List<ReactionBatch> =
        reactions
            .groupBy {
                it.matchId +
                    ":" +
                    it.eventIndex
            }
            .values
            .mapNotNull {
                    grouped ->
                val first =
                    grouped.firstOrNull()
                        ?: return@mapNotNull null
                ReactionBatch(
                    matchId = first.matchId,
                    eventIndex = first.eventIndex,
                    reactions =
                        grouped.sortedByDescending(
                            LudoPawsReaction::priority,
                        ),
                )
            }
            .sortedWith(
                compareByDescending<ReactionBatch> {
                    it.priority
                }.thenBy {
                    it.eventIndex
                },
            )

    private fun enqueue(
        incoming: List<ReactionBatch>,
    ) {
        if (incoming.isEmpty()) {
            return
        }
        queuedBatches += incoming
        queuedBatches.sortWith(
            compareByDescending<ReactionBatch> {
                it.priority
            }.thenBy {
                it.eventIndex
            },
        )
        while (queuedBatches.size > maxQueuedBatches) {
            queuedBatches.removeAt(
                queuedBatches.lastIndex,
            )
        }
    }

    private fun start(
        batch: ReactionBatch,
        nowMillis: Long,
    ) {
        activeMatchId =
            batch.matchId
        activePriority =
            batch.priority
        activeUntilMillis =
            nowMillis +
                playbackWindowMs
    }

    private fun shouldInterrupt(
        incomingPriority: Int,
        activePriority: Int,
    ): Boolean =
        incomingPriority >= PRIORITY_VICTORY ||
            (
                incomingPriority >= PRIORITY_CAPTURE &&
                    incomingPriority > activePriority
                )

    private fun LudoPawsReaction.stableKey(): String =
        reactionKey
            .takeIf(String::isNotBlank)
            ?: listOf(
                matchId.ifBlank { "legacy" },
                eventIndex.toString(),
                momentType?.name ?: voiceCue.name,
                playerId,
                tokenIndex?.toString().orEmpty(),
            ).joinToString(":")

    private companion object {
        const val PRIORITY_CAPTURE = 86
        const val PRIORITY_VICTORY = 100
    }
}
