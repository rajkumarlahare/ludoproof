package com.ludoproof.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.ludoproof.game.feature.characters.data.audio.LudoPawsVoicePlayer
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Phase 6 shell: proven Phase 5 board + non-authoritative reaction effects.
 * Gameplay state remains owned by LudoPawsBoardView / OfflineGameEngine.
 */
class LudoPawsReactiveBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val board =
        LudoPawsBoardView(context)
    private val reactionOverlay =
        LudoPawsReactionOverlayView(context)
    private val voicePlayer =
        LudoPawsVoicePlayer(context)
    private var previousSnapshot: MatchSnapshot? =
        null

    var onTokenSelected: ((Int) -> Unit)?
        get() =
            board.onTokenSelected
        set(value) {
            board.onTokenSelected =
                value
        }

    init {
        isFocusable =
            false
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_NO

        addView(
            board,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            reactionOverlay,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String? = null,
        characterIdsBySeat: List<String> = emptyList(),
    ) {
        val reactions =
            LudoPawsReactionEngine
                .detect(
                    previous = previousSnapshot,
                    current = state,
                )

        board.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        reactionOverlay.bind(
            state = state,
            perspectiveColor = perspectiveColor,
        )
        previousSnapshot =
            state

        if (reactions.isNotEmpty()) {
            reactionOverlay.play(
                reactions,
            )
            voicePlayer.playHighestPriority(
                reactions = reactions,
                characterIdsBySeat = characterIdsBySeat,
            )
        }
    }

    fun reloadStyle() {
        board.reloadStyle()
    }

    override fun onDetachedFromWindow() {
        reactionOverlay.stop()
        voicePlayer.shutdown()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired =
            density(380f)
                .roundToInt()
        val resolvedWidth =
            resolveSize(
                desired,
                widthMeasureSpec,
            )
        val resolvedHeight =
            resolveSize(
                resolvedWidth,
                heightMeasureSpec,
            )
        val size =
            min(
                resolvedWidth,
                resolvedHeight,
            )
        val exact =
            MeasureSpec.makeMeasureSpec(
                size,
                MeasureSpec.EXACTLY,
            )
        super.onMeasure(
            exact,
            exact,
        )
    }

    private fun density(
        value: Float,
    ): Float =
        value *
            resources.displayMetrics.density
}

