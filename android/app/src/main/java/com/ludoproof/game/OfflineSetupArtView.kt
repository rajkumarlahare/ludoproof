package com.ludoproof.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class OfflineSetupArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
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

        paint.shader =
            LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(
                    0xFF06317B.toInt(),
                    0xFF0754B3.toInt(),
                    0xFF031942.toInt(),
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
            0xFF47DFFF.toInt()
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

        val artCx =
            w * .22f
        val artCy =
            h * .53f
        val boardSize =
            min(
                w,
                h,
            ) * .64f

        paint.shader =
            RadialGradient(
                artCx,
                artCy,
                boardSize * .92f,
                intArrayOf(
                    0x9900C8FF.toInt(),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            artCx,
            artCy,
            boardSize,
            paint,
        )
        paint.shader =
            null

        val left =
            artCx -
                boardSize * .47f
        val top =
            artCy -
                boardSize * .47f
        val size =
            boardSize * .94f
        val half =
            size / 2f

        paint.color =
            0xFFF5F9FF.toInt()
        canvas.drawRoundRect(
            left,
            top,
            left + size,
            top + size,
            dp(8f),
            dp(8f),
            paint,
        )

        val colors =
            listOf(
                0xFFE9343E.toInt(),
                0xFF2CB65A.toInt(),
                0xFF258CFF.toInt(),
                0xFFFFD32B.toInt(),
            )

        colors.forEachIndexed {
                index,
                color,
            ->
            paint.color =
                color
            val x =
                left +
                    if (
                        index % 2 ==
                        0
                    ) {
                        0f
                    } else {
                        half
                    }
            val y =
                top +
                    if (
                        index < 2
                    ) {
                        0f
                    } else {
                        half
                    }
            canvas.drawRect(
                x,
                y,
                x + half,
                y + half,
                paint,
            )
        }

        stroke.color =
            0x66052568
        stroke.strokeWidth =
            dp(1f)
        for (
            i in
            1..4
        ) {
            val p =
                size *
                    i /
                    5f
            canvas.drawLine(
                left + p,
                top,
                left + p,
                top + size,
                stroke,
            )
            canvas.drawLine(
                left,
                top + p,
                left + size,
                top + p,
                stroke,
            )
        }

        drawPawn(
            canvas,
            left + size * .25f,
            top + size * .26f,
            size * .12f,
            colors[0],
        )
        drawPawn(
            canvas,
            left + size * .75f,
            top + size * .26f,
            size * .12f,
            colors[1],
        )
        drawPawn(
            canvas,
            left + size * .25f,
            top + size * .75f,
            size * .12f,
            colors[2],
        )
        drawPawn(
            canvas,
            left + size * .75f,
            top + size * .75f,
            size * .12f,
            colors[3],
        )

        drawDie(
            canvas,
            artCx,
            artCy,
            size * .28f,
        )

        paint.color =
            0x221FD6FF
        val slash =
            Path().apply {
                moveTo(
                    w * .47f,
                    0f,
                )
                lineTo(
                    w * .58f,
                    0f,
                )
                lineTo(
                    w * .47f,
                    h,
                )
                lineTo(
                    w * .36f,
                    h,
                )
                close()
            }
        canvas.drawPath(
            slash,
            paint,
        )

        drawStar(
            canvas,
            w * .90f,
            h * .20f,
            dp(7f),
        )
        drawStar(
            canvas,
            w * .82f,
            h * .76f,
            dp(5f),
        )
    }

    private fun drawPawn(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
    ) {
        paint.color =
            color
        canvas.drawCircle(
            cx,
            cy - size * .33f,
            size * .28f,
            paint,
        )
        canvas.drawOval(
            RectF(
                cx - size * .30f,
                cy - size * .10f,
                cx + size * .30f,
                cy + size * .38f,
            ),
            paint,
        )
        canvas.drawRoundRect(
            cx - size * .43f,
            cy + size * .27f,
            cx + size * .43f,
            cy + size * .47f,
            size * .10f,
            size * .10f,
            paint,
        )

        stroke.color =
            0x66FFFFFF
        stroke.strokeWidth =
            dp(1f)
        canvas.drawCircle(
            cx - size * .08f,
            cy - size * .39f,
            size * .09f,
            stroke,
        )
    }

    private fun drawDie(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
    ) {
        canvas.save()
        canvas.rotate(
            10f,
            cx,
            cy,
        )

        val rect =
            RectF(
                cx - size / 2f,
                cy - size / 2f,
                cx + size / 2f,
                cy + size / 2f,
            )

        paint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(
                    Color.WHITE,
                    0xFFE7F2FF.toInt(),
                    0xFFB7D7F6.toInt(),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            rect,
            size * .18f,
            size * .18f,
            paint,
        )
        paint.shader =
            null

        paint.color =
            0xFF063B8F.toInt()
        val offset =
            size * .21f
        listOf(
            Pair(-offset, -offset),
            Pair(offset, -offset),
            Pair(0f, 0f),
            Pair(-offset, offset),
            Pair(offset, offset),
        ).forEach {
                point ->
            canvas.drawCircle(
                cx + point.first,
                cy + point.second,
                size * .07f,
                paint,
            )
        }

        canvas.restore()
    }

    private fun drawStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
    ) {
        paint.color =
            0x9962E9FF.toInt()
        val path =
            Path().apply {
                moveTo(
                    cx,
                    cy - radius * 1.8f,
                )
                lineTo(
                    cx + radius * .34f,
                    cy - radius * .34f,
                )
                lineTo(
                    cx + radius * 1.8f,
                    cy,
                )
                lineTo(
                    cx + radius * .34f,
                    cy + radius * .34f,
                )
                lineTo(
                    cx,
                    cy + radius * 1.8f,
                )
                lineTo(
                    cx - radius * .34f,
                    cy + radius * .34f,
                )
                lineTo(
                    cx - radius * 1.8f,
                    cy,
                )
                lineTo(
                    cx - radius * .34f,
                    cy - radius * .34f,
                )
                close()
            }
        canvas.drawPath(
            path,
            paint,
        )
    }

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
