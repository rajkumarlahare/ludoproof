package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class LudoPawsFxPainter(
    context: Context,
) {
    private val density = context.resources.displayMetrics.density
    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f * density
        }
    private val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 2.3f * density
        }
    private val particlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val ghostPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    fun drawCaptureReturn(
        canvas: Canvas,
        from: Pair<Float, Float>,
        to: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val eased = easeOutCubic(progress)
        val x = lerp(from.first, to.first, eased)
        val y = lerp(from.second, to.second, eased)
        val fade = (1f - progress).coerceIn(0f, 1f)
        linePaint.color = withAlpha(color, (120f * fade).roundToInt())
        linePaint.strokeWidth = cell * .12f
        canvas.drawLine(from.first, from.second, x, y, linePaint)

        ghostPaint.color = withAlpha(color, (220f * fade).roundToInt())
        canvas.drawCircle(
            x,
            y,
            cell * (.25f + .05f * sin(progress * PI).toFloat()),
            ghostPaint,
        )
        drawPawMark(
            canvas = canvas,
            x = x,
            y = y,
            radius = cell * .14f,
            color = Color.WHITE,
            alpha = (210f * fade).roundToInt(),
        )
        if (progress > .72f) {
            drawDestinationFlash(canvas, to, color, cell)
        }
    }

    fun drawPawTrail(
        canvas: Canvas,
        from: Pair<Float, Float>,
        to: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
        emphasis: Float,
    ) {
        val fade = (1f - progress).coerceIn(0f, 1f)
        if (fade <= 0f) {
            return
        }
        repeat(PAW_TRAIL_COUNT) { index ->
            val fraction =
                ((index + 1f) / (PAW_TRAIL_COUNT + 1f)) *
                    progress.coerceAtLeast(.18f)
            drawPawMark(
                canvas = canvas,
                x = lerp(from.first, to.first, fraction),
                y = lerp(from.second, to.second, fraction),
                radius = cell * .085f * emphasis,
                color = color,
                alpha = (180f * fade * emphasis).roundToInt(),
            )
        }
    }

    fun drawReaction(
        canvas: Canvas,
        center: Pair<Float, Float>,
        cue: AnimationCue,
        cell: Float,
        progress: Float,
        particleCount: Int,
        allowConfetti: Boolean,
    ) {
        val color = reactionColor(cue)
        val fade = (1f - progress).coerceIn(0f, 1f)
        val pulse =
            cell * (.30f + .58f * easeOutCubic(progress))

        glowPaint.color = withAlpha(color, (75f * fade).roundToInt())
        canvas.drawCircle(center.first, center.second, pulse * .86f, glowPaint)
        ringPaint.color = withAlpha(color, (225f * fade).roundToInt())
        canvas.drawCircle(center.first, center.second, pulse, ringPaint)

        when (cue) {
            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            AnimationCue.CAPTURE,
            -> drawBurst(canvas, center, color, cell, particleCount, progress)

            AnimationCue.CAPTURED ->
                drawImpact(canvas, center, color, cell, progress, particleCount)

            AnimationCue.SAFE ->
                drawSafeShield(canvas, center, color, cell, progress)

            AnimationCue.HOME -> {
                drawBurst(canvas, center, color, cell, particleCount, progress)
                drawHomeStars(canvas, cell, progress)
            }

            AnimationCue.ANGRY ->
                drawAngryBolts(canvas, center, cell, progress)

            AnimationCue.NERVOUS ->
                drawNervousOrbit(canvas, center, color, cell, progress)

            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            -> drawSadDrops(canvas, center, cell, progress, particleCount)

            AnimationCue.VICTORY -> {
                drawBurst(canvas, center, color, cell, particleCount / 2, progress)
                if (allowConfetti) {
                    drawConfetti(canvas, cell, progress, particleCount)
                }
            }

            AnimationCue.IDLE ->
                drawIdleBreath(canvas, center, color, cell, progress)
        }
    }

    fun drawHomeStars(
        canvas: Canvas,
        cell: Float,
        progress: Float,
    ) {
        val count = 10
        repeat(count) { index ->
            val angle = index * 2.399963 + progress
            val radius = cell * (1f + (index % 3) * .7f)
            val cx = cell * 7.5f + cos(angle).toFloat() * radius
            val cy =
                cell * 7.5f + sin(angle).toFloat() * radius -
                    cell * progress * .7f
            drawStar(
                canvas = canvas,
                cx = cx,
                cy = cy,
                radius = cell * .10f,
                color = Color.rgb(255, 214, 64),
                alpha = ((1f - progress) * 230f).roundToInt(),
            )
        }
    }

    fun drawDestinationFlash(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
    ) {
        glowPaint.color = withAlpha(color, 90)
        canvas.drawCircle(center.first, center.second, cell * .40f, glowPaint)
        ringPaint.color = withAlpha(color, 220)
        ringPaint.strokeWidth = cell * .07f
        canvas.drawCircle(center.first, center.second, cell * .34f, ringPaint)
    }

    private fun drawBurst(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        particleCount: Int,
        progress: Float,
    ) {
        val count = particleCount.coerceIn(2, 18)
        val fade = (1f - progress).coerceIn(0f, 1f)
        particlePaint.color = withAlpha(color, (230f * fade).roundToInt())
        repeat(count) { index ->
            val angle = 2.0 * PI * index / count + progress * .55
            val radius = cell * (.36f + progress * .72f)
            canvas.drawCircle(
                center.first + cos(angle).toFloat() * radius,
                center.second + sin(angle).toFloat() * radius,
                cell * .055f,
                particlePaint,
            )
        }
    }

    private fun drawImpact(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
        particleCount: Int,
    ) {
        val count = particleCount.coerceIn(4, 12)
        linePaint.strokeWidth = cell * .08f
        linePaint.color = withAlpha(color, ((1f - progress) * 230f).roundToInt())
        repeat(count) { index ->
            val angle = 2.0 * PI * index / count
            val inner = cell * (.24f + .16f * progress)
            val outer = cell * (.52f + .28f * progress)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            canvas.drawLine(
                center.first + dx * inner,
                center.second + dy * inner,
                center.first + dx * outer,
                center.second + dy * outer,
                linePaint,
            )
        }
    }

    private fun drawSafeShield(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val fade = (1f - progress).coerceIn(0f, 1f)
        repeat(3) { index ->
            ringPaint.color = withAlpha(color, (200f * fade / (index + 1)).roundToInt())
            ringPaint.strokeWidth = cell * .07f
            canvas.drawCircle(
                center.first,
                center.second,
                cell * (.34f + index * .14f + progress * .15f),
                ringPaint,
            )
        }
    }

    private fun drawAngryBolts(
        canvas: Canvas,
        center: Pair<Float, Float>,
        cell: Float,
        progress: Float,
    ) {
        linePaint.color =
            withAlpha(
                Color.rgb(255, 92, 62),
                ((1f - progress) * 245f).roundToInt(),
            )
        linePaint.strokeWidth = cell * .08f
        repeat(4) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val yOffset = (index / 2) * cell * .20f
            val path = Path().apply {
                moveTo(center.first + side * cell * .38f, center.second - cell * .42f + yOffset)
                lineTo(center.first + side * cell * .58f, center.second - cell * .14f + yOffset)
                lineTo(center.first + side * cell * .43f, center.second + cell * .02f + yOffset)
                lineTo(center.first + side * cell * .68f, center.second + cell * .30f + yOffset)
            }
            canvas.drawPath(path, linePaint)
        }
    }

    private fun drawNervousOrbit(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val count = 5
        repeat(count) { index ->
            val angle = 2.0 * PI * index / count + progress * 8f
            particlePaint.color = withAlpha(color, ((1f - progress) * 220f).roundToInt())
            canvas.drawCircle(
                center.first + cos(angle).toFloat() * cell * .48f,
                center.second + sin(angle).toFloat() * cell * .48f,
                cell * .045f,
                particlePaint,
            )
        }
    }

    private fun drawSadDrops(
        canvas: Canvas,
        center: Pair<Float, Float>,
        cell: Float,
        progress: Float,
        particleCount: Int,
    ) {
        val count = particleCount.coerceIn(2, 8)
        particlePaint.color =
            withAlpha(
                Color.rgb(70, 150, 255),
                ((1f - progress) * 210f).roundToInt(),
            )
        repeat(count) { index ->
            val offset = (index - count / 2f) * cell * .12f
            val y =
                center.second +
                    cell * (.30f + progress * .55f)
            canvas.drawOval(
                center.first + offset - cell * .035f,
                y - cell * .07f,
                center.first + offset + cell * .035f,
                y + cell * .07f,
                particlePaint,
            )
        }
    }

    private fun drawConfetti(
        canvas: Canvas,
        cell: Float,
        progress: Float,
        particleCount: Int,
    ) {
        val boardPx = cell * LudoPawsFxBoardGeometry.BOARD_SIZE
        repeat(particleCount.coerceIn(8, 40)) { index ->
            val xSeed = ((index * 37) % 97) / 97f
            val ySeed = ((index * 53) % 89) / 89f
            val x = boardPx * xSeed + sin(progress * 8f + index) * cell * .25f
            val y = boardPx * ((ySeed * .35f + progress * 1.05f) % 1f)
            particlePaint.color =
                withAlpha(
                    CONFETTI_COLORS[index % CONFETTI_COLORS.size],
                    ((1f - progress * .55f) * 235f).roundToInt(),
                )
            canvas.save()
            canvas.rotate(progress * 360f + index * 19f, x, y)
            canvas.drawRect(
                x - cell * .055f,
                y - cell * .10f,
                x + cell * .055f,
                y + cell * .10f,
                particlePaint,
            )
            canvas.restore()
        }
    }

    private fun drawIdleBreath(
        canvas: Canvas,
        center: Pair<Float, Float>,
        color: Int,
        cell: Float,
        progress: Float,
    ) {
        val wave = sin(progress * PI).toFloat()
        ringPaint.color = withAlpha(color, ((1f - progress) * 120f).roundToInt())
        ringPaint.strokeWidth = cell * .045f
        canvas.drawCircle(
            center.first,
            center.second,
            cell * (.34f + wave * .06f),
            ringPaint,
        )
    }

    private fun drawPawMark(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
        alpha: Int,
    ) {
        particlePaint.color = withAlpha(color, alpha)
        canvas.drawOval(
            x - radius * .70f,
            y - radius * .25f,
            x + radius * .70f,
            y + radius * .80f,
            particlePaint,
        )
        repeat(3) { index ->
            canvas.drawCircle(
                x + (index - 1) * radius * .58f,
                y - radius * .62f,
                radius * .34f,
                particlePaint,
            )
        }
    }

    private fun drawStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
        alpha: Int,
    ) {
        val path = Path()
        repeat(10) { index ->
            val angle = -PI / 2 + index * PI / 5
            val r = if (index % 2 == 0) radius else radius * .45f
            val x = cx + cos(angle).toFloat() * r
            val y = cy + sin(angle).toFloat() * r
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        particlePaint.color = withAlpha(color, alpha)
        canvas.drawPath(path, particlePaint)
    }

    private fun reactionColor(cue: AnimationCue): Int =
        when (cue) {
            AnimationCue.CAPTURED,
            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            -> Color.rgb(232, 72, 85)

            AnimationCue.ANGRY -> Color.rgb(255, 112, 67)
            AnimationCue.SAFE -> Color.rgb(72, 202, 228)
            AnimationCue.HOME,
            AnimationCue.VICTORY,
            -> Color.rgb(255, 193, 7)

            AnimationCue.NERVOUS -> Color.rgb(171, 71, 188)
            AnimationCue.IDLE,
            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            AnimationCue.CAPTURE,
            -> Color.rgb(255, 235, 59)
        }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)

    private fun easeOutCubic(value: Float): Float {
        val t = 1f - value.coerceIn(0f, 1f)
        return 1f - t * t * t
    }

    private companion object {
        const val PAW_TRAIL_COUNT = 4
        val CONFETTI_COLORS =
            intArrayOf(
                Color.rgb(255, 193, 7),
                Color.rgb(244, 67, 54),
                Color.rgb(33, 150, 243),
                Color.rgb(76, 175, 80),
                Color.rgb(156, 39, 176),
            )
    }
}
