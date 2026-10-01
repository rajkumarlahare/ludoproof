package com.ludoproof.game

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import kotlin.math.min

/** Original game-style chrome, kept vector-sharp at every density. */
class GameButtonDrawable(private val colors: IntArray, private val radius: Float = 24f, private val rim: Int = 0xFFFFE788.toInt()) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pressed = false
    private var enabled = true
    override fun isStateful() = true
    override fun onStateChange(state: IntArray): Boolean {
        pressed = android.R.attr.state_pressed in state
        enabled = android.R.attr.state_enabled in state
        invalidateSelf()
        return true
    }
    override fun draw(canvas: Canvas) {
        val rect = RectF(bounds)
        val unit = min(rect.width(), rect.height()) / 54f
        val r = radius * unit
        paint.shader = null
        paint.color = if (enabled) 0xFF533400.toInt() else 0xFF263B69.toInt()
        canvas.drawRoundRect(rect, r, r, paint)
        rect.inset(1.5f * unit, 1.5f * unit)
        rect.bottom -= 2f * unit
        if (pressed) rect.top += 2f * unit
        val palette = if (enabled) colors else intArrayOf(0xFF8298B8.toInt(), 0xFF587194.toInt(), 0xFF344D75.toInt())
        paint.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, palette, null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * unit
        paint.color = if (enabled) rim else 0xFF97ACCA.toInt()
        canvas.drawRoundRect(rect, r, r, paint)
        rect.inset(3f * unit, 3f * unit)
        paint.strokeWidth = unit
        paint.color = 0x80FFFFFF.toInt()
        canvas.drawRoundRect(rect, r, r, paint)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, rect.top, 0f, rect.centerY(), 0x65FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(rect.left, rect.top, rect.right, rect.centerY()), r, r, paint)
        paint.shader = null
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
    @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
}

class GameGlyphDrawable(private val kind: String, private val tint: Int, private val size: Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val token = TokenArt(tint)
    override fun getIntrinsicWidth() = size
    override fun getIntrinsicHeight() = size
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 64f, bounds.height() / 64f)
        when {
            kind.startsWith("players") -> {
                val count = kind.last().digitToInt()
                repeat(count) { index ->
                    val x = 13f + index * (38f / (count - 1))
                    p.shader = null; p.color = 0x55001A44
                    canvas.drawOval(x - 10f, 47f, x + 10f, 55f, p)
                    p.color = Color.WHITE
                    canvas.drawRoundRect(x - 9f, 32f, x + 9f, 51f, 6f, 6f, p)
                    canvas.drawCircle(x, 22f, 8f, p)
                }
            }
            kind == "dice" -> {
                p.color = Color.WHITE
                p.shader = LinearGradient(0f, 6f, 0f, 60f, Color.WHITE, 0xFFBBD4F1.toInt(), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(7f, 7f, 59f, 61f, 11f, 11f, p)
                p.shader = null; p.color = 0xFF173A63.toInt()
                for (x in listOf(21f, 45f)) for (y in listOf(20f, 34f, 48f)) canvas.drawCircle(x, y, 4f, p)
            }
            else -> token.draw(canvas, 32f, 32f, 62f)
        }
        canvas.restore()
    }
    private fun dark(color: Int) = Color.rgb((Color.red(color)*.45f).toInt(), (Color.green(color)*.45f).toInt(), (Color.blue(color)*.45f).toInt())
    override fun setAlpha(alpha: Int) { p.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) { p.colorFilter = filter }
    @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
}

class LudoWordmarkView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(canvas: Canvas) {
        val s = min(width / 320f, height / 136f)
        canvas.save()
        canvas.translate((width-320f*s)/2f, (height-136f*s)/2f)
        canvas.scale(s,s)
        listOf(0xFFF42C3C.toInt(),0xFF20CC54.toInt(),0xFF20AAFF.toInt(),0xFFFFD02D.toInt()).forEachIndexed { i,c ->
            GameGlyphDrawable("pawn",c,42).apply { setBounds(68+i*47,3,110+i*47,45); draw(canvas) }
        }
        paint.textSize = 68f
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 10f; paint.color = 0xFF04174E.toInt()
        canvas.drawText("LUDO",160f,91f,paint)
        paint.strokeWidth = 5f; paint.color = 0xFFFFB52D.toInt()
        canvas.drawText("LUDO",160f,87f,paint)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f,37f,0f,89f,Color.WHITE,0xFFFFE57D.toInt(),Shader.TileMode.CLAMP)
        canvas.drawText("LUDO",160f,87f,paint)
        paint.shader = null; paint.textSize = 26f; paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f; paint.color = 0xFF021346.toInt()
        canvas.drawText("P R O O F",160f,123f,paint)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawText("P R O O F",160f,120f,paint)
        canvas.restore()
    }
}
