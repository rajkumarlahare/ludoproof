package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.min

/**
 * Keeps the existing legal-move affordance visible when the legacy 2D pawn-art
 * overlay is hidden behind the production 3D runtime.
 *
 * This view is presentation-only and reads the same authoritative snapshot used
 * by LudoBoardView. It never performs hit-testing or move validation.
 */
internal class LudoPaws3DLegalHaloView(
    context: Context,
) : View(context) {
    private var snapshot: MatchSnapshot? = null
    private var localPlayerId: String? = null
    private var perspectiveColor: String? = null

    private val haloPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(4.5f)
            color = Color.argb(235, 255, 193, 7)
        }
    private val corePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(1.6f)
            color = Color.WHITE
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
        invalidate()
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

        pending.legalTokenIndexes.forEach {
                tokenIndex ->
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
            val offset =
                LudoPawsPawnLayout
                    .tokenOffsetFraction(
                        slot = tokenIndex + player.seat,
                        position = position,
                    )
            val x =
                center.first + offset.first * cell
            val y =
                center.second + offset.second * cell
            val radius =
                cell *
                    LudoPawsPawnLayout
                        .radiusScale(
                            position = position,
                            occupancy = 1,
                        )
            canvas.drawCircle(
                x,
                y,
                radius * 1.14f,
                haloPaint,
            )
            canvas.drawCircle(
                x,
                y,
                radius * 1.03f,
                corePaint,
            )
        }

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun density(
        value: Float,
    ): Float =
        value * resources.displayMetrics.density
}