private class LudoPawsReactionOverlayView(
    context: Context,
) : View(context) {
    private var snapshot: MatchSnapshot? =
        null
    private var perspectiveColor: String? =
        null
    private var reactions: List<LudoPawsReaction> =
        emptyList()
    private var progress =
        0f
    private var animator: ValueAnimator? =
        null

    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                density(3.4f)
        }
    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.FILL
        }
    private val sparkPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                density(2.2f)
            strokeCap =
                Paint.Cap.ROUND
        }

    init {
        isClickable =
            false
        isFocusable =
            false
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(
        state: MatchSnapshot?,
        perspectiveColor: String?,
    ) {
        snapshot =
            state
        this.perspectiveColor =
            perspectiveColor
                ?.takeIf {
                    it in
                        OfflinePlayerLayout.COLORS
                }
        invalidate()
    }

    fun play(
        reactions: List<LudoPawsReaction>,
    ) {
        animator
            ?.cancel()
        this.reactions =
            reactions
                .take(MAX_SIMULTANEOUS_REACTIONS)
        progress =
            0f

        animator =
            ValueAnimator
                .ofFloat(
                    0f,
                    1f,
                )
                .apply {
                    duration =
                        900L
                    addUpdateListener {
                            valueAnimator ->
                        progress =
                            valueAnimator
                                .animatedValue as Float
                        invalidate()
                    }
                    addListener(
                        object :
                            AnimatorListenerAdapter() {
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
        animator
            ?.cancel()
        animator =
            null
        reactions =
            emptyList()
        progress =
            0f
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
        val state =
            snapshot
                ?: return
        if (reactions.isEmpty()) {
            return
        }

        val size =
            min(
                width,
                height,
            ).toFloat()
        if (size <= 0f) {
            return
        }
        val cell =
            size /
                15f
        val turns =
            perspectiveColor
                ?.let(
                    OfflinePlayerLayout::rotationQuarterTurns,
                )
                ?: 0

        if (turns != 0) {
            canvas.save()
            canvas.rotate(
                turns * 90f,
                size / 2f,
                size / 2f,
            )
        }

        reactions.forEach {
                reaction ->
            drawReaction(
                canvas = canvas,
                state = state,
                cell = cell,
                reaction = reaction,
            )
        }

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun drawReaction(
        canvas: Canvas,
        state: MatchSnapshot,
        cell: Float,
        reaction: LudoPawsReaction,
    ) {
        val player =
            state.players
                .firstOrNull {
                    it.playerId ==
                        reaction.playerId
                }
                ?: return
        val tokenIndex =
            reaction.tokenIndex
                ?: representativeTokenIndex(
                    player,
                )
        val position =
            player.tokens
                .getOrNull(
                    tokenIndex,
                )
                ?: return
        val center =
            tokenCenter(
                player = player,
                tokenIndex = tokenIndex,
                position = position,
                cell = cell,
            )
                ?: return

        val color =
            reactionColor(
                reaction.animationCue,
            )
        val fade =
            (
                1f -
                    progress
                )
                .coerceIn(
                    0f,
                    1f,
                )
        val pulse =
            cell *
                (
                    0.28f +
                        0.52f *
                        progress
                    )
        val alpha =
            (225f * fade)
                .roundToInt()
                .coerceIn(
                    0,
                    255,
                )

        glowPaint.color =
            withAlpha(
                color,
                (70f * fade)
                    .roundToInt(),
            )
        canvas.drawCircle(
            center.first,
            center.second,
            pulse * 0.82f,
            glowPaint,
        )

        ringPaint.color =
            withAlpha(
                color,
                alpha,
            )
        canvas.drawCircle(
            center.first,
            center.second,
            pulse,
            ringPaint,
        )

        sparkPaint.color =
            withAlpha(
                color,
                alpha,
            )
        val sparkBase =
            pulse +
                cell * 0.08f
        val sparkLength =
            cell *
                (
                    0.10f +
                        0.10f *
                        fade
                    )
        repeat(6) {
                index ->
            val angle =
                Math.toRadians(
                    (index * 60.0) +
                        progress * 24.0,
                )
            val dx =
                cos(angle)
                    .toFloat()
            val dy =
                sin(angle)
                    .toFloat()
            canvas.drawLine(
                center.first +
                    dx * sparkBase,
                center.second +
                    dy * sparkBase,
                center.first +
                    dx *
                    (sparkBase + sparkLength),
                center.second +
                    dy *
                    (sparkBase + sparkLength),
                sparkPaint,
            )
        }
    }

    private fun representativeTokenIndex(
        player: PlayerSnapshot,
    ): Int =
        player.tokens
            .indexOfFirst {
                it >= 0
            }
            .takeIf {
                it >= 0
            }
            ?: 0

    private fun tokenCenter(
        player: PlayerSnapshot,
        tokenIndex: Int,
        position: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        if (position == -1) {
            return yardTokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                cell = cell,
            )
        }

        if (position in 0..51) {
            val offset =
                START_OFFSETS[player.color]
                    ?: return null
            val global =
                (offset + position) %
                    TRACK.size
            val coord =
                TRACK[global]
            return centerForCell(
                row = coord.first,
                col = coord.second,
                cell = cell,
            )
        }

        if (position in 52..56) {
            val lane =
                HOME_LANES[player.color]
                    ?: return null
            val coord =
                lane[position - 52]
            return centerForCell(
                row = coord.first,
                col = coord.second,
                cell = cell,
            )
        }

        if (position == 57) {
            val unit =
                when (player.color) {
                    "RED" -> 6.9f to 7.5f
                    "GREEN" -> 7.5f to 6.9f
                    "YELLOW" -> 8.1f to 7.5f
                    "BLUE" -> 7.5f to 8.1f
                    else -> 7.5f to 7.5f
                }
            return unit.first * cell to
                unit.second * cell
        }

        return null
    }

    private fun yardTokenCenter(
        color: String,
        tokenIndex: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        val origin =
            when (color) {
                "RED" -> 0f to 0f
                "GREEN" -> 0f to 9f
                "YELLOW" -> 9f to 9f
                "BLUE" -> 9f to 0f
                else -> return null
            }
        val slot =
            when (tokenIndex) {
                0 -> 2f to 2f
                1 -> 2f to 4f
                2 -> 4f to 2f
                else -> 4f to 4f
            }
        return (
            origin.second +
                slot.second
            ) * cell to
            (
                origin.first +
                    slot.first
                ) * cell
    }

    private fun centerForCell(
        row: Int,
        col: Int,
        cell: Float,
    ): Pair<Float, Float> =
        (col + 0.5f) * cell to
            (row + 0.5f) * cell

    private fun reactionColor(
        cue: AnimationCue,
    ): Int =
        when (cue) {
            AnimationCue.CAPTURED,
            AnimationCue.SAD,
            AnimationCue.DEFEAT,
            -> Color.rgb(232, 72, 85)

            AnimationCue.ANGRY ->
                Color.rgb(255, 112, 67)

            AnimationCue.SAFE ->
                Color.rgb(72, 202, 228)

            AnimationCue.HOME,
            AnimationCue.VICTORY,
            -> Color.rgb(255, 193, 7)

            AnimationCue.NERVOUS ->
                Color.rgb(171, 71, 188)

            AnimationCue.IDLE,
            AnimationCue.EXCITED,
            AnimationCue.HAPPY,
            AnimationCue.CAPTURE,
            -> Color.rgb(255, 235, 59)
        }

    private fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int =
        Color.argb(
            alpha.coerceIn(0,255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun finishAnimation() {
        animator =
            null
        reactions =
            emptyList()
        progress =
            0f
        invalidate()
    }

    private fun density(
        value: Float,
    ): Float =
        value *
            resources.displayMetrics.density

    private companion object {
        const val MAX_SIMULTANEOUS_REACTIONS =
            6

        val START_OFFSETS =
            mapOf(
                "RED" to 0,
                "GREEN" to 13,
                "YELLOW" to 26,
                "BLUE" to 39,
            )

        val TRACK =
            listOf(
                6 to 1,
                6 to 2,
                6 to 3,
                6 to 4,
                6 to 5,
                5 to 6,
                4 to 6,
                3 to 6,
                2 to 6,
                1 to 6,
                0 to 6,
                0 to 7,
                0 to 8,
                1 to 8,
                2 to 8,
                3 to 8,
                4 to 8,
                5 to 8,
                6 to 9,
                6 to 10,
                6 to 11,
                6 to 12,
                6 to 13,
                6 to 14,
                7 to 14,
                8 to 14,
                8 to 13,
                8 to 12,
                8 to 11,
                8 to 10,
                8 to 9,
                9 to 8,
                10 to 8,
                11 to 8,
                12 to 8,
                13 to 8,
                14 to 8,
                14 to 7,
                14 to 6,
                13 to 6,
                12 to 6,
                11 to 6,
                10 to 6,
                9 to 6,
                8 to 5,
                8 to 4,
                8 to 3,
                8 to 2,
                8 to 1,
                8 to 0,
                7 to 0,
                6 to 0,
            )

        val HOME_LANES =
            mapOf(
                "RED" to
                    listOf(
                        7 to 1,
                        7 to 2,
                        7 to 3,
                        7 to 4,
                        7 to 5,
                    ),
                "GREEN" to
                    listOf(
                        1 to 7,
                        2 to 7,
                        3 to 7,
                        4 to 7,
                        5 to 7,
                    ),
                "YELLOW" to
                    listOf(
                        7 to 13,
                        7 to 12,
                        7 to 11,
                        7 to 10,
                        7 to 9,
                    ),
                "BLUE" to
                    listOf(
                        13 to 7,
                        12 to 7,
                        11 to 7,
                        10 to 7,
                        9 to 7,
                    ),
            )
    }
}
