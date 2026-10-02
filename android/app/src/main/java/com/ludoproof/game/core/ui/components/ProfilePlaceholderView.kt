package com.ludoproof.game.core.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

internal class ProfilePlaceholderView(
    context: Context,
) : View(context) {
    private val silhouette =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.FILL
            color =
                0xFFBDEFFF.toInt()
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

        canvas.drawCircle(
            50f,
            38f,
            17f,
            silhouette,
        )

        val shoulders =
            Path().apply {
                moveTo(
                    20f,
                    82f,
                )
                cubicTo(
                    23f,
                    63f,
                    34f,
                    55f,
                    50f,
                    55f,
                )
                cubicTo(
                    66f,
                    55f,
                    77f,
                    63f,
                    80f,
                    82f,
                )
                close()
            }
        canvas.drawPath(
            shoulders,
            silhouette,
        )

        canvas.restore()
    }
}
