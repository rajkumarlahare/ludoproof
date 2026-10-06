package com.ludoproof.game.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.min

/** Decorative Home-only mascot scene. It does not participate in gameplay state. */
internal class HomePetsHeroView(
    context: Context,
) : View(context) {
    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val designWidth = 440f
        val designHeight = 250f
        val scale = min(width / designWidth, height / designHeight)
        val dx = (width - designWidth * scale) / 2f
        val dy = (height - designHeight * scale) / 2f

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        drawBoard(canvas)
        drawGroundShadows(canvas)
        drawPedestal(canvas, 69f, 203f, 88f, 30f, 0xFF0A7DE7.toInt(), 0xFF18B7FF.toInt())
        drawPedestal(canvas, 161f, 211f, 69f, 25f, 0xFF7A24D6.toInt(), 0xFFC84CFF.toInt())
        drawPedestal(canvas, 269f, 211f, 75f, 25f, 0xFF12A75B.toInt(), 0xFF35D77A.toInt())
        drawPedestal(canvas, 371f, 203f, 88f, 30f, 0xFFD92C2F.toInt(), 0xFFFF5757.toInt())

        drawDog(canvas)
        drawCat(canvas)
        drawDuck(canvas)
        drawGoat(canvas)
        drawPawDie(canvas)

        canvas.restore()
    }

    private fun drawBoard(canvas: Canvas) {
        val board =
            Path().apply {
                moveTo(137f, 47f)
                lineTo(302f, 47f)
                lineTo(326f, 164f)
                lineTo(113f, 164f)
                close()
            }

        fill.shader =
            LinearGradient(
                0f,
                47f,
                0f,
                164f,
                intArrayOf(0xFFF8FCFF.toInt(), 0xFFDDE7E9.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawPath(board, fill)
        fill.shader = null

        canvas.save()
        canvas.clipPath(board)
        fill.color = 0xFFF0453D.toInt()
        canvas.drawRect(114f, 47f, 184f, 104f, fill)
        fill.color = 0xFFFFC328.toInt()
        canvas.drawRect(255f, 47f, 326f, 104f, fill)
        fill.color = 0xFF1BAE66.toInt()
        canvas.drawRect(255f, 104f, 326f, 166f, fill)
        fill.color = 0xFF2A87E8.toInt()
        canvas.drawRect(112f, 104f, 184f, 166f, fill)

        fill.color = 0xFFF7FBFF.toInt()
        canvas.drawRect(184f, 47f, 255f, 166f, fill)
        canvas.drawRect(112f, 89f, 326f, 123f, fill)

        val cell = 14.2f
        stroke.color = 0x55475663
        stroke.strokeWidth = 1.1f
        for (index in 0..5) {
            val x = 184f + index * cell
            canvas.drawLine(x, 47f, x, 166f, stroke)
        }
        for (index in 0..8) {
            val y = 47f + index * 14.8f
            canvas.drawLine(112f, y, 326f, y, stroke)
        }

        fill.color = 0xFFF24842.toInt()
        triangle(canvas, 219.5f, 89f, 199f, 104f, 240f, 104f)
        fill.color = 0xFFFFC62C.toInt()
        triangle(canvas, 219.5f, 89f, 240f, 104f, 219.5f, 120f)
        fill.color = 0xFF1FB367.toInt()
        triangle(canvas, 219.5f, 120f, 240f, 104f, 219.5f, 104f)
        fill.color = 0xFF2A8AE9.toInt()
        triangle(canvas, 199f, 104f, 219.5f, 120f, 219.5f, 104f)
        canvas.restore()

        stroke.color = 0x885E5B62.toInt()
        stroke.strokeWidth = 2.3f
        canvas.drawPath(board, stroke)

        fill.color = 0x33FFFFFF
        val gloss =
            Path().apply {
                moveTo(146f, 52f)
                lineTo(292f, 52f)
                lineTo(300f, 69f)
                lineTo(143f, 69f)
                close()
            }
        canvas.drawPath(gloss, fill)
    }

    private fun drawGroundShadows(canvas: Canvas) {
        fill.shader = null
        fill.color = 0x33000000
        canvas.drawOval(RectF(19f, 211f, 118f, 232f), fill)
        canvas.drawOval(RectF(126f, 216f, 198f, 232f), fill)
        canvas.drawOval(RectF(229f, 216f, 309f, 232f), fill)
        canvas.drawOval(RectF(324f, 210f, 420f, 231f), fill)
        canvas.drawOval(RectF(182f, 226f, 261f, 245f), fill)
    }

    private fun drawPedestal(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        darkColor: Int,
        brightColor: Int,
    ) {
        val top = RectF(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height * .18f)
        val body = RectF(cx - width / 2f, cy - height * .12f, cx + width / 2f, cy + height / 2f)

        fill.shader =
            LinearGradient(
                0f,
                body.top,
                0f,
                body.bottom,
                intArrayOf(brightColor, darkColor, darken(darkColor, .68f)),
                floatArrayOf(0f, .52f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(body, height * .28f, height * .28f, fill)
        fill.shader =
            RadialGradient(
                cx - width * .12f,
                top.top + height * .08f,
                width * .56f,
                intArrayOf(lighten(brightColor, 1.23f), brightColor, darkColor),
                floatArrayOf(0f, .58f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawOval(top, fill)
        fill.shader = null

        stroke.color = 0x99FFFFFF.toInt()
        stroke.strokeWidth = 1.4f
        canvas.drawArc(top, 198f, 142f, false, stroke)
        stroke.color = darken(darkColor, .55f)
        stroke.strokeWidth = 2.2f
        canvas.drawArc(body, 7f, 166f, false, stroke)
    }

    private fun drawDog(canvas: Canvas) {
        val cx = 69f

        // Body and legs.
        fill.shader =
            LinearGradient(0f, 111f, 0f, 196f, intArrayOf(0xFFFFC073.toInt(), 0xFFCE6D36.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 30f, 116f, cx + 30f, 190f), fill)
        fill.shader = null
        fill.color = 0xFFFFD59B.toInt()
        canvas.drawOval(RectF(cx - 17f, 142f, cx + 17f, 187f), fill)

        fill.color = 0xFFE98A4C.toInt()
        canvas.drawRoundRect(RectF(cx - 27f, 169f, cx - 7f, 202f), 9f, 9f, fill)
        canvas.drawRoundRect(RectF(cx + 7f, 169f, cx + 27f, 202f), 9f, 9f, fill)
        fill.color = 0xFFFFD7A0.toInt()
        canvas.drawOval(RectF(cx - 31f, 192f, cx - 4f, 207f), fill)
        canvas.drawOval(RectF(cx + 4f, 192f, cx + 31f, 207f), fill)

        // Floppy ears behind the head.
        fill.shader = LinearGradient(0f, 69f, 0f, 142f, intArrayOf(0xFFB8582D.toInt(), 0xFF7C351F.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 49f, 69f, cx - 19f, 143f), fill)
        canvas.drawOval(RectF(cx + 19f, 69f, cx + 49f, 143f), fill)
        fill.shader = null

        // Head.
        fill.shader =
            RadialGradient(
                cx - 12f,
                84f,
                68f,
                intArrayOf(0xFFFFCE8B.toInt(), 0xFFE98749.toInt(), 0xFFC65A2E.toInt()),
                floatArrayOf(0f, .58f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawOval(RectF(cx - 39f, 65f, cx + 39f, 148f), fill)
        fill.shader = null

        // White blaze + muzzle.
        fill.color = 0xFFFFF3DE.toInt()
        val blaze =
            Path().apply {
                moveTo(cx - 8f, 67f)
                cubicTo(cx - 18f, 84f, cx - 13f, 103f, cx - 18f, 118f)
                cubicTo(cx - 10f, 126f, cx + 11f, 126f, cx + 18f, 117f)
                cubicTo(cx + 11f, 99f, cx + 17f, 81f, cx + 8f, 67f)
                close()
            }
        canvas.drawPath(blaze, fill)
        canvas.drawOval(RectF(cx - 27f, 107f, cx + 27f, 143f), fill)

        drawGlossyEye(canvas, cx - 15f, 103f, 9.6f, 0xFF4D2A1B.toInt())
        drawGlossyEye(canvas, cx + 15f, 103f, 9.6f, 0xFF4D2A1B.toInt())

        fill.color = 0xFF201817.toInt()
        canvas.drawOval(RectF(cx - 8.5f, 117f, cx + 8.5f, 128f), fill)
        fill.color = 0x66FFFFFF
        canvas.drawOval(RectF(cx - 4.8f, 118f, cx + 1f, 121.5f), fill)

        stroke.color = 0xFF623322.toInt()
        stroke.strokeWidth = 2.2f
        canvas.drawLine(cx, 127f, cx, 132f, stroke)
        canvas.drawArc(RectF(cx - 13f, 126f, cx, 139f), 7f, 74f, false, stroke)
        canvas.drawArc(RectF(cx, 126f, cx + 13f, 139f), 99f, 74f, false, stroke)
        fill.color = 0xFFFF6F83.toInt()
        canvas.drawOval(RectF(cx - 7f, 133f, cx + 7f, 145f), fill)

        drawCollar(canvas, cx, 151f, 55f, 0xFF0F79DE.toInt(), 0xFFFFD447.toInt())
        drawChestHighlight(canvas, cx - 16f, 150f, 12f, 32f)
    }

    private fun drawCat(canvas: Canvas) {
        val cx = 161f
        val gray = 0xFF9AA0AD.toInt()
        val grayDark = 0xFF5D6370.toInt()

        fill.shader = LinearGradient(0f, 129f, 0f, 205f, intArrayOf(0xFFC5C9D1.toInt(), gray), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 27f, 128f, cx + 27f, 202f), fill)
        fill.shader = null
        fill.color = 0xFFF7F4EF.toInt()
        canvas.drawOval(RectF(cx - 17f, 147f, cx + 17f, 197f), fill)

        // Ears.
        fill.color = gray
        triangle(canvas, cx - 35f, 102f, cx - 27f, 65f, cx - 7f, 92f)
        triangle(canvas, cx + 7f, 92f, cx + 27f, 65f, cx + 35f, 102f)
        fill.color = 0xFFF3A7B6.toInt()
        triangle(canvas, cx - 29f, 91f, cx - 26f, 73f, cx - 14f, 91f)
        triangle(canvas, cx + 14f, 91f, cx + 26f, 73f, cx + 29f, 91f)

        fill.shader = RadialGradient(cx - 11f, 94f, 57f, intArrayOf(0xFFD6D9DF.toInt(), gray, grayDark), floatArrayOf(0f, .68f, 1f), Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 34f, 84f, cx + 34f, 153f), fill)
        fill.shader = null

        fill.color = 0xFFF7F4EF.toInt()
        canvas.drawOval(RectF(cx - 22f, 118f, cx + 22f, 149f), fill)

        // Forehead stripes.
        stroke.color = 0xFF666B76.toInt()
        stroke.strokeWidth = 3.1f
        canvas.drawLine(cx - 12f, 91f, cx - 8f, 106f, stroke)
        canvas.drawLine(cx, 89f, cx, 105f, stroke)
        canvas.drawLine(cx + 12f, 91f, cx + 8f, 106f, stroke)

        drawGlossyEye(canvas, cx - 13f, 116f, 8.5f, 0xFF2C87A9.toInt())
        drawGlossyEye(canvas, cx + 13f, 116f, 8.5f, 0xFF2C87A9.toInt())

        fill.color = 0xFFE78191.toInt()
        triangle(canvas, cx - 4.5f, 130f, cx + 4.5f, 130f, cx, 136f)
        stroke.color = 0xFF5B5156.toInt()
        stroke.strokeWidth = 1.5f
        canvas.drawArc(RectF(cx - 10f, 132f, cx, 141f), 8f, 74f, false, stroke)
        canvas.drawArc(RectF(cx, 132f, cx + 10f, 141f), 98f, 74f, false, stroke)
        canvas.drawLine(cx - 18f, 134f, cx - 33f, 131f, stroke)
        canvas.drawLine(cx - 18f, 138f, cx - 34f, 140f, stroke)
        canvas.drawLine(cx + 18f, 134f, cx + 33f, 131f, stroke)
        canvas.drawLine(cx + 18f, 138f, cx + 34f, 140f, stroke)

        drawCollar(canvas, cx, 154f, 46f, 0xFF8E2ACB.toInt(), 0xFFFFD54A.toInt())

        fill.color = 0xFFB2B6C0.toInt()
        canvas.drawRoundRect(RectF(cx - 24f, 174f, cx - 7f, 207f), 7f, 7f, fill)
        canvas.drawRoundRect(RectF(cx + 7f, 174f, cx + 24f, 207f), 7f, 7f, fill)
        fill.color = 0xFFF7F4EF.toInt()
        canvas.drawOval(RectF(cx - 27f, 199f, cx - 4f, 211f), fill)
        canvas.drawOval(RectF(cx + 4f, 199f, cx + 27f, 211f), fill)
    }

    private fun drawDuck(canvas: Canvas) {
        val cx = 269f

        fill.shader = RadialGradient(cx - 10f, 150f, 54f, intArrayOf(0xFFFFF06A.toInt(), 0xFFFFD535.toInt(), 0xFFEFB829.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 31f, 135f, cx + 31f, 205f), fill)
        fill.shader = null

        // Wings.
        fill.color = 0xFFF6C82B.toInt()
        canvas.drawOval(RectF(cx - 42f, 153f, cx - 15f, 188f), fill)
        canvas.drawOval(RectF(cx + 15f, 153f, cx + 42f, 188f), fill)

        // Head.
        fill.shader = RadialGradient(cx - 12f, 101f, 60f, intArrayOf(0xFFFFF47A.toInt(), 0xFFFFD83A.toInt(), 0xFFEBAF23.toInt()), floatArrayOf(0f, .68f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, 113f, 37f, fill)
        fill.shader = null

        // Tuft.
        fill.color = 0xFFFFE750.toInt()
        val tuft =
            Path().apply {
                moveTo(cx - 8f, 80f)
                quadraticTo(cx - 3f, 67f, cx + 1f, 80f)
                quadraticTo(cx + 8f, 68f, cx + 10f, 83f)
                quadraticTo(cx + 2f, 77f, cx - 8f, 80f)
                close()
            }
        canvas.drawPath(tuft, fill)

        drawGlossyEye(canvas, cx - 13f, 108f, 8.5f, 0xFF2B251B.toInt())
        drawGlossyEye(canvas, cx + 13f, 108f, 8.5f, 0xFF2B251B.toInt())

        // Open beak.
        fill.shader = LinearGradient(cx, 120f, cx, 140f, intArrayOf(0xFFFFA037.toInt(), 0xFFF0781F.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 18f, 122f, cx + 18f, 139f), fill)
        fill.shader = null
        stroke.color = 0xFFC45B16.toInt()
        stroke.strokeWidth = 1.6f
        canvas.drawLine(cx - 14f, 131f, cx + 14f, 131f, stroke)
        fill.color = 0xFF5B2A1C.toInt()
        canvas.drawOval(RectF(cx - 10f, 132f, cx + 10f, 138f), fill)

        drawCollar(canvas, cx, 153f, 45f, 0xFF16A766.toInt(), 0xFFFFD64D.toInt())

        fill.color = 0xFFF28A25.toInt()
        canvas.drawOval(RectF(cx - 31f, 198f, cx - 3f, 211f), fill)
        canvas.drawOval(RectF(cx + 3f, 198f, cx + 31f, 211f), fill)
    }

    private fun drawGoat(canvas: Canvas) {
        val cx = 371f
        val ivory = 0xFFF7F2E8.toInt()
        val cream = 0xFFE4DDD2.toInt()

        // Body.
        fill.shader = LinearGradient(0f, 125f, 0f, 197f, intArrayOf(0xFFFFFFFF.toInt(), 0xFFE9E3D9.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 31f, 124f, cx + 31f, 194f), fill)
        fill.shader = null

        fill.color = cream
        canvas.drawRoundRect(RectF(cx - 27f, 166f, cx - 7f, 202f), 8f, 8f, fill)
        canvas.drawRoundRect(RectF(cx + 7f, 166f, cx + 27f, 202f), 8f, 8f, fill)
        fill.color = ivory
        canvas.drawOval(RectF(cx - 31f, 193f, cx - 3f, 207f), fill)
        canvas.drawOval(RectF(cx + 3f, 193f, cx + 31f, 207f), fill)

        // Horns behind head.
        stroke.color = 0xFF8B5B37.toInt()
        stroke.strokeWidth = 7.5f
        canvas.drawArc(RectF(cx - 37f, 66f, cx - 5f, 106f), 180f, 122f, false, stroke)
        canvas.drawArc(RectF(cx + 5f, 66f, cx + 37f, 106f), 238f, 122f, false, stroke)
        stroke.color = 0xFFC49A6C.toInt()
        stroke.strokeWidth = 2.2f
        canvas.drawArc(RectF(cx - 35f, 68f, cx - 7f, 103f), 184f, 112f, false, stroke)
        canvas.drawArc(RectF(cx + 7f, 68f, cx + 35f, 103f), 244f, 112f, false, stroke)

        // Long ears.
        fill.color = 0xFFF2E9DE.toInt()
        canvas.save()
        canvas.rotate(-14f, cx - 30f, 104f)
        canvas.drawOval(RectF(cx - 56f, 94f, cx - 20f, 119f), fill)
        canvas.restore()
        canvas.save()
        canvas.rotate(14f, cx + 30f, 104f)
        canvas.drawOval(RectF(cx + 20f, 94f, cx + 56f, 119f), fill)
        canvas.restore()
        fill.color = 0xFFE7AEB0.toInt()
        canvas.drawOval(RectF(cx - 48f, 101f, cx - 27f, 112f), fill)
        canvas.drawOval(RectF(cx + 27f, 101f, cx + 48f, 112f), fill)

        // Head.
        fill.shader = RadialGradient(cx - 11f, 98f, 61f, intArrayOf(0xFFFFFFFF.toInt(), ivory, 0xFFD9D0C3.toInt()), floatArrayOf(0f, .72f, 1f), Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 34f, 83f, cx + 34f, 153f), fill)
        fill.shader = null

        fill.color = 0xFFF1C9BD.toInt()
        canvas.drawOval(RectF(cx - 22f, 121f, cx + 22f, 148f), fill)

        drawGlossyEye(canvas, cx - 13f, 112f, 8.2f, 0xFF3B2C25.toInt())
        drawGlossyEye(canvas, cx + 13f, 112f, 8.2f, 0xFF3B2C25.toInt())

        fill.color = 0xFF7C5B4F.toInt()
        canvas.drawOval(RectF(cx - 10f, 131f, cx - 3f, 136f), fill)
        canvas.drawOval(RectF(cx + 3f, 131f, cx + 10f, 136f), fill)
        stroke.color = 0xFF79564E.toInt()
        stroke.strokeWidth = 1.8f
        canvas.drawArc(RectF(cx - 10f, 134f, cx + 10f, 147f), 18f, 144f, false, stroke)

        drawCollar(canvas, cx, 153f, 50f, 0xFFD93A3E.toInt(), 0xFFFFD246.toInt())
        // Golden bell.
        fill.shader = LinearGradient(cx, 158f, cx, 174f, intArrayOf(0xFFFFF083.toInt(), 0xFFFFB925.toInt()), null, Shader.TileMode.CLAMP)
        val bell = Path().apply {
            moveTo(cx - 7f, 160f)
            quadraticTo(cx - 9f, 168f, cx - 11f, 171f)
            lineTo(cx + 11f, 171f)
            quadraticTo(cx + 9f, 168f, cx + 7f, 160f)
            close()
        }
        canvas.drawPath(bell, fill)
        fill.shader = null
        fill.color = 0xFF9A6112.toInt()
        canvas.drawCircle(cx, 173f, 2.5f, fill)
    }

    private fun drawPawDie(canvas: Canvas) {
        val cx = 220f
        val topY = 191f
        val left = 190f
        val right = 250f
        val bottom = 244f

        // Shadow.
        fill.color = 0x42000000
        canvas.drawOval(RectF(181f, 232f, 260f, 249f), fill)

        val topFace =
            Path().apply {
                moveTo(cx, topY)
                lineTo(right, 203f)
                lineTo(cx + 2f, 218f)
                lineTo(left, 205f)
                close()
            }
        fill.shader = LinearGradient(left, topY, right, 218f, intArrayOf(Color.WHITE, 0xFFE7EBF0.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(topFace, fill)
        fill.shader = null

        val leftFace =
            Path().apply {
                moveTo(left, 205f)
                lineTo(cx + 2f, 218f)
                lineTo(cx + 1f, bottom)
                lineTo(191f, 231f)
                close()
            }
        fill.shader = LinearGradient(left, 205f, cx, bottom, intArrayOf(0xFFF5F7FA.toInt(), 0xFFD7DCE4.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(leftFace, fill)
        fill.shader = null

        val rightFace =
            Path().apply {
                moveTo(cx + 2f, 218f)
                lineTo(right, 203f)
                lineTo(249f, 230f)
                lineTo(cx + 1f, bottom)
                close()
            }
        fill.shader = LinearGradient(cx, 207f, right, bottom, intArrayOf(0xFFE8ECF2.toInt(), 0xFFBEC6D0.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawPath(rightFace, fill)
        fill.shader = null

        stroke.color = 0xFFB4BBC5.toInt()
        stroke.strokeWidth = 1.8f
        canvas.drawPath(topFace, stroke)
        canvas.drawPath(leftFace, stroke)
        canvas.drawPath(rightFace, stroke)

        drawTinyPaw(canvas, cx + 1f, 205f, .62f, 0xFF3F3C50.toInt())
        drawTinyPaw(canvas, 205f, 225f, .42f, 0xFF292735.toInt())
        drawTinyPaw(canvas, 235f, 225f, .46f, 0xFF292735.toInt())

        fill.color = 0x55FFFFFF
        canvas.drawOval(RectF(199f, 198f, 222f, 204f), fill)
    }

    private fun drawCollar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        color: Int,
        pendantColor: Int,
    ) {
        fill.shader = LinearGradient(cx - width / 2f, cy, cx + width / 2f, cy + 8f, intArrayOf(lighten(color, 1.25f), color, darken(color, .72f)), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(cx - width / 2f, cy - 4f, cx + width / 2f, cy + 5f), 4.5f, 4.5f, fill)
        fill.shader = null
        fill.shader = RadialGradient(cx - 2f, cy + 10f, 9f, intArrayOf(lighten(pendantColor, 1.18f), pendantColor, darken(pendantColor, .72f)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy + 10f, 7f, fill)
        fill.shader = null
        fill.color = 0x88FFFFFF.toInt()
        canvas.drawCircle(cx - 2f, cy + 8f, 1.8f, fill)
    }

    private fun drawChestHighlight(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
    ) {
        fill.color = 0x44FFFFFF
        canvas.drawOval(RectF(cx - width / 2f, cy, cx + width / 2f, cy + height), fill)
    }

    private fun drawGlossyEye(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        irisColor: Int,
    ) {
        fill.color = 0xFFEDEFF3.toInt()
        canvas.drawCircle(cx, cy, radius + 1.6f, fill)
        fill.shader = RadialGradient(cx - radius * .2f, cy - radius * .2f, radius, intArrayOf(lighten(irisColor, 1.55f), irisColor, 0xFF151316.toInt()), floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, fill)
        fill.shader = null
        fill.color = 0xFF101014.toInt()
        canvas.drawCircle(cx, cy + radius * .12f, radius * .52f, fill)
        fill.color = Color.WHITE
        canvas.drawCircle(cx - radius * .32f, cy - radius * .34f, radius * .28f, fill)
        fill.color = 0x99FFFFFF.toInt()
        canvas.drawCircle(cx + radius * .28f, cy - radius * .06f, radius * .12f, fill)
    }

    private fun drawTinyPaw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        color: Int,
    ) {
        fill.shader = null
        fill.color = color
        canvas.drawOval(RectF(cx - 6f * scale, cy, cx + 6f * scale, cy + 7f * scale), fill)
        canvas.drawCircle(cx - 7f * scale, cy - 3f * scale, 2.5f * scale, fill)
        canvas.drawCircle(cx - 2.4f * scale, cy - 6f * scale, 2.6f * scale, fill)
        canvas.drawCircle(cx + 2.4f * scale, cy - 6f * scale, 2.6f * scale, fill)
        canvas.drawCircle(cx + 7f * scale, cy - 3f * scale, 2.5f * scale, fill)
    }

    private fun triangle(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        val path =
            Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
                lineTo(x3, y3)
                close()
            }
        canvas.drawPath(path, fill)
    }

    private fun lighten(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor).toInt().coerceAtMost(255),
            (Color.green(color) * factor).toInt().coerceAtMost(255),
            (Color.blue(color) * factor).toInt().coerceAtMost(255),
        )

    private fun darken(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor).toInt().coerceAtLeast(0),
            (Color.green(color) * factor).toInt().coerceAtLeast(0),
            (Color.blue(color) * factor).toInt().coerceAtLeast(0),
        )
}
