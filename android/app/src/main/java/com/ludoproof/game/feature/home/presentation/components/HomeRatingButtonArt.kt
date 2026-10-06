package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Reference-style Google Play mark used only by the Home rating action. */
internal class HomeGooglePlayIconView(
    context: Context,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val separator =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xCCFFFFFF.toInt()
            strokeWidth = 1.7f
            strokeJoin = Paint.Join.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val scale = size / 100f
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(scale, scale)

        // Soft depth behind the Play mark so it reads like the large reference icon.
        val shadow =
            Path().apply {
                moveTo(20f, 18f)
                lineTo(20f, 86f)
                lineTo(84f, 52f)
                close()
            }
        paint.color = 0x66000000
        canvas.save()
        canvas.translate(0f, 4f)
        canvas.drawPath(shadow, paint)
        canvas.restore()

        val leftBlue =
            Path().apply {
                moveTo(18f, 15f)
                lineTo(49f, 47f)
                lineTo(18f, 85f)
                close()
            }
        paint.color = 0xFF22B7F2.toInt()
        canvas.drawPath(leftBlue, paint)

        val topGreen =
            Path().apply {
                moveTo(18f, 15f)
                lineTo(56f, 38f)
                lineTo(49f, 47f)
                close()
            }
        paint.color = 0xFF34D27B.toInt()
        canvas.drawPath(topGreen, paint)

        val bottomRed =
            Path().apply {
                moveTo(18f, 85f)
                lineTo(49f, 47f)
                lineTo(57f, 62f)
                close()
            }
        paint.color = 0xFFF64B4B.toInt()
        canvas.drawPath(bottomRed, paint)

        val rightYellow =
            Path().apply {
                moveTo(49f, 47f)
                lineTo(56f, 38f)
                lineTo(84f, 50f)
                lineTo(57f, 62f)
                close()
            }
        paint.color = 0xFFFFD24A.toInt()
        canvas.drawPath(rightYellow, paint)

        // Fine bright separators give the multicolor mark a polished, beveled edge.
        canvas.drawLine(18f, 15f, 49f, 47f, separator)
        canvas.drawLine(18f, 85f, 49f, 47f, separator)
        canvas.drawLine(49f, 47f, 56f, 38f, separator)
        canvas.drawLine(49f, 47f, 57f, 62f, separator)

        // Small glass reflection rather than a hard white stripe.
        val gloss =
            Path().apply {
                moveTo(22f, 20f)
                lineTo(48f, 40f)
                lineTo(43f, 44f)
                lineTo(23f, 31f)
                close()
            }
        paint.shader =
            LinearGradient(
                20f,
                18f,
                48f,
                46f,
                intArrayOf(
                    0xC8FFFFFF.toInt(),
                    0x30FFFFFF,
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(gloss, paint)
        paint.shader = null

        canvas.restore()
    }
}

/** Five compact gold stars with depth and a top-light reflection. */
internal class HomeRatingStarsView(
    context: Context,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val outline =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val gap = width * 0.018f
        val starWidth = (width - gap * 4f) / 5f
        val radius = min(starWidth * 0.46f, height * 0.43f)
        val centerY = height * 0.48f

        for (index in 0 until 5) {
            val centerX =
                starWidth * index +
                    starWidth / 2f +
                    gap * index

            val shadowPath =
                starPath(
                    centerX = centerX,
                    centerY = centerY + radius * 0.16f,
                    outerRadius = radius,
                    innerRadius = radius * 0.46f,
                )
            paint.shader = null
            paint.color = 0xA25C2D00.toInt()
            canvas.drawPath(shadowPath, paint)

            val star =
                starPath(
                    centerX = centerX,
                    centerY = centerY,
                    outerRadius = radius,
                    innerRadius = radius * 0.46f,
                )
            paint.shader =
                LinearGradient(
                    0f,
                    centerY - radius,
                    0f,
                    centerY + radius,
                    intArrayOf(
                        0xFFFFF08A.toInt(),
                        0xFFFFC51E.toInt(),
                        0xFFF28B00.toInt(),
                    ),
                    floatArrayOf(0f, 0.54f, 1f),
                    Shader.TileMode.CLAMP,
                )
            canvas.drawPath(star, paint)
            paint.shader = null

            outline.color = 0xFFD97700.toInt()
            outline.strokeWidth = (radius * 0.11f).coerceAtLeast(1f)
            canvas.drawPath(star, outline)

            // A tiny upper-left highlight gives each star a toy-like 3D finish.
            paint.color = 0xB8FFFFFF.toInt()
            canvas.drawCircle(
                centerX - radius * 0.20f,
                centerY - radius * 0.36f,
                radius * 0.075f,
                paint,
            )
        }
    }

    private fun starPath(
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
    ): Path =
        Path().apply {
            for (index in 0 until 10) {
                val radius =
                    if (index % 2 == 0) {
                        outerRadius
                    } else {
                        innerRadius
                    }
                val angle =
                    Math.toRadians(
                        -90.0 + index * 36.0,
                    )
                val x =
                    centerX +
                        cos(angle).toFloat() * radius
                val y =
                    centerY +
                        sin(angle).toFloat() * radius
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
}
