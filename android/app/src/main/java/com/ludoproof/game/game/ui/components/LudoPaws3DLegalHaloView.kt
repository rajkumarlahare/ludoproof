package com.ludoproof.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min

/**
 * Presentation-only legal-move affordance for the 3D pawn runtime.
 *
 * Only the pawn(s) that can actually move pulse after a resolved roll. The
 * destination is intentionally never previewed here.
 */
internal class LudoPaws3DLegalHaloView(
    context: Context,
) : View(context) {
    private var snapshot: MatchSnapshot? = null
    private var localPlayerId: String? = null
    private var perspectiveColor: String? = null
    private var pulse = 0f
    private var pulseAnimator: ValueAnimator? = null

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val haloPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(3.8f)
        }
    private val corePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(1.35f)
        }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String?,
    ) {
        snapshot = state
        localPlayerId = playerId
        this.perspectiveColor =
            perspectiveColor
                ?.takeIf { it in OfflinePlayerLayout.COLORS }

        val shouldPulse =
            state?.pendingRoll?.let { pending ->
                pending.status == "RESOLVED" &&
                    pending.legalTokenIndexes.isNotEmpty() &&
                    playerId != null
            } == true
        if (shouldPulse) {
            startPulseIfNeeded()
        } else {
            stopPulse()
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopPulse()
        super.onDetachedFromWindow()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)
        val state = snapshot ?: return
        val pending = state.pendingRoll ?: return
        if (
            pending.status != "RESOLVED" ||
            pending.legalTokenIndexes.isEmpty()
        ) {
            return
        }
        val localId = localPlayerId ?: return
        val player =
            state.players.firstOrNull {
                it.playerId == localId
            } ?: return
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cell =
            size /
                LudoPawsFxBoardGeometry.BOARD_SIZE
        val stackPlacements =
            LudoPawsPawnLayout.stackPlacements(
                snapshot = state,
                cell = cell,
            )
        val turns =
            perspectiveColor
                ?.let(OfflinePlayerLayout::rotationQuarterTurns)
                ?: 0

        if (turns != 0) {
            canvas.save()
            canvas.rotate(
                turns * 90f,
                size / 2f,
                size / 2f,
            )
        }

        val breath = 0.5f - kotlin.math.abs(pulse - 0.5f)
        val radiusBoost = 1f + breath * 0.14f
        val glowAlpha = (22 + breath * 76f).toInt().coerceIn(0, 255)
        val haloAlpha = (135 + breath * 100f).toInt().coerceIn(0, 255)
        val coreAlpha = (95 + breath * 120f).toInt().coerceIn(0, 255)

        pending.legalTokenIndexes.forEach { tokenIndex ->
            val position =
                player.tokens.getOrNull(tokenIndex)
                    ?: return@forEach
            val center =
                LudoPawsFxBoardGeometry
                    .tokenCenter(
                        color = player.color,
                        tokenIndex = tokenIndex,
                        position = position,
                        cell = cell,
                    )
                    ?: return@forEach
            val placement =
                stackPlacements[
                    LudoPawsPawnVisualKey(
                        playerId = player.playerId,
                        tokenIndex = tokenIndex,
                    )
                ]
            val x =
                center.first +
                    (placement?.offsetXFraction ?: 0f) * cell
            val y =
                center.second +
                    (placement?.offsetYFraction ?: 0f) * cell
            val radius =
                cell *
                    LudoPawsPawnLayout
                        .radiusScale(
                            position = position,
                            occupancy = placement?.occupancy ?: 1,
                        )

            glowPaint.color = Color.argb(glowAlpha, 255, 193, 7)
            canvas.drawCircle(
                x,
                y,
                radius * 1.34f * radiusBoost,
                glowPaint,
            )
            haloPaint.color = Color.argb(haloAlpha, 255, 193, 7)
            canvas.drawCircle(
                x,
                y,
                radius * 1.18f * radiusBoost,
                haloPaint,
            )
            corePaint.color = Color.argb(coreAlpha, 255, 255, 255)
            canvas.drawCircle(
                x,
                y,
                radius * 1.05f * radiusBoost,
                corePaint,
            )
        }

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun startPulseIfNeeded() {
        if (pulseAnimator != null) return
        pulseAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = PULSE_DURATION_MS
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    pulse = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulse = 0f
        invalidate()
    }

    private fun density(
        value: Float,
    ): Float =
        value * resources.displayMetrics.density

    private companion object {
        const val PULSE_DURATION_MS = 760L
    }
}
