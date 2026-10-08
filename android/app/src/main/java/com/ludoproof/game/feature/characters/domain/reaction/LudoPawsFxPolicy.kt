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
 * Reduced Motion deliberately removes spatial motion and dense particles while
 * retaining short fades/highlights so important game feedback is not lost.
 */
object LudoPawsFxPolicy {
    fun plan(
        cue: AnimationCue,
        reducedMotion: Boolean,
    ): LudoPawsFxPlan {
        if (reducedMotion) {
            return LudoPawsFxPlan(
                durationMs = REDUCED_DURATION_MS,
                particleCount =
                    when (cue) {
                        AnimationCue.VICTORY,
                        AnimationCue.HOME,
                        -> 4

                        AnimationCue.IDLE -> 0
                        else -> 2
                    },
                allowTranslation = false,
                allowRotation = false,
                allowShake = false,
                allowConfetti = false,
            )
        }

        return when (cue) {
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
        )

    private const val REDUCED_DURATION_MS = 220L
}
