package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.min

/**
 * Presentation-only depth/chrome layer drawn over the locked Ludo board.
 *
 * Geometry, token coordinates, touch mapping and authoritative game state remain
 * owned by LudoBoardView/LudoPawsBoardView. This layer only adds the raised,
 * toy-board finish used by the garden reference: colored recessed homes,
 * beveled road tiles and glossy center treatment.
 */
internal class LudoPawsBoardChromeView(
    context: Context,
) : View(context) {
    private val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
    private val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x50000000
        }
    private val edgeHighlightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.15f)
            color = 0x82FFFFFF.toInt()
        }
    private val edgeShadePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.35f)
            color = 0x6A101820
        }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val cell = size / 15f

        drawYardDepth(canvas, cell)
        drawCrossTileDepth(canvas, cell)
        drawCenterDepth(canvas, cell)
    }

    private fun drawYardDepth(
        canvas: Canvas,
        cell: Float,
    ) {
        drawRaisedYard(canvas, cell, row = 0, col = 0, color = RED)
        drawRaisedYard(canvas, cell, row = 0, col = 9, color = GREEN)
        drawRaisedYard(canvas, cell, row = 9, col = 9, color = YELLOW)
        drawRaisedYard(canvas, cell, row = 9, col = 0, color = BLUE)
    }

    private fun drawRaisedYard(
        canvas: Canvas,
        cell: Float,
        row: Int,
        col: Int,
        color: Int,
    ) {
        val yard =
            RectF(
                axisBoundary(col, cell),
                axisBoundary(row, cell),
                axisBoundary(col + 6, cell),
                axisBoundary(row + 6, cell),
            )

        // Keep the large quadrant glossy, but let the pawn platform itself carry
        // the molded depth. No hard outline is used around the platform.
        fillPaint.shader =
            LinearGradient(
                yard.left,
                yard.top,
                yard.right,
                yard.bottom,
                intArrayOf(
                    0x48FFFFFF,
                    0x08FFFFFF,
                    0x30000000,
                ),
                floatArrayOf(0f, 0.46f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(yard, fillPaint)
        fillPaint.shader = null

        val platform =
            RectF(
                yard.left + cell * 0.50f,
                yard.top + cell * 0.50f,
                yard.right - cell * 0.50f,
                yard.bottom - cell * 0.50f,
            )
        val radius = cell * 0.40f
        val depth = cell * 0.13f

        // Soft lower body: this reads as thickness instead of a black border line.
        val platformDepth =
            RectF(
                platform.left + cell * 0.025f,
                platform.top + depth,
                platform.right + cell * 0.025f,
                platform.bottom + depth,
            )
        fillPaint.shader =
            LinearGradient(
                platformDepth.left,
                platformDepth.top,
                platformDepth.right,
                platformDepth.bottom,
                intArrayOf(
                    darken(color, 0.76f),
                    darken(color, 0.58f),
                    darken(color, 0.42f),
                ),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            platformDepth,
            radius,
            radius,
            fillPaint,
        )
        fillPaint.shader = null

        // Broad outer bevel. Because this is a filled surface rather than a stroke,
        // the rim fades naturally around the curved corners like molded plastic.
        fillPaint.shader =
            LinearGradient(
                platform.left,
                platform.top,
                platform.right,
                platform.bottom,
                intArrayOf(
                    brighten(color, 1.28f),
                    brighten(color, 1.12f),
                    color,
                    darken(color, 0.68f),
                ),
                floatArrayOf(0f, 0.24f, 0.66f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            platform,
            radius,
            radius,
            fillPaint,
        )
        fillPaint.shader = null

        val face =
            RectF(
                platform.left + cell * 0.115f,
                platform.top + cell * 0.105f,
                platform.right - cell * 0.115f,
                platform.bottom - cell * 0.145f,
            )
        val faceRadius = radius * 0.74f

        // Inset face creates the curved sidewall without a visible white/black line.
        fillPaint.shader =
            LinearGradient(
                face.left,
                face.top,
                face.right,
                face.bottom,
                intArrayOf(
                    brighten(color, 1.18f),
                    brighten(color, 1.07f),
                    color,
                    darken(color, 0.84f),
                ),
                floatArrayOf(0f, 0.28f, 0.70f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            face,
            faceRadius,
            faceRadius,
            fillPaint,
        )
        fillPaint.shader = null

        // Diffuse top sheen: a broad translucent fill, intentionally not a stroke.
        val sheen =
            RectF(
                face.left + cell * 0.035f,
                face.top + cell * 0.025f,
                face.right - cell * 0.035f,
                face.top + face.height() * 0.46f,
            )
        fillPaint.shader =
            LinearGradient(
                sheen.left,
                sheen.top,
                sheen.left,
                sheen.bottom,
                intArrayOf(
                    0x55FFFFFF,
                    0x18FFFFFF,
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            sheen,
            faceRadius * 0.90f,
            faceRadius * 0.90f,
            fillPaint,
        )
        fillPaint.shader = null
    }

    private fun drawCrossTileDepth(
        canvas: Canvas,
        cell: Float,
    ) {
        for (row in 0 until 6) {
            for (col in 6 until 9) {
                drawRaisedCellOverlay(canvas, cell, row, col)
            }
        }
        for (row in 9 until 15) {
            for (col in 6 until 9) {
                drawRaisedCellOverlay(canvas, cell, row, col)
            }
        }
        for (row in 6 until 9) {
            for (col in 0 until 6) {
                drawRaisedCellOverlay(canvas, cell, row, col)
            }
        }
        for (row in 6 until 9) {
            for (col in 9 until 15) {
                drawRaisedCellOverlay(canvas, cell, row, col)
            }
        }
    }

    private fun drawRaisedCellOverlay(
        canvas: Canvas,
        cell: Float,
        row: Int,
        col: Int,
    ) {
        val raw =
            RectF(
                axisBoundary(col, cell),
                axisBoundary(row, cell),
                axisBoundary(col + 1, cell),
                axisBoundary(row + 1, cell),
            )
        val inset = min(raw.width(), raw.height()) * 0.035f
        val depth = min(raw.width(), raw.height()) * 0.075f
        val radius = min(raw.width(), raw.height()) * 0.105f

        val shadow =
            RectF(
                raw.left + inset,
                raw.top + inset + depth,
                raw.right - inset,
                raw.bottom - inset,
            )
        shadowPaint.color = 0x44000000
        canvas.drawRoundRect(
            shadow,
            radius,
            radius,
            shadowPaint,
        )

        val face =
            RectF(
                raw.left + inset,
                raw.top + inset,
                raw.right - inset,
                raw.bottom - inset - depth * 0.42f,
            )
        fillPaint.shader =
            LinearGradient(
                face.left,
                face.top,
                face.right,
                face.bottom,
                intArrayOf(
                    0x62FFFFFF,
                    0x20FFFFFF,
                    0x05000000,
                    0x26000000,
                ),
                floatArrayOf(0f, 0.30f, 0.66f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            face,
            radius,
            radius,
            fillPaint,
        )
        fillPaint.shader = null

        canvas.drawRoundRect(
            face,
            radius,
            radius,
            edgeHighlightPaint,
        )

        edgeShadePaint.strokeWidth = dp(1.05f)
        canvas.drawLine(
            face.left + radius * 0.65f,
            face.bottom,
            face.right - radius * 0.65f,
            face.bottom,
            edgeShadePaint,
        )
        canvas.drawLine(
            face.right,
            face.top + radius * 0.65f,
            face.right,
            face.bottom - radius * 0.65f,
            edgeShadePaint,
        )
    }

    private fun drawCenterDepth(
        canvas: Canvas,
        cell: Float,
    ) {
        val center =
            RectF(
                axisBoundary(6, cell),
                axisBoundary(6, cell),
                axisBoundary(9, cell),
                axisBoundary(9, cell),
            )
        fillPaint.shader =
            LinearGradient(
                center.left,
                center.top,
                center.right,
                center.bottom,
                intArrayOf(
                    0x42FFFFFF,
                    0x08FFFFFF,
                    0x26000000,
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(center, fillPaint)
        fillPaint.shader = null

        strokePaint.strokeWidth = dp(1.3f)
        strokePaint.color = 0x8EFFFFFF.toInt()
        canvas.drawLine(
            center.left + dp(1f),
            center.top + dp(1f),
            center.right - dp(1f),
            center.top + dp(1f),
            strokePaint,
        )
        canvas.drawLine(
            center.left + dp(1f),
            center.top + dp(1f),
            center.left + dp(1f),
            center.bottom - dp(1f),
            strokePaint,
        )
    }

    /** Mirrors the released LudoBoardView visual boundaries without changing them. */
    private fun axisBoundary(
        index: Int,
        cell: Float,
    ): Float {
        val yardSpan = cell * 5f
        val roadSpan = cell * 5f
        val yardStep = yardSpan / 6f
        val roadStep = roadSpan / 3f

        return when {
            index <= 6 -> index * yardStep
            index <= 9 -> yardSpan + (index - 6) * roadStep
            else -> yardSpan + roadSpan + (index - 9) * yardStep
        }
    }

    private fun brighten(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255),
        )

    private fun darken(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255),
        )

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private companion object {
        val RED = Color.rgb(241, 37, 47)
        val GREEN = Color.rgb(0, 169, 80)
        val YELLOW = Color.rgb(255, 216, 27)
        val BLUE = Color.rgb(48, 151, 215)
    }
}
