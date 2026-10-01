package com.ludoproof.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
    }

    private var snapshot: MatchSnapshot? = null
    private var localPlayerId: String? = null
    private val tokenArt = mutableMapOf<Int, TokenArt>()
    private val tokenHits = mutableListOf<TokenHit>()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var pressedToken: TokenHit? = null
    private var touchStartX = 0f
    private var touchStartY = 0f

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
    ) {
        snapshot = state
        localPlayerId = playerId
        // A response can arrive between touch-down and touch-up.
        pressedToken = null
        tokenHits.clear()

        val localPlayer =
            state?.players?.find {
                it.playerId == playerId
            }
        val legalTokens =
            selectableTokenIndexes(state, playerId)
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

        invalidate()
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

        canvas.drawColor(Color.rgb(248, 248, 248))
        drawYards(canvas, cell)
        drawTrack(canvas, cell)
        drawHomeLanes(canvas, cell)
        drawCenter(canvas, cell)
        drawTokens(canvas, cell)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            pressedToken = null
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                pressedToken = tokenHits.minByOrNull {
                    hypot(event.x - it.x, event.y - it.y)
                }?.takeIf {
                    hypot(event.x - it.x, event.y - it.y) <= it.radius
                }
                return pressedToken != null
            }
            MotionEvent.ACTION_MOVE -> {
                if (hypot(event.x - touchStartX, event.y - touchStartY) > touchSlop) {
                    pressedToken = null
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> pressedToken = null
            MotionEvent.ACTION_UP -> {
                val hit = pressedToken
                pressedToken = null
                if (hit != null &&
                    hypot(event.x - touchStartX, event.y - touchStartY) <= touchSlop &&
                    hypot(event.x - hit.x, event.y - hit.y) <= hit.radius
                ) {
                    performClick()
                    onTokenSelected?.invoke(hit.tokenIndex)
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
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

        fillPaint.color = Color.WHITE
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
                    else -> Color.WHITE
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
            selectableTokenIndexes(state, localPlayerId)
        val localId = localPlayerId

        state.players.forEach { player ->
            player.tokens.forEachIndexed {
                    tokenIndex,
                    position,
                ->
                val base =
                    tokenCenter(
                        player,
                        tokenIndex,
                        position,
                        cell,
                    ) ?: return@forEachIndexed

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
                val radius = cell * 0.39f
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
        tokenArt.getOrPut(color) { TokenArt(color) }.draw(canvas, x, y, radius * 3.1f)
    }

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

    private fun colorFor(name: String): Int =
        when (name) {
            "RED" -> Color.rgb(241, 37, 47)
            "GREEN" -> Color.rgb(0, 169, 80)
            "YELLOW" -> Color.rgb(255, 216, 27)
            "BLUE" -> Color.rgb(48, 151, 215)
            else -> Color.rgb(108, 117, 125)
        }

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density

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
