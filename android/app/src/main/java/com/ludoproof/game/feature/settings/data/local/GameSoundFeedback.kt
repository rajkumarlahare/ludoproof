package com.ludoproof.game.feature.settings.data.local

import android.content.Context
import com.ludoproof.game.core.audio.LudoPawsAudioCatalog
import com.ludoproof.game.feature.characters.data.audio.LudoPawsAudioAssetPlayer
import com.ludoproof.game.feature.characters.data.audio.LudoPawsProceduralAudio
import com.ludoproof.game.feature.characters.domain.reaction.GameMomentType
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction

/**
 * Short game/UI sound facade.
 *
 * Every sound resolves one stable preferred authored asset family and otherwise
 * uses the existing res/raw/generated fallback. Legacy placeholder WAV aliases
 * are not consulted because several historical files contained identical audio.
 */
object GameSoundFeedback {
    fun click(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_ui_click"),
            fallback = LudoPawsProceduralAudio.Sfx.CLICK,
            volume = .46f,
        )

    /** One short physical tick for exactly one visual pawn step. */
    fun moveStep(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_move_paw"),
            fallback = LudoPawsProceduralAudio.Sfx.MOVE_PAW,
            // Authored jump/tick clips are intentionally more prominent than the
            // old generic movement cue so every step remains clearly audible.
            volume = .72f,
            assetPaths = LudoPawsAudioCatalog.Sfx.MOVE_JUMP,
        )

    fun move(
        context: Context,
        characterId: String? = null,
    ) {
        val movement =
            when (characterId) {
                "goat" ->
                    Triple(
                        listOf("lp_sfx_move_hoof"),
                        LudoPawsProceduralAudio.Sfx.MOVE_HOOF,
                        .40f,
                    )
                "duck" ->
                    Triple(
                        listOf("lp_sfx_move_web"),
                        LudoPawsProceduralAudio.Sfx.MOVE_WEB,
                        .38f,
                    )
                else ->
                    Triple(
                        listOf("lp_sfx_move_paw"),
                        LudoPawsProceduralAudio.Sfx.MOVE_PAW,
                        .38f,
                    )
            }
        play(
            context = context,
            names = movement.first,
            fallback = movement.second,
            volume = movement.third,
            assetPaths = LudoPawsAudioCatalog.Sfx.MOVE_JUMP,
        )
    }

    fun roll(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_dice_roll"),
            fallback = LudoPawsProceduralAudio.Sfx.DICE_ROLL,
            volume = .50f,
        )

    fun six(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_six"),
            fallback = LudoPawsProceduralAudio.Sfx.SIX_SPARK,
            volume = .45f,
        )

    fun yardExit(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_yard_exit"),
            fallback = LudoPawsProceduralAudio.Sfx.YARD_EXIT,
            volume = .43f,
        )

    fun capture(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_capture_impact"),
            fallback = LudoPawsProceduralAudio.Sfx.CAPTURE_IMPACT,
            volume = .62f,
        )

    fun safe(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_safe_shimmer"),
            fallback = LudoPawsProceduralAudio.Sfx.SAFE_SHIMMER,
            volume = .44f,
        )

    fun homeLane(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_home_lane"),
            fallback = LudoPawsProceduralAudio.Sfx.HOME_LANE,
            volume = .45f,
        )

    fun home(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_home_sparkle"),
            fallback = LudoPawsProceduralAudio.Sfx.HOME_SPARKLE,
            volume = .56f,
        )

    fun frustrated(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_fail_soft"),
            fallback = LudoPawsProceduralAudio.Sfx.FAIL_SOFT,
            volume = .32f,
        )

    fun thirdSix(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_third_six"),
            fallback = LudoPawsProceduralAudio.Sfx.THIRD_SIX,
            volume = .48f,
        )

    fun victory(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_victory_sting"),
            fallback = LudoPawsProceduralAudio.Sfx.VICTORY,
            volume = .60f,
        )

    fun defeat(context: Context) =
        play(
            context = context,
            names = listOf("lp_sfx_defeat_sting"),
            fallback = LudoPawsProceduralAudio.Sfx.DEFEAT,
            volume = .42f,
        )

    /** Returns true when this batch emitted a dedicated physical/game SFX. */
    fun reaction(
        context: Context,
        reactions: List<LudoPawsReaction>,
        characterIdsBySeat: List<String> = emptyList(),
    ): Boolean {
        val highest =
            reactions.maxByOrNull(LudoPawsReaction::priority)
                ?: return false
        val characterId = characterIdsBySeat.getOrNull(highest.seat)

        return when (highest.momentType) {
            GameMomentType.SIX_ROLLED -> played { six(context) }
            GameMomentType.TOKEN_LEFT_YARD -> played { yardExit(context) }
            // Per-cell movement ticks are emitted by the synchronized 3D movement
            // clock. Playing the legacy one-shot move cue here would double-fire
            // the sound for every move.
            GameMomentType.ONLY_LEGAL_MOVE -> false
            GameMomentType.CAPTURE_MADE,
            GameMomentType.TOKEN_CAPTURED,
            -> played { capture(context) }
            GameMomentType.SAFE_REACHED -> played { safe(context) }
            GameMomentType.HOME_LANE_ENTERED -> played { homeLane(context) }
            GameMomentType.HOME_REACHED -> played { home(context) }
            GameMomentType.POOR_ROLL_STREAK,
            GameMomentType.EXACT_HOME_MISS,
            GameMomentType.NO_LEGAL_MOVE,
            -> played { frustrated(context) }
            GameMomentType.THIRD_SIX_FORFEIT -> played { thirdSix(context) }
            GameMomentType.MATCH_WIN,
            GameMomentType.TEAM_WIN,
            -> played { victory(context) }
            GameMomentType.MATCH_LOSS,
            GameMomentType.TEAM_LOSS,
            -> played { defeat(context) }
            GameMomentType.TURN_STARTED,
            GameMomentType.ROLL_STARTED,
            GameMomentType.LOW_ROLL,
            GameMomentType.TOKEN_MOVED,
            GameMomentType.TOKEN_THREATENED,
            GameMomentType.PLAYER_LEADING,
            GameMomentType.IDLE_WAITING,
            null,
            -> false
        }
    }

    private inline fun played(block: () -> Unit): Boolean {
        block()
        return true
    }

    private fun play(
        context: Context,
        names: List<String>,
        fallback: LudoPawsProceduralAudio.Sfx,
        volume: Float,
        playbackRate: Float = 1f,
        assetPaths: List<String> = emptyList(),
    ) {
        if (
            !GameSettingsStore(context)
                .snapshot()
                .soundEnabled
        ) {
            return
        }
        LudoPawsAudioAssetPlayer.playSfx(
            context = context,
            rawResourceNames = names,
            fallback = fallback,
            volume = volume,
            playbackRate = playbackRate,
            assetPaths = assetPaths,
        )
    }
}
