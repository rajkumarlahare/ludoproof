package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DiceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val facePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 39, 47)
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
        }

    private val pipPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 39, 47)
            style = Paint.Style.FILL
        }

    private val rollingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(13, 110, 253)
            style = Paint.Style.STROKE
            strokeWidth = dp(4f)
        }

    private var face = 1
    private var rolling = false

    private val ticker =
        object : Runnable {
            override fun run() {
                if (!rolling) return
                face = face % 6 + 1
                invalidate()
                postDelayed(this, 90L)
            }
        }

    fun startRolling() {
        if (rolling) return
        rolling = true
        removeCallbacks(ticker)
        post(ticker)
    }

    fun showOutcome(outcome: Int) {
        if (outcome !in 1..6) return
        rolling = false
        removeCallbacks(ticker)
        face = outcome
        invalidate()
    }

    fun stopRolling() {
        rolling = false
        removeCallbacks(ticker)
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
        val desired = dp(92f).toInt()
        val width =
            resolveSize(desired, widthMeasureSpec)
        val height =
            resolveSize(desired, heightMeasureSpec)
        val size = minOf(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return

        val inset = dp(7f)
        val rect =
            RectF(
                inset,
                inset,
                size - inset,
                size - inset,
            )
        val radius = size * 0.16f

        canvas.drawRoundRect(
            rect,
            radius,
            radius,
            facePaint,
        )
        canvas.drawRoundRect(
            rect,
            radius,
            radius,
            if (rolling) rollingPaint else borderPaint,
        )

        val left = size * 0.31f
        val center = size * 0.50f
        val right = size * 0.69f
        val top = size * 0.31f
        val middle = size * 0.50f
        val bottom = size * 0.69f
        val pipRadius = size * 0.055f

        fun pip(x: Float, y: Float) {
            canvas.drawCircle(
                x,
                y,
                pipRadius,
                pipPaint,
            )
        }

        when (face) {
            1 -> {
                pip(center, middle)
            }

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

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
