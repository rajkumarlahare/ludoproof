package com.ludoproof.game

import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

data class LudoPaws3DReactionPose(
    val liftY: Float = 0f,
    val bodyYawDegrees: Float = 0f,
    val headTiltDegrees: Float = 0f,
    val primaryAppendageDegrees: Float = 0f,
    val secondaryAppendageDegrees: Float = 0f,
    val tertiaryAppendageDegrees: Float = 0f,
    val scale: Float = 1f,
)

internal data class LudoPaws3DActiveReaction(
    val cue: AnimationCue,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val priority: Int,
)

/**
 * Pure additive body-language layer for code-generated 3D animal pawns.
 *
 * primary/secondary/tertiary are intentionally generic so the renderer maps
 * them to each animal's anatomy: Dog ears/tail, Goat ears/beard/tail, Duck
 * wings and Cat ears/tail. HOME never adds another full spin because the base
 * movement timeline already owns the home-arrival rotation.
 */
object LudoPaws3DReactionMotion {
    fun durationMillis(
        species: LudoPaws3DSpecies,
        cue: AnimationCue,
    ): Long {
        val base =
            when (cue) {
                AnimationCue.IDLE -> 520L
                AnimationCue.EXCITED -> 620L
                AnimationCue.HAPPY -> 680L
                AnimationCue.SAD -> 760L
                AnimationCue.ANGRY -> 650L
                AnimationCue.NERVOUS -> 720L
                AnimationCue.CAPTURE -> 700L
                AnimationCue.CAPTURED -> 620L
                AnimationCue.SAFE -> 640L
                AnimationCue.HOME -> 900L
                AnimationCue.VICTORY -> 1_250L
                AnimationCue.DEFEAT -> 950L
            }
        return when (species) {
            LudoPaws3DSpecies.DOG -> base
            LudoPaws3DSpecies.GOAT -> (base * 1.04f).toLong()
            LudoPaws3DSpecies.DUCK -> (base * .94f).toLong()
            LudoPaws3DSpecies.CAT -> (base * .98f).toLong()
        }
    }

    fun sample(
        species: LudoPaws3DSpecies,
        cue: AnimationCue,
        progress: Float,
    ): LudoPaws3DReactionPose {
        val p = progress.coerceIn(0f, 1f)
        val arc = sin(p * PI).toFloat().coerceAtLeast(0f)
        val wave = sin(p * PI * 2.0).toFloat()
        val fast = sin(p * PI * 5.0).toFloat()
        val bounce = abs(sin(p * PI * 2.0).toFloat()) * (1f - p * .20f)

        return when (species) {
            LudoPaws3DSpecies.DOG -> dog(cue, p, arc, wave, fast, bounce)
            LudoPaws3DSpecies.GOAT -> goat(cue, p, arc, wave, fast, bounce)
            LudoPaws3DSpecies.DUCK -> duck(cue, p, arc, wave, fast, bounce)
            LudoPaws3DSpecies.CAT -> cat(cue, p, arc, wave, fast, bounce)
        }
    }

