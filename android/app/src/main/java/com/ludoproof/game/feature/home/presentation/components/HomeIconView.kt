package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import com.ludoproof.game.LudoProofTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal enum class HomeIconKind {
    SHOP,
    BADGE,
    SHARE,
}

internal class HomeIconView(
    context: Context,
) : View(context) {
    var kind:
        HomeIconKind =
        HomeIconKind.SHOP
        set(value) {
            field = value
            invalidate()
        }

    var iconColor:
        Int =
        Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    private val stroke =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                6f
            strokeCap =
                Paint.Cap.ROUND
            strokeJoin =
                Paint.Join.ROUND
        }

    private val fill =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.FILL
        }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(
            canvas,
        )

        val size =
            min(
                width,
                height,
            )
                .toFloat()
        if (
            size <=
            0f
        ) {
            return
        }

        val scale =
            size /
                100f
        val left =
            (
                width -
                    size
                ) /
                2f
        val top =
            (
                height -
                    size
                ) /
                2f

        canvas.save()
        canvas.translate(
            left,
            top,
        )
        canvas.scale(
            scale,
            scale,
        )

        stroke.color =
            iconColor
        fill.color =
            iconColor

        when (
            kind
        ) {
            HomeIconKind.SHOP ->
                drawShop(
                    canvas,
                )
            HomeIconKind.BADGE ->
                drawBadge(
                    canvas,
                )
            HomeIconKind.SHARE ->
                drawShare(
                    canvas,
                )
        }

        canvas.restore()
    }

    private fun drawShop(
        canvas: Canvas,
    ) {
        val awning =
            Path().apply {
                moveTo(
                    18f,
                    38f,
                )
                lineTo(
                    25f,
                    23f,
                )
                lineTo(
                    75f,
                    23f,
                )
                lineTo(
                    82f,
                    38f,
                )
            }
        canvas.drawPath(
            awning,
            stroke,
        )

        canvas.drawLine(
            21f,
            39f,
            79f,
            39f,
            stroke,
        )
        canvas.drawLine(
            31f,
            24f,
            29f,
            39f,
            stroke,
        )
        canvas.drawLine(
            43f,
            24f,
            43f,
            39f,
            stroke,
        )
        canvas.drawLine(
            57f,
            24f,
            57f,
            39f,
            stroke,
        )
        canvas.drawLine(
            69f,
            24f,
            71f,
            39f,
            stroke,
        )

        canvas.drawRoundRect(
            RectF(
                24f,
                41f,
                76f,
                78f,
            ),
            4f,
            4f,
            stroke,
        )
        canvas.drawRoundRect(
            RectF(
                31f,
                52f,
                49f,
                66f,
            ),
            2f,
            2f,
            stroke,
        )
        canvas.drawRect(
            RectF(
                57f,
                53f,
                69f,
                78f,
            ),
            stroke,
        )
    }

    private fun drawBadge(
        canvas: Canvas,
    ) {
        val leftRibbon =
            Path().apply {
                moveTo(
                    39f,
                    58f,
                )
                lineTo(
                    34f,
                    83f,
                )
                lineTo(
                    50f,
                    74f,
                )
                close()
            }
        val rightRibbon =
            Path().apply {
                moveTo(
                    61f,
                    58f,
                )
                lineTo(
                    66f,
                    83f,
                )
                lineTo(
                    50f,
                    74f,
                )
                close()
            }

        canvas.drawPath(
            leftRibbon,
            stroke,
        )
        canvas.drawPath(
            rightRibbon,
            stroke,
        )
        canvas.drawCircle(
            50f,
            42f,
            21f,
            stroke,
        )
        canvas.drawCircle(
            50f,
            42f,
            15f,
            stroke,
        )

        val star =
            Path()
        for (
            index in
            0 until 10
        ) {
            val radius =
                if (
                    index %
                    2 ==
                    0
                ) {
                    10f
                } else {
                    4.5f
                }
            val angle =
                Math.toRadians(
                    -90.0 +
                        index *
                        36.0,
                )
            val x =
                50f +
                    cos(
                        angle,
                    )
                        .toFloat() *
                    radius
            val y =
                42f +
                    sin(
                        angle,
                    )
                        .toFloat() *
                    radius

            if (
                index ==
                0
            ) {
                star.moveTo(
                    x,
                    y,
                )
            } else {
                star.lineTo(
                    x,
                    y,
                )
            }
        }
        star.close()
        canvas.drawPath(
            star,
            fill,
        )
    }

    private fun drawShare(
        canvas: Canvas,
    ) {
        val leftX =
            28f
        val topX =
            70f
        val topY =
            31f
        val bottomY =
            69f
        val centerY =
            50f

        canvas.drawLine(
            35f,
            47f,
            62f,
            34f,
            stroke,
        )
        canvas.drawLine(
            35f,
            53f,
            62f,
            66f,
            stroke,
        )

        canvas.drawCircle(
            leftX,
            centerY,
            8f,
            fill,
        )
        canvas.drawCircle(
            topX,
            topY,
            8f,
            fill,
        )
        canvas.drawCircle(
            topX,
            bottomY,
            8f,
            fill,
        )
    }
}

internal fun homeBlueCircularIconBackground(
    context: Context,
):
    StateListDrawable =
    StateListDrawable().apply {
        addState(
            intArrayOf(
                android.R.attr.state_pressed,
            ),
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF08498F.toInt(),
                    0xFF042E6F.toInt(),
                    0xFF021743.toInt(),
                ),
            ).apply {
                shape =
                    GradientDrawable.OVAL
                setStroke(
                    LudoProofTheme.dp(
                        context,
                        2,
                    ),
                    0xFFFFD45E.toInt(),
                )
            },
        )
        addState(
            intArrayOf(),
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF0D66C8.toInt(),
                    0xFF063B8C.toInt(),
                    0xFF031D57.toInt(),
                ),
            ).apply {
                shape =
                    GradientDrawable.OVAL
                setStroke(
                    LudoProofTheme.dp(
                        context,
                        2,
                    ),
                    0xFF58E3FF.toInt(),
                )
            },
        )
    }
