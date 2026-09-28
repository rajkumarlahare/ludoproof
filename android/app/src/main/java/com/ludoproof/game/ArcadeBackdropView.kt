package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ArcadeBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        paint.shader =
            LinearGradient(
                0f,
                0f,
                0f,
                h,
                intArrayOf(
                    Color.rgb(5, 38, 126),
                    Color.rgb(8, 82, 202),
                    Color.rgb(3, 50, 145),
                    Color.rgb(2, 25, 82),
                ),
                floatArrayOf(
                    0f,
                    0.42f,
                    0.72f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(
            0f,
            0f,
            w,
            h,
            paint,
        )

        paint.shader =
            RadialGradient(
                w * 0.52f,
                h * 0.45f,
                min(w, h) * 0.58f,
                intArrayOf(
                    Color.argb(
                        125,
                        12,
                        133,
                        255,
                    ),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(
            0f,
            0f,
            w,
            h,
            paint,
        )
        paint.shader = null

        drawSoftMotifs(
            canvas,
            w,
            h,
        )
        drawStars(
            canvas,
            w,
            h,
        )
    }

    private fun drawSoftMotifs(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        linePaint.color =
            Color.argb(
                24,
                123,
                194,
                255,
            )
        linePaint.strokeWidth =
            dp(9f)

        val positions =
            listOf(
                0.04f to 0.22f,
                0.68f to 0.17f,
                0.16f to 0.60f,
                0.74f to 0.73f,
                0.43f to 0.88f,
            )

        positions.forEachIndexed {
                index,
                point,
            ->
            val cx = w * point.first
            val cy = h * point.second
            val size =
                min(w, h) *
                    if (
                        index % 2 == 0
                    ) {
                        0.12f
                    } else {
                        0.10f
                    }

            canvas.save()
            canvas.rotate(
                -18f +
                    index * 9f,
                cx,
                cy,
            )
            canvas.drawRoundRect(
                cx - size,
                cy - size,
                cx + size,
                cy + size,
                size * 0.18f,
                size * 0.18f,
                linePaint,
            )

            for (
                row in
                0..1
            ) {
                for (
                    col in
                    0..1
                ) {
                    paint.shader = null
                    paint.color =
                        Color.argb(
                            20,
                            147,
                            209,
                            255,
                        )
                    canvas.drawCircle(
                        cx +
                            (
                                col * 2 -
                                    1
                                ) *
                                size *
                                0.36f,
                        cy +
                            (
                                row * 2 -
                                    1
                                ) *
                                size *
                                0.36f,
                        size * 0.11f,
                        paint,
                    )
                }
            }
            canvas.restore()
        }
    }

    private fun drawStars(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val stars =
            listOf(
                Triple(
                    0.09f,
                    0.10f,
                    0.008f,
                ),
                Triple(
                    0.52f,
                    0.15f,
                    0.013f,
                ),
                Triple(
                    0.87f,
                    0.08f,
                    0.010f,
                ),
                Triple(
                    0.94f,
                    0.35f,
                    0.006f,
                ),
                Triple(
                    0.17f,
                    0.45f,
                    0.007f,
                ),
                Triple(
                    0.55f,
                    0.55f,
                    0.009f,
                ),
                Triple(
                    0.26f,
                    0.75f,
                    0.006f,
                ),
                Triple(
                    0.70f,
                    0.86f,
                    0.012f,
                ),
                Triple(
                    0.90f,
                    0.93f,
                    0.006f,
                ),
            )

        stars.forEach {
                star ->
            val cx = w * star.first
            val cy = h * star.second
            val radius =
                min(w, h) *
                    star.third
            paint.shader = null
            paint.color =
                Color.argb(
                    170,
                    117,
                    207,
                    255,
                )
            val path =
                Path().apply {
                    moveTo(
                        cx,
                        cy - radius * 2f,
                    )
                    lineTo(
                        cx + radius * 0.38f,
                        cy - radius * 0.38f,
                    )
                    lineTo(
                        cx + radius * 2f,
                        cy,
                    )
                    lineTo(
                        cx + radius * 0.38f,
                        cy + radius * 0.38f,
                    )
                    lineTo(
                        cx,
                        cy + radius * 2f,
                    )
                    lineTo(
                        cx - radius * 0.38f,
                        cy + radius * 0.38f,
                    )
                    lineTo(
                        cx - radius * 2f,
                        cy,
                    )
                    lineTo(
                        cx - radius * 0.38f,
                        cy - radius * 0.38f,
                    )
                    close()
                }
            canvas.drawPath(
                path,
                paint,
            )
        }
    }

    private fun dp(value: Float): Float =
        value *
            resources
                .displayMetrics
                .density
}
