package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.characters.domain.model.AnimalPersonality
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsVoiceLines
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import java.util.Locale

/**
 * Lightweight on-device voice renderer for Phase 6 reactions.
 *
 * Voice phrases are intentionally presentation-only. Missing TTS support,
 * disabled sound, unsupported cues, or missing character metadata fail silent
 * and never affect gameplay.
 */
class LudoPawsVoicePlayer(
    context: Context,
) : TextToSpeech.OnInitListener {
    private val appContext =
        context.applicationContext
    private var textToSpeech: TextToSpeech? =
        null
    private var ready =
        false
    private var closed =
        false
    private var pending: PendingSpeech? =
        null

    init {
        textToSpeech =
            runCatching {
                TextToSpeech(
                    appContext,
                    this,
                )
            }
                .getOrNull()
    }

    override fun onInit(
        status: Int,
    ) {
        if (closed) {
            return
        }

        ready =
            status ==
                TextToSpeech.SUCCESS
        if (!ready) {
            pending =
                null
            return
        }

        runCatching {
            textToSpeech
                ?.language =
                Locale.US
        }

        val queued =
            pending
        pending =
            null
        if (queued != null) {
            speakNow(
                queued,
            )
        }
    }

    fun playHighestPriority(
        reactions: List<LudoPawsReaction>,
        characterIdsBySeat: List<String>,
    ) {
        if (
            closed ||
            reactions.isEmpty() ||
            !GameSettingsStore(appContext)
                .snapshot()
                .soundEnabled
        ) {
            return
        }

        val candidate =
            reactions
                .asSequence()
                .sortedByDescending(
                    LudoPawsReaction::priority,
                )
                .mapNotNull {
                        reaction ->
                    val characterId =
                        characterIdsBySeat
                            .getOrNull(
                                reaction.seat,
                            )
                            ?: return@mapNotNull null
                    val character =
                        LudoPawsCharacterCatalog
                            .character(
                                characterId,
                            )
                            ?: return@mapNotNull null
                    val voiceSet =
                        LudoPawsCharacterCatalog
                            .voiceSet(
                                character.voiceSetId,
                            )
                            ?: return@mapNotNull null
                    if (
                        reaction.voiceCue !in
                        voiceSet.supportedCues
                    ) {
                        return@mapNotNull null
                    }
                    PendingSpeech(
                        character = character,
                        reaction = reaction,
                        line =
                            LudoPawsVoiceLines
                                .line(
                                    character = character,
                                    cue = reaction.voiceCue,
                                ),
                    )
                }
                .firstOrNull()
                ?: return

        if (ready) {
            speakNow(
                candidate,
            )
        } else {
            pending =
                candidate
        }
    }

    fun shutdown() {
        if (closed) {
            return
        }
        closed =
            true
        ready =
            false
        pending =
            null
        runCatching {
            textToSpeech
                ?.stop()
            textToSpeech
                ?.shutdown()
        }
        textToSpeech =
            null
    }

    private fun speakNow(
        speech: PendingSpeech,
    ) {
        if (
            closed ||
            !ready
        ) {
            return
        }

        val profile =
            voiceProfile(
                speech.character,
            )
        runCatching {
            textToSpeech
                ?.setPitch(
                    profile.pitch,
                )
            textToSpeech
                ?.setSpeechRate(
                    profile.rate,
                )
            textToSpeech
                ?.speak(
                    speech.line,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ludo_paws_${speech.reaction.playerId}_${speech.reaction.voiceCue.wireName}",
                )
        }
    }

    private fun voiceProfile(
        character: AnimalCharacter,
    ): VoiceProfile =
        when (character.personality) {
            AnimalPersonality.CHEERFUL ->
                VoiceProfile(
                    pitch = 1.22f,
                    rate = 1.05f,
                )
            AnimalPersonality.MISCHIEVOUS ->
                VoiceProfile(
                    pitch = 1.10f,
                    rate = 1.14f,
                )
            AnimalPersonality.SHY ->
                VoiceProfile(
                    pitch = 1.28f,
                    rate = 0.92f,
                )
            AnimalPersonality.GENTLE ->
                VoiceProfile(
                    pitch = 0.94f,
                    rate = 0.90f,
                )
            AnimalPersonality.BOLD,
            AnimalPersonality.PROUD,
            AnimalPersonality.BRAVE,
            ->
                VoiceProfile(
                    pitch = 0.90f,
                    rate = 1.02f,
                )
            AnimalPersonality.PLAYFUL,
            AnimalPersonality.CURIOUS,
            AnimalPersonality.SASSY,
            ->
                VoiceProfile(
                    pitch = 1.12f,
                    rate = 1.10f,
                )
            AnimalPersonality.CALM,
            AnimalPersonality.NERVOUS,
            ->
                VoiceProfile(
                    pitch = 1.00f,
                    rate = 0.96f,
                )
        }

    private data class PendingSpeech(
        val character: AnimalCharacter,
        val reaction: LudoPawsReaction,
        val line: String,
    )

    private data class VoiceProfile(
        val pitch: Float,
        val rate: Float,
    )
}
