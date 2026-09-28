package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

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
            color = 0xFF20344F.toInt()
        }
    private val pipPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111827.toInt()
            style = Paint.Style.FILL
            setShadowLayer(
                dp(1.5f),
                0f,
                dp(1f),
                0x55000000,
            )
        }
    private val rollingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            color = LudoProofTheme.GOLD
            setShadowLayer(
                dp(6f),
                0f,
                0f,
                0xAAFFB000.toInt(),
            )
        }

    private var face = 1
    private var rolling = false

    private val ticker =
        object : Runnable {
            override fun run() {
                if (!rolling) return
                face = face % 6 + 1
                invalidate()
                postDelayed(
                    this,
                    85L,
                )
            }
        }

    fun startRolling() {
        if (rolling) return
        rolling = true
        contentDescription =
            "Dice verification in progress."
        removeCallbacks(ticker)
        post(ticker)
    }

    fun showOutcome(outcome: Int) {
        if (outcome !in 1..6) return
        rolling = false
        removeCallbacks(ticker)
        face = outcome
        contentDescription =
            "Verified dice outcome $outcome."
        invalidate()
    }

    fun stopRolling() {
        rolling = false
        removeCallbacks(ticker)
        contentDescription =
            "Dice verification stopped."
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
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

        val shadow =
            RectF(
                size * .11f,
                size * .14f,
                size * .92f,
                size * .94f,
            )
        facePaint.shader = null
        facePaint.color =
            0x55000000
        facePaint.setShadowLayer(
            dp(8f),
            0f,
            dp(4f),
            0x77000000,
        )
        canvas.drawRoundRect(
            shadow,
            size * .16f,
            size * .16f,
            facePaint,
        )
        facePaint.clearShadowLayer()

        val inset =
            size * .10f
        val rect =
            RectF(
                inset,
                inset,
                size - inset,
                size - inset,
            )
        facePaint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(
                    Color.WHITE,
                    0xFFF3F6FA.toInt(),
                    0xFFD5DDE8.toInt(),
                ),
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

        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            if (rolling) {
                rollingPaint
            } else {
                borderPaint
            },
        )

        val left = size * .31f
        val center = size * .50f
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
            1 -> pip(center, middle)
            2 -> {
                pip(left, top)
                pip(right, bottom)
            }
            3 -> {
                pip(left, top)
                pip(center, middle)
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
                pip(center, middle)
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
    }

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
