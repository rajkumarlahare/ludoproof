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
            style =
                Paint.Style.STROKE
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
                (w * .78f).toInt(),
                heightMeasureSpec,
            ),
        )
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
                    0xFF062B72.toInt(),
                    0xFF074DAA.toInt(),
                    0xFF031A51.toInt(),
                ),
                floatArrayOf(
                    0f,
                    .55f,
                    1f,
                ),
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
        paint.shader =
            null

        paint.color =
            0x2215C7FF
        val grid =
            dp(24f)
        var x =
            0f
        while (
            x <= w
        ) {
            canvas.drawLine(
                x,
                0f,
                x,
                h,
                paint,
            )
            x +=
                grid
        }
        var y =
            0f
        while (
            y <= h
        ) {
            canvas.drawLine(
                0f,
                y,
                w,
                y,
                paint,
            )
            y +=
                grid
        }

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
            h * .47f
        val r =
            min(
                w,
                h,
            ) * .25f

        paint.shader =
            RadialGradient(
                cx,
                cy,
                r * 2.2f,
                intArrayOf(
                    0xAA00C8FF.toInt(),
                    0x330079FF,
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            cx,
            cy,
            r * 2.05f,
            paint,
        )
        paint.shader =
            null

        paint.color =
            0xFF073B91.toInt()
        canvas.drawCircle(
            cx,
            cy,
            r,
            paint,
        )

        stroke.color =
            0xFF8CEAFF.toInt()
        stroke.strokeWidth =
            dp(2f)
        canvas.drawCircle(
            cx,
            cy,
            r,
            stroke,
        )
        canvas.drawOval(
            RectF(
                cx - r * .48f,
                cy - r,
                cx + r * .48f,
                cy + r,
            ),
            stroke,
        )
        canvas.drawOval(
            RectF(
                cx - r,
                cy - r * .42f,
                cx + r,
                cy + r * .42f,
            ),
            stroke,
        )
        canvas.drawLine(
            cx - r,
            cy,
            cx + r,
            cy,
            stroke,
        )

        val nodes =
            listOf(
                Pair(
                    w * .18f,
                    h * .25f,
                ),
                Pair(
                    w * .82f,
                    h * .25f,
                ),
                Pair(
                    w * .16f,
                    h * .72f,
                ),
                Pair(
                    w * .84f,
                    h * .72f,
                ),
            )
        nodes.forEachIndexed {
                index,
                node,
            ->
            stroke.color =
                0x665EE9FF
            stroke.strokeWidth =
                dp(1.3f)
            canvas.drawLine(
                node.first,
                node.second,
                cx,
                cy,
                stroke,
            )
            drawNode(
                canvas,
                node.first,
                node.second,
                listOf(
                    0xFF35C83D.toInt(),
                    0xFFF22E35.toInt(),
                    0xFF2A8CFF.toInt(),
                    0xFFFFD324.toInt(),
                )[index],
            )
        }

        drawShield(
            canvas,
            cx,
            cy + r * .12f,
            r * .78f,
        )

        drawDie(
            canvas,
            w * .19f,
            h * .68f,
            r * .56f,
            0xFFF4F7FF.toInt(),
            -14f,
            5,
            0xFF083F9A.toInt(),
        )
        drawDie(
            canvas,
            w * .81f,
            h * .67f,
            r * .56f,
            0xFFF4F7FF.toInt(),
            13f,
            4,
            0xFF083F9A.toInt(),
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
            ) * .67f
        val left =
            (w - size) /
                2f
        val top =
            h * .16f

        paint.shader =
            RadialGradient(
                w * .5f,
                h * .48f,
                size,
                intArrayOf(
                    0x8800C8FF.toInt(),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            w * .5f,
            h * .48f,
            size,
            paint,
        )
        paint.shader =
            null

        paint.color =
            0xFFF4F8FF.toInt()
        canvas.drawRoundRect(
            left,
            top,
            left + size,
            top + size,
            dp(9f),
            dp(9f),
            paint,
        )

        val half =
            size /
                2f
        val inset =
            size *
                .04f
        val colors =
            listOf(
                0xFFE9323C.toInt(),
                0xFF29B557.toInt(),
                0xFF238CFF.toInt(),
                0xFFFFD12C.toInt(),
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
                        index %
                            2 ==
                        0
                    ) {
                        inset
                    } else {
                        half
                    }
            val y =
                top +
                    if (
                        index <
                            2
                    ) {
                        inset
                    } else {
                        half
                    }
            canvas.drawRoundRect(
                x,
                y,
                x + half - inset,
                y + half - inset,
                dp(5f),
                dp(5f),
                paint,
            )
        }

        stroke.color =
            0x55052A69
        stroke.strokeWidth =
            dp(1f)
        for (
            i in
            1..5
        ) {
            val p =
                size *
                    i /
                    6f
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

        val pawnSize =
            size * .095f
        drawPawn(
            canvas,
            left + size * .23f,
            top + size * .25f,
            pawnSize,
            0xFFE9323C.toInt(),
        )
        drawPawn(
            canvas,
            left + size * .77f,
            top + size * .25f,
            pawnSize,
            0xFF29B557.toInt(),
        )
        drawPawn(
            canvas,
            left + size * .24f,
            top + size * .77f,
            pawnSize,
            0xFF238CFF.toInt(),
        )
        drawPawn(
            canvas,
            left + size * .77f,
            top + size * .77f,
            pawnSize,
            0xFFFFD12C.toInt(),
        )

        drawDie(
            canvas,
            w * .5f,
            top + size * .5f,
            size * .20f,
            0xFFF4F7FF.toInt(),
            8f,
            5,
            0xFF073B91.toInt(),
        )
    }

    private fun drawShield(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
    ) {
        val path =
            Path().apply {
                moveTo(
                    cx,
                    cy - size * .62f,
                )
                lineTo(
                    cx + size * .52f,
                    cy - size * .38f,
                )
                lineTo(
                    cx + size * .43f,
                    cy + size * .25f,
                )
                quadTo(
                    cx,
                    cy + size * .72f,
                    cx,
                    cy + size * .72f,
                )
                quadTo(
                    cx,
                    cy + size * .72f,
                    cx - size * .43f,
                    cy + size * .25f,
                )
                lineTo(
                    cx - size * .52f,
                    cy - size * .38f,
                )
                close()
            }

        paint.color =
            0xFFFFC62E.toInt()
        canvas.drawPath(
            path,
            paint,
        )

        val inner =
            Path().apply {
                moveTo(
                    cx,
                    cy - size * .48f,
                )
                lineTo(
                    cx + size * .39f,
                    cy - size * .29f,
                )
                lineTo(
                    cx + size * .31f,
                    cy + size * .18f,
                )
                quadTo(
                    cx,
                    cy + size * .53f,
                    cx,
                    cy + size * .53f,
                )
                quadTo(
                    cx,
                    cy + size * .53f,
                    cx - size * .31f,
                    cy + size * .18f,
                )
                lineTo(
                    cx - size * .39f,
                    cy - size * .29f,
                )
                close()
            }
        paint.color =
            0xFF1078E6.toInt()
        canvas.drawPath(
            inner,
            paint,
        )

        stroke.color =
            Color.WHITE
        stroke.strokeWidth =
            maxOf(
                dp(2f),
                size * .08f,
            )
        stroke.strokeCap =
            Paint.Cap.ROUND
        canvas.drawLine(
            cx - size * .19f,
            cy,
            cx - size * .03f,
            cy + size * .17f,
            stroke,
        )
        canvas.drawLine(
            cx - size * .03f,
            cy + size * .17f,
            cx + size * .24f,
            cy - size * .17f,
            stroke,
        )
        stroke.strokeCap =
            Paint.Cap.BUTT
    }

    private fun drawDie(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        fill: Int,
        angle: Float,
        value: Int,
        pipColor: Int,
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
                    Color.WHITE,
                    fill,
                    0xFFD6E8FF.toInt(),
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
            0xAAFFFFFF.toInt()
        stroke.strokeWidth =
            dp(1.2f)
        canvas.drawRoundRect(
            rect,
            size * .18f,
            size * .18f,
            stroke,
        )

        val offset =
            size * .22f
        val points =
            when (
                value
            ) {
                4 ->
                    listOf(
                        Pair(-offset, -offset),
                        Pair(offset, -offset),
                        Pair(-offset, offset),
                        Pair(offset, offset),
                    )
                else ->
                    listOf(
                        Pair(-offset, -offset),
                        Pair(offset, -offset),
                        Pair(0f, 0f),
                        Pair(-offset, offset),
                        Pair(offset, offset),
                    )
            }
        paint.color =
            pipColor
        points.forEach {
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
            cy - size * .35f,
            size * .30f,
            paint,
        )
        canvas.drawOval(
            RectF(
                cx - size * .32f,
                cy - size * .15f,
                cx + size * .32f,
                cy + size * .44f,
            ),
            paint,
        )
        canvas.drawRoundRect(
            cx - size * .46f,
            cy + size * .28f,
            cx + size * .46f,
            cy + size * .50f,
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
            cy - size * .42f,
            size * .11f,
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
            dp(9f),
            paint,
        )
        paint.color =
            color
        canvas.drawCircle(
            x,
            y,
            dp(6f),
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
