package com.ludoproof.game

import android.graphics.*

/** A glazed colour cap seated in a pearl-white playing piece. */
class TokenArt(private val color: Int) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = Path().apply {
        moveTo(13f, 29f)
        cubicTo(7f, 48f, 17f, 59f, 32f, 60f)
        cubicTo(47f, 59f, 57f, 48f, 51f, 29f)
        close()
    }
    private val inset = Path().apply {
        moveTo(17f, 32f); lineTo(32f, 53f); lineTo(47f, 32f)
    }
    private val shell = LinearGradient(10f, 30f, 54f, 54f,
        intArrayOf(Color.WHITE, 0xFFEBF6FC.toInt(), 0xFF8299A7.toInt()), null, Shader.TileMode.CLAMP)
    private val cap = RadialGradient(23f, 14f, 34f,
        intArrayOf(light(color), color, dark(color)), floatArrayOf(0f, .38f, 1f), Shader.TileMode.CLAMP)

    fun draw(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.save()
        canvas.translate(x - size / 2, y - size / 2)
        canvas.scale(size / 64f, size / 64f)
        paint.style = Paint.Style.FILL; paint.shader = null; paint.color = 0x5000172A
        canvas.drawOval(9f, 49f, 57f, 64f, paint)
        paint.color = Color.WHITE; paint.shader = shell
        canvas.drawPath(body, paint)
        paint.shader = null; paint.color = dark(color); paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
        canvas.drawPath(body, paint)
        paint.color = 0xFFB7C7CE.toInt(); paint.strokeWidth = 1.7f
        canvas.drawPath(inset, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawCircle(32f, 24f, 21f, paint)
        paint.shader = cap
        canvas.drawCircle(32f, 24f, 18f, paint)
        paint.shader = null; paint.color = 0x85FFFFFF.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
        canvas.drawArc(17f, 9f, 47f, 39f, 200f, 100f, false, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }

    private fun dark(c: Int) = Color.rgb(Color.red(c) / 3, Color.green(c) / 3, Color.blue(c) / 3)
    private fun light(c: Int) = Color.rgb((Color.red(c) + 255) / 2, (Color.green(c) + 255) / 2, (Color.blue(c) + 255) / 2)
}
