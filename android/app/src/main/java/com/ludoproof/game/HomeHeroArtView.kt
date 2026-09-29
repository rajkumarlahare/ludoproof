package com.ludoproof.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class HomeHeroArtView @JvmOverloads constructor(
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
                0f,
                h,
                intArrayOf(
                    0xFF083A91.toInt(),
                    0xFF06265F.toInt(),
                    0xFF03173E.toInt(),
                ),
                floatArrayOf(
                    0f,
                    .58f,
                    1f,
                ),
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
            0xFF48DEFF.toInt()
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

        drawGlow(
            canvas,
            w * .17f,
            h * .58f,
            min(
                w,
                h,
            ) * .40f,
            0xAA00BFFF.toInt(),
        )
        drawGlow(
            canvas,
            w * .83f,
            h * .58f,
            min(
                w,
                h,
            ) * .40f,
            0x88FF2850.toInt(),
        )

        drawPodium(
            canvas,
            w * .16f,
            h * .78f,
            w * .22f,
            0xFF1DAEFF.toInt(),
        )
        drawPodium(
            canvas,
            w * .84f,
            h * .78f,
            w * .22f,
            0xFFFF4D65.toInt(),
        )

        drawDie(
            canvas,
            w * .17f,
            h * .50f,
            min(
                w,
                h,
            ) * .34f,
            0xFF148CFF.toInt(),
            -17f,
            5,
        )
        drawDie(
            canvas,
            w * .83f,
            h * .50f,
            min(
                w,
                h,
            ) * .34f,
            0xFFE92E47.toInt(),
            17f,
            4,
        )

        drawDiamond(
            canvas,
            w * .07f,
            h * .22f,
            dp(9f),
        )
        drawDiamond(
            canvas,
            w * .93f,
            h * .25f,
            dp(8f),
        )
        drawDiamond(
            canvas,
            w * .24f,
            h * .15f,
            dp(6f),
        )
        drawDiamond(
            canvas,
            w * .76f,
            h * .16f,
            dp(6f),
        )

        paint.color =
            0x221FC8FF
        val shard =
            Path().apply {
                moveTo(
                    w * .09f,
                    0f,
                )
                lineTo(
                    w * .18f,
                    0f,
                )
                lineTo(
                    w * .25f,
                    h * .28f,
                )
                lineTo(
                    w * .20f,
                    h * .34f,
                )
                close()
            }
        canvas.drawPath(
            shard,
            paint,
        )

        val shardRight =
            Path().apply {
                moveTo(
                    w * .91f,
                    0f,
                )
                lineTo(
                    w * .82f,
                    0f,
                )
                lineTo(
                    w * .75f,
                    h * .28f,
                )
                lineTo(
                    w * .80f,
                    h * .34f,
                )
                close()
            }
        canvas.drawPath(
            shardRight,
            paint,
        )
    }

    private fun drawGlow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
    ) {
        paint.shader =
            RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    color,
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            cx,
            cy,
            radius,
            paint,
        )
        paint.shader =
            null
    }

    private fun drawPodium(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        color: Int,
    ) {
        stroke.style =
            Paint.Style.STROKE
        stroke.strokeWidth =
            dp(2f)
        stroke.color =
            color
        canvas.drawOval(
            RectF(
                cx - width / 2f,
                cy - width * .12f,
                cx + width / 2f,
                cy + width * .12f,
            ),
            stroke,
        )
        stroke.color =
            0x884FEAFF.toInt()
        canvas.drawOval(
            RectF(
                cx - width * .38f,
                cy - width * .07f,
                cx + width * .38f,
                cy + width * .07f,
            ),
            stroke,
        )
        paint.color =
            0x331CD7FF
        canvas.drawOval(
            RectF(
                cx - width * .48f,
                cy - width * .09f,
                cx + width * .48f,
                cy + width * .09f,
            ),
            paint,
        )
    }

    private fun drawDie(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        angle: Float,
        value: Int,
    ) {
        canvas.save()
        canvas.rotate(
            angle,
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
                    lighten(
                        color,
                        1.38f,
                    ),
                    color,
                    darken(
                        color,
                        .68f,
                    ),
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

        stroke.color =
            0xCCB9F5FF.toInt()
        stroke.strokeWidth =
            dp(1.5f)
        canvas.drawRoundRect(
            rect,
            size * .18f,
            size * .18f,
            stroke,
        )

        val offset =
            size * .22f
        val positions =
            if (
                value ==
                4
            ) {
                listOf(
                    Pair(-offset, -offset),
                    Pair(offset, -offset),
                    Pair(-offset, offset),
                    Pair(offset, offset),
                )
            } else {
                listOf(
                    Pair(-offset, -offset),
                    Pair(offset, -offset),
                    Pair(0f, 0f),
                    Pair(-offset, offset),
                    Pair(offset, offset),
                )
            }

        paint.color =
            Color.WHITE
        positions.forEach {
                point ->
            canvas.drawCircle(
                cx + point.first,
                cy + point.second,
                size * .07f,
                paint,
            )
        }

        paint.color =
            0x55FFFFFF
        canvas.drawRoundRect(
            rect.left + size * .08f,
            rect.top + size * .07f,
            rect.right - size * .14f,
            rect.top + size * .18f,
            size * .05f,
            size * .05f,
            paint,
        )

        canvas.restore()
    }

    private fun drawDiamond(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
    ) {
        paint.color =
            0x8832CFFF.toInt()
        val path =
            Path().apply {
                moveTo(
                    cx,
                    cy - size,
                )
                lineTo(
                    cx + size * .62f,
                    cy,
                )
                lineTo(
                    cx,
                    cy + size,
                )
                lineTo(
                    cx - size * .62f,
                    cy,
                )
                close()
            }
        canvas.drawPath(
            path,
            paint,
        )
    }

    private fun lighten(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor)
                .toInt()
                .coerceAtMost(255),
            (Color.green(color) * factor)
                .toInt()
                .coerceAtMost(255),
            (Color.blue(color) * factor)
                .toInt()
                .coerceAtMost(255),
        )

    private fun darken(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor)
                .toInt()
                .coerceAtLeast(0),
            (Color.green(color) * factor)
                .toInt()
                .coerceAtLeast(0),
            (Color.blue(color) * factor)
                .toInt()
                .coerceAtLeast(0),
        )

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
