package com.ludoproof.game

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.characters.domain.model.AnimalCharacter
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Local-game board shell that keeps the proven LudoBoardView responsible for
 * board geometry and touch hit-testing while layering cosmetic Ludo Paws
 * characters on top of its authoritative token positions.
 *
 * BOARD GEOMETRY LOCK: LudoBoardView is the approved/final board. This wrapper and its animal
 * overlay must never independently move, resize, or reinterpret the board, road, yards, center,
 * or token coordinates. Geometry here must remain an exact mirror of LudoBoardView.
 *
 * The overlay consumes only MatchSnapshot + persisted cosmetic assignments.
 * It never changes dice, moves, captures, proof material, or engine state.
 */
class LudoPawsBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val baseBoard =
        LudoBoardView(context)
    private val pawnOverlay =
        AnimalPawnOverlayView(context)

    var onTokenSelected: ((Int) -> Unit)?
        get() =
            baseBoard.onTokenSelected
        set(value) {
            baseBoard.onTokenSelected =
                value
        }

    init {
        isFocusable =
            false
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_NO
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(
                    view: View,
                    outline: Outline,
                ) {
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        density(3f),
                    )
                }
            }
        clipToOutline =
            true

        addView(
            baseBoard,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            pawnOverlay,
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
        baseBoard.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
        )
        pawnOverlay.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
            characterIdsBySeat = characterIdsBySeat,
        )
    }

    fun reloadStyle() {
        baseBoard.reloadStyle()
    }

    // BOARD SIZE LOCK: this shell must exactly match the square measured by LudoBoardView.
    // Do not give the pawn overlay a different aspect ratio, inset, or independent board size.
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

/**
 * COSMETIC GEOMETRY MIRROR — LOCKED.
 *
 * This layer draws only animal pawns. Its coordinate math must stay pixel-identical to the fixed
 * LudoBoardView geometry. Do not adjust overlay positions to "look right" independently; any
 * explicit future board redesign must update both files together.
 */
