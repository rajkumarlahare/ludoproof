package com.ludoproof.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ModeArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    enum class Mode {
        ONLINE,
        LOCAL,
    }

    var mode: Mode =
        Mode.ONLINE
        set(value) {
            field = value
            invalidate()
        }

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth =
                dp(3f)
            color =
                Color.WHITE
        }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired =
            dp(150f).toInt()
        val w =
            resolveSize(
                desired,
                widthMeasureSpec,
            )
        setMeasuredDimension(
            w,
            resolveSize(
                (w * .85f).toInt(),
                heightMeasureSpec,
            ),
        )
    }

    override fun onDraw(canvas: Canvas) {
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
                if (
                    mode ==
                    Mode.ONLINE
                ) {
                    intArrayOf(
                        0xFF35C9FF.toInt(),
                        0xFF1289E7.toInt(),
                    )
                } else {
                    intArrayOf(
                        0xFF41D8FF.toInt(),
                        0xFF20A7D9.toInt(),
                    )
                },
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            0f,
            0f,
            w,
            h,
            dp(16f),
            dp(16f),
            paint,
        )
        paint.shader = null

        if (
            mode ==
            Mode.ONLINE
        ) {
            drawOnline(
                canvas,
                w,
                h,
            )
        } else {
            drawLocal(
                canvas,
                w,
                h,
            )
        }
    }

    private fun drawOnline(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val cx =
            w * .5f
        val cy =
            h * .48f
        val r =
            min(
                w,
                h,
            ) * .22f

        paint.color =
            0xFFF4FBFF.toInt()
        canvas.drawCircle(
            cx,
            cy,
            r,
            paint,
        )
        stroke.color =
            0xFF0964BC.toInt()
        stroke.strokeWidth =
            dp(3f)
        canvas.drawCircle(
            cx,
            cy,
            r,
            stroke,
        )

        stroke.strokeWidth =
            dp(2f)
        canvas.drawOval(
            cx - r * .5f,
            cy - r,
            cx + r * .5f,
            cy + r,
            stroke,
        )
        canvas.drawOval(
            cx - r,
            cy - r * .45f,
            cx + r,
            cy + r * .45f,
            stroke,
        )

        paint.color =
            0xFFFFC82D.toInt()
        val die =
            RectF(
                cx - r * .38f,
                cy - r * .38f,
                cx + r * .38f,
                cy + r * .38f,
            )
        canvas.drawRoundRect(
            die,
            r * .18f,
            r * .18f,
            paint,
        )
        paint.color =
            Color.WHITE
        for (
            point in
            listOf(
                -.18f to -.18f,
                .18f to .18f,
                .18f to -.18f,
                -.18f to .18f,
            )
        ) {
            canvas.drawCircle(
                cx +
                    point.first *
                    r,
                cy +
                    point.second *
                    r,
                r * .06f,
                paint,
            )
        }

        drawNode(
            canvas,
            w * .20f,
            h * .28f,
            0xFF35C83D.toInt(),
        )
        drawNode(
            canvas,
            w * .80f,
            h * .30f,
            0xFFF22E35.toInt(),
        )
        drawNode(
            canvas,
            w * .22f,
            h * .72f,
            0xFFFFD324.toInt(),
        )
        drawNode(
            canvas,
            w * .78f,
            h * .72f,
            0xFF218CFF.toInt(),
        )
    }

    private fun drawLocal(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val size =
            min(
                w,
                h,
            ) * .52f
        val left =
            (w - size) / 2f
        val top =
            h * .18f

        paint.color =
            Color.WHITE
        canvas.drawRoundRect(
            left,
            top,
            left + size,
            top + size,
            dp(10f),
            dp(10f),
            paint,
        )

        val half =
            size / 2f
        val colors =
            listOf(
                0xFFF22E35.toInt(),
                0xFF35C83D.toInt(),
                0xFF218CFF.toInt(),
                0xFFFFD324.toInt(),
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

        paint.color =
            Color.WHITE
        canvas.drawCircle(
            w * .35f,
            h * .24f,
            size * .09f,
            paint,
        )
        canvas.drawCircle(
            w * .65f,
            h * .24f,
            size * .09f,
            paint,
        )
        canvas.drawCircle(
            w * .35f,
            h * .78f,
            size * .09f,
            paint,
        )
        canvas.drawCircle(
            w * .65f,
            h * .78f,
            size * .09f,
            paint,
        )

        stroke.color =
            0xFF092F79.toInt()
        stroke.strokeWidth =
            dp(2f)
        canvas.drawRoundRect(
            left,
            top,
            left + size,
            top + size,
            dp(10f),
            dp(10f),
            stroke,
        )
    }

    private fun drawNode(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
    ) {
        paint.color =
            Color.WHITE
        canvas.drawCircle(
            x,
            y,
            dp(14f),
            paint,
        )
        paint.color =
            color
        canvas.drawCircle(
            x,
            y,
            dp(10f),
            paint,
        )
    }

    private fun dp(value: Float): Float =
        value *
            resources
                .displayMetrics
                .density
}
