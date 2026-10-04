package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory

class DiceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    init {
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription =
            "Dice. No verified outcome yet."
        setLayerType(
            LAYER_TYPE_SOFTWARE,
            null,
        )
    }

    private val facePaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.4f)
            color = 0xFF20344F.toInt()
        }
    private val pipPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111827.toInt()
            style = Paint.Style.FILL
            setShadowLayer(
                dp(1.5f),
                0f,
                dp(1f),
                0x38000000,
            )
        }
    private val rollingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(5f)
            color = LudoProofTheme.GOLD
            setShadowLayer(
                dp(6f),
                0f,
                0f,
                0xAAFFB000.toInt(),
            )
        }

    private var diceStyleId =
        "dice_classic"
    private var face = 1
    private var rolling = false

    private val ticker =
        object : Runnable {
            override fun run() {
                if (!rolling) return
                face = face % 6 + 1
                invalidate()
                postDelayed(
                    this,
                    85L,
                )
            }
        }

    fun startRolling() {
        if (rolling) return
        rolling = true
        contentDescription =
            "Dice verification in progress."
        removeCallbacks(ticker)
        post(ticker)
    }

    fun showOutcome(outcome: Int) {
        if (outcome !in 1..6) return
        rolling = false
        removeCallbacks(ticker)
        face = outcome
        contentDescription =
            "Dice outcome $outcome."
        invalidate()
    }

    fun stopRolling() {
        rolling = false
        removeCallbacks(ticker)
        contentDescription =
            "Dice verification stopped."
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reloadStyle()
    }

    fun reloadStyle() {
        diceStyleId =
            CosmeticInventoryStore(
                context,
            )
                .selectedId(
                    CosmeticCategory.DICE,
                )
        invalidate()
    }

    override fun onDetachedFromWindow() {
        rolling =
            false
        removeCallbacks(
            ticker,
        )
        super.onDetachedFromWindow()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired =
            dp(96f).toInt()
        val width =
            resolveSize(
                desired,
                widthMeasureSpec,
            )
        val height =
            resolveSize(
                desired,
                heightMeasureSpec,
            )
        val size =
            minOf(
                width,
                height,
            )
        setMeasuredDimension(
            size,
            size,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size =
            minOf(
                width,
                height,
            ).toFloat()
        if (size <= 0f) return

        val shadow =
            RectF(
                size * .09f,
                size * .11f,
                size * .91f,
                size * .92f,
            )
        facePaint.shader = null
        facePaint.color =
            0x55000000
        facePaint.setShadowLayer(
            dp(3.5f),
            0f,
            dp(2f),
            0x3D000000,
        )
        canvas.drawRoundRect(
            shadow,
            size * .16f,
            size * .16f,
            facePaint,
        )
        facePaint.clearShadowLayer()

        val inset =
            size * .07f
        val rect =
            RectF(
                inset,
                inset,
                size - inset,
                size - inset,
            )
        val palette =
            dicePalette()
        facePaint.shader =
            LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                palette.faceColors,
                null,
                Shader.TileMode.CLAMP,
            )
        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            facePaint,
        )
        facePaint.shader = null
        facePaint.color =
            0x2EFFFFFF
        canvas.drawRoundRect(
            RectF(
                rect.left + size * .035f,
                rect.top + size * .035f,
                rect.right - size * .035f,
                rect.top + rect.height() * .43f,
            ),
            size * .13f,
            size * .13f,
            facePaint,
        )

        borderPaint.color =
            palette.borderColor
        pipPaint.color =
            palette.pipColor
        canvas.drawRoundRect(
            rect,
            size * .17f,
            size * .17f,
            if (rolling) {
                rollingPaint
            } else {
                borderPaint
            },
        )

        val left = size * .31f
        val center = size * .50f
        val right = size * .69f
        val top = size * .31f
        val middle = size * .50f
        val bottom = size * .69f
        val pipRadius =
            size * .055f

        fun pip(
            x: Float,
            y: Float,
        ) {
            canvas.drawCircle(
                x,
                y,
                pipRadius,
                pipPaint,
            )
        }

        when (face) {
            1 -> pip(center, middle)
            2 -> {
                pip(left, top)
                pip(right, bottom)
            }
            3 -> {
                pip(left, top)
                pip(center, middle)
                pip(right, bottom)
            }
            4 -> {
                pip(left, top)
                pip(right, top)
                pip(left, bottom)
                pip(right, bottom)
            }
            5 -> {
                pip(left, top)
                pip(right, top)
                pip(center, middle)
                pip(left, bottom)
                pip(right, bottom)
            }
            6 -> {
                pip(left, top)
                pip(right, top)
                pip(left, middle)
                pip(right, middle)
                pip(left, bottom)
                pip(right, bottom)
            }
        }
    }

    private fun dicePalette():
        DicePalette =
        when (diceStyleId) {
            "dice_pumpkin" ->
                DicePalette(
                    intArrayOf(
                        0xFFFFD59A.toInt(),
                        0xFFFFAD55.toInt(),
                        0xFFF47A2A.toInt(),
                    ),
                    0xFF5B2B18.toInt(),
                    0xFF2E1A12.toInt(),
                )
            "dice_diwali" ->
                DicePalette(
                    intArrayOf(
                        0xFFFF9A90.toInt(),
                        0xFFF45361.toInt(),
                        0xFFC92D48.toInt(),
                    ),
                    0xFFFFD44A.toInt(),
                    Color.WHITE,
                )
            "dice_football" ->
                DicePalette(
                    intArrayOf(
                        Color.WHITE,
                        0xFFE6E6E6.toInt(),
                        0xFFCFCFCF.toInt(),
                    ),
                    0xFF1A1A1A.toInt(),
                    0xFF111111.toInt(),
                )
            "dice_cricket" ->
                DicePalette(
                    intArrayOf(
                        0xFFF5FFE9.toInt(),
                        0xFFA8D99A.toInt(),
                        0xFF62A978.toInt(),
                    ),
                    0xFF174D35.toInt(),
                    Color.WHITE,
                )
            "dice_summers" ->
                DicePalette(
                    intArrayOf(
                        0xFFFFF7DE.toInt(),
                        0xFFFFE1AE.toInt(),
                        0xFFF3B08E.toInt(),
                    ),
                    0xFFAD5A44.toInt(),
                    0xFFD92B32.toInt(),
                )
            "dice_colors" ->
                DicePalette(
                    intArrayOf(
                        0xFFFFEA75.toInt(),
                        0xFFFF7BB5.toInt(),
                        0xFF6ADCE8.toInt(),
                    ),
                    0xFF6A267D.toInt(),
                    0xFF3C235A.toInt(),
                )
            "dice_heart" ->
                DicePalette(
                    intArrayOf(
                        Color.WHITE,
                        0xFFF9ECEC.toInt(),
                        0xFFE5D7D7.toInt(),
                    ),
                    0xFFB62534.toInt(),
                    0xFFD82E3D.toInt(),
                )
            else ->
                DicePalette(
                    intArrayOf(
                        Color.WHITE,
                        0xFFF9FBFF.toInt(),
                        0xFFEAF0F7.toInt(),
                    ),
                    0xFF20344F.toInt(),
                    0xFF111827.toInt(),
                )
        }

    private data class DicePalette(
        val faceColors: IntArray,
        val borderColor: Int,
        val pipColor: Int,
    )

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
