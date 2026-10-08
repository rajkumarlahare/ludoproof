package com.ludoproof.game.feature.characters.domain.reaction

import com.ludoproof.game.feature.characters.domain.model.AnimationCue

data class LudoPawsFxPlan(
    val durationMs: Long,
    val particleCount: Int,
    val allowTranslation: Boolean,
    val allowRotation: Boolean,
    val allowShake: Boolean,
    val allowConfetti: Boolean,
)

/**
 * Pure presentation policy for Phase 10 animation and effects.
 *
 * Full-motion presentation policy for board reactions and effects.
 */
object LudoPawsFxPolicy {
    fun plan(
        cue: AnimationCue,
    ): LudoPawsFxPlan =
        when (cue) {
            AnimationCue.IDLE ->
                LudoPawsFxPlan(
                    durationMs = 900L,
                    particleCount = 0,
                    allowTranslation = false,
                    allowRotation = false,
                    allowShake = false,
                    allowConfetti = false,
                )

            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            ->
                lively(
                    durationMs = 760L,
                    particleCount = 8,
                )

            AnimationCue.CAPTURE ->
                lively(
                    durationMs = 880L,
                    particleCount = 12,
                )

            AnimationCue.CAPTURED ->
                LudoPawsFxPlan(
                    durationMs = 1050L,
                    particleCount = 8,
                    allowTranslation = true,
                    allowRotation = true,
                    allowShake = true,
                    allowConfetti = false,
                )

            AnimationCue.SAFE ->
                LudoPawsFxPlan(
                    durationMs = 820L,
                    particleCount = 6,
                    allowTranslation = false,
                    allowRotation = false,
                    allowShake = false,
                    allowConfetti = false,
                )

            AnimationCue.HOME ->
                LudoPawsFxPlan(
                    durationMs = 1050L,
                    particleCount = 14,
                    allowTranslation = true,
                    allowRotation = true,
                    allowShake = false,
                    allowConfetti = false,
                )

            AnimationCue.ANGRY ->
                LudoPawsFxPlan(
                    durationMs = 760L,
                    particleCount = 6,
                    allowTranslation = false,
                    allowRotation = true,
                    allowShake = true,
                    allowConfetti = false,
                )

            AnimationCue.NERVOUS ->
                LudoPawsFxPlan(
                    durationMs = 900L,
                    particleCount = 4,
                    allowTranslation = false,
                    allowRotation = true,
                    allowShake = true,
                    allowConfetti = false,
                )

            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            ->
                LudoPawsFxPlan(
                    durationMs = 980L,
                    particleCount = 7,
                    allowTranslation = true,
                    allowRotation = true,
                    allowShake = false,
                    allowConfetti = false,
                )

            AnimationCue.VICTORY ->
                LudoPawsFxPlan(
                    durationMs = 1500L,
                    particleCount = 32,
                    allowTranslation = true,
                    allowRotation = true,
                    allowShake = false,
                    allowConfetti = true,
                )
        }
    }

    private fun lively(
        durationMs: Long,
        particleCount: Int,
    ): LudoPawsFxPlan =
        LudoPawsFxPlan(
            durationMs = durationMs,
            particleCount = particleCount,
            allowTranslation = true,
            allowRotation = true,
            allowShake = false,
            allowConfetti = false,
        )}