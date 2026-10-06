package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ModeArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    enum class Mode {
        ONLINE,
        TEAM_UP,
        FRIENDS,
        COMPUTER,
        PASS_AND_PLAY,
    }

    var mode: Mode = Mode.ONLINE
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    init {
        // Software rendering is intentional for the tiny icon shadows only.
        // The surrounding Home scene keeps its normal renderer.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desiredW = dp(64f).toInt()
        val desiredH = dp(64f).toInt()
        setMeasuredDimension(
            resolveSize(desiredW, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        when (mode) {
            Mode.ONLINE -> drawOnline(canvas, w, h)
            Mode.TEAM_UP -> drawTeamUp(canvas, w, h)
            Mode.FRIENDS -> drawFriends(canvas, w, h)
            Mode.COMPUTER -> drawComputer(canvas, w, h)
            Mode.PASS_AND_PLAY -> drawPassAndPlay(canvas, w, h)
        }
    }

    private fun drawOnline(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val size = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = size * 0.36f

        paint.shader = null
        paint.color = 0x65002157
        paint.setShadowLayer(dp(3.2f), 0f, dp(2.2f), 0x7B001534)
        canvas.drawCircle(cx, cy + dp(1f), r * 1.04f, paint)
        paint.clearShadowLayer()

        paint.shader =
            RadialGradient(
                cx - r * 0.38f,
                cy - r * 0.42f,
                r * 1.65f,
                intArrayOf(
                    0xFF8EFAFF.toInt(),
                    0xFF20B8FF.toInt(),
                    0xFF0870D7.toInt(),
                    0xFF064AA2.toInt(),
                ),
                floatArrayOf(0f, 0.32f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        stroke.color = 0xFFF0FFFF.toInt()
        stroke.strokeWidth = dp(1.8f)
        canvas.drawCircle(cx, cy, r, stroke)

        stroke.color = 0xE6E5FEFF.toInt()
        stroke.strokeWidth = dp(1.45f)
        canvas.drawOval(
            RectF(
                cx - r * 0.45f,
                cy - r,
                cx + r * 0.45f,
                cy + r,
            ),
            stroke,
        )
        canvas.drawOval(
            RectF(
                cx - r * 0.78f,
                cy - r,
                cx + r * 0.78f,
                cy + r,
            ),
            stroke,
        )
        canvas.drawOval(
            RectF(
                cx - r,
                cy - r * 0.42f,
                cx + r,
                cy + r * 0.42f,
            ),
            stroke,
        )
        canvas.drawLine(cx - r, cy, cx + r, cy, stroke)

        paint.shader =
            RadialGradient(
                cx - r * 0.46f,
                cy - r * 0.52f,
                r * 0.42f,
                0xC8FFFFFF.toInt(),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(cx - r * 0.30f, cy - r * 0.34f, r * 0.48f, paint)
        paint.shader = null
    }

    private fun drawTeamUp(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val size = min(w, h)
        val back = size * 0.125f
        val front = size * 0.15f

        drawPerson(
            canvas = canvas,
            cx = w * 0.31f,
            cy = h * 0.48f,
            radius = back,
            topColor = 0xFF8CEBFF.toInt(),
            bottomColor = 0xFF217FE5.toInt(),
        )
        drawPerson(
            canvas = canvas,
            cx = w * 0.69f,
            cy = h * 0.48f,
            radius = back,
            topColor = 0xFFE9B8FF.toInt(),
            bottomColor = 0xFF8B36DF.toInt(),
        )
        drawPerson(
            canvas = canvas,
            cx = w * 0.50f,
            cy = h * 0.52f,
            radius = front,
            topColor = 0xFFF9F3FF.toInt(),
            bottomColor = 0xFF8F4AE7.toInt(),
            front = true,
        )
    }

    private fun drawFriends(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val size = min(w, h)
        val radius = size * 0.16f

        drawPerson(
            canvas = canvas,
            cx = w * 0.38f,
            cy = h * 0.52f,
            radius = radius,
            topColor = 0xFFFFD64B.toInt(),
            bottomColor = 0xFFFF8B12.toInt(),
            front = true,
        )
        drawPerson(
            canvas = canvas,
            cx = w * 0.62f,
            cy = h * 0.49f,
            radius = radius * 1.05f,
            topColor = 0xFF83EFFF.toInt(),
            bottomColor = 0xFF1479E5.toInt(),
            front = true,
        )
    }

    private fun drawPerson(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        topColor: Int,
        bottomColor: Int,
        front: Boolean = false,
    ) {
        val headR = radius * 0.55f
        val bodyW = radius * 1.52f
        val bodyH = radius * 1.22f
        val headCy = cy - radius * 0.62f
        val bodyTop = cy + radius * 0.04f

        paint.shader = null
        paint.color = 0x6100183F
        paint.setShadowLayer(
            dp(if (front) 3.2f else 2.4f),
            0f,
            dp(2f),
            0x7800112D,
        )
        canvas.drawOval(
            RectF(
                cx - bodyW * 0.58f,
                bodyTop - dp(1f),
                cx + bodyW * 0.58f,
                bodyTop + bodyH,
            ),
            paint,
        )
        paint.clearShadowLayer()

        paint.shader =
            LinearGradient(
                cx,
                bodyTop,
                cx,
                bodyTop + bodyH,
                topColor,
                bottomColor,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            RectF(
                cx - bodyW * 0.5f,
                bodyTop,
                cx + bodyW * 0.5f,
                bodyTop + bodyH,
            ),
            radius * 0.55f,
            radius * 0.55f,
            paint,
        )

        paint.shader =
            RadialGradient(
                cx - headR * 0.32f,
                headCy - headR * 0.35f,
                headR * 1.65f,
                intArrayOf(
                    Color.WHITE,
                    topColor,
                    bottomColor,
                ),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(cx, headCy, headR, paint)
        paint.shader = null

        stroke.color = 0xCFFFFFFF.toInt()
        stroke.strokeWidth = dp(if (front) 1.4f else 1.1f)
        canvas.drawCircle(cx, headCy, headR, stroke)
        canvas.drawRoundRect(
            RectF(
                cx - bodyW * 0.5f,
                bodyTop,
                cx + bodyW * 0.5f,
                bodyTop + bodyH,
            ),
            radius * 0.55f,
            radius * 0.55f,
            stroke,
        )
    }

    private fun drawComputer(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val size = min(w, h)
        val cx = w * 0.5f
        val body =
            RectF(
                cx - size * 0.34f,
                h * 0.28f,
                cx + size * 0.34f,
                h * 0.76f,
            )

        paint.shader = null
        paint.color = 0x7000183B
        paint.setShadowLayer(dp(3.5f), 0f, dp(2.4f), 0x80001432.toInt())
        canvas.drawRoundRect(
            RectF(body).apply { offset(0f, dp(1.5f)) },
            size * 0.12f,
            size * 0.12f,
            paint,
        )
        paint.clearShadowLayer()

        paint.shader =
            LinearGradient(
                body.left,
                body.top,
                body.right,
                body.bottom,
                intArrayOf(
                    0xFF94EEFF.toInt(),
                    0xFF3B83ED.toInt(),
                    0xFF6644D6.toInt(),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(body, size * 0.12f, size * 0.12f, paint)
        paint.shader = null

        stroke.color = 0xEFFFFFFF.toInt()
        stroke.strokeWidth = dp(1.4f)
        canvas.drawRoundRect(body, size * 0.12f, size * 0.12f, stroke)

        val screen =
            RectF(
                body.left + size * 0.075f,
                body.top + size * 0.085f,
                body.right - size * 0.075f,
                body.bottom - size * 0.095f,
            )
        paint.shader =
            LinearGradient(
                screen.left,
                screen.top,
                screen.left,
                screen.bottom,
                0xFF123B78.toInt(),
                0xFF081B46.toInt(),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(screen, size * 0.085f, size * 0.085f, paint)
        paint.shader = null

        stroke.color = 0xFF68EAFF.toInt()
        stroke.strokeWidth = dp(1.2f)
        canvas.drawRoundRect(screen, size * 0.085f, size * 0.085f, stroke)

        // Antenna and cap.
        stroke.color = 0xFFEAFDFF.toInt()
        stroke.strokeWidth = dp(2f)
        canvas.drawLine(
            cx,
            body.top,
            cx,
            body.top - size * 0.14f,
            stroke,
        )
        paint.color = 0xFF34B9FF.toInt()
        canvas.drawCircle(cx, body.top - size * 0.16f, size * 0.055f, paint)
        stroke.color = Color.WHITE
        stroke.strokeWidth = dp(1f)
        canvas.drawCircle(cx, body.top - size * 0.16f, size * 0.055f, stroke)

        // Ear blocks.
        paint.color = 0xFF4D6DE4.toInt()
        canvas.drawRoundRect(
            RectF(
                body.left - size * 0.08f,
                body.centerY() - size * 0.09f,
                body.left + size * 0.02f,
                body.centerY() + size * 0.09f,
            ),
            size * 0.04f,
            size * 0.04f,
            paint,
        )
        canvas.drawRoundRect(
            RectF(
                body.right - size * 0.02f,
                body.centerY() - size * 0.09f,
                body.right + size * 0.08f,
                body.centerY() + size * 0.09f,
            ),
            size * 0.04f,
            size * 0.04f,
            paint,
        )

        // Friendly cyan eyes and small smile.
        paint.shader =
            RadialGradient(
                screen.left + screen.width() * 0.34f,
                screen.centerY() - size * 0.02f,
                size * 0.10f,
                0xFFFFFFFF.toInt(),
                0xFF3EEFFF.toInt(),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            screen.left + screen.width() * 0.34f,
            screen.centerY() - size * 0.02f,
            size * 0.055f,
            paint,
        )
        paint.shader =
            RadialGradient(
                screen.left + screen.width() * 0.66f,
                screen.centerY() - size * 0.02f,
                size * 0.10f,
                0xFFFFFFFF.toInt(),
                0xFF3EEFFF.toInt(),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            screen.left + screen.width() * 0.66f,
            screen.centerY() - size * 0.02f,
            size * 0.055f,
            paint,
        )
        paint.shader = null

        stroke.color = 0xFF8DF6FF.toInt()
        stroke.strokeWidth = dp(1.5f)
        val smile = Path()
        smile.moveTo(screen.left + screen.width() * 0.40f, screen.centerY() + size * 0.10f)
        smile.quadTo(
            screen.centerX(),
            screen.centerY() + size * 0.16f,
            screen.left + screen.width() * 0.60f,
            screen.centerY() + size * 0.10f,
        )
        canvas.drawPath(smile, stroke)
    }

    private fun drawPassAndPlay(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val size = min(w * 0.62f, h)
        val dieSize = size * 0.50f

        drawDie(
            canvas = canvas,
            cx = w * 0.39f,
            cy = h * 0.53f,
            size = dieSize,
            rotation = -13f,
            pipColor = 0xFFE52B45.toInt(),
            value = 5,
        )
        drawDie(
            canvas = canvas,
            cx = w * 0.62f,
            cy = h * 0.46f,
            size = dieSize,
            rotation = 11f,
            pipColor = 0xFF2757CF.toInt(),
            value = 4,
        )
    }

    private fun drawDie(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        rotation: Float,
        pipColor: Int,
        value: Int,
    ) {
        canvas.save()
        canvas.rotate(rotation, cx, cy)

        val rect =
            RectF(
                cx - size * 0.5f,
                cy - size * 0.5f,
                cx + size * 0.5f,
                cy + size * 0.5f,
            )

        paint.shader = null
        paint.color = 0x70001438
        paint.setShadowLayer(dp(3f), 0f, dp(2f), 0x7C00112D)
        canvas.drawRoundRect(
            RectF(rect).apply { offset(0f, dp(1.3f)) },
            size * 0.18f,
            size * 0.18f,
            paint,
        )
        paint.clearShadowLayer()

        paint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                intArrayOf(
                    0xFFFFFFFF.toInt(),
                    0xFFF5F7FF.toInt(),
                    0xFFD6DDF0.toInt(),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, paint)
        paint.shader = null

        stroke.color = 0xC8FFFFFF.toInt()
        stroke.strokeWidth = dp(1.2f)
        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, stroke)

        // Small face reflection.
        paint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.centerY(),
                0xA8FFFFFF.toInt(),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            RectF(
                rect.left + size * 0.08f,
                rect.top + size * 0.06f,
                rect.right - size * 0.08f,
                rect.centerY(),
            ),
            size * 0.13f,
            size * 0.13f,
            paint,
        )
        paint.shader = null

        drawPips(canvas, rect, value, pipColor)
        canvas.restore()
    }

    private fun drawPips(
        canvas: Canvas,
        rect: RectF,
        value: Int,
        color: Int,
    ) {
        val x1 = rect.left + rect.width() * 0.29f
        val x2 = rect.centerX()
        val x3 = rect.right - rect.width() * 0.29f
        val y1 = rect.top + rect.height() * 0.29f
        val y2 = rect.centerY()
        val y3 = rect.bottom - rect.height() * 0.29f
        val r = rect.width() * 0.075f

        paint.shader = null
        paint.color = color

        fun pip(x: Float, y: Float) {
            paint.setShadowLayer(dp(0.8f), 0f, dp(0.5f), 0x55000000)
            canvas.drawCircle(x, y, r, paint)
            paint.clearShadowLayer()
        }

        when (value) {
            1 -> pip(x2, y2)
            2 -> {
                pip(x1, y1)
                pip(x3, y3)
            }
            3 -> {
                pip(x1, y1)
                pip(x2, y2)
                pip(x3, y3)
            }
            4 -> {
                pip(x1, y1)
                pip(x3, y1)
                pip(x1, y3)
                pip(x3, y3)
            }
            else -> {
                pip(x1, y1)
                pip(x3, y1)
                pip(x2, y2)
                pip(x1, y3)
                pip(x3, y3)
            }
        }
    }

    private fun dp(value: Float): Float = value * density
}
