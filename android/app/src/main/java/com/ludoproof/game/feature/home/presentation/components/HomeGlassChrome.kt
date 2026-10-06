package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import com.ludoproof.game.LudoProofTheme

internal enum class HomeGlassShape {
    PILL,
    TILE,
    CIRCLE,
}

internal enum class HomeGlassTone {
    GLASS,
    BLUE,
    PURPLE,
}

internal fun homeGlassBackground(
    context: Context,
    shape: HomeGlassShape,
    tone: HomeGlassTone = HomeGlassTone.GLASS,
): Drawable =
    HomeGlassChromeDrawable(
        context = context,
        shape = shape,
        tone = tone,
    )

/**
 * Size-independent glass chrome used by the Ludo Paws home controls.
 *
 * The drawable deliberately owns the translucent body, cyan edge, inner rim,
 * top reflection and pressed treatment so pills, tiles and micro buttons keep
 * the same visual language without bitmap stretching artifacts.
 */
private class HomeGlassChromeDrawable(
    context: Context,
    private val shape: HomeGlassShape,
    private val tone: HomeGlassTone,
) : Drawable() {
    private val density =
        context.resources.displayMetrics.density

    private val bodyPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3.5f)
        }
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1.35f)
        }
    private val innerRimPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(0.75f)
            color = 0x72FFFFFF
        }
    private val glossPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomShadePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressedPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26000000
        }

    private val outerRect = RectF()
    private val innerRect = RectF()
    private val clipPath = Path()

    private var radius = 0f
    private var pressed = false
    private var drawableAlpha = 255

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)

        val glowInset = dp(2.5f)
        outerRect.set(
            bounds.left + glowInset,
            bounds.top + glowInset,
            bounds.right - glowInset,
            bounds.bottom - glowInset,
        )
        innerRect.set(outerRect)
        innerRect.inset(dp(2f), dp(2f))

        radius =
            when (shape) {
                HomeGlassShape.PILL,
                HomeGlassShape.CIRCLE,
                -> outerRect.height() / 2f

                HomeGlassShape.TILE ->
                    minOf(
                        dp(18f),
                        outerRect.height() / 2f,
                    )
            }

        val palette = paletteFor(tone)
        bodyPaint.shader =
            LinearGradient(
                outerRect.left,
                outerRect.top,
                outerRect.left,
                outerRect.bottom,
                intArrayOf(
                    palette.top,
                    palette.middle,
                    palette.bottom,
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
        glossPaint.shader =
            LinearGradient(
                0f,
                outerRect.top,
                0f,
                outerRect.top + outerRect.height() * 0.58f,
                intArrayOf(
                    0xA8FFFFFF.toInt(),
                    0x34FFFFFF,
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
        bottomShadePaint.shader =
            LinearGradient(
                0f,
                outerRect.centerY(),
                0f,
                outerRect.bottom,
                intArrayOf(
                    Color.TRANSPARENT,
                    0x36001022,
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        glowPaint.color = palette.glow
        borderPaint.color = palette.border

        clipPath.reset()
        clipPath.addRoundRect(
            outerRect,
            radius,
            radius,
            Path.Direction.CW,
        )
    }

    override fun draw(canvas: Canvas) {
        if (outerRect.isEmpty) return

        val alphaScale = drawableAlpha / 255f
        bodyPaint.alpha = drawableAlpha
        glowPaint.alpha = (0x92 * alphaScale).toInt()
        borderPaint.alpha = drawableAlpha
        innerRimPaint.alpha = (0x72 * alphaScale).toInt()
        glossPaint.alpha = (0xA8 * alphaScale).toInt()
        bottomShadePaint.alpha = drawableAlpha
        pressedPaint.alpha = if (pressed) (0x26 * alphaScale).toInt() else 0

        canvas.drawRoundRect(
            outerRect,
            radius,
            radius,
            glowPaint,
        )
        canvas.drawRoundRect(
            outerRect,
            radius,
            radius,
            bodyPaint,
        )

        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawRect(
            outerRect.left,
            outerRect.top,
            outerRect.right,
            outerRect.top + outerRect.height() * 0.52f,
            glossPaint,
        )
        canvas.drawRect(
            outerRect.left,
            outerRect.centerY(),
            outerRect.right,
            outerRect.bottom,
            bottomShadePaint,
        )
        if (pressed) {
            canvas.drawRoundRect(
                outerRect,
                radius,
                radius,
                pressedPaint,
            )
        }
        canvas.restore()

        canvas.drawRoundRect(
            outerRect,
            radius,
            radius,
            borderPaint,
        )
        canvas.drawRoundRect(
            innerRect,
            (radius - dp(2f)).coerceAtLeast(0f),
            (radius - dp(2f)).coerceAtLeast(0f),
            innerRimPaint,
        )
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(stateSet: IntArray): Boolean {
        val nextPressed =
            stateSet.contains(android.R.attr.state_pressed)
        if (nextPressed == pressed) return false

        pressed = nextPressed
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bodyPaint.colorFilter = colorFilter
        glowPaint.colorFilter = colorFilter
        borderPaint.colorFilter = colorFilter
        innerRimPaint.colorFilter = colorFilter
        glossPaint.colorFilter = colorFilter
        bottomShadePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in the Android framework")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun dp(value: Float): Float =
        LudoProofTheme.dp(context = contextRef, value = value).toFloat()

    private val contextRef: Context = context.applicationContext

    private data class Palette(
        val top: Int,
        val middle: Int,
        val bottom: Int,
        val border: Int,
        val glow: Int,
    )

    private fun paletteFor(tone: HomeGlassTone): Palette =
        when (tone) {
            HomeGlassTone.GLASS ->
                Palette(
                    top = 0xC9297894.toInt(),
                    middle = 0xC112526F.toInt(),
                    bottom = 0xE408293F.toInt(),
                    border = 0xFF8BEAFF.toInt(),
                    glow = 0x9970E6FF.toInt(),
                )

            HomeGlassTone.BLUE ->
                Palette(
                    top = 0xFF48D4FF.toInt(),
                    middle = 0xFF1A85E8.toInt(),
                    bottom = 0xFF07519E.toInt(),
                    border = 0xFFC7F7FF.toInt(),
                    glow = 0xB45BE5FF.toInt(),
                )

            HomeGlassTone.PURPLE ->
                Palette(
                    top = 0xFFB176FF.toInt(),
                    middle = 0xFF7537DA.toInt(),
                    bottom = 0xFF3B177F.toInt(),
                    border = 0xFFE2C8FF.toInt(),
                    glow = 0xB5B779FF.toInt(),
                )
        }
}
