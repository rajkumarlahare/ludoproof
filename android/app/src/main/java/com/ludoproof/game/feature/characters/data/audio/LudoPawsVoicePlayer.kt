package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ludoproof.game.feature.characters.domain.audio.LudoPawsReactionAudioProfile
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.reaction.GameMomentType
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.settings.data.local.GameMusicController
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore

/**
 * Non-verbal animal reaction renderer.
 *
 * The historical class name is retained to avoid unnecessary UI churn, but no
 * TextToSpeech or spoken sentence path exists here. Named packaged animal clips
 * are preferred; missing clips use the copyright-safe procedural fallback.
 */
class LudoPawsVoicePlayer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scheduled = mutableSetOf<Runnable>()
    private var closed = false

    fun playHighestPriority(
        reactions: List<LudoPawsReaction>,
        characterIdsBySeat: List<String>,
    ) {
        if (closed || reactions.isEmpty()) return
        if (
            !GameSettingsStore(appContext)
                .snapshot()
                .animalVoicesEnabled
        ) {
            cancelPending()
            return
        }

        val selected = selectVocalBatch(reactions)
        selected.forEach { reaction ->
            val characterId =
                characterIdsBySeat
                    .getOrNull(reaction.seat)
                    ?: canonicalCharacterForSeat(reaction.seat)
            val character =
                LudoPawsCharacterCatalog.character(characterId)
                    ?: return@forEach
            val voiceSet =
                LudoPawsCharacterCatalog.voiceSet(character.voiceSetId)
                    ?: return@forEach
            if (reaction.voiceCue !in voiceSet.supportedCues) {
                return@forEach
            }

            val plan =
                LudoPawsReactionAudioProfile.plan(
                    characterId = character.id,
                    species = character.species,
                    reaction = reaction,
                ) ?: return@forEach
            val reactionKey =
                reaction.reactionKey.ifBlank {
                    "${reaction.matchId}:${reaction.eventIndex}:${reaction.playerId}:${reaction.voiceCue.wireName}"
                }
            if (
                !LudoPawsReactionAudioProfile.shouldPlay(
                    plan = plan,
                    reactionKey = reactionKey,
                )
            ) {
                return@forEach
            }

            val variantIndex =
                LudoPawsReactionAudioProfile
                    .deterministicVariantIndex(
                        reactionKey = reactionKey,
                        variantCount = plan.rawResourceNames.size,
                    )
            schedule(
                delayMillis = plan.delayMillis,
            ) {
                if (closed) return@schedule
                GameMusicController.duckForVoice(VOCAL_DUCK_MILLIS)
                LudoPawsAudioAssetPlayer.playVocal(
                    context = appContext,
                    rawResourceNames = plan.rawResourceNames,
                    variantIndex = variantIndex,
                    fallback = plan.fallback,
                    volume = plan.volume,
                    playbackRate = plan.playbackRate,
                )
            }
        }
    }

    fun shutdown() {
        if (closed) return
        closed = true
        cancelPending()
    }

    private fun selectVocalBatch(
        reactions: List<LudoPawsReaction>,
    ): List<LudoPawsReaction> {
        val highest =
            reactions.maxByOrNull(LudoPawsReaction::priority)
                ?: return emptyList()

        if (highest.momentType == GameMomentType.CAPTURE_MADE) {
            return reactions
                .filter {
                    it.eventIndex == highest.eventIndex &&
                        (
                            it.momentType == GameMomentType.CAPTURE_MADE ||
                                it.momentType == GameMomentType.TOKEN_CAPTURED
                            )
                }
                .sortedByDescending(LudoPawsReaction::priority)
                .take(MAX_CAPTURE_VOCALS)
        }
        return listOf(highest)
    }

    private fun schedule(
        delayMillis: Long,
        action: () -> Unit,
    ) {
        lateinit var task: Runnable
        task =
            Runnable {
                scheduled.remove(task)
                if (!closed) action()
            }
        scheduled += task
        if (delayMillis <= 0L) {
            mainHandler.post(task)
        } else {
            mainHandler.postDelayed(task, delayMillis)
        }
    }

    private fun cancelPending() {
        scheduled.forEach(mainHandler::removeCallbacks)
        scheduled.clear()
    }

    private fun canonicalCharacterForSeat(seat: Int): String =
        when (seat) {
            0 -> "dog"
            1 -> "goat"
            2 -> "duck"
            3 -> "cat"
            else -> LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
        }

    private companion object {
        const val MAX_CAPTURE_VOCALS = 2
        const val VOCAL_DUCK_MILLIS = 650L
    }
}
