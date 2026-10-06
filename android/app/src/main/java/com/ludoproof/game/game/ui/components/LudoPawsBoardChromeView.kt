package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/**
 * Presentation-only chrome drawn over the locked Ludo board.
 *
 * This view never changes board geometry, token coordinates, touch mapping or
 * authoritative game state. It only gives the square a cleaner game-board edge
 * and small Ludo-color corner accents that remain readable on every board skin.
 */
internal class LudoPawsBoardChromeView(
    context: Context,
) : View(context) {
    private val outerStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3.5f)
            color = 0xE61A263B.toInt()
        }
    private val innerStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.0f)
            color = 0xA6FFFFFF.toInt()
        }
    private val accentStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3.0f)
            strokeCap = Paint.Cap.ROUND
        }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val outerInset = outerStroke.strokeWidth / 2f
        val outer =
            RectF(
                outerInset,
                outerInset,
                size - outerInset,
                size - outerInset,
            )
        val outerRadius = dp(4.5f)
        canvas.drawRoundRect(
            outer,
            outerRadius,
            outerRadius,
            outerStroke,
        )

        val innerInset = dp(5.0f)
        val inner =
            RectF(
                innerInset,
                innerInset,
                size - innerInset,
                size - innerInset,
            )
        canvas.drawRoundRect(
            inner,
            dp(2.5f),
            dp(2.5f),
            innerStroke,
        )

        val accentLength = size * 0.105f
        val accentInset = dp(8f)
        drawCornerAccent(
            canvas = canvas,
            x = accentInset,
            y = accentInset,
            dx = accentLength,
            dy = accentLength,
            color = RED,
        )
        drawCornerAccent(
            canvas = canvas,
            x = size - accentInset,
            y = accentInset,
            dx = -accentLength,
            dy = accentLength,
            color = GREEN,
        )
        drawCornerAccent(
            canvas = canvas,
            x = size - accentInset,
            y = size - accentInset,
            dx = -accentLength,
            dy = -accentLength,
            color = YELLOW,
        )
        drawCornerAccent(
            canvas = canvas,
            x = accentInset,
            y = size - accentInset,
            dx = accentLength,
            dy = -accentLength,
            color = BLUE,
        )
    }

    private fun drawCornerAccent(
        canvas: Canvas,
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        color: Int,
    ) {
        accentStroke.color = color
        canvas.drawLine(
            x,
            y,
            x + dx,
            y,
            accentStroke,
        )
        canvas.drawLine(
            x,
            y,
            x,
            y + dy,
            accentStroke,
        )
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private companion object {
        val RED = Color.rgb(241, 37, 47)
        val GREEN = Color.rgb(0, 169, 80)
        val YELLOW = Color.rgb(255, 216, 27)
        val BLUE = Color.rgb(48, 151, 215)
    }
}