private class AnimalPawnOverlayView(
    context: Context,
) : View(context) {
    private var snapshot: MatchSnapshot? =
        null
    private var localPlayerId: String? =
        null
    private var perspectiveColor: String? =
        null
    private var characterIdsBySeat: List<String> =
        emptyList()
    private var moveAnimator: ValueAnimator? =
        null
    private var moveAnimation: TokenMoveAnimation? =
        null

    private val drawableCache =
        mutableMapOf<String, android.graphics.drawable.Drawable?>()
    private val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.FILL
            color =
                Color.argb(
                    76,
                    0,
                    0,
                    0,
                )
        }
    private val legalHaloPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                density(4.5f)
            color =
                Color.argb(
                    235,
                    255,
                    193,
                    7,
                )
        }
    private val legalCorePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                density(1.6f)
            color =
                Color.WHITE
        }
    private val clipPath =
        Path()

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
        playerId: String?,
        perspectiveColor: String?,
        characterIdsBySeat: List<String>,
    ) {
        val previous =
            snapshot
        snapshot =
            state
        localPlayerId =
            playerId
        this.perspectiveColor =
            perspectiveColor
                ?.takeIf {
                    it in
                        OfflinePlayerLayout.COLORS
                }
        this.characterIdsBySeat =
            characterIdsBySeat
                .take(4)

        startMoveAnimationIfNeeded(
            previous = previous,
            current = state,
        )
        invalidate()
    }

    override fun onDetachedFromWindow() {
        moveAnimator
            ?.cancel()
        moveAnimator =
            null
        moveAnimation =
            null
        super.onDetachedFromWindow()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)
        val state =
            snapshot
                ?: return
        val size =
            min(
                width,
                height,
            ).toFloat()
        if (size <= 0f) {
            return
        }
        // BASE COORDINATE LOCK: mirror LudoBoardView's logical 15x15 coordinate base exactly.
        // Visual stretching is applied only through the matching axisBoundary() below.
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

        drawAnimalTokens(
            canvas = canvas,
            state = state,
            cell = cell,
        )

        if (turns != 0) {
            canvas.restore()
        }
    }

    private fun drawAnimalTokens(
        canvas: Canvas,
        state: MatchSnapshot,
        cell: Float,
    ) {
        val legal =
            state.pendingRoll
                ?.legalTokenIndexes
                .orEmpty()
        val localId =
            localPlayerId
        val occupancy =
            occupancyByCell(state)

        state.players.forEach {
                player ->
            val characterId =
                LudoPawsPawnLayout
                    .characterIdForSeat(
                        characterIdsBySeat =
                            characterIdsBySeat,
                        seat =
                            player.seat,
                    )
                    ?: return@forEach
            val character =
                LudoPawsCharacterCatalog
                    .character(
                        characterId,
                    )
                    ?: return@forEach
            val drawable =
                drawableFor(character)
                    ?: return@forEach

            player.tokens.forEachIndexed {
                    tokenIndex,
                    position,
                ->
                val base =
                    animatedTokenCenter(
                        player = player,
                        tokenIndex = tokenIndex,
                        currentPosition = position,
                        cell = cell,
                    )
                        ?: return@forEachIndexed
                val offsetFraction =
                    LudoPawsPawnLayout
                        .tokenOffsetFraction(
                            slot =
                                tokenIndex +
                                    player.seat,
                            position =
                                position,
                        )
                val x =
                    base.first +
                        offsetFraction.first *
                        cell
                val y =
                    base.second +
                        offsetFraction.second *
                        cell
                val occupants =
                    occupancy[
                        occupancyKey(
                            player = player,
                            tokenIndex = tokenIndex,
                            position = position,
                        ),
                    ]
                        ?: 1
                val radius =
                    cell *
                        LudoPawsPawnLayout
                            .radiusScale(
                                position = position,
                                occupancy = occupants,
                            )
                val isLegal =
                    player.playerId == localId &&
                        tokenIndex in legal &&
                        state.pendingRoll?.status ==
                        "RESOLVED"

                drawAnimalPawn(
                    canvas = canvas,
                    drawable = drawable,
                    x = x,
                    y = y,
                    radius = radius,
                    legal = isLegal,
                )
            }
        }
    }

    private fun occupancyByCell(
        state: MatchSnapshot,
    ): Map<String, Int> {
        val counts =
            mutableMapOf<String, Int>()
        state.players.forEach {
                player ->
            player.tokens.forEachIndexed {
                    tokenIndex,
                    position,
                ->
                val key =
                    occupancyKey(
                        player = player,
                        tokenIndex = tokenIndex,
                        position = position,
                    )
                counts[key] =
                    (counts[key] ?: 0) +
                        1
            }
        }
        return counts
    }

    private fun occupancyKey(
        player: PlayerSnapshot,
        tokenIndex: Int,
        position: Int,
    ): String {
        val renderPosition =
            LudoPathEncoding
                .normalizeLegacyEntry(position)
        return when {
            renderPosition == -1 ->
                "Y:${player.seat}:$tokenIndex"
            LudoPathEncoding.isTrackPosition(renderPosition) -> {
                val start =
                    START_OFFSETS[player.color]
                        ?: 0
                "T:${(start + renderPosition) % TRACK.size}"
            }
            renderPosition in
                LudoPathEncoding.FIRST_HOME_LANE_POSITION..
                    LudoPathEncoding.LAST_HOME_LANE_POSITION ->
                "H:${player.color}:$renderPosition"
            renderPosition == LudoPathEncoding.HOME_POSITION ->
                "C:${player.color}"
            else ->
                "X:${player.seat}:$tokenIndex:$position"
        }
    }

    private fun drawAnimalPawn(
        canvas: Canvas,
        drawable: android.graphics.drawable.Drawable,
        x: Float,
        y: Float,
        radius: Float,
        legal: Boolean,
    ) {
        shadowPaint.color =
            Color.argb(
                74,
                0,
                0,
                0,
            )
        canvas.drawOval(
            x - radius * 0.80f,
            y + radius * 0.67f,
            x + radius * 0.80f,
            y + radius * 1.02f,
            shadowPaint,
        )

        if (legal) {
            canvas.drawCircle(
                x,
                y,
                radius * 1.14f,
                legalHaloPaint,
            )
            canvas.drawCircle(
                x,
                y,
                radius * 1.03f,
                legalCorePaint,
            )
        }

        // Draw only the character art at rest. The old permanent colored/white
        // outer disks made pawns look bulky and dated; legal moves still get a halo.
        val artRadius =
            radius *
                0.96f
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
        drawable.bounds =
            bounds
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun drawableFor(
        character: AnimalCharacter,
    ): android.graphics.drawable.Drawable? {
        if (
            drawableCache.containsKey(
                character.id,
            )
        ) {
            return drawableCache[character.id]
        }

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
                context.getDrawable(
                    drawableId,
                )
            }
        drawableCache[character.id] =
            drawable
        return drawable
    }

    private fun startMoveAnimationIfNeeded(
        previous: MatchSnapshot?,
        current: MatchSnapshot?,
    ) {
        moveAnimator
            ?.cancel()
        moveAnimator =
            null
        moveAnimation =
            null

        if (
            previous == null ||
            current == null ||
            previous.matchId != current.matchId
        ) {
            return
        }

        val movement =
            current.players
                .asSequence()
                .mapNotNull {
                        currentPlayer ->
                    val previousPlayer =
                        previous.players
                            .firstOrNull {
                                it.playerId ==
                                    currentPlayer.playerId
                            }
                            ?: return@mapNotNull null

                    currentPlayer.tokens
                        .indices
                        .firstNotNullOfOrNull {
                                tokenIndex ->
                            val from =
                                previousPlayer.tokens
                                    .getOrNull(
                                        tokenIndex,
                                    )
                                    ?: return@firstNotNullOfOrNull null
                            val to =
                                currentPlayer.tokens
                                    .getOrNull(
                                        tokenIndex,
                                    )
                                    ?: return@firstNotNullOfOrNull null

                            if (to > from) {
                                TokenMoveAnimation(
                                    playerId =
                                        currentPlayer.playerId,
                                    tokenIndex =
                                        tokenIndex,
                                    fromPosition = from,
                                    toPosition = to,
                                    progress = 0f,
                                )
                            } else {
                                null
                            }
                        }
                }
                .firstOrNull()
                ?: return

        val steps =
            LudoPathEncoding
                .visualStepCount(
                    fromPosition = movement.fromPosition,
                    toPosition = movement.toPosition,
                )
                .coerceAtLeast(1)
        val settings =
            GameSettingsStore(context)
                .snapshot()
        val speed =
            settings.gameSpeed
        val duration =
            (
                steps.toLong() *
                    speed.moveStepMs
                )
                .coerceAtLeast(
                    speed.moveStepMs,
                )

        moveAnimation =
            movement.copy(
                hopEnabled =
                    !settings.reducedMotionEnabled,
            )
        moveAnimator =
            ValueAnimator
                .ofFloat(
                    0f,
                    steps.toFloat(),
                )
                .apply {
                    this.duration =
                        duration
                    addUpdateListener {
                            animator ->
                        moveAnimation =
                            moveAnimation
                                ?.copy(
                                    progress =
                                        animator
                                            .animatedValue as Float,
                                )
                        invalidate()
                    }
                    addListener(
                        object :
                            AnimatorListenerAdapter() {
                            override fun onAnimationEnd(
                                animation: Animator,
                            ) {
                                moveAnimation =
                                    null
                                moveAnimator =
                                    null
                                invalidate()
                            }

                            override fun onAnimationCancel(
                                animation: Animator,
                            ) {
                                moveAnimation =
                                    null
                                moveAnimator =
                                    null
                                invalidate()
                            }
                        },
                    )
                    start()
                }
    }

    private fun animatedTokenCenter(
        player: PlayerSnapshot,
        tokenIndex: Int,
        currentPosition: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        val animation =
            moveAnimation
                ?.takeIf {
                    it.playerId == player.playerId &&
                        it.tokenIndex == tokenIndex
                }
                ?: return tokenCenter(
                    player = player,
                    tokenIndex = tokenIndex,
                    position = currentPosition,
                    cell = cell,
                )

        val totalSteps =
            LudoPathEncoding
                .visualStepCount(
                    fromPosition = animation.fromPosition,
                    toPosition = animation.toPosition,
                )
                .coerceAtLeast(1)
        val progress =
            animation.progress
                .coerceIn(
                    0f,
                    totalSteps.toFloat(),
                )
        val whole =
            floor(progress)
                .toInt()
                .coerceAtMost(
                    totalSteps - 1,
                )
        val fraction =
            (
                progress -
                    whole
                )
                .coerceIn(
                    0f,
                    1f,
                )
        val fromPosition =
            LudoPathEncoding
                .positionAtVisualStep(
                    fromPosition = animation.fromPosition,
                    step = whole,
                )
                ?: return null
        val toPosition =
            LudoPathEncoding
                .positionAtVisualStep(
                    fromPosition = animation.fromPosition,
                    step =
                        (whole + 1)
                            .coerceAtMost(totalSteps),
                )
                ?: return null
        val from =
            tokenCenter(
                player = player,
                tokenIndex = tokenIndex,
                position = fromPosition,
                cell = cell,
            )
                ?: return null
        val to =
            tokenCenter(
                player = player,
                tokenIndex = tokenIndex,
                position = toPosition,
                cell = cell,
            )
                ?: return from

        // Move one board cell at a time with a visible take-off and landing.
        // Smoothstep removes the old rail-like constant-speed glide; the sine lift
        // gives every visual step a small frog-hop arc before it lands in the next cell.
        val easedFraction =
            fraction *
                fraction *
                (3f - 2f * fraction)
        val hopFraction =
            if (animation.hopEnabled) {
                sin(
                    PI *
                        fraction.toDouble(),
                ).toFloat()
            } else {
                0f
            }
        val hopHeight =
            cell * 0.34f

        return (
            from.first +
                (
                    to.first -
                        from.first
                    ) *
                easedFraction
            ) to
            (
                from.second +
                    (
                        to.second -
                            from.second
                        ) *
                    easedFraction -
                    hopHeight *
                    hopFraction
                )
    }

    private fun movementHopScale(
        playerId: String,
        tokenIndex: Int,
    ): Float {
        val animation =
            moveAnimation
                ?.takeIf {
                    it.playerId == playerId &&
                        it.tokenIndex == tokenIndex &&
                        it.hopEnabled
                }
                ?: return 1f
        val whole =
            floor(animation.progress)
        val fraction =
            (animation.progress - whole)
                .coerceIn(0f, 1f)
        val hop =
            sin(
                PI *
                    fraction.toDouble(),
            ).toFloat()
                .coerceAtLeast(0f)
        return 1f + hop * 0.12f
    }

    // TOKEN POSITION MIRROR LOCK: mirror the corrected shared-track exit exactly.
    private fun tokenCenter(
        player: PlayerSnapshot,
        tokenIndex: Int,
        position: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        val renderPosition =
            LudoPathEncoding
                .normalizeLegacyEntry(position)

        if (renderPosition == -1) {
            return yardTokenCenter(
                color = player.color,
                tokenIndex = tokenIndex,
                cell = cell,
            )
        }

        if (LudoPathEncoding.isTrackPosition(renderPosition)) {
            val offset =
                START_OFFSETS[player.color]
                    ?: return null
            val global =
                (offset + renderPosition) %
                    TRACK.size
            val coord =
                TRACK[global]
            return centerForCell(
                row = coord.first,
                col = coord.second,
                cell = cell,
            )
        }

        if (
            renderPosition in
            LudoPathEncoding.FIRST_HOME_LANE_POSITION..
                LudoPathEncoding.LAST_HOME_LANE_POSITION
        ) {
            val lane =
                HOME_LANES[player.color]
                    ?: return null
            val coord =
                lane[
                    renderPosition -
                        LudoPathEncoding.FIRST_HOME_LANE_POSITION
                ]
            return centerForCell(
                row = coord.first,
                col = coord.second,
                cell = cell,
            )
        }

        if (renderPosition == LudoPathEncoding.HOME_POSITION) {
            val left = axisBoundary(6, cell)
            val top = axisBoundary(6, cell)
            val right = axisBoundary(9, cell)
            val bottom = axisBoundary(9, cell)
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            val offset = (right - left) * 0.20f
            return when (player.color) {
                "RED" -> cx - offset to cy
                "GREEN" -> cx to cy - offset
                "YELLOW" -> cx + offset to cy
                "BLUE" -> cx to cy + offset
                else -> cx to cy
            }
        }

        return null
    }

    // YARD TOKEN MIRROR LOCK: fixed 0.5-cell inset, fixed 4x4 white home, fixed 25%/75% slots.
    private fun yardTokenCenter(
        color: String,
        tokenIndex: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        val origin =
            when (color) {
                "RED" -> 0 to 0
                "GREEN" -> 0 to 9
                "YELLOW" -> 9 to 9
                "BLUE" -> 9 to 0
                else -> return null
            }

        val yardLeft = axisBoundary(origin.second, cell)
        val yardTop = axisBoundary(origin.first, cell)
        val whiteLeft = yardLeft + cell * 0.5f
        val whiteTop = yardTop + cell * 0.5f
        val whiteSize = cell * 4f

        val rowFraction =
            if (tokenIndex < 2) 0.25f else 0.75f
        val colFraction =
            if (tokenIndex % 2 == 0) 0.25f else 0.75f

        return (whiteLeft + whiteSize * colFraction) to
            (whiteTop + whiteSize * rowFraction)
    }

    // CELL CENTER MIRROR LOCK: use the exact same boundary midpoints as LudoBoardView.
    private fun centerForCell(
        row: Int,
        col: Int,
        cell: Float,
    ): Pair<Float, Float> =
        ((axisBoundary(col, cell) +
            axisBoundary(col + 1, cell)) / 2f) to
            ((axisBoundary(row, cell) +
                axisBoundary(row + 1, cell)) / 2f)

    /**
     * FINAL BOARD GEOMETRY MIRROR — LOCKED.
     *
     * Must stay identical to LudoBoardView.axisBoundary() so the cosmetic animal layer remains
     * exactly on top of the authoritative board. The approved distribution is 5 base cells for
     * the first yard span, 5 for the stretched three-lane road, and 5 for the opposite yard span.
     *
     * Do not change these ratios or boundary ranges independently of LudoBoardView.
     */
    private fun axisBoundary(
        index: Int,
        cell: Float,
    ): Float {
        val yardSpan = cell * 5f
        val roadSpan = cell * 5f
        val yardStep = yardSpan / 6f
        val roadStep = roadSpan / 3f

        return when {
            index <= 6 -> index * yardStep
            index <= 9 -> yardSpan + (index - 6) * roadStep
            else -> yardSpan + roadSpan + (index - 9) * yardStep
        }
    }

    private fun playerColor(
        name: String,
    ): Int =
        when (name) {
            "RED" -> Color.rgb(241, 37, 47)
            "GREEN" -> Color.rgb(0, 169, 80)
            "YELLOW" -> Color.rgb(255, 216, 27)
            "BLUE" -> Color.rgb(48, 151, 215)
            else -> Color.rgb(108, 117, 125)
        }

    private fun density(
        value: Float,
    ): Float =
        value *
            resources.displayMetrics.density

    private data class TokenMoveAnimation(
        val playerId: String,
        val tokenIndex: Int,
        val fromPosition: Int,
        val toPosition: Int,
        val progress: Float,
        val hopEnabled: Boolean = true,
    )

    private companion object {
        val START_OFFSETS =
            mapOf(
                "RED" to 0,
                "GREEN" to 13,
                "YELLOW" to 26,
                "BLUE" to 39,
            )

        // LOGICAL TRACK MIRROR LOCK: same 52 positions and order as LudoBoardView.TRACK.
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

        // LOGICAL HOME-LANE MIRROR LOCK: same five cells per color as LudoBoardView.HOME_LANES.
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
