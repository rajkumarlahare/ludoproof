package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class NaturalWorldMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val fillPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        )
    private val strokePaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                dp(
                    1f,
                )
            color =
                0xAAFFFFFF.toInt()
        }
    private val samplePaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                dp(
                    3f,
                )
            color =
                LudoProofTheme.GOLD
            setShadowLayer(
                dp(
                    5f,
                ),
                0f,
                0f,
                0xAAFFC000.toInt(),
            )
        }
    private val textPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            color =
                Color.WHITE
            textAlign =
                Paint.Align.CENTER
            isFakeBoldText =
                true
        }

    private var audit:
        OfflineRandomnessAudit? =
        null

    fun bind(
        value: OfflineRandomnessAudit,
    ) {
        audit =
            value
        contentDescription =
            "EntroNex v4 local Natural World " +
                value.width +
                " by " +
                value.height +
                ", outcome " +
                value.outcome +
                ", sampled cell " +
                value.sampleIndex
        requestLayout()
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width =
            MeasureSpec.getSize(
                widthMeasureSpec,
            )
                .takeIf {
                    it >
                        0
                }
                ?: dp(
                    320f,
                ).toInt()
        val current =
            audit
        val ratio =
            if (
                current !=
                null &&
                current.width >
                0
            ) {
                current.height
                    .toFloat() /
                    current.width
                    .toFloat()
            } else {
                2f /
                    3f
            }
        val desiredHeight =
            (
                width *
                    ratio
                ).toInt()
        setMeasuredDimension(
            resolveSize(
                width,
                widthMeasureSpec,
            ),
            resolveSize(
                desiredHeight,
                heightMeasureSpec,
            ),
        )
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(
            canvas,
        )
        val current =
            audit
                ?: return
        if (
            current.width <=
                0 ||
            current.height <=
                0 ||
            current.field.size !=
                current.width *
                current.height
        ) {
            return
        }

        val pad =
            dp(
                4f,
            )
        val availableWidth =
            width -
                pad *
                2f
        val availableHeight =
            height -
                pad *
                2f
        val cell =
            min(
                availableWidth /
                    current.width,
                availableHeight /
                    current.height,
            )
        val mapWidth =
            cell *
                current.width
        val mapHeight =
            cell *
                current.height
        val left =
            (
                width -
                    mapWidth
                ) /
                2f
        val top =
            (
                height -
                    mapHeight
                ) /
                2f

        textPaint.textSize =
            cell *
                0.42f

        current.field
            .forEachIndexed {
                    index,
                    outcome ->
                val x =
                    index %
                        current.width
                val y =
                    index /
                        current.width
                val rect =
                    RectF(
                        left +
                            x *
                            cell,
                        top +
                            y *
                            cell,
                        left +
                            (
                                x +
                                    1
                                ) *
                            cell,
                        top +
                            (
                                y +
                                    1
                                ) *
                            cell,
                    )

                fillPaint.color =
                    colorFor(
                        outcome,
                    )
                canvas.drawRect(
                    rect,
                    fillPaint,
                )
                canvas.drawRect(
                    rect,
                    strokePaint,
                )

                val baseline =
                    rect.centerY() -
                        (
                            textPaint.ascent() +
                                textPaint.descent()
                            ) /
                        2f
                canvas.drawText(
                    outcome.toString(),
                    rect.centerX(),
                    baseline,
                    textPaint,
                )

                if (
                    index ==
                    current.sampleIndex
                ) {
                    canvas.drawRect(
                        rect,
                        samplePaint,
                    )
                }
            }
    }

    private fun colorFor(
        outcome: Int,
    ): Int =
        when (
            outcome
        ) {
            1 ->
                0xFFE53935.toInt()
            2 ->
                0xFF1E88E5.toInt()
            3 ->
                0xFF43A047.toInt()
            4 ->
                0xFFF9A825.toInt()
            5 ->
                0xFF8E24AA.toInt()
            6 ->
                0xFF00ACC1.toInt()
            else ->
                0xFF455A64.toInt()
        }

    private fun dp(
        value: Float,
    ): Float =
        value *
            resources
                .displayMetrics
                .density
}
