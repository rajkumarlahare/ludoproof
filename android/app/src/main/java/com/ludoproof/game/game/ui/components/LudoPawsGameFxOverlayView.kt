package com.ludoproof.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFxPolicy
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Phase 10 presentation-only board FX layer.
 *
 * It derives token transitions only to visualize state the game engine has
 * already committed. It never mutates MatchSnapshot, legal moves, turns, dice,
 * captures, winners, proof data, or persistence.
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

    private var previousSnapshot: MatchSnapshot? = null
    private var snapshot: MatchSnapshot? = null
    private var perspectiveColor: String? = null
    private var reactions: List<LudoPawsReaction> = emptyList()
    private var transitions: List<TokenTransition> = emptyList()
    private var reducedMotion = false
    private var reactionProgress = 0f
    private var transitionProgress = 1f
    private var reactionAnimator: ValueAnimator? = null
    private var transitionAnimator: ValueAnimator? = null

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(3.2f)
        }
    private val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = density(2.3f)
        }
    private val particlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val ghostPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

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
        previousSnapshot = previous
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
            this.reactions
                .maxOf {
                    LudoPawsFxPolicy
                        .plan(
                            cue = it.animationCue,
                            reducedMotion = reducedMotion,
                        )
                        .durationMs
                }

        reactionAnimator =
            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
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
        val cell = size / BOARD_SIZE
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

        drawTransitions(
            canvas = canvas,
            current = current,
            cell = cell,
        )
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
            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
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
        if (previous == null || current == null || previous.matchId != current.matchId) {
            return emptyList()
        }
        val previousById = previous.players.associateBy(PlayerSnapshot::playerId)
        return buildList {
            current.players.forEach { player ->
                val before = previousById[player.playerId] ?: return@forEach
                player.tokens.forEachIndexed { tokenIndex, toPosition ->
                    val fromPosition = before.tokens.getOrNull(tokenIndex) ?: return@forEachIndexed
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
        current: MatchSnapshot,
        cell: Float,
    ) {
        transitions.forEach { transition ->
            val from =
                tokenCenter(
                    color = transition.color,
                    tokenIndex = transition.tokenIndex,
                    position = transition.fromPosition,
                    cell = cell,
                )
            val to =
                tokenCenter(
                    color = transition.color,
                    tokenIndex = transition.tokenIndex,
                    position = transition.toPosition,
                    cell = cell,
                )
            if (from == null || to == null) {
                return@forEach
            }

            when {
                transition.isCaptureReturn ->
                    drawCaptureReturn(
                        canvas = canvas,
                        from = from,
                        to = to,
                        color = playerColor(transition.color),
                        cell = cell,
                    )

                transition.isHomeArrival ->
                    drawPawTrail(
                        canvas = canvas,
                        from = from,
                        to = to,
                        color = playerColor(transition.color),
                        cell = cell,
                        emphasis = 1f,
                    )

                else -> {
                    // Keep ordinary movement subtle because the board already
                    // owns the authoritative step animation.
                    drawPawTrail(
                        canvas = canvas,
                        from = from,
                        to = to,
                        color = playerColor(transition.color),
                        cell = cell,
                        emphasis = .55f,
                    )
                }
            }
        }

        if (
            transitions.any(TokenTransition::isHomeArrival) &&
            !reducedMotion
        ) {
            drawHomeStars(
                canvas = canvas,
                cell = cell,
                progress = transitionProgress,
            )
        }
    }

    private fun drawCaptureReturn(
        canvas: Canvas,
        from: Pair<Float, Float>,
        to: Pair<Float, Float>,
        color: Int,
        cell: Float,
    ) {
        if (reducedMotion) {
            drawDestinationFlash(
                canvas = canvas,
                center = to,
                color = color,
                cell = cell,
            )
            return
        }

        val eased = easeOutCubic(transitionProgress)
        val x = lerp(from.first, to.first, eased)
        val y = lerp(from.second, to.second, eased)
        val fade = (1f - transitionProgress).coerceIn(0f, 1f)

        linePaint.color = withAlpha(color, (120f * fade).roundToInt())
        linePaint.strokeWidth = cell * .12f
        canvas.drawLine(from.first, from.second, x, y, linePaint)

        ghostPaint.color = withAlpha(color, (220f * fade).roundToInt())
        canvas.drawCircle(
            x,
            y,
            cell * (.25f + .05f * sin(transitionProgress * PI).toFloat()),
            ghostPaint,
        )
        drawPawMark(
            canvas = canvas,
            x = x,
            y = y,
            radius = cell * .14f,
            color = Color.WHITE,
            alpha = (210f * fade).roundToInt(),
        )

        if (transitionProgress > .72f) {
            drawDestinationFlash(
                canvas = canvas,
                center = to,
                color = color,
                cell = cell,
            )
        }
    }

    private fun drawPawTrail(
        canvas: Canvas,
        from: Pair<Float, Float>,
        to: Pair<Float, Float>,
        color: Int,
        cell: Float,
        emphasis: Float,
    ) {
        if (reducedMotion) {
            drawDestinationFlash(canvas, to, color, cell)
            return
        }

        val fade = (1f - transitionProgress).coerceIn(0f, 1f)
        if (fade <= 0f) {
            return
        }
        repeat(PAW_TRAIL_COUNT) { index ->
            val fraction =
                ((index + 1f) / (PAW_TRAIL_COUNT + 1f)) *
                    transitionProgress.coerceAtLeast(.18f)
            val x = lerp(from.first, to.first, fraction)
            val y = lerp(from.second, to.second, fraction)
            drawPawMark(
                canvas = canvas,
                x = x,
                y = y,
                radius = cell * .085f * emphasis,
                color = color,
                alpha = (180f * fade * emphasis).roundToInt(),
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
        val position = player.tokens.getOrNull(tokenIndex) ?: return
        val center =
            tokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                position = position,
                cell = cell,
            ) ?: return

        val cue = reaction.animationCue
        val plan = LudoPawsFxPolicy.plan(cue, reducedMotion)
        val color = reactionColor(cue)
        val fade = (1f - reactionProgress).coerceIn(0f, 1f)
        val pulse =
            if (reducedMotion) {
                cell * .46f
            } else {
                cell * (.30f + .58f * easeOutCubic(reactionProgress))
            }

        glowPaint.color = withAlpha(color, (75f * fade).roundToInt())
        canvas.drawCircle(center.first, center.second, pulse * .86f, glowPaint)
        ringPaint.color = withAlpha(color, (225f * fade).roundToInt())
        canvas.drawCircle(center.first, center.second, pulse, ringPaint)

        when (cue) {
            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            AnimationCue.CAPTURE,
            -> drawBurst(canvas, center, color, cell, plan.particleCount, reactionProgress)

            AnimationCue.CAPTURED ->
                drawImpact(canvas, center, color, cell, reactionProgress, plan.particleCount)

            AnimationCue.SAFE ->
                drawSafeShield(canvas, center, color, cell, reactionProgress)

            AnimationCue.HOME -> {
                drawBurst(canvas, center, color, cell, plan.particleCount, reactionProgress)
                drawHomeStars(canvas, cell, reactionProgress)
            }

            AnimationCue.ANGRY ->
                drawAngryBolts(canvas, center, cell, reactionProgress)

            AnimationCue.NERVOUS ->
                drawNervousOrbit(canvas, center, color, cell, reactionProgress)

            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            -> drawSadDrops(canvas, center, cell, reactionProgress, plan.particleCount)

            AnimationCue.VICTORY -> {
                drawBurst(canvas, center, color, cell, plan.particleCount / 2, reactionProgress)
                if (plan.allowConfetti) {
                    drawConfetti(canvas, cell, reactionProgress, plan.particleCount)
                }
            }

            AnimationCue.IDLE ->
                drawIdleBreath(canvas, center, color, cell, reactionProgress)
        }
    }

    private fun drawBurst(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        particleCount: Int,
        progress: Float,
    ) {
        val count = particleCount.coerceIn(2, 18)
        val fade = (1f - progress).coerceIn(0f, 1f)
        particlePaint.color = withAlpha(color, (230f * fade).roundToInt())
        repeat(count) { index ->
            val angle = 2.0 * PI * index / count + progress * .55
            val radius = cell * (.36f + progress * .72f)
            val x = center.first + cos(angle).toFloat() * radius
            val y = center.second + sin(angle).toFloat() * radius
            canvas.drawCircle(x, y, cell * .055f, particlePaint)
        }
    }

    private fun drawImpact(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
        particleCount: Int,
    ) {
        val count = particleCount.coerceIn(4, 12)
        linePaint.strokeWidth = cell * .08f
        linePaint.color = withAlpha(color, ((1f - progress) * 230f).roundToInt())
        repeat(count) { index ->
            val angle = 2.0 * PI * index / count
            val inner = cell * (.24f + .16f * progress)
            val outer = cell * (.52f + .28f * progress)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            canvas.drawLine(
                center.first + dx * inner,
                center.second + dy * inner,
                center.first + dx * outer,
                center.second + dy * outer,
                linePaint,
            )
        }
    }

    private fun drawSafeShield(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val fade = (1f - progress).coerceIn(0f, 1f)
        repeat(if (reducedMotion) 1 else 3) { index ->
            ringPaint.color = withAlpha(color, (200f * fade / (index + 1)).roundToInt())
            ringPaint.strokeWidth = cell * .07f
            canvas.drawCircle(
                center.first,
                center.second,
                cell * (.34f + index * .14f + progress * .15f),
                ringPaint,
            )
        }
    }

    private fun drawHomeStars(
        canvas: Canvas,
        cell: Float,
        progress: Float,
    ) {
        val count = if (reducedMotion) 4 else 10
        repeat(count) { index ->
            val angle = (index * 2.399963f) + progress
            val radius = cell * (1.0f + (index % 3) * .7f)
            val cx = cell * 7.5f + cos(angle) * radius
            val cy = cell * 7.5f + sin(angle) * radius -
                if (reducedMotion) 0f else cell * progress * .7f
            drawStar(
                canvas = canvas,
                cx = cx,
                cy = cy,
                radius = cell * .10f,
                color = Color.rgb(255, 214, 64),
                alpha = ((1f - progress) * 230f).roundToInt(),
            )
        }
    }

    private fun drawAngryBolts(
        canvas: Canvas,
        center: Pair<Float, Float>,
        cell: Float,
        progress: Float,
    ) {
        linePaint.color = withAlpha(
            Color.rgb(255, 92, 62),
            ((1f - progress) * 245f).roundToInt(),
        )
        linePaint.strokeWidth = cell * .08f
        repeat(if (reducedMotion) 2 else 4) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val yOffset = (index / 2) * cell * .20f
            val path = Path().apply {
                moveTo(center.first + side * cell * .38f, center.second - cell * .42f + yOffset)
                lineTo(center.first + side * cell * .58f, center.second - cell * .14f + yOffset)
                lineTo(center.first + side * cell * .43f, center.second + cell * .02f + yOffset)
                lineTo(center.first + side * cell * .68f, center.second + cell * .30f + yOffset)
            }
            canvas.drawPath(path, linePaint)
        }
    }

    private fun drawNervousOrbit(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val count = if (reducedMotion) 2 else 5
        repeat(count) { index ->
            val angle = 2.0 * PI * index / count + progress * 8f
            val radius = cell * .48f
            particlePaint.color = withAlpha(color, ((1f - progress) * 220f).roundToInt())
            canvas.drawCircle(
                center.first + cos(angle).toFloat() * radius,
                center.second + sin(angle).toFloat() * radius,
                cell * .045f,
                particlePaint,
            )
        }
    }

    private fun drawSadDrops(
        canvas: Canvas,
        center: Pair<Float, Float>,
        cell: Float,
        progress: Float,
        particleCount: Int,
    ) {
        val count = particleCount.coerceIn(2, 8)
        particlePaint.color = withAlpha(
            Color.rgb(70, 150, 255),
            ((1f - progress) * 210f).roundToInt(),
        )
        repeat(count) { index ->
            val offset = (index - count / 2f) * cell * .12f
            val y = center.second + cell * (.30f + progress * if (reducedMotion) .05f else .55f)
            canvas.drawOval(
                center.first + offset - cell * .035f,
                y - cell * .07f,
                center.first + offset + cell * .035f,
                y + cell * .07f,
                particlePaint,
            )
        }
    }

    private fun drawConfetti(
        canvas: Canvas,
        cell: Float,
        progress: Float,
        particleCount: Int,
    ) {
        val widthPx = cell * BOARD_SIZE
        val heightPx = widthPx
        val colors = CONFETTI_COLORS
        repeat(particleCount.coerceIn(8, 40)) { index ->
            val xSeed = ((index * 37) % 97) / 97f
            val ySeed = ((index * 53) % 89) / 89f
            val x = widthPx * xSeed + sin(progress * 8f + index) * cell * .25f
            val y = heightPx * ((ySeed * .35f + progress * 1.05f) % 1f)
            particlePaint.color =
                withAlpha(
                    colors[index % colors.size],
                    ((1f - progress * .55f) * 235f).roundToInt(),
                )
            canvas.save()
            canvas.rotate(progress * 360f + index * 19f, x, y)
            canvas.drawRect(
                x - cell * .055f,
                y - cell * .10f,
                x + cell * .055f,
                y + cell * .10f,
                particlePaint,
            )
            canvas.restore()
        }
    }

    private fun drawIdleBreath(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val wave = if (reducedMotion) 0f else sin(progress * PI).toFloat()
        ringPaint.color = withAlpha(color, ((1f - progress) * 120f).roundToInt())
        ringPaint.strokeWidth = cell * .045f
        canvas.drawCircle(
            center.first,
            center.second,
            cell * (.34f + wave * .06f),
            ringPaint,
        )
    }

    private fun drawDestinationFlash(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
    ) {
        glowPaint.color = withAlpha(color, 90)
        canvas.drawCircle(center.first, center.second, cell * .40f, glowPaint)
        ringPaint.color = withAlpha(color, 220)
        ringPaint.strokeWidth = cell * .07f
        canvas.drawCircle(center.first, center.second, cell * .34f, ringPaint)
    }

    private fun drawPawMark(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
        alpha: Int,
    ) {
        particlePaint.color = withAlpha(color, alpha)
        canvas.drawOval(
            x - radius * .70f,
            y - radius * .25f,
            x + radius * .70f,
            y + radius * .80f,
            particlePaint,
        )
        repeat(3) { index ->
            val dx = (index - 1) * radius * .58f
            canvas.drawCircle(
                x + dx,
                y - radius * .62f,
                radius * .34f,
                particlePaint,
            )
        }
    }

    private fun drawStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
        alpha: Int,
    ) {
        val path = Path()
        repeat(10) { index ->
            val angle = -PI / 2 + index * PI / 5
            val r = if (index % 2 == 0) radius else radius * .45f
            val x = cx + cos(angle).toFloat() * r
            val y = cy + sin(angle).toFloat() * r
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        particlePaint.color = withAlpha(color, alpha)
        canvas.drawPath(path, particlePaint)
    }

    private fun representativeTokenIndex(player: PlayerSnapshot): Int =
        player.tokens
            .indexOfFirst { it >= 0 }
            .takeIf { it >= 0 }
            ?: 0

    private fun tokenCenter(
        color: String,
        tokenIndex: Int,
        position: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        if (position == -1) {
            return yardTokenCenter(color, tokenIndex, cell)
        }
        if (position in 0..51) {
            val offset = START_OFFSETS[color] ?: return null
            val coord = TRACK[(offset + position) % TRACK.size]
            return centerForCell(coord.first, coord.second, cell)
        }
        if (position in 52..56) {
            val lane = HOME_LANES[color] ?: return null
            val coord = lane[position - 52]
            return centerForCell(coord.first, coord.second, cell)
        }
        if (position == 57) {
            val unit =
                when (color) {
                    "RED" -> 6.9f to 7.5f
                    "GREEN" -> 7.5f to 6.9f
                    "YELLOW" -> 8.1f to 7.5f
                    "BLUE" -> 7.5f to 8.1f
                    else -> 7.5f to 7.5f
                }
            return unit.first * cell to unit.second * cell
        }
        return null
    }

    private fun yardTokenCenter(
        color: String,
        tokenIndex: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        val origin =
            when (color) {
                "RED" -> 0f to 0f
                "GREEN" -> 0f to 9f
                "YELLOW" -> 9f to 9f
                "BLUE" -> 9f to 0f
                else -> return null
            }
        val slot =
            when (tokenIndex) {
                0 -> 2f to 2f
                1 -> 2f to 4f
                2 -> 4f to 2f
                else -> 4f to 4f
            }
        return (origin.second + slot.second) * cell to
            (origin.first + slot.first) * cell
    }

    private fun centerForCell(
        row: Int,
        col: Int,
        cell: Float,
    ): Pair<Float, Float> =
        (col + .5f) * cell to (row + .5f) * cell

    private fun reactionColor(cue: AnimationCue): Int =
        when (cue) {
            AnimationCue.CAPTURED,
            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            -> Color.rgb(232, 72, 85)

            AnimationCue.ANGRY -> Color.rgb(255, 112, 67)
            AnimationCue.SAFE -> Color.rgb(72, 202, 228)
            AnimationCue.HOME,
            AnimationCue.VICTORY,
            -> Color.rgb(255, 193, 7)

            AnimationCue.NERVOUS -> Color.rgb(171, 71, 188)
            AnimationCue.IDLE,
            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            AnimationCue.CAPTURE,
            -> Color.rgb(255, 235, 59)
        }

    private fun playerColor(color: String): Int =
        when (color) {
            "RED" -> Color.rgb(229, 57, 53)
            "GREEN" -> Color.rgb(46, 160, 67)
            "YELLOW" -> Color.rgb(255, 193, 7)
            "BLUE" -> Color.rgb(30, 136, 229)
            else -> Color.WHITE
        }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)

    private fun easeOutCubic(value: Float): Float {
        val t = 1f - value.coerceIn(0f, 1f)
        return 1f - t * t * t
    }

    private fun finishReactionAnimation() {
        reactionAnimator = null
        reactions = emptyList()
        reactionProgress = 0f
        invalidate()
    }

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density

    private companion object {
        const val BOARD_SIZE = 15f
        const val MAX_SIMULTANEOUS_REACTIONS = 6
        const val MAX_TRANSITIONS = 8
        const val PAW_TRAIL_COUNT = 4
        const val TRANSITION_DURATION_MS = 720L

        val CONFETTI_COLORS =
            intArrayOf(
                Color.rgb(255, 193, 7),
                Color.rgb(244, 67, 54),
                Color.rgb(33, 150, 243),
                Color.rgb(76, 175, 80),
                Color.rgb(156, 39, 176),
            )

        val START_OFFSETS =
            mapOf(
                "RED" to 0,
                "GREEN" to 13,
                "YELLOW" to 26,
                "BLUE" to 39,
            )

        val TRACK =
            listOf(
                6 to 1, 6 to 2, 6 to 3, 6 to 4, 6 to 5,
                5 to 6, 4 to 6, 3 to 6, 2 to 6, 1 to 6,
                0 to 6, 0 to 7, 0 to 8, 1 to 8, 2 to 8,
                3 to 8, 4 to 8, 5 to 8, 6 to 9, 6 to 10,
                6 to 11, 6 to 12, 6 to 13, 6 to 14, 7 to 14,
                8 to 14, 8 to 13, 8 to 12, 8 to 11, 8 to 10,
                8 to 9, 9 to 8, 10 to 8, 11 to 8, 12 to 8,
                13 to 8, 14 to 8, 14 to 7, 14 to 6, 13 to 6,
                12 to 6, 11 to 6, 10 to 6, 9 to 6, 8 to 5,
                8 to 4, 8 to 3, 8 to 2, 8 to 1, 8 to 0,
                7 to 0, 6 to 0,
            )

        val HOME_LANES =
            mapOf(
                "RED" to listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),
                "GREEN" to listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),
                "YELLOW" to listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),
                "BLUE" to listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7),
            )
    }
}
