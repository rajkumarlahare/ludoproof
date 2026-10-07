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
 * glossy raised road tiles and glossy center treatment.
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

        // The legacy board underneath draws a dark rounded stroke exactly on this
        // platform boundary. Cover the tiny outward half of that stroke with a
        // slightly larger molded shell so no black pencil-like outline can leak
        // around the glossy home platform in normal Ludo Paws presentation.
        val outlineCover = dp(1.4f)
        val platformShell =
            RectF(
                platform.left - outlineCover,
                platform.top - outlineCover,
                platform.right + outlineCover,
                platform.bottom + outlineCover,
            )
        val shellRadius = radius + outlineCover
        fillPaint.shader =
            LinearGradient(
                platformShell.left,
                platformShell.top,
                platformShell.right,
                platformShell.bottom,
                intArrayOf(
                    brighten(color, 1.24f),
                    brighten(color, 1.10f),
                    color,
                    darken(color, 0.80f),
                ),
                floatArrayOf(0f, 0.25f, 0.68f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            platformShell,
            shellRadius,
            shellRadius,
            fillPaint,
        )
        fillPaint.shader = null

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
                    darken(color, 0.80f),
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
        val tileSize = min(raw.width(), raw.height())
        val inset = tileSize * 0.035f
        val depth = tileSize * 0.075f
        val radius = tileSize * 0.105f

        // Keep enough lower depth for the approved raised-board look, but remove the
        // old hard grey strip. The softer shadow now reads as molded thickness.
        val shadow =
            RectF(
                raw.left + inset,
                raw.top + inset + depth,
                raw.right - inset,
                raw.bottom - inset,
            )
        shadowPaint.color = 0x26000000
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

        // Premium glossy face. A vertical translucent gradient preserves the actual
        // white/colored road cell underneath instead of turning its lower half grey.
        fillPaint.shader =
            LinearGradient(
                face.left,
                face.top,
                face.left,
                face.bottom,
                intArrayOf(
                    0x70FFFFFF,
                    0x34FFFFFF,
                    0x12FFFFFF,
                    0x0E000000,
                ),
                floatArrayOf(0f, 0.25f, 0.70f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            face,
            radius,
            radius,
            fillPaint,
        )
        fillPaint.shader = null

        // Broad reflected-light band instead of a drawn white outline. The fade makes
        // each road tile look polished/glassy without the previous capsule-button line.
        val sheenInset = tileSize * 0.055f
        val sheen =
            RectF(
                face.left + sheenInset,
                face.top + tileSize * 0.025f,
                face.right - sheenInset,
                face.top + face.height() * 0.46f,
            )
        fillPaint.shader =
            LinearGradient(
                sheen.left,
                sheen.top,
                sheen.left,
                sheen.bottom,
                intArrayOf(
                    0x78FFFFFF,
                    0x30FFFFFF,
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.46f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            sheen,
            radius * 0.82f,
            radius * 0.82f,
            fillPaint,
        )
        fillPaint.shader = null

        // A very light lower polish keeps the face visually clean while the separate
        // shadow underneath supplies depth. There is intentionally no hard bottom/right stroke.
        val lowerPolish =
            RectF(
                face.left + sheenInset,
                face.top + face.height() * 0.70f,
                face.right - sheenInset,
                face.bottom - tileSize * 0.025f,
            )
        fillPaint.shader =
            LinearGradient(
                lowerPolish.left,
                lowerPolish.top,
                lowerPolish.left,
                lowerPolish.bottom,
                intArrayOf(
                    Color.TRANSPARENT,
                    0x14FFFFFF,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            lowerPolish,
            radius * 0.72f,
            radius * 0.72f,
            fillPaint,
        )
        fillPaint.shader = null
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
