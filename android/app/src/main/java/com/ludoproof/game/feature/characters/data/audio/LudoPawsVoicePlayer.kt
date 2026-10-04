package com.ludoproof.game.feature.characters.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import com.ludoproof.game.R
import com.ludoproof.game.core.audio.LudoPawsSoundPool
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.characters.domain.model.AnimalPersonality
import com.ludoproof.game.feature.characters.domain.model.AnimalSpecies
import com.ludoproof.game.feature.characters.domain.model.VoiceCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsVoiceLines
import com.ludoproof.game.feature.settings.data.local.GameMusicController
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import java.util.Locale

/**
 * Animal voice renderer for reaction playback.
 *
 * Reaction SFX and haptics intentionally belong to the mode-specific feedback
 * dispatchers. Keeping this class voice-only prevents one game transition from
 * playing the same feedback twice through both the reactive board and its
 * presentation layer.
 *
 * Starter Paws species use low-latency nonverbal SoundPool clips. Characters
 * without a packaged species clip fall back to on-device TTS so future packs
 * can still react before their final voice assets arrive.
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
            textToSpeech
                ?.setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(
                            AudioAttributes.USAGE_GAME,
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SPEECH,
                        )
                        .build(),
                )
        }

        val queued =
            pending
        pending =
            null
        if (queued != null) {
            playPrepared(
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
            reactions.isEmpty()
        ) {
            return
        }

        if (
            !GameSettingsStore(appContext)
                .snapshot()
                .animalVoicesEnabled
        ) {
            pending = null
            runCatching {
                textToSpeech?.stop()
            }
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

        playPrepared(
            candidate,
        )
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

    private fun playPrepared(
        speech: PendingSpeech,
    ) {
        if (
            playSpeciesClip(
                character = speech.character,
                cue = speech.reaction.voiceCue,
            )
        ) {
            pending = null
            return
        }

        if (ready) {
            speakNow(
                speech,
            )
        } else {
            pending =
                speech
        }
    }

    private fun playSpeciesClip(
        character: AnimalCharacter,
        cue: VoiceCue,
    ): Boolean {
        val resourceId =
            when (character.species) {
                AnimalSpecies.DUCK ->
                    R.raw.lp_voice_duck
                AnimalSpecies.SQUIRREL ->
                    R.raw.lp_voice_squirrel
                AnimalSpecies.HEDGEHOG ->
                    R.raw.lp_voice_hedgehog
                AnimalSpecies.SHEEP ->
                    R.raw.lp_voice_sheep
                else ->
                    return false
            }

        val scheduled =
            LudoPawsSoundPool.play(
                context = appContext,
                resourceId = resourceId,
                volume = .62f,
                rate = clipRate(cue),
            )
        if (scheduled) {
            GameMusicController
                .duckForVoice(
                    SPECIES_DUCK_MILLIS,
                )
        }
        return scheduled
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
        GameMusicController
            .duckForVoice(
                TTS_DUCK_MILLIS,
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

    private fun clipRate(
        cue: VoiceCue,
    ): Float =
        when (cue) {
            VoiceCue.SIX -> 1.15f
            VoiceCue.CAPTURE -> 1.12f
            VoiceCue.CAPTURED -> .84f
            VoiceCue.SAFE -> 1.05f
            VoiceCue.HOME -> 1.10f
            VoiceCue.FRUSTRATED -> .82f
            VoiceCue.THIRD_SIX -> .76f
            VoiceCue.IDLE -> .92f
            VoiceCue.NERVOUS -> 1.20f
            VoiceCue.VICTORY -> 1.18f
            VoiceCue.DEFEAT -> .78f
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

    private companion object {
        const val SPECIES_DUCK_MILLIS = 620L
        const val TTS_DUCK_MILLIS = 1_450L
    }
}
