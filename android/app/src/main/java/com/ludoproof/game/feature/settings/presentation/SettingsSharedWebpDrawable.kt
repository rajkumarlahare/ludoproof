package com.ludoproof.game.ui.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.ludoproof.game.R
import kotlin.math.max

/** Rounded center-crop rendering of the app-wide WebP with a contrast overlay. */
internal class SettingsSharedWebpDrawable(
    context: Context,
) : Drawable() {
    private val bitmap: Bitmap? =
        runCatching {
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ludo_paws_game_background,
                BitmapFactory.Options().apply {
                    inSampleSize = 2
                    inScaled = false
                },
            )
        }.getOrNull()

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xA909204B.toInt()
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFC735.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(context, 2).toFloat()
    }
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF082A69.toInt()
    }
    private val cornerRadius = dp(context, 16).toFloat()

    override fun draw(canvas: Canvas) {
        val boundsRect = bounds
        if (boundsRect.isEmpty) return
        val outer = RectF(boundsRect)
        val clip = Path().apply {
            addRoundRect(outer, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        val save = canvas.save()
        canvas.clipPath(clip)

        val source = bitmap
        if (source == null || source.width <= 0 || source.height <= 0) {
            canvas.drawRoundRect(outer, cornerRadius, cornerRadius, fallbackPaint)
        } else {
            val scale = max(
                outer.width() / source.width.toFloat(),
                outer.height() / source.height.toFloat(),
            )
            val drawnWidth = source.width * scale
            val drawnHeight = source.height * scale
            val destination = RectF(
                outer.centerX() - drawnWidth / 2f,
                outer.centerY() - drawnHeight / 2f,
                outer.centerX() + drawnWidth / 2f,
                outer.centerY() + drawnHeight / 2f,
            )
            canvas.drawBitmap(source, Rect(0, 0, source.width, source.height), destination, imagePaint)
            canvas.drawRect(outer, overlayPaint)
        }
        canvas.restoreToCount(save)
        canvas.drawRoundRect(outer, cornerRadius, cornerRadius, borderPaint)
    }

    override fun setAlpha(alpha: Int) {
        imagePaint.alpha = alpha
        overlayPaint.alpha = alpha * 0xA9 / 255
        borderPaint.alpha = alpha
        fallbackPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        imagePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android Drawable API")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
