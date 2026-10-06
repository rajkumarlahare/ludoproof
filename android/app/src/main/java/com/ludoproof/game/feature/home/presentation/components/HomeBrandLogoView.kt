package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import kotlin.math.min

internal class HomeBrandLogoView(
    context: Context,
) : View(context) {
    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

    init {
        contentDescription = "Ludo Paws"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val designWidth = 360f
        val designHeight = 200f
        val scale =
            min(
                width / designWidth,
                height / designHeight,
            )
        val dx = (width - designWidth * scale) / 2f
        val dy = (height - designHeight * scale) / 2f

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        drawWoodPlaque(canvas)
        drawSidePaws(canvas)
        drawLogoWord(
            canvas = canvas,
            text = "Ludo",
            centerX = 179f,
            baselineY = 91f,
            textSize = 78f,
            topColor = 0xFFFFF06A.toInt(),
            middleColor = 0xFFFFC52D.toInt(),
            bottomColor = 0xFFF47A08.toInt(),
            outlineColor = 0xFF45208F.toInt(),
        )
        drawLudoPaw(canvas)
        drawLogoWord(
            canvas = canvas,
            text = "Paws",
            centerX = 184f,
            baselineY = 157f,
            textSize = 76f,
            topColor = 0xFFB9F7FF.toInt(),
            middleColor = 0xFF39C8F6.toInt(),
            bottomColor = 0xFF0799DB.toInt(),
            outlineColor = 0xFF4A218F.toInt(),
        )

        canvas.restore()
    }

    private fun drawWoodPlaque(canvas: Canvas) {
        val shadowRect = RectF(45f, 78f, 319f, 181f)
        fill.shader = null
        fill.color = 0xB33B1830.toInt()
        canvas.drawRoundRect(
            shadowRect,
            24f,
            24f,
            fill,
        )

        val plank = RectF(42f, 70f, 316f, 173f)
        fill.shader =
            LinearGradient(
                0f,
                plank.top,
                0f,
                plank.bottom,
                intArrayOf(
                    0xFFB96A43.toInt(),
                    0xFF8C452F.toInt(),
                    0xFF633123.toInt(),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            plank,
            22f,
            22f,
            fill,
        )
        fill.shader = null

        stroke.color = 0xFF4A2033.toInt()
        stroke.strokeWidth = 5f
        canvas.drawRoundRect(
            plank,
            22f,
            22f,
            stroke,
        )

        stroke.color = 0x55FFD2A2
        stroke.strokeWidth = 3f
        canvas.drawArc(
            RectF(58f, 91f, 151f, 139f),
            205f,
            105f,
            false,
            stroke,
        )
        canvas.drawArc(
            RectF(201f, 103f, 299f, 151f),
            198f,
            111f,
            false,
            stroke,
        )
        canvas.drawLine(79f, 155f, 143f, 151f, stroke)
        canvas.drawLine(224f, 87f, 280f, 92f, stroke)

        fill.color = 0x77601F20
        canvas.drawCircle(73f, 116f, 6f, fill)
        canvas.drawCircle(286f, 139f, 5f, fill)
        fill.color = 0x66F7C995
        canvas.drawCircle(71f, 114f, 2.2f, fill)
        canvas.drawCircle(284f, 137f, 1.8f, fill)
    }

    private fun drawSidePaws(canvas: Canvas) {
        drawPawWithOutline(
            canvas = canvas,
            centerX = 48f,
            centerY = 126f,
            scale = 0.82f,
            color = 0xFFF34EF6.toInt(),
            outline = 0xFF45208F.toInt(),
        )
        drawPawWithOutline(
            canvas = canvas,
            centerX = 316f,
            centerY = 146f,
            scale = 0.60f,
            color = 0xFF79F2FF.toInt(),
            outline = 0xFF1C5BA2.toInt(),
        )
    }

    private fun drawLogoWord(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        textSize: Float,
        topColor: Int,
        middleColor: Int,
        bottomColor: Int,
        outlineColor: Int,
    ) {
        textPaint.textSize = textSize
        textPaint.style = Paint.Style.STROKE
        textPaint.shader = null
        textPaint.color = 0xB31C0B38.toInt()
        textPaint.strokeWidth = 17f
        canvas.drawText(
            text,
            centerX + 2.5f,
            baselineY + 6f,
            textPaint,
        )

        textPaint.color = outlineColor
        textPaint.strokeWidth = 13f
        canvas.drawText(
            text,
            centerX,
            baselineY,
            textPaint,
        )

        textPaint.color = 0xFFF8E9FF.toInt()
        textPaint.strokeWidth = 5.5f
        canvas.drawText(
            text,
            centerX,
            baselineY,
            textPaint,
        )

        textPaint.style = Paint.Style.FILL
        textPaint.shader =
            LinearGradient(
                0f,
                baselineY - textSize,
                0f,
                baselineY + 8f,
                intArrayOf(
                    topColor,
                    middleColor,
                    bottomColor,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawText(
            text,
            centerX,
            baselineY,
            textPaint,
        )
        textPaint.shader = null

        textPaint.style = Paint.Style.STROKE
        textPaint.color = 0x66FFFFFF
        textPaint.strokeWidth = 1.7f
        canvas.drawText(
            text,
            centerX,
            baselineY - 1.5f,
            textPaint,
        )
    }

    private fun drawLudoPaw(canvas: Canvas) {
        textPaint.textSize = 78f
        val totalWidth = textPaint.measureText("Ludo")
        val startX = 179f - totalWidth / 2f
        val pawX =
            startX +
                textPaint.measureText("Lud") +
                textPaint.measureText("o") / 2f

        drawPawWithOutline(
            canvas = canvas,
            centerX = pawX,
            centerY = 61f,
            scale = 0.43f,
            color = 0xFF38206F.toInt(),
            outline = 0xFFFFD84E.toInt(),
            outlineWidth = 2.7f,
        )
    }

    private fun drawPawWithOutline(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        scale: Float,
        color: Int,
        outline: Int,
        outlineWidth: Float = 4f,
    ) {
        stroke.color = outline
        stroke.strokeWidth = outlineWidth
        fill.color = color

        val pad = RectF(
            centerX - 12f * scale,
            centerY - 1f * scale,
            centerX + 12f * scale,
            centerY + 17f * scale,
        )
        canvas.drawOval(pad, fill)
        canvas.drawOval(pad, stroke)

        val toes =
            arrayOf(
                Triple(centerX - 14f * scale, centerY - 8f * scale, 5.2f * scale),
                Triple(centerX - 5f * scale, centerY - 14f * scale, 5.5f * scale),
                Triple(centerX + 5f * scale, centerY - 14f * scale, 5.5f * scale),
                Triple(centerX + 14f * scale, centerY - 8f * scale, 5.2f * scale),
            )
        toes.forEach { (x, y, radius) ->
            canvas.drawCircle(x, y, radius, fill)
            canvas.drawCircle(x, y, radius, stroke)
        }
    }
}