    private fun dog(
        cue: AnimationCue,
        p: Float,
        arc: Float,
        wave: Float,
        fast: Float,
        bounce: Float,
    ): LudoPaws3DReactionPose =
        when (cue) {
            AnimationCue.IDLE -> LudoPaws3DReactionPose(headTiltDegrees = 7f * wave, primaryAppendageDegrees = 5f * -wave, secondaryAppendageDegrees = 30f * fast)
            AnimationCue.EXCITED -> LudoPaws3DReactionPose(liftY = .24f * bounce, headTiltDegrees = -7f * arc, primaryAppendageDegrees = 15f * arc, secondaryAppendageDegrees = 42f * fast, scale = 1f + .045f * arc)
            AnimationCue.HAPPY, AnimationCue.SAFE -> LudoPaws3DReactionPose(liftY = .13f * arc, bodyYawDegrees = 8f * wave, headTiltDegrees = 6f * wave, primaryAppendageDegrees = 7f * arc, secondaryAppendageDegrees = 34f * fast)
            AnimationCue.CAPTURE -> LudoPaws3DReactionPose(liftY = .19f * arc, bodyYawDegrees = 18f * wave, headTiltDegrees = -10f * arc, primaryAppendageDegrees = 12f * arc, secondaryAppendageDegrees = 46f * fast, scale = 1f + .055f * arc)
            AnimationCue.CAPTURED -> LudoPaws3DReactionPose(liftY = .12f * arc, bodyYawDegrees = 16f * fast * (1f - p), headTiltDegrees = 13f * wave, primaryAppendageDegrees = -9f * arc, secondaryAppendageDegrees = 8f * wave, scale = 1f - .055f * arc)
            AnimationCue.SAD, AnimationCue.DEFEAT -> LudoPaws3DReactionPose(liftY = -.055f * arc, headTiltDegrees = 13f * arc, primaryAppendageDegrees = -8f * arc, secondaryAppendageDegrees = 7f * wave, scale = 1f - .035f * arc)
            AnimationCue.ANGRY -> LudoPaws3DReactionPose(liftY = .045f * bounce, bodyYawDegrees = 11f * fast * (1f - p), headTiltDegrees = -8f * arc, primaryAppendageDegrees = 7f * fast, secondaryAppendageDegrees = 16f * fast)
            AnimationCue.NERVOUS -> LudoPaws3DReactionPose(bodyYawDegrees = 6f * fast * (1f - p), headTiltDegrees = 8f * wave, primaryAppendageDegrees = 5f * fast, secondaryAppendageDegrees = 10f * wave)
            AnimationCue.HOME -> LudoPaws3DReactionPose(liftY = .16f * bounce, headTiltDegrees = 7f * wave, primaryAppendageDegrees = 11f * arc, secondaryAppendageDegrees = 44f * fast, scale = 1f + .035f * arc)
            AnimationCue.VICTORY -> LudoPaws3DReactionPose(liftY = .22f * bounce, bodyYawDegrees = 360f * p, headTiltDegrees = 7f * wave, primaryAppendageDegrees = 13f * arc, secondaryAppendageDegrees = 52f * fast, scale = 1f + .05f * arc)
        }

