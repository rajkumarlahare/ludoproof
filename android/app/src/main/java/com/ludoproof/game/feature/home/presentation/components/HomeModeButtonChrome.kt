package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.min

enum class HomeModeTone {
    ONLINE,
    TEAM_UP,
    FRIENDS,
    COMPUTER,
    PASS_AND_PLAY,
}

internal fun homeModeButtonBackground(
    context: Context,
    tone: HomeModeTone,
    wide: Boolean = false,
): Drawable =
    HomeModeButtonChromeDrawable(
        density = context.resources.displayMetrics.density,
        tone = tone,
        wide = wide,
    )

/**
 * Glossy, size-independent chrome for the five Home game-mode buttons.
 *
 * The reference look is built from paint instead of a bitmap so the same
 * highlight, glow, rim and paw decoration remain crisp on every density.
 */
private class HomeModeButtonChromeDrawable(
    private val density: Float,
    private val tone: HomeModeTone,
    private val wide: Boolean,
) : Drawable() {
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5.5f)
        }
    private val outerRimPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.1f)
        }
    private val innerRimPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(0.9f)
            color = 0xB8FFFFFF.toInt()
        }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pawPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressedPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x2B001126
        }

    private val rect = RectF()
    private val innerRect = RectF()
    private val clipPath = Path()
    private var radius = 0f
    private var pressed = false
    private var drawableAlpha = 255

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)

        val inset = dp(3.6f)
        rect.set(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset,
        )
        innerRect.set(rect)
        innerRect.inset(dp(2.5f), dp(2.5f))
        radius = min(dp(24f), rect.height() * 0.38f)

        val palette = paletteFor(tone)
        bodyPaint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                intArrayOf(
                    palette.top,
                    palette.upperMid,
                    palette.lowerMid,
                    palette.bottom,
                ),
                floatArrayOf(0f, 0.28f, 0.68f, 1f),
                Shader.TileMode.CLAMP,
            )
        glossPaint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.top + rect.height() * 0.58f,
                intArrayOf(
                    0xC8FFFFFF.toInt(),
                    0x54FFFFFF,
                    0x10FFFFFF,
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.20f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
        reflectionPaint.shader =
            RadialGradient(
                rect.centerX() - rect.width() * 0.08f,
                rect.top + rect.height() * 0.035f,
                rect.width() * 0.58f,
                intArrayOf(
                    0xB8FFFFFF.toInt(),
                    0x66FFFFFF,
                    0x24FFFFFF,
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.22f, 0.58f, 1f),
                Shader.TileMode.CLAMP,
            )
        shadePaint.shader =
            LinearGradient(
                rect.left,
                rect.centerY(),
                rect.left,
                rect.bottom,
                intArrayOf(
                    Color.TRANSPARENT,
                    0x18000000,
                    0x5100112B,
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )

        glowPaint.color = palette.glow
        outerRimPaint.color = palette.rim
        pawPaint.color = palette.paw
        sparklePaint.color = 0x80FFFFFF.toInt()

        clipPath.reset()
        clipPath.addRoundRect(
            rect,
            radius,
            radius,
            Path.Direction.CW,
        )
    }

    override fun draw(canvas: Canvas) {
        if (rect.isEmpty) return

        val alphaScale = drawableAlpha / 255f
        bodyPaint.alpha = drawableAlpha
        glowPaint.alpha = (0xA8 * alphaScale).toInt()
        outerRimPaint.alpha = drawableAlpha
        innerRimPaint.alpha = (0xB8 * alphaScale).toInt()
        glossPaint.alpha = (0xC8 * alphaScale).toInt()
        reflectionPaint.alpha = (0xA8 * alphaScale).toInt()
        shadePaint.alpha = drawableAlpha
        pawPaint.alpha = (0x72 * alphaScale).toInt()
        sparklePaint.alpha = (0x80 * alphaScale).toInt()
        pressedPaint.alpha = if (pressed) (0x2B * alphaScale).toInt() else 0

        // Diffuse neon halo first, then a crisp pale rim like the supplied art.
        canvas.drawRoundRect(rect, radius, radius, glowPaint)
        canvas.drawRoundRect(rect, radius, radius, bodyPaint)

        canvas.save()
        canvas.clipPath(clipPath)

        // Large soft top reflection.
        canvas.drawRoundRect(
            RectF(
                rect.left + dp(3f),
                rect.top + dp(2f),
                rect.right - dp(3f),
                rect.top + rect.height() * 0.48f,
            ),
            radius * 0.82f,
            radius * 0.82f,
            glossPaint,
        )

        // Soft elliptical glass sheen. Unlike the former white stroke, this
        // blooms in the middle and fades toward both ends like reflected light.
        canvas.drawOval(
            RectF(
                rect.left + radius * 0.56f,
                rect.top - rect.height() * 0.07f,
                rect.right - radius * 0.56f,
                rect.top + rect.height() * 0.27f,
            ),
            reflectionPaint,
        )

        canvas.drawRect(
            rect.left,
            rect.centerY(),
            rect.right,
            rect.bottom,
            shadePaint,
        )

        drawPaw(
            canvas = canvas,
            cx = rect.right - dp(if (wide) 18f else 15f),
            cy = rect.bottom - dp(15f),
            scale = if (wide) 0.96f else 0.80f,
            rotateDegrees = -18f,
        )

        if (wide) {
            drawPaw(
                canvas = canvas,
                cx = rect.left + dp(18f),
                cy = rect.bottom - dp(15f),
                scale = 0.96f,
                rotateDegrees = 18f,
            )
        }

        if (tone == HomeModeTone.PASS_AND_PLAY) {
            drawPaw(
                canvas = canvas,
                cx = rect.left + dp(34f),
                cy = rect.top + dp(19f),
                scale = 0.44f,
                rotateDegrees = -22f,
            )
            drawPaw(
                canvas = canvas,
                cx = rect.right - dp(35f),
                cy = rect.top + dp(21f),
                scale = 0.52f,
                rotateDegrees = 20f,
            )
        }

        // Two tiny reflective dots help retain the toy/plastic finish.
        canvas.drawCircle(
            rect.right - dp(10f),
            rect.top + dp(12f),
            dp(1.7f),
            sparklePaint,
        )
        canvas.drawCircle(
            rect.right - dp(15f),
            rect.top + dp(8f),
            dp(1.0f),
            sparklePaint,
        )

        if (pressed) {
            canvas.drawRoundRect(rect, radius, radius, pressedPaint)
        }
        canvas.restore()

        canvas.drawRoundRect(rect, radius, radius, outerRimPaint)
        canvas.drawRoundRect(
            innerRect,
            (radius - dp(2.5f)).coerceAtLeast(0f),
            (radius - dp(2.5f)).coerceAtLeast(0f),
            innerRimPaint,
        )
    }

    private fun drawPaw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        rotateDegrees: Float,
    ) {
        canvas.save()
        canvas.rotate(rotateDegrees, cx, cy)

        val padW = dp(8.6f) * scale
        val padH = dp(6.5f) * scale
        canvas.drawOval(
            RectF(
                cx - padW * 0.5f,
                cy - padH * 0.05f,
                cx + padW * 0.5f,
                cy + padH * 0.95f,
            ),
            pawPaint,
        )

        val toeR = dp(2.25f) * scale
        val toeY = cy - dp(4.4f) * scale
        canvas.drawCircle(cx - dp(5.2f) * scale, toeY + dp(1.0f) * scale, toeR, pawPaint)
        canvas.drawCircle(cx - dp(1.8f) * scale, toeY - dp(1.3f) * scale, toeR, pawPaint)
        canvas.drawCircle(cx + dp(1.8f) * scale, toeY - dp(1.3f) * scale, toeR, pawPaint)
        canvas.drawCircle(cx + dp(5.2f) * scale, toeY + dp(1.0f) * scale, toeR, pawPaint)

        canvas.restore()
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(stateSet: IntArray): Boolean {
        val nextPressed = stateSet.contains(android.R.attr.state_pressed)
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
        outerRimPaint.colorFilter = colorFilter
        innerRimPaint.colorFilter = colorFilter
        glossPaint.colorFilter = colorFilter
        reflectionPaint.colorFilter = colorFilter
        shadePaint.colorFilter = colorFilter
        pawPaint.colorFilter = colorFilter
        sparklePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in the Android framework")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun dp(value: Float): Float = value * density

    private data class Palette(
        val top: Int,
        val upperMid: Int,
        val lowerMid: Int,
        val bottom: Int,
        val rim: Int,
        val glow: Int,
        val paw: Int,
    )

    private fun paletteFor(tone: HomeModeTone): Palette =
        when (tone) {
            HomeModeTone.ONLINE ->
                Palette(
                    top = 0xFF35E5FF.toInt(),
                    upperMid = 0xFF079EF8.toInt(),
                    lowerMid = 0xFF0275E2.toInt(),
                    bottom = 0xFF074AA8.toInt(),
                    rim = 0xFFD5FBFF.toInt(),
                    glow = 0xFF19CCFF.toInt(),
                    paw = 0x7ACCFBFF,
                )

            HomeModeTone.TEAM_UP ->
                Palette(
                    top = 0xFFF26CFF.toInt(),
                    upperMid = 0xFFC02DF2.toInt(),
                    lowerMid = 0xFF8A16D4.toInt(),
                    bottom = 0xFF59109B.toInt(),
                    rim = 0xFFF8DAFF.toInt(),
                    glow = 0xFFE64DFF.toInt(),
                    paw = 0x7AF7D8FF,
                )

            HomeModeTone.FRIENDS ->
                Palette(
                    top = 0xFF35F0AF.toInt(),
                    upperMid = 0xFF0BD486.toInt(),
                    lowerMid = 0xFF04A965.toInt(),
                    bottom = 0xFF087B51.toInt(),
                    rim = 0xFFCAFFE9.toInt(),
                    glow = 0xFF16EFA2.toInt(),
                    paw = 0x7AD2FFEE,
                )

            HomeModeTone.COMPUTER ->
                Palette(
                    top = 0xFFFFC82F.toInt(),
                    upperMid = 0xFFFF9E0B.toInt(),
                    lowerMid = 0xFFEF7200.toInt(),
                    bottom = 0xFFB74800.toInt(),
                    rim = 0xFFFFF0B2.toInt(),
                    glow = 0xFFFFAD22.toInt(),
                    paw = 0x7AFFF1C7,
                )

            HomeModeTone.PASS_AND_PLAY ->
                Palette(
                    top = 0xFFFF66E4.toInt(),
                    upperMid = 0xFFF329C7.toInt(),
                    lowerMid = 0xFFD90AA7.toInt(),
                    bottom = 0xFF8F0A70.toInt(),
                    rim = 0xFFFFD1F7.toInt(),
                    glow = 0xFFFF45D4.toInt(),
                    paw = 0x7AFFD6F6,
                )
        }
}
