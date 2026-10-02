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
import kotlin.math.hypot
import kotlin.math.min

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
        fillPaint.color = color
        canvas.drawRect(
            col * cell,
            row * cell,
            (col + 6) * cell,
            (row + 6) * cell,
            fillPaint,
        )

        fillPaint.color = neutralCellColor()
        canvas.drawRoundRect(
            RectF(
                (col + 1) * cell,
                (row + 1) * cell,
                (col + 5) * cell,
                (row + 5) * cell,
            ),
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
            RectF(
                (col + 1) * cell,
                (row + 1) * cell,
                (col + 5) * cell,
                (row + 5) * cell,
            ),
            cell * 0.35f,
            cell * 0.35f,
            strokePaint,
        )
        strokePaint.strokeWidth = density(1f)
        strokePaint.color = Color.rgb(70, 70, 70)
    }

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

    private fun drawCenter(
        canvas: Canvas,
        cell: Float,
    ) {
        val left = 6f * cell
        val top = 6f * cell
        val right = 9f * cell
        val bottom = 9f * cell
        val cx = 7.5f * cell
        val cy = 7.5f * cell

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
                col * cell,
                row * cell,
                (col + 1) * cell,
                (row + 1) * cell,
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
            (
                movement.toPosition -
                    movement.fromPosition
                )
                .coerceAtLeast(
                    1,
                )
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
            (
                animation.toPosition -
                    animation.fromPosition
                )
                .coerceAtLeast(
                    1,
                )
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
            animation.fromPosition +
                whole
        val toPosition =
            (
                fromPosition +
                    1
                )
                .coerceAtMost(
                    animation.toPosition,
                )
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

    private fun tokenCenter(
        player: PlayerSnapshot,
        tokenIndex: Int,
        position: Int,
        cell: Float,
    ): Pair<Float, Float>? {
        if (position == -1) {
            return yardTokenCenter(
                player.color,
                tokenIndex,
                cell,
            )
        }

        if (position in 0..51) {
            val offset =
                START_OFFSETS[player.color]
                    ?: return null
            val global =
                (offset + position) %
                    TRACK.size
            val coord = TRACK[global]
            return centerForCell(
                coord.first,
                coord.second,
                cell,
            )
        }

        if (position in 52..56) {
            val lane =
                HOME_LANES[player.color]
                    ?: return null
            val coord =
                lane[position - 52]
            return centerForCell(
                coord.first,
                coord.second,
                cell,
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
        val row = origin.first + slot.first
        val col = origin.second + slot.second
        return col * cell to row * cell
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

    private fun centerForCell(
        row: Int,
        col: Int,
        cell: Float,
    ): Pair<Float, Float> =
        (col + 0.5f) * cell to
            (row + 0.5f) * cell

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
