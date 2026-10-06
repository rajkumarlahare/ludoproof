package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
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
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            isFakeBoldText = true
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
        drawLudo(canvas)
        drawLogoWord(
            canvas = canvas,
            text = "Paws",
            centerX = 181f,
            baselineY = 160f,
            textSize = 82f,
            topColor = 0xFFBAF8FF.toInt(),
            middleColor = 0xFF48D1FF.toInt(),
            bottomColor = 0xFF079BDD.toInt(),
            outlineColor = 0xFF45208F.toInt(),
            textScaleX = 1.06f,
        )

        canvas.restore()
    }

    private fun drawWoodPlaque(canvas: Canvas) {
        val plank = Path().apply {
            moveTo(55f, 73f)
            cubicTo(39f, 73f, 31f, 82f, 31f, 96f)
            lineTo(31f, 146f)
            cubicTo(31f, 160f, 42f, 170f, 58f, 170f)
            lineTo(302f, 170f)
            cubicTo(319f, 170f, 330f, 159f, 330f, 145f)
            lineTo(330f, 97f)
            cubicTo(330f, 82f, 320f, 73f, 304f, 73f)
            close()
        }

        fill.shader = null
        fill.color = 0xA83A1731.toInt()
        canvas.save()
        canvas.translate(0f, 8f)
        canvas.drawPath(plank, fill)
        canvas.restore()

        fill.shader =
            LinearGradient(
                0f,
                72f,
                0f,
                171f,
                intArrayOf(
                    0xFFC37A4F.toInt(),
                    0xFF975038.toInt(),
                    0xFF633225.toInt(),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(plank, fill)
        fill.shader = null

        stroke.color = 0xFF512142.toInt()
        stroke.strokeWidth = 5.5f
        canvas.drawPath(plank, stroke)

        stroke.color = 0x55FFD7A8
        stroke.strokeWidth = 3f
        canvas.drawArc(RectF(48f, 93f, 142f, 137f), 202f, 108f, false, stroke)
        canvas.drawArc(RectF(206f, 103f, 315f, 149f), 195f, 112f, false, stroke)
        canvas.drawLine(75f, 154f, 145f, 150f, stroke)
        canvas.drawLine(220f, 89f, 286f, 94f, stroke)

        fill.color = 0x6F5D2527
        canvas.drawCircle(69f, 118f, 5.5f, fill)
        canvas.drawCircle(290f, 139f, 5f, fill)
        fill.color = 0x66F6CCA0
        canvas.drawCircle(67f, 116f, 2f, fill)
        canvas.drawCircle(288f, 137f, 1.8f, fill)
    }

    private fun drawSidePaws(canvas: Canvas) {
        drawPawWithOutline(
            canvas = canvas,
            centerX = 31f,
            centerY = 126f,
            scale = 0.78f,
            color = 0xFFF74EF8.toInt(),
            outline = 0xFF4B208F.toInt(),
            outlineWidth = 4.2f,
        )
        drawPawWithOutline(
            canvas = canvas,
            centerX = 329f,
            centerY = 144f,
            scale = 0.61f,
            color = 0xFF75EEFF.toInt(),
            outline = 0xFF2556A5.toInt(),
            outlineWidth = 4f,
        )
    }

    private fun drawLudo(canvas: Canvas) {
        drawLogoWord(
            canvas = canvas,
            text = "Lud",
            centerX = 144f,
            baselineY = 91f,
            textSize = 83f,
            topColor = 0xFFFFF16A.toInt(),
            middleColor = 0xFFFFC82E.toInt(),
            bottomColor = 0xFFF57E08.toInt(),
            outlineColor = 0xFF45208F.toInt(),
            textScaleX = 1.04f,
        )
        drawSolidPawO(canvas)
    }

    private fun drawSolidPawO(canvas: Canvas) {
        val centerX = 254f
        val centerY = 57f
        val radius = 30f

        fill.shader = null
        fill.color = 0xA91B0A3B.toInt()
        canvas.drawCircle(centerX + 2.5f, centerY + 6f, radius + 5f, fill)

        fill.color = 0xFF4A208F.toInt()
        canvas.drawCircle(centerX, centerY, radius + 5f, fill)

        fill.shader =
            LinearGradient(
                0f,
                centerY - radius,
                0f,
                centerY + radius,
                intArrayOf(
                    0xFFFFF36B.toInt(),
                    0xFFFFC72B.toInt(),
                    0xFFF47B08.toInt(),
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(centerX, centerY, radius, fill)
        fill.shader = null

        stroke.color = 0x55FFFFFF
        stroke.strokeWidth = 2f
        canvas.drawArc(
            RectF(
                centerX - 21f,
                centerY - 20f,
                centerX + 21f,
                centerY + 18f,
            ),
            205f,
            92f,
            false,
            stroke,
        )

        drawPawWithOutline(
            canvas = canvas,
            centerX = centerX,
            centerY = centerY + 1f,
            scale = 0.48f,
            color = 0xFF45206F.toInt(),
            outline = 0xFF45206F.toInt(),
            outlineWidth = 0f,
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
        textScaleX: Float,
    ) {
        textPaint.textSize = textSize
        textPaint.textScaleX = textScaleX
        textPaint.style = Paint.Style.STROKE
        textPaint.shader = null

        textPaint.color = 0xB31C0B38.toInt()
        textPaint.strokeWidth = 18f
        canvas.drawText(
            text,
            centerX + 2.5f,
            baselineY + 6f,
            textPaint,
        )

        textPaint.color = outlineColor
        textPaint.strokeWidth = 14f
        canvas.drawText(text, centerX, baselineY, textPaint)

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
        canvas.drawText(text, centerX, baselineY, textPaint)
        textPaint.shader = null

        textPaint.style = Paint.Style.STROKE
        textPaint.color = 0x55FFFFFF
        textPaint.strokeWidth = 1.8f
        canvas.drawText(
            text,
            centerX,
            baselineY - 2f,
            textPaint,
        )

        textPaint.textScaleX = 1f
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
        fill.shader = null
        fill.color = color

        val pad =
            RectF(
                centerX - 12f * scale,
                centerY - 1f * scale,
                centerX + 12f * scale,
                centerY + 17f * scale,
            )
        canvas.drawOval(pad, fill)
        if (outlineWidth > 0f) {
            canvas.drawOval(pad, stroke)
        }

        val toes =
            arrayOf(
                Triple(centerX - 14f * scale, centerY - 8f * scale, 5.2f * scale),
                Triple(centerX - 5f * scale, centerY - 14f * scale, 5.5f * scale),
                Triple(centerX + 5f * scale, centerY - 14f * scale, 5.5f * scale),
                Triple(centerX + 14f * scale, centerY - 8f * scale, 5.2f * scale),
            )
        toes.forEach { (x, y, radius) ->
            canvas.drawCircle(x, y, radius, fill)
            if (outlineWidth > 0f) {
                canvas.drawCircle(x, y, radius, stroke)
            }
        }
    }
}
