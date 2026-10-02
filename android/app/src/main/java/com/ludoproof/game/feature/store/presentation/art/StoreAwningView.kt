package com.ludoproof.game.feature.store.presentation.art

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class StoreAwningView(
    context: Context,
) : View(context) {
    private val paint =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        )

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(
            canvas,
        )

        val width =
            this.width
                .toFloat()
        val height =
            this.height
                .toFloat()
        val top =
            height *
                .18f
        val bottom =
            height *
                .80f
        val stripeWidth =
            width /
                10f

        paint.color =
            0xFF80279D.toInt()
        canvas.drawRect(
            0f,
            0f,
            width,
            top,
            paint,
        )

        for (
            index in
            0 until
                10
        ) {
            paint.color =
                if (
                    index %
                        2 ==
                        0
                ) {
                    0xFF8A259C.toInt()
                } else {
                    0xFFFFA6A9.toInt()
                }

            val left =
                index *
                    stripeWidth
            val right =
                left +
                    stripeWidth

            val path =
                Path().apply {
                    moveTo(
                        left,
                        top,
                    )
                    lineTo(
                        right,
                        top,
                    )
                    lineTo(
                        right -
                            stripeWidth *
                                .10f,
                        bottom,
                    )
                    lineTo(
                        left +
                            stripeWidth *
                                .10f,
                        bottom,
                    )
                    close()
                }
            canvas.drawPath(
                path,
                paint,
            )

            canvas.drawCircle(
                left +
                    stripeWidth /
                        2f,
                bottom,
                stripeWidth *
                    .42f,
                paint,
            )
        }
    }
}