    private fun goat(
        cue: AnimationCue,
        p: Float,
        arc: Float,
        wave: Float,
        fast: Float,
        bounce: Float,
    ): LudoPaws3DReactionPose =
        when (cue) {
            AnimationCue.IDLE -> LudoPaws3DReactionPose(headTiltDegrees = 10f * wave, primaryAppendageDegrees = 7f * fast, secondaryAppendageDegrees = 6f * -wave, tertiaryAppendageDegrees = 9f * fast)
            AnimationCue.EXCITED, AnimationCue.HAPPY -> LudoPaws3DReactionPose(liftY = .22f * bounce, headTiltDegrees = -6f * arc + 4f * wave, primaryAppendageDegrees = 14f * arc, secondaryAppendageDegrees = 13f * wave, tertiaryAppendageDegrees = 18f * fast)
            AnimationCue.SAFE -> LudoPaws3DReactionPose(liftY = .08f * arc, headTiltDegrees = 7f * wave, primaryAppendageDegrees = 5f * wave, secondaryAppendageDegrees = 5f * -wave)
            AnimationCue.CAPTURE -> LudoPaws3DReactionPose(liftY = .13f * arc, bodyYawDegrees = 13f * wave, headTiltDegrees = -16f * arc, primaryAppendageDegrees = 11f * arc, secondaryAppendageDegrees = 18f * arc, tertiaryAppendageDegrees = 16f * fast, scale = 1f + .04f * arc)
            AnimationCue.CAPTURED -> LudoPaws3DReactionPose(liftY = .11f * arc, bodyYawDegrees = 13f * fast * (1f - p), headTiltDegrees = 11f * wave, primaryAppendageDegrees = -7f * arc, secondaryAppendageDegrees = 15f * wave, tertiaryAppendageDegrees = -10f * arc, scale = 1f - .045f * arc)
            AnimationCue.SAD, AnimationCue.DEFEAT -> LudoPaws3DReactionPose(liftY = -.05f * arc, headTiltDegrees = 12f * arc, primaryAppendageDegrees = -7f * arc, secondaryAppendageDegrees = 10f * arc, tertiaryAppendageDegrees = -7f * arc)
            AnimationCue.ANGRY -> LudoPaws3DReactionPose(liftY = .035f * bounce, bodyYawDegrees = 7f * fast * (1f - p), headTiltDegrees = -12f * arc, primaryAppendageDegrees = 8f * fast, secondaryAppendageDegrees = 13f * fast, tertiaryAppendageDegrees = 11f * fast)
            AnimationCue.NERVOUS -> LudoPaws3DReactionPose(bodyYawDegrees = 5f * fast * (1f - p), headTiltDegrees = 9f * wave, primaryAppendageDegrees = 8f * fast, secondaryAppendageDegrees = 7f * wave)
            AnimationCue.HOME -> LudoPaws3DReactionPose(liftY = .15f * bounce, headTiltDegrees = 8f * wave, primaryAppendageDegrees = 10f * fast, secondaryAppendageDegrees = 17f * wave, tertiaryAppendageDegrees = 22f * fast, scale = 1f + .03f * arc)
            AnimationCue.VICTORY -> LudoPaws3DReactionPose(liftY = .20f * bounce, bodyYawDegrees = 360f * p, headTiltDegrees = 8f * wave, primaryAppendageDegrees = 12f * fast, secondaryAppendageDegrees = 20f * wave, tertiaryAppendageDegrees = 26f * fast, scale = 1f + .04f * arc)
        }

    private fun duck(
        cue: AnimationCue,
        p: Float,
        arc: Float,
        wave: Float,
        fast: Float,
        bounce: Float,
    ): LudoPaws3DReactionPose =
        when (cue) {
            AnimationCue.IDLE -> LudoPaws3DReactionPose(liftY = .025f * wave, headTiltDegrees = 7f * wave, primaryAppendageDegrees = 8f * wave)
            AnimationCue.EXCITED, AnimationCue.HAPPY -> LudoPaws3DReactionPose(liftY = .25f * bounce, bodyYawDegrees = 7f * wave, headTiltDegrees = -7f * arc, primaryAppendageDegrees = 38f * arc + 9f * fast, scale = 1f + .04f * arc)
            AnimationCue.SAFE -> LudoPaws3DReactionPose(liftY = .07f * arc, headTiltDegrees = 5f * wave, primaryAppendageDegrees = 11f * arc)
            AnimationCue.CAPTURE -> LudoPaws3DReactionPose(liftY = .18f * arc, bodyYawDegrees = 13f * wave, headTiltDegrees = -8f * arc, primaryAppendageDegrees = 42f * arc, scale = 1f + .045f * arc)
            AnimationCue.CAPTURED -> LudoPaws3DReactionPose(liftY = .12f * arc, bodyYawDegrees = 15f * fast * (1f - p), headTiltDegrees = 12f * wave, primaryAppendageDegrees = 28f * fast * (1f - p), scale = 1f - .045f * arc)
            AnimationCue.SAD, AnimationCue.DEFEAT -> LudoPaws3DReactionPose(liftY = -.045f * arc, headTiltDegrees = 13f * arc, primaryAppendageDegrees = -5f * arc, scale = 1f - .03f * arc)
            AnimationCue.ANGRY -> LudoPaws3DReactionPose(bodyYawDegrees = 10f * fast * (1f - p), headTiltDegrees = -8f * arc, primaryAppendageDegrees = 26f * fast)
            AnimationCue.NERVOUS -> LudoPaws3DReactionPose(bodyYawDegrees = 6f * fast * (1f - p), headTiltDegrees = 10f * wave, primaryAppendageDegrees = 12f * fast)
            AnimationCue.HOME -> LudoPaws3DReactionPose(liftY = .18f * bounce, headTiltDegrees = 8f * wave, primaryAppendageDegrees = 42f * arc + 6f * fast, scale = 1f + .035f * arc)
            AnimationCue.VICTORY -> LudoPaws3DReactionPose(liftY = .23f * bounce, bodyYawDegrees = 360f * p, headTiltDegrees = 8f * wave, primaryAppendageDegrees = 50f * arc + 8f * fast, scale = 1f + .05f * arc)
        }

