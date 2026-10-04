package com.ludoproof.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFxPolicy
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Dedicated Phase 10 character-expression layer.
 *
 * The board pawn remains readable while a larger copy of the equipped animal
 * briefly reacts near its home corner. All transforms are presentation-only.
 */
internal class LudoPawsCharacterReactionOverlayView(
    context: Context,
) : View(context) {
    private var snapshot: MatchSnapshot? = null
    private var perspectiveColor: String? = null
    private var characterIdsBySeat: List<String> = emptyList()
    private var reactions: List<LudoPawsReaction> = emptyList()
    private var reducedMotion = false
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val drawableCache = mutableMapOf<String, Drawable?>()
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
    private val badgePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val facePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(
        state: MatchSnapshot?,
        perspectiveColor: String?,
        characterIdsBySeat: List<String>,
    ) {
        snapshot = state
        this.perspectiveColor =
            perspectiveColor?.takeIf {
                it in OfflinePlayerLayout.COLORS
            }
        this.characterIdsBySeat = characterIdsBySeat
        invalidate()
    }

    fun play(
        reactions: List<LudoPawsReaction>,
        reducedMotion: Boolean,
    ) {
        animator?.cancel()
        this.reducedMotion = reducedMotion
        this.reactions = reactions.take(MAX_REACTIONS)
        progress = 0f
        if (this.reactions.isEmpty()) {
            invalidate()
            return
        }
        val duration =
            this.reactions.maxOf {
                LudoPawsFxPolicy.plan(
                    cue = it.animationCue,
                    reducedMotion = reducedMotion,
                ).durationMs
            }
        animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            finish()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            finish()
                        }
                    },
                )
                start()
            }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        reactions = emptyList()
        progress = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stop()
        drawableCache.clear()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val state = snapshot ?: return
        if (reactions.isEmpty()) {
            return
        }
        val size = min(width, height).toFloat()
        if (size <= 0f) {
            return
        }
        val cell = size / 15f
        val turns =
            perspectiveColor
                ?.let(OfflinePlayerLayout::rotationQuarterTurns)
                ?: 0
        if (turns != 0) {
            canvas.save()
            canvas.rotate(turns * 90f, size / 2f, size / 2f)
        }

        reactions.forEach { reaction ->
            val player =
                state.players.firstOrNull {
                    it.playerId == reaction.playerId
                } ?: return@forEach
            drawCharacterReaction(
                canvas = canvas,
                player = player,
                reaction = reaction,
                cell = cell,
            )
        }

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun drawCharacterReaction(
        canvas: Canvas,
        player: PlayerSnapshot,
        reaction: LudoPawsReaction,
        cell: Float,
    ) {
        val characterId =
            characterIdsBySeat.getOrNull(reaction.seat)
                ?: characterIdsBySeat.getOrNull(player.seat)
                ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
        val character =
            LudoPawsCharacterCatalog.character(characterId)
                ?: LudoPawsCharacterCatalog.character(
                    LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID,
                )
                ?: return
        val drawable = resolveDrawable(character.fallbackDrawableName)
        val anchor = yardReactionAnchor(player.color, cell) ?: return
        val cue = reaction.animationCue
        val plan = LudoPawsFxPolicy.plan(cue, reducedMotion)
        val alpha = ((1f - progress) * 255f).toInt().coerceIn(0, 255)

        val transform = transformFor(cue, plan, cell)
        val portraitSize = cell * 1.25f * transform.scale

        canvas.save()
        canvas.translate(
            anchor.first + transform.offsetX,
            anchor.second + transform.offsetY,
        )
        if (plan.allowRotation) {
            canvas.rotate(transform.rotationDegrees)
        }

        ringPaint.color = withAlpha(playerColor(player.color), alpha)
        ringPaint.strokeWidth = cell * .09f
        canvas.drawCircle(0f, 0f, portraitSize * .54f, ringPaint)

        drawable?.let {
            it.alpha = alpha
            it.bounds =
                Rect(
                    (-portraitSize / 2f).toInt(),
                    (-portraitSize / 2f).toInt(),
                    (portraitSize / 2f).toInt(),
                    (portraitSize / 2f).toInt(),
                )
            it.draw(canvas)
            it.alpha = 255
        }

        drawEmotionBadge(
            canvas = canvas,
            cue = cue,
            cell = cell,
            alpha = alpha,
            portraitRadius = portraitSize * .52f,
        )
        canvas.restore()
    }

    private data class CharacterTransform(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
        val rotationDegrees: Float,
    )

    private fun transformFor(
        cue: AnimationCue,
        plan: com.ludoproof.game.feature.characters.domain.reaction.LudoPawsFxPlan,
        cell: Float,
    ): CharacterTransform {
        if (reducedMotion) {
            return CharacterTransform(
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                rotationDegrees = 0f,
            )
        }

        val wave = sin(progress * PI).toFloat()
        val fastWave = sin(progress * PI * 8.0).toFloat()
        return when (cue) {
            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            AnimationCue.HOME,
            AnimationCue.VICTORY,
            ->
                CharacterTransform(
                    scale = 1f + .16f * wave,
                    offsetX = 0f,
                    offsetY = if (plan.allowTranslation) -cell * .30f * wave else 0f,
                    rotationDegrees = if (plan.allowRotation) fastWave * 5f else 0f,
                )

            AnimationCue.CAPTURE ->
                CharacterTransform(
                    scale = 1f + .22f * wave,
                    offsetX = if (plan.allowTranslation) cell * .20f * wave else 0f,
                    offsetY = -cell * .12f * wave,
                    rotationDegrees = fastWave * 7f,
                )

            AnimationCue.CAPTURED,
            AnimationCue.ANGRY,
            ->
                CharacterTransform(
                    scale = 1f - .05f * wave,
                    offsetX = if (plan.allowShake) cell * .12f * fastWave else 0f,
                    offsetY = 0f,
                    rotationDegrees = if (plan.allowRotation) fastWave * 6f else 0f,
                )

            AnimationCue.NERVOUS ->
                CharacterTransform(
                    scale = 1f,
                    offsetX = if (plan.allowShake) cell * .06f * fastWave else 0f,
                    offsetY = 0f,
                    rotationDegrees = if (plan.allowRotation) fastWave * 4f else 0f,
                )

            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            ->
                CharacterTransform(
                    scale = 1f - .10f * wave,
                    offsetX = 0f,
                    offsetY = if (plan.allowTranslation) cell * .18f * wave else 0f,
                    rotationDegrees = if (plan.allowRotation) -7f * wave else 0f,
                )

            AnimationCue.IDLE -> {
                val blink =
                    if (progress in .42f..55f) .94f else 1f
                CharacterTransform(
                    scale = blink + .025f * wave,
                    offsetX = 0f,
                    offsetY = -cell * .025f * wave,
                    rotationDegrees = 0f,
                )
            }

            AnimationCue.SAFE ->
                CharacterTransform(
                    scale = 1f + .06f * wave,
                    offsetX = 0f,
                    offsetY = 0f,
                    rotationDegrees = 0f,
                )
        }
    }

    private fun drawEmotionBadge(
        canvas: Canvas,
        cue: AnimationCue,
        cell: Float,
        alpha: Int,
        portraitRadius: Float,
    ) {
        val cx = portraitRadius * .72f
        val cy = -portraitRadius * .72f
        val radius = cell * .25f
        badgePaint.color = withAlpha(Color.WHITE, (alpha * .95f).toInt())
        canvas.drawCircle(cx, cy, radius, badgePaint)
        ringPaint.color = withAlpha(reactionColor(cue), alpha)
        ringPaint.strokeWidth = cell * .045f
        canvas.drawCircle(cx, cy, radius, ringPaint)

        facePaint.color = withAlpha(Color.rgb(25, 36, 50), alpha)
        facePaint.strokeWidth = cell * .045f
        val eyeY = cy - radius * .20f
        when (cue) {
            AnimationCue.IDLE -> {
                canvas.drawLine(cx - radius * .48f, eyeY, cx - radius * .18f, eyeY, facePaint)
                canvas.drawLine(cx + radius * .18f, eyeY, cx + radius * .48f, eyeY, facePaint)
            }

            AnimationCue.ANGRY -> {
                canvas.drawLine(cx - radius * .50f, eyeY - radius * .12f, cx - radius * .15f, eyeY + radius * .04f, facePaint)
                canvas.drawLine(cx + radius * .15f, eyeY + radius * .04f, cx + radius * .50f, eyeY - radius * .12f, facePaint)
            }

            else -> {
                canvas.drawCircle(cx - radius * .32f, eyeY, radius * .08f, facePaint)
                canvas.drawCircle(cx + radius * .32f, eyeY, radius * .08f, facePaint)
            }
        }

        val mouthY = cy + radius * .24f
        val smile =
            cue in setOf(
                AnimationCue.EXCITED,
                AnimationCue.HAPPY,
                AnimationCue.CAPTURE,
                AnimationCue.SAFE,
                AnimationCue.HOME,
                AnimationCue.VICTORY,
            )
        val sad =
            cue in setOf(
                AnimationCue.CAPTURED,
                AnimationCue.SAD,
                AnimationCue.DEFEAT,
            )
        when {
            smile -> {
                val path = android.graphics.Path().apply {
                    moveTo(cx - radius * .36f, mouthY - radius * .05f)
                    quadTo(cx, mouthY + radius * .36f, cx + radius * .36f, mouthY - radius * .05f)
                }
                canvas.drawPath(path, facePaint)
            }

            sad -> {
                val path = android.graphics.Path().apply {
                    moveTo(cx - radius * .36f, mouthY + radius * .14f)
                    quadTo(cx, mouthY - radius * .22f, cx + radius * .36f, mouthY + radius * .14f)
                }
                canvas.drawPath(path, facePaint)
            }

            else ->
                canvas.drawLine(
                    cx - radius * .25f,
                    mouthY,
                    cx + radius * .25f,
                    mouthY,
                    facePaint,
                )
        }
    }

    private fun resolveDrawable(name: String): Drawable? =
        drawableCache.getOrPut(name) {
            val id =
                resources.getIdentifier(
                    name,
                    "drawable",
                    context.packageName,
                )
            if (id == 0) {
                null
            } else {
                runCatching {
                    context.getDrawable(id)?.mutate()
                }.getOrNull()
            }
        }

    private fun yardReactionAnchor(
        color: String,
        cell: Float,
    ): Pair<Float, Float>? =
        when (color) {
            "RED" -> 3.0f * cell to 3.0f * cell
            "GREEN" -> 12.0f * cell to 3.0f * cell
            "YELLOW" -> 12.0f * cell to 12.0f * cell
            "BLUE" -> 3.0f * cell to 12.0f * cell
            else -> null
        }

    private fun playerColor(color: String): Int =
        when (color) {
            "RED" -> Color.rgb(229, 57, 53)
            "GREEN" -> Color.rgb(46, 160, 67)
            "YELLOW" -> Color.rgb(255, 193, 7)
            "BLUE" -> Color.rgb(30, 136, 229)
            else -> Color.WHITE
        }

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

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun finish() {
        animator = null
        reactions = emptyList()
        progress = 0f
        invalidate()
    }

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density

    private companion object {
        const val MAX_REACTIONS = 4
    }
}
