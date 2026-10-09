package com.ludoproof.game

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.view.View
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * BOARD GEOMETRY LOCK (approved final layout).
 *
 * The board position and all board geometry in this view are fixed by product decision:
 * board size/aspect, four yard locations, white-home size and margins, all road squares,
 * home lanes, center triangles, safe cells, track order, and token coordinate mapping.
 *
 * Do not move, resize, redistribute, reorder, or "normalize" any board geometry unless an
 * explicit future board-redesign task requires it. Any intentional geometry change here must
 * be mirrored in LudoPawsBoardView/AnimalPawnOverlayView so cosmetics remain pixel-aligned.
 */
class LudoBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var onTokenSelected: ((Int) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility =
            IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Ludo board"
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
        clipToOutline = true
    }

    private var boardThemeId =
        "board_classic"
    private var snapshot: MatchSnapshot? = null
    private var localPlayerId: String? = null
    private var perspectiveColor: String? = null
    private var ludoPawsTeamSigilsEnabled = false
    private var moveAnimator: ValueAnimator? = null
    private var moveAnimation: TokenMoveAnimation? = null
    private val tokenHits = mutableListOf<TokenHit>()

    private val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(1f)
            color = Color.rgb(70, 70, 70)
        }
    private val tokenStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(1.5f)
            color = Color.rgb(35, 35, 35)
        }
    private val legalPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density(4f)
            color = Color.rgb(255, 193, 7)
        }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    private val teamSigilFillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    private val teamSigilStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private val teamSigilPath = Path()

    fun setLudoPawsTeamSigilsEnabled(enabled: Boolean) {
        if (ludoPawsTeamSigilsEnabled == enabled) return
        ludoPawsTeamSigilsEnabled = enabled
        invalidate()
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String? = null,
    ) {
        val previous =
            snapshot
        snapshot = state
        localPlayerId = playerId
        this.perspectiveColor =
            perspectiveColor
                ?.takeIf {
                    it in
                        OfflinePlayerLayout.COLORS
                }

        val localPlayer =
            state?.players?.find {
                it.playerId == playerId
            }
        val legalTokens =
            state?.pendingRoll
                ?.legalTokenIndexes
                .orEmpty()
                .sorted()
                .joinToString(", ") {
                    (it + 1).toString()
                }

        contentDescription =
            when {
                state == null ->
                    "Ludo board. No active match."
                state.status == "FINISHED" ->
                    "Ludo board. Match finished."
                localPlayer == null ->
                    "Ludo board. Spectator state."
                legalTokens.isNotBlank() ->
                    "Ludo board. Your legal tokens are $legalTokens."
                else ->
                    "Ludo board. No legal token is currently selectable."
            }

        startMoveAnimationIfNeeded(
            previous =
                previous,
            current =
                state,
        )
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reloadStyle()
    }

    fun reloadStyle() {
        boardThemeId =
            CosmeticInventoryStore(
                context,
            )
                .selectedId(
                    CosmeticCategory.BOARD,
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

    // BOARD SIZE LOCK: keep the board square. Do not change this sizing contract as part of
    // unrelated UI work; all approved board geometry scales inside this square.
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired = density(380f).toInt()
        val width =
            resolveSize(desired, widthMeasureSpec)
        val height =
            resolveSize(width, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        tokenHits.clear()

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        // BASE COORDINATE LOCK: gameplay remains a logical 15x15 board. The approved visual
        // stretching is handled only by axisBoundary(); do not replace it with uniform cells.
        val cell = size / 15f

        canvas.drawColor(boardSurfaceColor())

        val turns =
            perspectiveColor
                ?.let {
                    OfflinePlayerLayout
                        .rotationQuarterTurns(
                            it,
                        )
                }
                ?: 0

        if (
            turns !=
            0
        ) {
            canvas.save()
            canvas.rotate(
                turns *
                    90f,
                size /
                    2f,
                size /
                    2f,
            )
        }

        drawYards(canvas, cell)
        if (ludoPawsTeamSigilsEnabled) {
            drawYardTeamSigils(canvas, cell)
        }
        drawTrack(canvas, cell)
        drawHomeLanes(canvas, cell)
        drawCenter(canvas, cell)
        drawTokens(canvas, cell)

        if (
            turns !=
            0
        ) {
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return true
        }

        val size =
            min(
                width,
                height,
            ).toFloat()
        val turns =
            perspectiveColor
                ?.let {
                    OfflinePlayerLayout
                        .rotationQuarterTurns(
                            it,
                        )
                }
                ?: 0
        val logicalTouch =
            unrotateTouch(
                event.x,
                event.y,
                size,
                turns,
            )

        val hit =
            tokenHits.minByOrNull {
                hypot(
                    (
                        logicalTouch.first -
                            it.x
                        ).toDouble(),
                    (
                        logicalTouch.second -
                            it.y
                        ).toDouble(),
                )
            }

        if (
            hit != null &&
            hypot(
                (
                    logicalTouch.first -
                        hit.x
                    ).toDouble(),
                (
                    logicalTouch.second -
                        hit.y
                    ).toDouble(),
            ) <= hit.radius
        ) {
            performClick()
            onTokenSelected?.invoke(hit.tokenIndex)
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun unrotateTouch(
        x: Float,
        y: Float,
        size: Float,
        quarterTurns: Int,
    ): Pair<Float, Float> =
        when (
            (
                quarterTurns %
                    4 +
                    4
                ) %
                4
        ) {
            1 ->
                y to
                    (
                        size -
                            x
                        )
            2 ->
                (
                    size -
                        x
                    ) to
                    (
                        size -
                            y
                        )
            3 ->
                (
                    size -
                        y
                    ) to
                    x
            else ->
                x to
                    y
        }

    // YARD POSITION LOCK: these four logical origins permanently anchor the four color homes.
    // RED=top-left, GREEN=top-right, YELLOW=bottom-right, BLUE=bottom-left.
    private fun drawYards(
        canvas: Canvas,
        cell: Float,
    ) {
        yard(
            canvas,
            cell,
            row = 0,
            col = 0,
            color = colorFor("RED"),
        )
        yard(
            canvas,
            cell,
            row = 0,
            col = 9,
            color = colorFor("GREEN"),
        )
        yard(
            canvas,
            cell,
            row = 9,
            col = 9,
            color = colorFor("YELLOW"),
        )
        yard(
            canvas,
            cell,
            row = 9,
            col = 0,
            color = colorFor("BLUE"),
        )
    }

    private fun yard(
        canvas: Canvas,
        cell: Float,
        row: Int,
        col: Int,
        color: Int,
    ) {
        val yardRect =
            RectF(
                axisBoundary(col, cell),
                axisBoundary(row, cell),
                axisBoundary(col + 6, cell),
                axisBoundary(row + 6, cell),
            )

        // WHITE-HOME GEOMETRY LOCK: 0.5 base-cell margin on ALL four sides.
        // The white home remains exactly 4 x 4 base cells. Do not move or resize it independently;
        // the released road stretch, yard token centers, and overlay geometry depend on this.
        val whiteRect =
            RectF(
                yardRect.left + cell * 0.5f,
                yardRect.top + cell * 0.5f,
                yardRect.right - cell * 0.5f,
                yardRect.bottom - cell * 0.5f,
            )

        fillPaint.color = color
        canvas.drawRect(
            yardRect,
            fillPaint,
        )

        fillPaint.color = neutralCellColor()
        canvas.drawRoundRect(
            whiteRect,
            cell * 0.35f,
            cell * 0.35f,
            fillPaint,
        )

        strokePaint.color = Color.argb(
            230,
            22,
            33,
            52,
        )
        strokePaint.strokeWidth = density(1.4f)
        canvas.drawRoundRect(
            whiteRect,
            cell * 0.35f,
            cell * 0.35f,
            strokePaint,
        )
        strokePaint.strokeWidth = density(1f)
        strokePaint.color = Color.rgb(70, 70, 70)
    }

    // ROAD/TRACK LOCK: draw the approved 52-square movement path exactly from TRACK.
    // Do not change road-square positions, order, start cells, or safe-cell alignment here.
    private fun drawTrack(
        canvas: Canvas,
        cell: Float,
    ) {
        for ((index, coord) in TRACK.withIndex()) {
            val color =
                when (index) {
                    0 -> colorFor("RED")
                    13 -> colorFor("GREEN")
                    26 -> colorFor("YELLOW")
                    39 -> colorFor("BLUE")
                    else -> neutralCellColor()
                }
            drawCell(
                canvas,
                cell,
                coord.first,
                coord.second,
                color,
            )

            if (index in SAFE_GLOBAL_CELLS) {
                val center =
                    centerForCell(
                        coord.first,
                        coord.second,
                        cell,
                    )
                drawStar(
                    canvas,
                    center.first,
                    center.second,
                    cell * 0.27f,
                    when (index) {
                        0 -> colorFor("RED")
                        13 -> colorFor("GREEN")
                        26 -> colorFor("YELLOW")
                        39 -> colorFor("BLUE")
                        else -> Color.rgb(45, 138, 204)
                    },
                )
            }
        }
    }

    // HOME-LANE LOCK: each color keeps the approved five-cell lane into the center.
    // These coordinates are part of the fixed board and must stay synchronized with tokenCenter().
    private fun drawHomeLanes(
        canvas: Canvas,
        cell: Float,
    ) {
        HOME_LANES.forEach { (colorName, lane) ->
            val color = colorFor(colorName)
            lane.forEach { coord ->
                drawCell(
                    canvas,
                    cell,
                    coord.first,
                    coord.second,
                    color,
                )
            }
        }
    }

    // CENTER LOCK: the four finish triangles are permanently bounded by logical indices 6..9.
    // Derive them from axisBoundary() so they always meet the stretched three-lane road exactly.
    private fun drawCenter(
        canvas: Canvas,
        cell: Float,
    ) {
        val left = axisBoundary(6, cell)
        val top = axisBoundary(6, cell)
        val right = axisBoundary(9, cell)
        val bottom = axisBoundary(9, cell)
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f

        triangle(
            canvas,
            colorFor("RED"),
            left,
            top,
            left,
            bottom,
            cx,
            cy,
        )
        triangle(
            canvas,
            colorFor("GREEN"),
            left,
            top,
            right,
            top,
            cx,
            cy,
        )
        triangle(
            canvas,
            colorFor("YELLOW"),
            right,
            top,
            right,
            bottom,
            cx,
            cy,
        )
        triangle(
            canvas,
            colorFor("BLUE"),
            left,
            bottom,
            right,
            bottom,
            cx,
            cy,
        )
    }

    private fun triangle(
        canvas: Canvas,
        color: Int,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    ) {
        fillPaint.color = color
        val path =
            Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
                lineTo(x3, y3)
                close()
            }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    // ROAD CELL RECT LOCK: every square/rectangle must come from axisBoundary().
    // Do not calculate track cells with col * cell / row * cell; that would break the final layout.
    private fun drawCell(
        canvas: Canvas,
        cell: Float,
        row: Int,
        col: Int,
        color: Int,
    ) {
        fillPaint.color = color
        val rect =
            RectF(
                axisBoundary(col, cell),
                axisBoundary(row, cell),
                axisBoundary(col + 1, cell),
                axisBoundary(row + 1, cell),
            )
        canvas.drawRect(rect, fillPaint)
        canvas.drawRect(rect, strokePaint)
    }

    private fun drawTokens(
        canvas: Canvas,
        cell: Float,
    ) {
        val state = snapshot ?: return
        val legal =
            state.pendingRoll
                ?.legalTokenIndexes
                .orEmpty()
        val localId = localPlayerId

        state.players.forEach { player ->
            player.tokens.forEachIndexed {
                    tokenIndex,
                    position,
                ->
                val base =
                    animatedTokenCenter(
                        player =
                            player,
                        tokenIndex =
                            tokenIndex,
                        currentPosition =
                            position,
                        cell =
                            cell,
                    )
                        ?: return@forEachIndexed

                val offset =
                    if (position in 0..57) {
                        tokenOffset(
                            tokenIndex + player.seat,
                            cell,
                        )
                    } else {
                        0f to 0f
                    }

                val x = base.first + offset.first
                val y = base.second + offset.second
                val radius = cell * 0.27f
                val isLegal =
                    player.playerId == localId &&
                        tokenIndex in legal &&
                        state.pendingRoll?.status ==
                        "RESOLVED"

                if (isLegal) {
                    canvas.drawCircle(
                        x,
                        y,
                        radius * 1.28f,
                        legalPaint,
                    )
                    tokenHits +=
                        TokenHit(
                            tokenIndex = tokenIndex,
                            x = x,
                            y = y,
                            radius = radius * 1.8f,
                        )
                }

                drawPawn(
                    canvas,
                    x,
                    y,
                    radius,
                    colorFor(player.color),
                )
            }
        }
    }


    /**
     * Owner marks are presentation-only watermarks in the four home slots.
     * They are opt-in for Ludo Paws so the classic board stays pixel-identical.
     * Pawns are drawn after this pass and naturally cover a mark while occupying
     * the slot; no marker follows or animates with a pawn.
     */
    private fun drawYardTeamSigils(
        canvas: Canvas,
        cell: Float,
    ) {
        for (team in LudoPawsTeamColor.entries) {
            for (tokenIndex in 0..3) {
                val center =
                    yardTokenCenter(
                        color = team.wireName,
                        tokenIndex = tokenIndex,
                        cell = cell,
                    ) ?: continue
                drawTeamSigil(
                    canvas = canvas,
                    team = team,
                    x = center.first,
                    y = center.second,
                    radius = cell * 0.15f,
                )
            }
        }
    }

    private fun drawTeamSigil(
        canvas: Canvas,
        team: LudoPawsTeamColor,
        x: Float,
        y: Float,
        radius: Float,
    ) {
        val rgb = team.sigilArgb
        teamSigilFillPaint.color =
            Color.argb(
                38,
                Color.red(rgb),
                Color.green(rgb),
                Color.blue(rgb),
            )
        teamSigilStrokePaint.color =
            Color.argb(
                190,
                Color.red(rgb),
                Color.green(rgb),
                Color.blue(rgb),
            )
        teamSigilStrokePaint.strokeWidth =
            maxOf(density(1.05f), radius * 0.12f)

        when (team.sigil) {
            LudoPawsTeamSigil.DIAMOND -> {
                teamSigilPath.reset()
                teamSigilPath.moveTo(x, y - radius)
                teamSigilPath.lineTo(x + radius * 0.78f, y)
                teamSigilPath.lineTo(x, y + radius)
                teamSigilPath.lineTo(x - radius * 0.78f, y)
                teamSigilPath.close()
                canvas.drawPath(teamSigilPath, teamSigilFillPaint)
                canvas.drawPath(teamSigilPath, teamSigilStrokePaint)
            }

            LudoPawsTeamSigil.LEAF -> {
                teamSigilPath.reset()
                teamSigilPath.moveTo(x, y + radius)
                teamSigilPath.cubicTo(
                    x - radius * 1.35f, y + radius * 0.15f,
                    x - radius * 0.85f, y - radius * 1.05f,
                    x, y - radius,
                )
                teamSigilPath.cubicTo(
                    x + radius * 1.15f, y - radius * 0.65f,
                    x + radius * 1.25f, y + radius * 0.30f,
                    x, y + radius,
                )
                teamSigilPath.close()
                canvas.drawPath(teamSigilPath, teamSigilFillPaint)
                canvas.drawPath(teamSigilPath, teamSigilStrokePaint)
                teamSigilPath.reset()
                teamSigilPath.moveTo(x - radius * 0.55f, y + radius * 0.65f)
                teamSigilPath.quadTo(
                    x - radius * 0.05f, y + radius * 0.1f,
                    x + radius * 0.58f, y - radius * 0.58f,
                )
                canvas.drawPath(teamSigilPath, teamSigilStrokePaint)
            }

            LudoPawsTeamSigil.WAVE -> {
                teamSigilPath.reset()
                teamSigilPath.moveTo(x - radius, y - radius * 0.25f)
                teamSigilPath.quadTo(
                    x - radius * 0.5f, y - radius * 0.95f,
                    x, y - radius * 0.25f,
                )
                teamSigilPath.quadTo(
                    x + radius * 0.5f, y + radius * 0.45f,
                    x + radius, y - radius * 0.25f,
                )
                canvas.drawPath(teamSigilPath, teamSigilStrokePaint)
                teamSigilPath.reset()
                teamSigilPath.moveTo(x - radius, y + radius * 0.48f)
                teamSigilPath.quadTo(
                    x - radius * 0.5f, y - radius * 0.20f,
                    x, y + radius * 0.48f,
                )
                teamSigilPath.quadTo(
                    x + radius * 0.5f, y + radius * 1.1f,
                    x + radius, y + radius * 0.48f,
                )
                canvas.drawPath(teamSigilPath, teamSigilStrokePaint)
            }

            LudoPawsTeamSigil.SUN -> {
                canvas.drawCircle(x, y, radius * 0.36f, teamSigilFillPaint)
                canvas.drawCircle(x, y, radius * 0.36f, teamSigilStrokePaint)
                for (step in 0 until 8) {
                    val angle = step * (Math.PI.toFloat() / 4f)
                    val inner = radius * 0.58f
                    val outer = radius
                    canvas.drawLine(
                        x + cos(angle) * inner,
                        y + sin(angle) * inner,
                        x + cos(angle) * outer,
                        y + sin(angle) * outer,
                        teamSigilStrokePaint,
                    )
                }
            }
        }
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
            previous ==
                null ||
            current ==
                null ||
            previous.matchId !=
                current.matchId
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

                            if (
                                to >
                                from
                            ) {
                                TokenMoveAnimation(
                                    playerId =
                                        currentPlayer.playerId,
                                    tokenIndex =
                                        tokenIndex,
                                    fromPosition =
                                        from,
                                    toPosition =
                                        to,
                                    progress =
                                        0f,
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
        val speed =
            GameSettingsStore(
                context,
            )
                .snapshot()
                .gameSpeed
        val duration =
            (
                steps.toLong() *
                    speed.moveStepMs
                )
                .coerceAtLeast(
                    speed.moveStepMs,
                )

        moveAnimation =
            movement
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
                            android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(
                                animation: android.animation.Animator,
                            ) {
                                moveAnimation =
                                    null
                                moveAnimator =
                                    null
                                invalidate()
                            }

                            override fun onAnimationCancel(
                                animation: android.animation.Animator,
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
                    it.playerId ==
                        player.playerId &&
                        it.tokenIndex ==
                        tokenIndex
                }
                ?: return tokenCenter(
                    player,
                    tokenIndex,
                    currentPosition,
                    cell,
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
            kotlin.math.floor(
                progress,
            )
                .toInt()
                .coerceAtMost(
                    totalSteps -
                        1,
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
                player,
                tokenIndex,
                fromPosition,
                cell,
            )
                ?: return null
        val to =
            tokenCenter(
                player,
                tokenIndex,
                toPosition,
                cell,
            )
                ?: return from

        return (
            from.first +
                (
                    to.first -
                        from.first
                    ) *
                fraction
            ) to
            (
                from.second +
                    (
                        to.second -
                            from.second
                        ) *
                    fraction
                )
    }

    private fun drawStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        color: Int,
    ) {
        val path = Path()
        for (i in 0 until 10) {
            val angle =
                Math.toRadians(
                    (-90.0 + i * 36.0),
                )
            val r =
                if (i % 2 == 0) {
                    radius
                } else {
                    radius * 0.45f
                }
            val x =
                cx +
                    (Math.cos(angle) * r)
                        .toFloat()
            val y =
                cy +
                    (Math.sin(angle) * r)
                        .toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        fillPaint.shader = null
        fillPaint.color = color
        canvas.drawPath(path, fillPaint)
    }

    private fun drawPawn(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
    ) {
        fillPaint.shader = null
        fillPaint.color =
            Color.argb(
                80,
                0,
                0,
                0,
            )
        canvas.drawOval(
            x - radius * 0.8f,
            y + radius * 0.62f,
            x + radius * 0.8f,
            y + radius * 1.02f,
            fillPaint,
        )

        fillPaint.shader =
            RadialGradient(
                x - radius * 0.28f,
                y - radius * 0.35f,
                radius * 1.25f,
                intArrayOf(
                    Color.WHITE,
                    color,
                    darken(color),
                ),
                floatArrayOf(
                    0f,
                    0.30f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            x,
            y - radius * 0.18f,
            radius * 0.72f,
            fillPaint,
        )
        fillPaint.shader = null
        fillPaint.color = color
        canvas.drawRoundRect(
            x - radius * 0.58f,
            y + radius * 0.16f,
            x + radius * 0.58f,
            y + radius * 0.72f,
            radius * 0.28f,
            radius * 0.28f,
            fillPaint,
        )
        canvas.drawCircle(
            x,
            y - radius * 0.18f,
            radius * 0.72f,
            tokenStrokePaint,
        )
    }

    private fun darken(
        color: Int,
    ): Int =
        Color.rgb(
            (Color.red(color) * 0.56f)
                .toInt(),
            (Color.green(color) * 0.56f)
                .toInt(),
            (Color.blue(color) * 0.56f)
                .toInt(),
        )

    // TOKEN POSITION LOCK: movement exits the shared track after relative position 50.
    // Encoded position 51 is legacy-only and is rendered as the first home-lane position.
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
                player.color,
                tokenIndex,
                cell,
            )
        }

        if (LudoPathEncoding.isTrackPosition(renderPosition)) {
            val offset =
                START_OFFSETS[player.color]
                    ?: return null
            val global =
                (offset + renderPosition) %
                    TRACK.size
            val coord = TRACK[global]
            return centerForCell(
                coord.first,
                coord.second,
                cell,
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
                coord.first,
                coord.second,
                cell,
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

    // YARD TOKEN LOCK: the four resting-token slots are tied to the fixed 4x4 white home.
    // Keep the 0.5-cell inset and 25%/75% slot fractions unchanged unless the board is redesigned.
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

        val yardRect =
            RectF(
                axisBoundary(origin.second, cell),
                axisBoundary(origin.first, cell),
                axisBoundary(origin.second + 6, cell),
                axisBoundary(origin.first + 6, cell),
            )
        val whiteLeft = yardRect.left + cell * 0.5f
        val whiteTop = yardRect.top + cell * 0.5f
        val whiteSize = cell * 4f

        val rowFraction =
            if (tokenIndex < 2) 0.25f else 0.75f
        val colFraction =
            if (tokenIndex % 2 == 0) 0.25f else 0.75f

        return (whiteLeft + whiteSize * colFraction) to
            (whiteTop + whiteSize * rowFraction)
    }

    private fun tokenOffset(
        slot: Int,
        cell: Float,
    ): Pair<Float, Float> =
        when (slot % 4) {
            0 -> -cell * 0.13f to -cell * 0.13f
            1 -> cell * 0.13f to -cell * 0.13f
            2 -> -cell * 0.13f to cell * 0.13f
            else -> cell * 0.13f to cell * 0.13f
        }

    // CELL CENTER LOCK: road/home-lane token centers are the midpoint of the same boundaries
    // used to draw their cells. This keeps hit-testing, animation, and visuals aligned.
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
     * FINAL BOARD GEOMETRY — LOCKED.
     *
     * Maps the logical 15x15 Ludo grid onto the approved final visual geometry:
     * - left/top corner-home span: 5 base cells
     * - middle three-lane road span: 5 base cells
     * - right/bottom corner-home span: 5 base cells
     * - white home inside each yard: 0.5 + 4 + 0.5 base cells
     *
     * Six logical yard rows/columns share a 5-cell physical span, while the three logical road
     * rows/columns share a 5-cell physical span. This is the intentional road stretch.
     *
     * DO NOT change yardSpan, roadSpan, yardStep, roadStep, or the boundary ranges for routine UI
     * work. Changing this method moves every road square, yard, center triangle, and token center.
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

    private fun colorFor(
        name: String,
    ): Int {
        val classic =
            when (name) {
                "RED" -> Color.rgb(241, 37, 47)
                "GREEN" -> Color.rgb(0, 169, 80)
                "YELLOW" -> Color.rgb(255, 216, 27)
                "BLUE" -> Color.rgb(48, 151, 215)
                else -> Color.rgb(108, 117, 125)
            }

        return when (boardThemeId) {
            "board_diwali" ->
                brighten(
                    classic,
                    1.08f,
                )
            "board_denim" ->
                mixWith(
                    classic,
                    0xFF2D83C5.toInt(),
                    .20f,
                )
            "board_neon" ->
                brighten(
                    classic,
                    1.22f,
                )
            "board_pirate" ->
                mixWith(
                    classic,
                    0xFFC9A86A.toInt(),
                    .18f,
                )
            "board_alien" ->
                mixWith(
                    classic,
                    0xFF4EDA73.toInt(),
                    .20f,
                )
            "board_penguin" ->
                mixWith(
                    classic,
                    0xFFBDEBFF.toInt(),
                    .24f,
                )
            else ->
                classic
        }
    }

    private fun boardSurfaceColor(): Int =
        when (boardThemeId) {
            "board_checkers" -> 0xFFF5EEE2.toInt()
            "board_chess" -> 0xFFF1E3C4.toInt()
            "board_diwali" -> 0xFFFFE7A0.toInt()
            "board_denim" -> 0xFFD8EEFA.toInt()
            "board_neon" -> 0xFF10131C.toInt()
            "board_pirate" -> 0xFFE8D5A9.toInt()
            "board_alien" -> 0xFF18243B.toInt()
            "board_penguin" -> 0xFFE5F8FF.toInt()
            else -> 0xFFF8F8F8.toInt()
        }

    private fun neutralCellColor(): Int =
        when (boardThemeId) {
            "board_neon" -> 0xFF1E2638.toInt()
            "board_alien" -> 0xFF24324A.toInt()
            "board_diwali" -> 0xFFFFF6DC.toInt()
            "board_pirate" -> 0xFFFFF2D2.toInt()
            "board_penguin" -> 0xFFF5FCFF.toInt()
            else -> Color.WHITE
        }

    private fun brighten(
        color: Int,
        factor: Float,
    ): Int =
        Color.rgb(
            (Color.red(color) * factor)
                .toInt()
                .coerceIn(0,255),
            (Color.green(color) * factor)
                .toInt()
                .coerceIn(0,255),
            (Color.blue(color) * factor)
                .toInt()
                .coerceIn(0,255),
        )

    private fun mixWith(
        base: Int,
        tint: Int,
        amount: Float,
    ): Int {
        val keep =
            1f -
                amount
        return Color.rgb(
            (
                Color.red(base) *
                    keep +
                    Color.red(tint) *
                    amount
                )
                .toInt(),
            (
                Color.green(base) *
                    keep +
                    Color.green(tint) *
                    amount
                )
                .toInt(),
            (
                Color.blue(base) *
                    keep +
                    Color.blue(tint) *
                    amount
                )
                .toInt(),
        )
    }

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density

    private data class TokenMoveAnimation(
        val playerId: String,
        val tokenIndex: Int,
        val fromPosition: Int,
        val toPosition: Int,
        val progress: Float,
    )

    private data class TokenHit(
        val tokenIndex: Int,
        val x: Float,
        val y: Float,
        val radius: Float,
    )

    private companion object {
        val SAFE_GLOBAL_CELLS =
            setOf(0, 8, 13, 21, 26, 34, 39, 47)

        val START_OFFSETS =
            mapOf(
                "RED" to 0,
                "GREEN" to 13,
                "YELLOW" to 26,
                "BLUE" to 39,
            )

        // LOGICAL TRACK LOCK: exactly 52 movement positions in this exact order.
        // Geometry may scale through axisBoundary(), but these logical coordinates must not drift.
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

        // LOGICAL HOME-LANE LOCK: exactly five approach cells per color in these positions.
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
