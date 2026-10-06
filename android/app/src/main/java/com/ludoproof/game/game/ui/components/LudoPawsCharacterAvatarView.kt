package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Small, readable 2D mascot portrait for gameplay rails and character selection.
 *
 * Board pawns remain the shared 3D animals. This portrait deliberately uses
 * simple vector geometry so Dog/Goat/Duck/Cat never fall back to a blank
 * placeholder while final authored artwork can still replace it later.
 */
class LudoPawsCharacterAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private var characterId: String =
        LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
    private var ringColor: Int =
        0xFF5BE0FF.toInt()
    private var highlighted: Boolean = false

    fun bind(
        characterId: String?,
        ringColor: Int = 0xFF5BE0FF.toInt(),
        highlighted: Boolean = false,
    ) {
        this.characterId =
            LudoPawsCharacterCatalog
                .canonicalCharacterId(characterId)
                ?: LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID
        this.ringColor = ringColor
        this.highlighted = highlighted
        contentDescription =
            LudoPawsCharacterCatalog
                .character(this.characterId)
                ?.displayName
                ?: "Ludo Paws character"
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired = dp(56)
        val measuredWidth = resolveSize(desired, widthMeasureSpec)
        val measuredHeight = resolveSize(desired, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val scale = size / 100f
        canvas.save()
        canvas.translate(
            (width - size) / 2f,
            (height - size) / 2f,
        )
        canvas.scale(scale, scale)

        fill.color = 0x22000000
        canvas.drawCircle(50f, 53f, 44f, fill)

        fill.color = 0xFFF8FBFF.toInt()
        canvas.drawCircle(50f, 49f, 43f, fill)

        stroke.color = ringColor
        stroke.strokeWidth = if (highlighted) 6f else 4f
        canvas.drawCircle(50f, 49f, 43f, stroke)

        when (characterId) {
            "goat" -> drawGoat(canvas)
            "duck" -> drawDuck(canvas)
            "cat" -> drawCat(canvas)
            else -> drawDog(canvas)
        }

        canvas.restore()
    }

    private fun drawDog(canvas: Canvas) {
        val tan = 0xFFD89558.toInt()
        val dark = 0xFF8B5535.toInt()
        val cream = 0xFFFFE4BB.toInt()

        fill.color = dark
        canvas.drawOval(RectF(18f, 29f, 36f, 66f), fill)
        canvas.drawOval(RectF(64f, 29f, 82f, 66f), fill)

        fill.color = tan
        canvas.drawOval(RectF(25f, 20f, 75f, 73f), fill)

        fill.color = cream
        canvas.drawOval(RectF(33f, 48f, 67f, 72f), fill)

        drawEye(canvas, 40f, 43f)
        drawEye(canvas, 60f, 43f)

        fill.color = 0xFF31251F.toInt()
        canvas.drawOval(RectF(45f, 54f, 55f, 62f), fill)

        stroke.color = 0xFF5B3A2A.toInt()
        stroke.strokeWidth = 2.4f
        canvas.drawLine(50f, 62f, 50f, 66f, stroke)
        canvas.drawArc(RectF(42f, 61f, 50f, 69f), 15f, 70f, false, stroke)
        canvas.drawArc(RectF(50f, 61f, 58f, 69f), 95f, 70f, false, stroke)

        fill.color = 0xFF2688E8.toInt()
        canvas.drawRoundRect(RectF(31f, 69f, 69f, 77f), 4f, 4f, fill)
        fill.color = 0xFFFFCC42.toInt()
        canvas.drawCircle(50f, 78f, 5.2f, fill)
    }

    private fun drawGoat(canvas: Canvas) {
        val warmGray = 0xFFD8D2C7.toInt()
        val ivory = 0xFFF6E8CE.toInt()
        val horn = 0xFFC99958.toInt()

        fill.color = horn
        triangle(canvas, 31f, 31f, 35f, 9f, 43f, 31f)
        triangle(canvas, 57f, 31f, 65f, 9f, 69f, 31f)

        fill.color = warmGray
        canvas.drawOval(RectF(17f, 34f, 38f, 53f), fill)
        canvas.drawOval(RectF(62f, 34f, 83f, 53f), fill)

        fill.color = ivory
        canvas.drawOval(RectF(28f, 18f, 72f, 72f), fill)

        fill.color = 0xFFECC8A4.toInt()
        canvas.drawOval(RectF(34f, 49f, 66f, 70f), fill)

        drawEye(canvas, 40f, 42f)
        drawEye(canvas, 60f, 42f)

        fill.color = 0xFF6B5545.toInt()
        canvas.drawOval(RectF(42f, 56f, 47f, 60f), fill)
        canvas.drawOval(RectF(53f, 56f, 58f, 60f), fill)

        fill.color = 0xFFA8957F.toInt()
        val beard = Path().apply {
            moveTo(41f, 68f)
            lineTo(59f, 68f)
            lineTo(50f, 85f)
            close()
        }
        canvas.drawPath(beard, fill)
    }

    private fun drawDuck(canvas: Canvas) {
        val yellow = 0xFFFFD94D.toInt()
        val yellowDark = 0xFFF3BB2D.toInt()
        val orange = 0xFFF18A24.toInt()

        fill.color = yellowDark
        triangle(canvas, 43f, 23f, 49f, 8f, 54f, 24f)
        triangle(canvas, 49f, 23f, 58f, 11f, 59f, 28f)

        fill.color = yellow
        canvas.drawOval(RectF(23f, 18f, 77f, 72f), fill)

        drawEye(canvas, 39f, 42f)
        drawEye(canvas, 61f, 42f)

        fill.color = orange
        canvas.drawRoundRect(RectF(31f, 53f, 69f, 69f), 8f, 8f, fill)
        stroke.color = 0xFFD96D16.toInt()
        stroke.strokeWidth = 2f
        canvas.drawLine(35f, 61f, 65f, 61f, stroke)

        fill.color = 0xFF4DB6E9.toInt()
        canvas.drawArc(RectF(35f, 67f, 65f, 84f), 0f, 180f, true, fill)
    }

    private fun drawCat(canvas: Canvas) {
        val brown = 0xFFB97D4B.toInt()
        val darkBrown = 0xFF7C4B2E.toInt()
        val cream = 0xFFFFE5BD.toInt()
        val pink = 0xFFE98B93.toInt()

        fill.color = brown
        triangle(canvas, 25f, 38f, 30f, 11f, 45f, 30f)
        triangle(canvas, 55f, 30f, 70f, 11f, 75f, 38f)

        fill.color = pink
        triangle(canvas, 30f, 30f, 33f, 18f, 40f, 29f)
        triangle(canvas, 60f, 29f, 67f, 18f, 70f, 30f)

        fill.color = brown
        canvas.drawOval(RectF(24f, 22f, 76f, 74f), fill)

        fill.color = cream
        canvas.drawOval(RectF(33f, 49f, 67f, 72f), fill)

        stroke.color = darkBrown
        stroke.strokeWidth = 3f
        canvas.drawLine(45f, 27f, 43f, 37f, stroke)
        canvas.drawLine(50f, 25f, 50f, 36f, stroke)
        canvas.drawLine(55f, 27f, 57f, 37f, stroke)

        drawEye(canvas, 40f, 44f)
        drawEye(canvas, 60f, 44f)

        fill.color = pink
        triangle(canvas, 46f, 55f, 54f, 55f, 50f, 61f)

        stroke.color = darkBrown
        stroke.strokeWidth = 1.7f
        canvas.drawLine(34f, 58f, 22f, 55f, stroke)
        canvas.drawLine(34f, 62f, 21f, 63f, stroke)
        canvas.drawLine(66f, 58f, 78f, 55f, stroke)
        canvas.drawLine(66f, 62f, 79f, 63f, stroke)

        fill.color = 0xFF2688E8.toInt()
        canvas.drawRoundRect(RectF(31f, 70f, 69f, 77f), 3.5f, 3.5f, fill)
        fill.color = 0xFFFFCC42.toInt()
        canvas.drawCircle(50f, 78f, 5f, fill)
    }

    private fun drawEye(
        canvas: Canvas,
        x: Float,
        y: Float,
    ) {
        fill.color = 0xFF22252A.toInt()
        canvas.drawCircle(x, y, 5.5f, fill)
        fill.color = Color.WHITE
        canvas.drawCircle(x - 1.8f, y - 1.8f, 1.7f, fill)
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
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
            lineTo(x3, y3)
            close()
        }
        canvas.drawPath(path, fill)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density)
            .roundToInt()
}
