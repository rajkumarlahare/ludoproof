package com.ludoproof.game.ui.dialogs

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import java.util.Random
import kotlin.math.max

/**
 * Deterministic matte, rough-grain settings texture. The same drawable is used
 * by the main Settings panel and its option dialogs so both share one visual style.
 */
internal class SettingsSharedWebpDrawable(
    context: Context,
) : Drawable() {
    private data class Grain(
        val x: Float,
        val y: Float,
        val radiusDp: Float,
        val color: Int,
    )

    private val density = context.resources.displayMetrics.density
    private val cornerRadius = 16f * density
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scratchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = .55f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFC9A15C.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
    }
    private val grain = run {
        val random = Random(0x4C55444F50534C)
        List(430) {
            val color = when (random.nextInt(4)) {
                0 -> 0x24FFFFFF
                1 -> 0x2A120805
                2 -> 0x25D0A06A
                else -> 0x2A06090F
            }
            Grain(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radiusDp = .25f + random.nextFloat() * .7f,
                color = color,
            )
        }
    }
    private val scratches = run {
        val random = Random(0x524F554748)
        List(64) {
            floatArrayOf(
                random.nextFloat(),
                random.nextFloat(),
                .008f + random.nextFloat() * .035f,
                (random.nextFloat() - .5f) * .012f,
            )
        }
    }

    override fun draw(canvas: Canvas) {
        val rect = bounds
        if (rect.isEmpty) return
        val outer = RectF(rect)
        val clipPath = Path().apply {
            addRoundRect(outer, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        val save = canvas.save()
        canvas.clipPath(clipPath)

        backgroundPaint.shader = LinearGradient(
            outer.left,
            outer.top,
            outer.right,
            outer.bottom,
            intArrayOf(
                0xFF624036.toInt(),
                0xFF47332D.toInt(),
                0xFF2F2E34.toInt(),
                0xFF222832.toInt(),
            ),
            floatArrayOf(0f, .33f, .72f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(outer, cornerRadius, cornerRadius, backgroundPaint)

        grain.forEach { dot ->
            grainPaint.color = dot.color
            val x = outer.left + outer.width() * dot.x
            val y = outer.top + outer.height() * dot.y
            canvas.drawCircle(x, y, max(.35f * density, dot.radiusDp * density), grainPaint)
        }

        scratches.forEachIndexed { index, scratch ->
            scratchPaint.color =
                if (index % 3 == 0) 0x2BFFFFFF else 0x25100A08
            val x = outer.left + outer.width() * scratch[0]
            val y = outer.top + outer.height() * scratch[1]
            val endX = x + outer.width() * scratch[2]
            val endY = y + outer.height() * scratch[3]
            canvas.drawLine(x, y, endX, endY, scratchPaint)
        }

        canvas.restoreToCount(save)
        canvas.drawRoundRect(outer, cornerRadius, cornerRadius, borderPaint)
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        grainPaint.alpha = alpha
        scratchPaint.alpha = alpha
        borderPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        grainPaint.colorFilter = colorFilter
        scratchPaint.colorFilter = colorFilter
        borderPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android Drawable API")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
