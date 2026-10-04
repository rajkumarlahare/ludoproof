package com.ludoproof.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Presentation-only capture-return layer.
 *
 * The authoritative snapshot already contains the victim in its yard. This
 * overlay temporarily masks that destination pawn and renders the same animal
 * at its previous track position, then performs impact -> pop -> return ->
 * settle. It never changes dice, token positions, captures, proof material,
 * persistence, or network state.
 */
internal class LudoPawsCaptureReturnOverlayView(
    context: Context,
) : View(context) {
    private data class CapturePawn(
        val motion: LudoPawsPawnMotion,
        val seat: Int,
        val color: String,
        val drawable: android.graphics.drawable.Drawable,
    )

    private var captures: List<CapturePawn> = emptyList()
    private var perspectiveColor: String? = null
    private var progress = 1f
    private var animator: ValueAnimator? = null

    private val settingsStore = GameSettingsStore(context)
    private val destinationMaskPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
    private val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(78, 0, 0, 0)
        }
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val innerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
    private val impactPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
    private val clipPath = Path()
    private val drawableCache =
        mutableMapOf<String, android.graphics.drawable.Drawable?>()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
        perspectiveColor: String?,
        characterIdsBySeat: List<String>,
        reducedMotion: Boolean,
    ) {
        animator?.cancel()
        animator = null
        progress = 1f
        captures = emptyList()
        this.perspectiveColor =
            perspectiveColor?.takeIf {
                it in OfflinePlayerLayout.COLORS
            }

        if (
            reducedMotion ||
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            invalidate()
            return
        }

        val currentById =
            current.players.associateBy(PlayerSnapshot::playerId)
        captures =
            LudoPawsPawnAnimationPolicy
                .plans(
                    previous = previous,
                    current = current,
                )
                .asSequence()
                .filter {
                    it.kind == LudoPawsPawnMotionKind.CAPTURE_RETURN
                }
                .mapNotNull {
                        motion ->
                    val player =
                        currentById[motion.playerId]
                            ?: return@mapNotNull null
                    val characterId =
                        LudoPawsPawnLayout
                            .characterIdForSeat(
                                characterIdsBySeat = characterIdsBySeat,
                                seat = player.seat,
                            )
                            ?: return@mapNotNull null
                    val drawable =
                        drawableFor(characterId)
                            ?: return@mapNotNull null
                    CapturePawn(
                        motion = motion,
                        seat = player.seat,
                        color = player.color,
                        drawable = drawable,
                    )
                }
                .take(MAX_CAPTURE_RETURNS)
                .toList()

        if (captures.isEmpty()) {
            invalidate()
            return
        }

        val speed =
            settingsStore
                .snapshot()
                .gameSpeed
        val duration =
            (
                captures.maxOf {
                    it.motion.visualSteps
                }.toLong() *
                    speed.moveStepMs
                )
                .coerceIn(
                    MIN_CAPTURE_DURATION_MS,
                    MAX_CAPTURE_DURATION_MS,
                )

        progress = 0f
        animator =
            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
                    this.duration = duration
                    addUpdateListener {
                        progress =
                            it.animatedValue as Float
                        invalidate()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(
                                animation: Animator,
                            ) {
                                finishAnimation()
                            }

                            override fun onAnimationCancel(
                                animation: Animator,
                            ) {
                                finishAnimation()
                            }
                        },
                    )
                    start()
                }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        captures = emptyList()
        progress = 1f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)
        if (captures.isEmpty()) {
            return
        }
        val size =
            min(width, height)
                .toFloat()
        if (size <= 0f) {
            return
        }
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

        captures.forEach {
            drawCapturePawn(
                canvas = canvas,
                capture = it,
                cell = cell,
            )
        }

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun drawCapturePawn(
        canvas: Canvas,
        capture: CapturePawn,
        cell: Float,
    ) {
        val motion = capture.motion
        val rawFrom =
            LudoPawsFxBoardGeometry
                .tokenCenter(
                    color = capture.color,
                    tokenIndex = motion.tokenIndex,
                    position = motion.fromPosition,
                    cell = cell,
                )
                ?: return
        val fromOffset =
            LudoPawsPawnLayout
                .tokenOffsetFraction(
                    slot = motion.tokenIndex + capture.seat,
                    position = motion.fromPosition,
                )
        val from =
            (
                rawFrom.first +
                    fromOffset.first * cell
                ) to
                (
                    rawFrom.second +
                        fromOffset.second * cell
                    )
        val to =
            LudoPawsFxBoardGeometry
                .tokenCenter(
                    color = capture.color,
                    tokenIndex = motion.tokenIndex,
                    position = -1,
                    cell = cell,
                )
                ?: return
        val frame =
            LudoPawsPawnAnimationPolicy
                .captureReturnFrame(progress)

        if (progress < DESTINATION_REVEAL_PROGRESS) {
            val destinationRadius =
                cell *
                    LudoPawsPawnLayout
                        .radiusScale(
                            position = -1,
                            occupancy = 1,
                        )
            canvas.drawCircle(
                to.first,
                to.second,
                destinationRadius * 1.22f,
                destinationMaskPaint,
            )
        }

        val x =
            lerp(
                from.first,
                to.first,
                frame.routeProgress,
            ) +
                frame.shakeXCells *
                cell
        val y =
            lerp(
                from.second,
                to.second,
                frame.routeProgress,
            ) -
                frame.liftCells *
                cell

        val fromRadius =
            cell *
                LudoPawsPawnLayout
                    .radiusScale(
                        position = motion.fromPosition,
                        occupancy = 1,
                    )
        val toRadius =
            cell *
                LudoPawsPawnLayout
                    .radiusScale(
                        position = -1,
                        occupancy = 1,
                    )
        val radius =
            lerp(
                fromRadius,
                toRadius,
                frame.routeProgress,
            ) *
                frame.scale

        if (
            frame.phase == LudoPawsCaptureReturnPhase.IMPACT_SHAKE ||
            frame.phase == LudoPawsCaptureReturnPhase.POP
        ) {
            val phaseProgress =
                (progress / .34f)
                    .coerceIn(0f, 1f)
            impactPaint.color =
                Color.argb(
                    ((1f - phaseProgress) * 205f)
                        .roundToInt(),
                    255,
                    255,
                    255,
                )
            impactPaint.strokeWidth =
                cell * .07f
            canvas.drawCircle(
                from.first,
                from.second,
                cell * (.38f + phaseProgress * .30f),
                impactPaint,
            )
        }

        drawAnimalPawn(
            canvas = canvas,
            drawable = capture.drawable,
            x = x,
            y = y,
            radius = radius,
            playerColor = playerColor(capture.color),
        )
    }

    private fun drawAnimalPawn(
        canvas: Canvas,
        drawable: android.graphics.drawable.Drawable,
        x: Float,
        y: Float,
        radius: Float,
        playerColor: Int,
    ) {
        canvas.drawOval(
            x - radius * .80f,
            y + radius * .67f,
            x + radius * .80f,
            y + radius * 1.02f,
            shadowPaint,
        )

        ringPaint.color = playerColor
        canvas.drawCircle(
            x,
            y,
            radius,
            ringPaint,
        )
        canvas.drawCircle(
            x,
            y,
            radius * .84f,
            innerPaint,
        )

        val artRadius =
            radius * .80f
        val bounds =
            Rect(
                (x - artRadius).roundToInt(),
                (y - artRadius).roundToInt(),
                (x + artRadius).roundToInt(),
                (y + artRadius).roundToInt(),
            )
        clipPath.reset()
        clipPath.addCircle(
            x,
            y,
            artRadius,
            Path.Direction.CW,
        )
        canvas.save()
        canvas.clipPath(clipPath)
        drawable.bounds = bounds
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun drawableFor(
        characterId: String,
    ): android.graphics.drawable.Drawable? {
        if (drawableCache.containsKey(characterId)) {
            return drawableCache[characterId]
        }
        val character =
            LudoPawsCharacterCatalog
                .character(characterId)
                ?: return null
        val drawableId =
            resources.getIdentifier(
                character.fallbackDrawableName,
                "drawable",
                context.packageName,
            )
        val drawable =
            if (drawableId == 0) {
                null
            } else {
                context.getDrawable(drawableId)
                    ?.mutate()
            }
        drawableCache[characterId] = drawable
        return drawable
    }

    private fun playerColor(
        color: String,
    ): Int =
        when (color) {
            "RED" -> Color.rgb(241, 37, 47)
            "GREEN" -> Color.rgb(0, 169, 80)
            "YELLOW" -> Color.rgb(255, 216, 27)
            "BLUE" -> Color.rgb(48, 151, 215)
            else -> Color.rgb(108, 117, 125)
        }

    private fun finishAnimation() {
        animator = null
        captures = emptyList()
        progress = 1f
        invalidate()
    }

    private fun lerp(
        start: Float,
        end: Float,
        fraction: Float,
    ): Float =
        start +
            (end - start) *
            fraction.coerceIn(0f, 1f)

    private companion object {
        const val MAX_CAPTURE_RETURNS = 4
        const val MIN_CAPTURE_DURATION_MS = 520L
        const val MAX_CAPTURE_DURATION_MS = 1_200L
        const val DESTINATION_REVEAL_PROGRESS = .96f
    }
}
