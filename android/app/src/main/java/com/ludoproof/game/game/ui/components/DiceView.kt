package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory

class DiceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    init {
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription =
            "Dice. No verified outcome yet."
        setLayerType(
            LAYER_TYPE_SOFTWARE,
            null,
        )
    }

    private val facePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.4f)
            color = 0xFF8A8A8A.toInt()
        }
    private val pipPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3A3A3A.toInt()
            style = Paint.Style.FILL
            setShadowLayer(
                dp(1.2f),
                0f,
                dp(.8f),
                0x26000000,
            )
        }
    private val rollingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            color = 0xFFB8B8B8.toInt()
            setShadowLayer(
                dp(4f),
                0f,
                0f,
                0x446B7280,
            )
        }
    // The actionable-turn cue gets a warm gold edge instead of the idle grey.
    // Both strokes are drawn inward enough to stay complete during the breathing zoom.
    private val attentionPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.8f)
            color = 0xFFFFD54F.toInt()
            strokeJoin = Paint.Join.ROUND
        }
    private val attentionGlowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            color = 0xFFFFC107.toInt()
            strokeJoin = Paint.Join.ROUND
        }

    private var diceStyleId =
        "dice_classic"
    private var face = 1
    private var rolling = false
    private var settling = false
    private var rollStartedAtMillis = 0L
    private var settleStartedAtMillis = 0L
    private var settleStartRotationDegrees = 18f
    private var targetOutcome = 1
    private var rotationDegrees = 0f
    private var scale = 1f
    private var translationYFraction = 0f
    private var borderPulse = 0f
    private var attentionPulsing = false
    private var attentionPhaseStartedAtMillis = 0L

    private val animationTicker =
        object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                when {
                    rolling -> {
                        val elapsed =
                            now - rollStartedAtMillis
                        applyFrame(
                            DiceRollAnimationPolicy
                                .rollingFrame(elapsed),
                        )
                    }

                    settling -> {
                        val frame =
                            DiceRollAnimationPolicy
                                .settleFrame(
                                    elapsedMillis =
                                        now - settleStartedAtMillis,
                                    outcome = targetOutcome,
                                    startRotationDegrees =
                                        settleStartRotationDegrees,
                                )
                        applyFrame(frame)
                        if (frame.finished) {
                            settling = false
                            resetTransform()
                        }
                    }

                    attentionPulsing -> {
                        val elapsed =
                            (now - attentionPhaseStartedAtMillis)
                                .coerceAtLeast(0L)
                        applyFrame(
                            DiceAttentionAnimationPolicy.frame(
                                elapsedMillis = elapsed,
                            ),
                        )
                    }

                    else -> return
                }

                invalidate()
                // The attention cue is a real animation, not a one-shot callback.
                // Keep scheduling its frames while enabled, just like rolling and
                // settling, and let every phase share this single ticker.
                if (rolling || settling || attentionPulsing) {
                    postOnAnimation(this)
                }
            }
        }

    fun setAttentionEnabled(enabled: Boolean) {
        val shouldPulse =
            enabled &&
                !rolling &&
                !settling
        if (shouldPulse == attentionPulsing) {
            return
        }

        attentionPulsing = shouldPulse
        if (shouldPulse) {
            attentionPhaseStartedAtMillis = SystemClock.uptimeMillis()
            postOnAnimation(animationTicker)
        } else {
            removeCallbacks(animationTicker)
            resetTransform()
            invalidate()
        }
    }

    fun startRolling() {
        if (rolling) return
        attentionPulsing = false
        removeCallbacks(animationTicker)
        rolling = true
        settling = false
        rollStartedAtMillis =
            SystemClock.uptimeMillis()
        contentDescription =
            "Dice verification in progress."
        postOnAnimation(animationTicker)
    }

    fun showOutcome(
        outcome: Int,
        animate: Boolean = true,
    ) {
        if (outcome !in 1..6) return

        attentionPulsing = false
        val wasRolling = rolling
        rolling = false
        removeCallbacks(animationTicker)
        targetOutcome = outcome
        face = outcome
        contentDescription =
            "Dice outcome $outcome."

        if (!animate) {
            settling = false
            resetTransform()
            invalidate()
            return
        }

        settleStartRotationDegrees =
            if (wasRolling && kotlin.math.abs(rotationDegrees) > 1f) {
                rotationDegrees.coerceIn(-20f, 20f)
            } else {
                18f
            }
        settling = true
        settleStartedAtMillis =
            SystemClock.uptimeMillis()
        postOnAnimation(animationTicker)
    }

    fun stopRolling() {
        rolling = false
        settling = false
        attentionPulsing = false
        removeCallbacks(animationTicker)
        resetTransform()
        contentDescription =
            "Dice verification stopped."
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reloadStyle()
    }

    fun reloadStyle() {
        diceStyleId =
            CosmeticInventoryStore(
                context,
            )
                .selectedId(
                    CosmeticCategory.DICE,
                )
        invalidate()
    }

    override fun onDetachedFromWindow() {
        rolling = false
        settling = false
        attentionPulsing = false
        removeCallbacks(animationTicker)
        resetTransform()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired =
            dp(96f).toInt()
        val width =
            resolveSize(
                desired,
                widthMeasureSpec,
            )
        val height =
            resolveSize(
                desired,
                heightMeasureSpec,
            )
        val size =
            minOf(
                width,
                height,
            )
        setMeasuredDimension(
            size,
            size,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size =
            minOf(
                width,
                height,
            ).toFloat()
        if (size <= 0f) return

        val center = size * .5f
        val saveCount = canvas.save()
        canvas.translate(
            0f,
            size * translationYFraction,
        )
        canvas.scale(
            scale,
            scale,
            center,
            center,
        )
        canvas.rotate(
            rotationDegrees,
            center,
            center,
        )

        val shadow =
            RectF(
                size * .09f,
                size * .11f,
                size * .91f,
                size * .92f,
            )
        facePaint.shader = null
        facePaint.color =
            0x33000000
        facePaint.setShadowLayer(
            dp(3f),
            0f,
            dp(1.5f),
            0x26000000,
        )
        canvas.drawRoundRect(
            shadow,
            size * .16f,
            size * .16f,
            facePaint,
        )
        facePaint.clearShadowLayer()

        val inset =
            size * .07f
        val rect =
            RectF(
                inset,
                inset,
                size - inset,
                size - inset,
            )
        val palette =
            dicePalette()

        facePaint.alpha = 255
        facePaint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                palette.faceColors,
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            facePaint,
        )
        facePaint.shader = null
        facePaint.color =
            0x42FFFFFF
        canvas.drawRoundRect(
            RectF(
                rect.left + size * .035f,
                rect.top + size * .035f,
                rect.right - size * .035f,
                rect.top + rect.height() * .43f,
            ),
            size * .13f,
            size * .13f,
            facePaint,
        )

        borderPaint.color =
            palette.borderColor
        pipPaint.color =
            palette.pipColor
        rollingPaint.alpha =
            (145f + borderPulse.coerceIn(0f, 1f) * 110f)
                .toInt()
        if (attentionPulsing) {
            // Keep the soft gold halo on the inside of the same rounded outline.
            // An external shadow would be clipped by this View's own drawing bounds.
            val glowInset = dp(2.6f)
            val glowRect =
                RectF(
                    rect.left + glowInset,
                    rect.top + glowInset,
                    rect.right - glowInset,
                    rect.bottom - glowInset,
                )
            attentionGlowPaint.alpha =
                (34f + borderPulse.coerceIn(0f, 1f) * 42f)
                    .toInt()
            canvas.drawRoundRect(
                glowRect,
                (size * .17f - glowInset).coerceAtLeast(0f),
                (size * .17f - glowInset).coerceAtLeast(0f),
                attentionGlowPaint,
            )
            attentionPaint.alpha =
                (190f + borderPulse.coerceIn(0f, 1f) * 65f)
                    .toInt()
        }
        val outlinePaint =
            when {
                rolling || settling -> rollingPaint
                attentionPulsing -> attentionPaint
                else -> borderPaint
            }
        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            outlinePaint,
        )

        val left = size * .31f
        val pipCenter = size * .50f
        val right = size * .69f
        val top = size * .31f
        val middle = size * .50f
        val bottom = size * .69f
        val pipRadius =
            size * .055f

        fun pip(
            x: Float,
            y: Float,
        ) {
            canvas.drawCircle(
                x,
                y,
                pipRadius,
                pipPaint,
            )
        }

        when (face) {
            1 -> pip(pipCenter, middle)
            2 -> {
                pip(left, top)
                pip(right, bottom)
            }
            3 -> {
                pip(left, top)
                pip(pipCenter, middle)
                pip(right, bottom)
            }
            4 -> {
                pip(left, top)
                pip(right, top)
                pip(left, bottom)
                pip(right, bottom)
            }
            5 -> {
                pip(left, top)
                pip(right, top)
                pip(pipCenter, middle)
                pip(left, bottom)
                pip(right, bottom)
            }
            6 -> {
                pip(left, top)
                pip(right, top)
                pip(left, middle)
                pip(right, middle)
                pip(left, bottom)
                pip(right, bottom)
            }
        }

        canvas.restoreToCount(saveCount)
    }

    private fun applyFrame(
        frame: DiceRollAnimationPolicy.Frame,
    ) {
        face = frame.face
        rotationDegrees = frame.rotationDegrees
        scale = frame.scale
        translationYFraction = frame.translationYFraction
        borderPulse = frame.borderPulse
    }

    private fun resetTransform() {
        rotationDegrees = 0f
        scale = 1f
        translationYFraction = 0f
        borderPulse = 0f
    }

    /**
     * Gameplay dice intentionally use one high-contrast neutral palette.
     *
     * The selected cosmetic id is still retained by the inventory/store contract,
     * but the in-match face stays white/light-gray so it remains clearly visible
     * against the dark gameplay HUD in every state.
     */
    private fun dicePalette():
        DicePalette {
        @Suppress("UNUSED_VARIABLE")
        val selectedStyle =
            diceStyleId

        return DicePalette(
            intArrayOf(
                0xFFF4F4F4.toInt(),
                0xFFF4F4F4.toInt(),
                0xFFF4F4F4.toInt(),
            ),
            0xFF8A8A8A.toInt(),
            0xFF3A3A3A.toInt(),
        )
    }

    private data class DicePalette(
        val faceColors: IntArray,
        val borderColor: Int,
        val pipColor: Int,
    )

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
