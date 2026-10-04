package com.ludoproof.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFxPolicy
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import kotlin.math.min

/**
 * Phase 10 presentation-only board FX layer.
 *
 * It visualizes already-committed snapshot transitions and reaction cues. It
 * never mutates gameplay state, proof state, persistence, or network state.
 */
internal class LudoPawsGameFxOverlayView(
    context: Context,
) : View(context) {
    private data class TokenTransition(
        val playerId: String,
        val seat: Int,
        val tokenIndex: Int,
        val color: String,
        val fromPosition: Int,
        val toPosition: Int,
    ) {
        val isCaptureReturn: Boolean =
            fromPosition >= 0 && toPosition == -1
        val isHomeArrival: Boolean =
            fromPosition != 57 && toPosition == 57
    }

    private var snapshot: MatchSnapshot? = null
    private var perspectiveColor: String? = null
    private var reactions: List<LudoPawsReaction> = emptyList()
    private var transitions: List<TokenTransition> = emptyList()
    private var reducedMotion = false
    private var reactionProgress = 0f
    private var transitionProgress = 1f
    private var reactionAnimator: ValueAnimator? = null
    private var transitionAnimator: ValueAnimator? = null
    private val painter = LudoPawsFxPainter(context)

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
        perspectiveColor: String?,
        reducedMotion: Boolean,
    ) {
        snapshot = current
        this.perspectiveColor =
            perspectiveColor?.takeIf {
                it in OfflinePlayerLayout.COLORS
            }
        this.reducedMotion = reducedMotion
        transitions = detectTransitions(previous, current)
        startTransitionAnimationIfNeeded()
        invalidate()
    }

    fun play(
        reactions: List<LudoPawsReaction>,
        reducedMotion: Boolean,
    ) {
        reactionAnimator?.cancel()
        this.reducedMotion = reducedMotion
        this.reactions = reactions.take(MAX_SIMULTANEOUS_REACTIONS)
        reactionProgress = 0f

        if (this.reactions.isEmpty()) {
            invalidate()
            return
        }

        val duration =
            this.reactions.maxOf {
                LudoPawsFxPolicy
                    .plan(
                        cue = it.animationCue,
                        reducedMotion = reducedMotion,
                    )
                    .durationMs
            }

        reactionAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration
                addUpdateListener {
                    reactionProgress = it.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            finishReactionAnimation()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            finishReactionAnimation()
                        }
                    },
                )
                start()
            }
    }

    fun stop() {
        reactionAnimator?.cancel()
        transitionAnimator?.cancel()
        reactionAnimator = null
        transitionAnimator = null
        reactions = emptyList()
        transitions = emptyList()
        reactionProgress = 0f
        transitionProgress = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = snapshot ?: return
        val size = min(width, height).toFloat()
        if (size <= 0f) {
            return
        }
        val cell = size / LudoPawsFxBoardGeometry.BOARD_SIZE
        val turns =
            perspectiveColor
                ?.let(OfflinePlayerLayout::rotationQuarterTurns)
                ?: 0

        if (turns != 0) {
            canvas.save()
            canvas.rotate(
                turns * 90f,
                size / 2f,
                size / 2f,
            )
        }

        drawTransitions(canvas, cell)
        reactions.forEach { reaction ->
            drawReaction(
                canvas = canvas,
                state = current,
                cell = cell,
                reaction = reaction,
            )
        }

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun startTransitionAnimationIfNeeded() {
        transitionAnimator?.cancel()
        if (transitions.isEmpty() || reducedMotion) {
            transitionProgress = 1f
            transitionAnimator = null
            return
        }

        transitionProgress = 0f
        transitionAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = TRANSITION_DURATION_MS
                addUpdateListener {
                    transitionProgress = it.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            transitionProgress = 1f
                            transitionAnimator = null
                            invalidate()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            transitionAnimator = null
                        }
                    },
                )
                start()
            }
    }

    private fun detectTransitions(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ): List<TokenTransition> {
        if (
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            return emptyList()
        }

        val previousById =
            previous.players.associateBy(PlayerSnapshot::playerId)
        return buildList {
            current.players.forEach { player ->
                val before =
                    previousById[player.playerId]
                        ?: return@forEach
                player.tokens.forEachIndexed { tokenIndex, toPosition ->
                    val fromPosition =
                        before.tokens.getOrNull(tokenIndex)
                            ?: return@forEachIndexed
                    if (fromPosition != toPosition) {
                        add(
                            TokenTransition(
                                playerId = player.playerId,
                                seat = player.seat,
                                tokenIndex = tokenIndex,
                                color = player.color,
                                fromPosition = fromPosition,
                                toPosition = toPosition,
                            ),
                        )
                    }
                }
            }
        }.take(MAX_TRANSITIONS)
    }

    private fun drawTransitions(
        canvas: Canvas,
        cell: Float,
    ) {
        transitions.forEach { transition ->
            val from =
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = transition.color,
                    tokenIndex = transition.tokenIndex,
                    position = transition.fromPosition,
                    cell = cell,
                )
            val to =
                LudoPawsFxBoardGeometry.tokenCenter(
                    color = transition.color,
                    tokenIndex = transition.tokenIndex,
                    position = transition.toPosition,
                    cell = cell,
                )
            if (from == null || to == null) {
                return@forEach
            }

            val color = playerColor(transition.color)
            when {
                transition.isCaptureReturn ->
                    painter.drawCaptureReturn(
                        canvas = canvas,
                        from = from,
                        to = to,
                        color = color,
                        cell = cell,
                        progress = transitionProgress,
                        reducedMotion = reducedMotion,
                    )

                transition.isHomeArrival ->
                    painter.drawPawTrail(
                        canvas = canvas,
                        from = from,
                        to = to,
                        color = color,
                        cell = cell,
                        progress = transitionProgress,
                        emphasis = 1f,
                        reducedMotion = reducedMotion,
                    )

                else ->
                    painter.drawPawTrail(
                        canvas = canvas,
                        from = from,
                        to = to,
                        color = color,
                        cell = cell,
                        progress = transitionProgress,
                        emphasis = .55f,
                        reducedMotion = reducedMotion,
                    )
            }
        }

        if (transitions.any(TokenTransition::isHomeArrival)) {
            painter.drawHomeStars(
                canvas = canvas,
                cell = cell,
                progress = transitionProgress,
                reducedMotion = reducedMotion,
            )
        }
    }

    private fun drawReaction(
        canvas: Canvas,
        state: MatchSnapshot,
        cell: Float,
        reaction: LudoPawsReaction,
    ) {
        val player =
            state.players.firstOrNull {
                it.playerId == reaction.playerId
            } ?: return
        val tokenIndex =
            reaction.tokenIndex
                ?: representativeTokenIndex(player)
        val position =
            player.tokens.getOrNull(tokenIndex)
                ?: return
        val center =
            LudoPawsFxBoardGeometry.tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = position,
                cell = cell,
            ) ?: return
        val plan =
            LudoPawsFxPolicy.plan(
                cue = reaction.animationCue,
                reducedMotion = reducedMotion,
            )

        painter.drawReaction(
            canvas = canvas,
            center = center,
            cue = reaction.animationCue,
            cell = cell,
            progress = reactionProgress,
            particleCount = plan.particleCount,
            allowConfetti = plan.allowConfetti,
            reducedMotion = reducedMotion,
        )
    }

    private fun representativeTokenIndex(player: PlayerSnapshot): Int =
        player.tokens
            .indexOfFirst { it >= 0 }
            .takeIf { it >= 0 }
            ?: 0

    private fun playerColor(color: String): Int =
        when (color) {
            "RED" -> Color.rgb(229, 57, 53)
            "GREEN" -> Color.rgb(46, 160, 67)
            "YELLOW" -> Color.rgb(255, 193, 7)
            "BLUE" -> Color.rgb(30, 136, 229)
            else -> Color.WHITE
        }

    private fun finishReactionAnimation() {
        reactionAnimator = null
        reactions = emptyList()
        reactionProgress = 0f
        invalidate()
    }

    private companion object {
        const val MAX_SIMULTANEOUS_REACTIONS = 6
        const val MAX_TRANSITIONS = 8
        const val TRANSITION_DURATION_MS = 720L
    }
}
