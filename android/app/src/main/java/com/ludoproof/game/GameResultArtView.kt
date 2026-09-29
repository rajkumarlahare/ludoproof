package com.ludoproof.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class GameResultArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    enum class Mode {
        ONLINE,
        OFFLINE,
    }

    var mode: Mode =
        Mode.ONLINE
        set(value) {
            field =
                value
            invalidate()
        }

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.STROKE
        }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        val w =
            width.toFloat()
        val h =
            height.toFloat()
        if (
            w <= 0f ||
            h <= 0f
        ) {
            return
        }

        val accent =
            if (
                mode ==
                Mode.ONLINE
            ) {
                0xFF24D9FF.toInt()
            } else {
                0xFF46E8A4.toInt()
            }

        paint.shader =
            LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(
                    0xFF082E70.toInt(),
                    0xFF0A4B9E.toInt(),
                    0xFF03173D.toInt(),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            0f,
            0f,
            w,
            h,
            dp(20f),
            dp(20f),
            paint,
        )
        paint.shader =
            null

        stroke.color =
            0xFFFFD45E.toInt()
        stroke.strokeWidth =
            dp(2f)
        canvas.drawRoundRect(
            dp(1f),
            dp(1f),
            w - dp(1f),
            h - dp(1f),
            dp(20f),
            dp(20f),
            stroke,
        )

        val cx =
            w * .20f
        val cy =
            h * .51f
        val radius =
            min(
                w,
                h,
            ) * .29f

        paint.shader =
            RadialGradient(
                cx,
                cy,
                radius * 2f,
                intArrayOf(
                    Color.argb(
                        150,
                        Color.red(accent),
                        Color.green(accent),
                        Color.blue(accent),
                    ),
                    0x22FFD45E,
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            cx,
            cy,
            radius * 2f,
            paint,
        )
        paint.shader =
            null

        drawTrophy(
            canvas,
            cx,
            cy,
            radius * 1.35f,
        )

        drawConfetti(
            canvas,
            w,
            h,
            accent,
        )
    }

    private fun drawTrophy(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
    ) {
        paint.shader =
            LinearGradient(
                cx - size,
                cy - size,
                cx + size,
                cy + size,
                intArrayOf(
                    0xFFFFF1A1.toInt(),
                    0xFFFFC62E.toInt(),
                    0xFFF18A00.toInt(),
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        val cup =
            Path().apply {
                moveTo(
                    cx - size * .42f,
                    cy - size * .48f,
                )
                lineTo(
                    cx + size * .42f,
                    cy - size * .48f,
                )
                lineTo(
                    cx + size * .30f,
                    cy + size * .02f,
                )
                quadTo(
                    cx,
                    cy + size * .30f,
                    cx - size * .30f,
                    cy + size * .02f,
                )
                close()
            }
        canvas.drawPath(
            cup,
            paint,
        )

        stroke.color =
            0xFFFFD45E.toInt()
        stroke.strokeWidth =
            size * .11f
        stroke.strokeCap =
            Paint.Cap.ROUND

        canvas.drawArc(
            RectF(
                cx - size * .70f,
                cy - size * .38f,
                cx - size * .18f,
                cy + size * .08f,
            ),
            80f,
            210f,
            false,
            stroke,
        )
        canvas.drawArc(
            RectF(
                cx + size * .18f,
                cy - size * .38f,
                cx + size * .70f,
                cy + size * .08f,
            ),
            -110f,
            210f,
            false,
            stroke,
        )
        stroke.strokeCap =
            Paint.Cap.BUTT

        paint.shader =
            null
        paint.color =
            0xFFFFC62E.toInt()
        canvas.drawRoundRect(
            cx - size * .09f,
            cy + size * .18f,
            cx + size * .09f,
            cy + size * .52f,
            size * .05f,
            size * .05f,
            paint,
        )
        canvas.drawRoundRect(
            cx - size * .32f,
            cy + size * .47f,
            cx + size * .32f,
            cy + size * .62f,
            size * .07f,
            size * .07f,
            paint,
        )

        paint.color =
            Color.WHITE
        val star =
            Path().apply {
                val r1 =
                    size * .16f
                val r2 =
                    size * .07f
                for (
                    i in
                    0 until 10
                ) {
                    val angle =
                        Math.toRadians(
                            (-90 + i * 36)
                                .toDouble(),
                        )
                    val r =
                        if (
                            i % 2 ==
                            0
                        ) {
                            r1
                        } else {
                            r2
                        }
                    val x =
                        cx +
                            Math.cos(
                                angle,
                            ).toFloat() *
                            r
                    val y =
                        cy -
                            size * .15f +
                            Math.sin(
                                angle,
                            ).toFloat() *
                            r
                    if (
                        i ==
                        0
                    ) {
                        moveTo(
                            x,
                            y,
                        )
                    } else {
                        lineTo(
                            x,
                            y,
                        )
                    }
                }
                close()
            }
        canvas.drawPath(
            star,
            paint,
        )
    }

    private fun drawConfetti(
        canvas: Canvas,
        w: Float,
        h: Float,
        accent: Int,
    ) {
        val pieces =
            listOf(
                Triple(
                    .08f,
                    .18f,
                    -24f,
                ),
                Triple(
                    .34f,
                    .14f,
                    20f,
                ),
                Triple(
                    .42f,
                    .77f,
                    -18f,
                ),
                Triple(
                    .77f,
                    .18f,
                    28f,
                ),
                Triple(
                    .90f,
                    .35f,
                    -20f,
                ),
                Triple(
                    .72f,
                    .82f,
                    18f,
                ),
                Triple(
                    .94f,
                    .75f,
                    32f,
                ),
            )

        pieces.forEachIndexed {
                index,
                piece ->
            canvas.save()
            val x =
                w *
                    piece.first
            val y =
                h *
                    piece.second
            canvas.rotate(
                piece.third,
                x,
                y,
            )
            paint.color =
                when (
                    index % 3
                ) {
                    0 ->
                        accent

                    1 ->
                        0xFFFFD45E.toInt()

                    else ->
                        0xFFFF5D72.toInt()
                }
            canvas.drawRoundRect(
                x - dp(2.5f),
                y - dp(7f),
                x + dp(2.5f),
                y + dp(7f),
                dp(2f),
                dp(2f),
                paint,
            )
            canvas.restore()
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