    private fun cat(
        cue: AnimationCue,
        p: Float,
        arc: Float,
        wave: Float,
        fast: Float,
        bounce: Float,
    ): LudoPaws3DReactionPose =
        when (cue) {
            AnimationCue.IDLE -> LudoPaws3DReactionPose(headTiltDegrees = 5f * wave, primaryAppendageDegrees = 4f * fast, secondaryAppendageDegrees = 28f * wave)
            AnimationCue.EXCITED -> LudoPaws3DReactionPose(liftY = .16f * arc, headTiltDegrees = -5f * arc, primaryAppendageDegrees = 9f * arc, secondaryAppendageDegrees = 26f * fast)
            AnimationCue.HAPPY, AnimationCue.SAFE -> LudoPaws3DReactionPose(liftY = .07f * arc, bodyYawDegrees = 5f * wave, headTiltDegrees = 6f * wave, primaryAppendageDegrees = 5f * fast, secondaryAppendageDegrees = 32f * wave)
            AnimationCue.CAPTURE -> LudoPaws3DReactionPose(liftY = .12f * arc, bodyYawDegrees = 14f * wave, headTiltDegrees = -7f * arc, primaryAppendageDegrees = 8f * arc, secondaryAppendageDegrees = 39f * fast, scale = 1f + .035f * arc)
            AnimationCue.CAPTURED -> LudoPaws3DReactionPose(liftY = .10f * arc, bodyYawDegrees = 12f * fast * (1f - p), headTiltDegrees = 11f * wave, primaryAppendageDegrees = -9f * arc, secondaryAppendageDegrees = 14f * fast, scale = 1f - .04f * arc)
            AnimationCue.SAD, AnimationCue.DEFEAT -> LudoPaws3DReactionPose(liftY = -.04f * arc, headTiltDegrees = 9f * arc, primaryAppendageDegrees = -7f * arc, secondaryAppendageDegrees = 13f * wave, scale = 1f - .025f * arc)
            AnimationCue.ANGRY -> LudoPaws3DReactionPose(bodyYawDegrees = 5f * fast * (1f - p), headTiltDegrees = -6f * arc, primaryAppendageDegrees = -8f * arc, secondaryAppendageDegrees = 26f * fast)
            AnimationCue.NERVOUS -> LudoPaws3DReactionPose(bodyYawDegrees = 4f * fast * (1f - p), headTiltDegrees = 7f * wave, primaryAppendageDegrees = 7f * fast, secondaryAppendageDegrees = 18f * fast)
            AnimationCue.HOME -> LudoPaws3DReactionPose(liftY = .12f * bounce, headTiltDegrees = 7f * wave, primaryAppendageDegrees = 8f * fast, secondaryAppendageDegrees = 38f * fast, scale = 1f + .025f * arc)
            // Complete a full turn so the final frame matches the starting heading.
            AnimationCue.VICTORY -> LudoPaws3DReactionPose(liftY = .17f * bounce, bodyYawDegrees = 360f * p, headTiltDegrees = 7f * wave, primaryAppendageDegrees = 10f * fast, secondaryAppendageDegrees = 48f * fast, scale = 1f + .035f * arc)
        }
}
