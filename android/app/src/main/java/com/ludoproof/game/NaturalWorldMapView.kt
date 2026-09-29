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
                    .8f,
                )
            color =
                0x66FFFFFF
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
                    6f,
                ),
                0f,
                0f,
                0xCCFFC000.toInt(),
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
            setShadowLayer(
                dp(
                    1.5f,
                ),
                0f,
                dp(
                    1f,
                ),
                0x77000000,
            )
        }

    private var audit:
        OfflineRandomnessAudit? =
        null

    init {
        setLayerType(
            LAYER_TYPE_SOFTWARE,
            null,
        )
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_YES
    }

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
                8f,
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
        val gap =
            min(
                dp(
                    1.4f,
                ),
                cell *
                    .08f,
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
                0.40f

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
                val raw =
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
                val rect =
                    RectF(
                        raw.left +
                            gap,
                        raw.top +
                            gap,
                        raw.right -
                            gap,
                        raw.bottom -
                            gap,
                    )

                fillPaint.color =
                    colorFor(
                        outcome,
                    )
                canvas.drawRoundRect(
                    rect,
                    cell *
                        .12f,
                    cell *
                        .12f,
                    fillPaint,
                )
                canvas.drawRoundRect(
                    rect,
                    cell *
                        .12f,
                    cell *
                        .12f,
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
                    val sampleRect =
                        RectF(
                            rect.left -
                                gap *
                                .8f,
                            rect.top -
                                gap *
                                .8f,
                            rect.right +
                                gap *
                                .8f,
                            rect.bottom +
                                gap *
                                .8f,
                        )
                    canvas.drawRoundRect(
                        sampleRect,
                        cell *
                            .14f,
                        cell *
                            .14f,
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
