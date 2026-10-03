package com.ludoproof.game.feature.friends.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

internal enum class FriendsIcon {
    BACK,
    PEOPLE,
    ID_CARD,
    ADD_FRIEND,
    HOUSE,
    REQUESTS,
    INVITES,
    SEARCH,
    COPY,
    SHARE,
    PLAY,
    INVITE,
    MESSAGE,
    CLOCK,
    MORE,
}

internal enum class FriendsButtonStyle {
    BLUE,
    GREEN,
    ORANGE,
    PURPLE,
    RED,
    NAVY,
}

internal object FriendsVisualKit {
    const val CYAN = 0xFF22CFFF.toInt()
    const val CYAN_SOFT = 0xFF63E5FF.toInt()
    const val GOLD = 0xFFFFC928.toInt()
    const val TEXT = 0xFFF5FAFF.toInt()
    const val MUTED = 0xFFB8D6FF.toInt()
    const val GREEN = 0xFF40EE3B.toInt()
    const val RED = 0xFFFF4358.toInt()

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun card(
        context: Context,
        accent: Int = CYAN,
        radiusDp: Int = 18,
        paddingDp: Int = 14,
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(context, paddingDp),
                dp(context, paddingDp),
                dp(context, paddingDp),
                dp(context, paddingDp),
            )
            background = gradient(
                context = context,
                top = 0xE91A66C6.toInt(),
                bottom = 0xEE073C8A.toInt(),
                stroke = accent,
                strokeDp = 2,
                radiusDp = radiusDp,
            )
            elevation = dp(context, 4).toFloat()
        }

    fun compactCard(
        context: Context,
        accent: Int = CYAN,
    ): LinearLayout =
        card(
            context = context,
            accent = accent,
            radiusDp = 16,
            paddingDp = 11,
        )

    fun rowSurface(context: Context): GradientDrawable =
        gradient(
            context = context,
            top = 0xF3144C99.toInt(),
            bottom = 0xF30A3575.toInt(),
            stroke = 0x882EC8FF.toInt(),
            strokeDp = 1,
            radiusDp = 14,
        )

    fun inputSurface(context: Context): GradientDrawable =
        gradient(
            context = context,
            top = 0xFF0A2C66.toInt(),
            bottom = 0xFF061F4E.toInt(),
            stroke = 0xFF3C9CFF.toInt(),
            strokeDp = 2,
            radiusDp = 15,
        )

    fun friendIdSurface(context: Context): GradientDrawable =
        gradient(
            context = context,
            top = 0xFF182B7E.toInt(),
            bottom = 0xFF101F68.toInt(),
            stroke = 0xFF6A63FF.toInt(),
            strokeDp = 2,
            radiusDp = 14,
        )

    fun title(
        context: Context,
        value: String,
        sizeSp: Float = 20f,
        gold: Boolean = false,
    ): TextView =
        TextView(context).apply {
            text = value
            setTextColor(if (gold) GOLD else TEXT)
            textSize = sizeSp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            includeFontPadding = false
        }

    fun body(
        context: Context,
        value: String,
        sizeSp: Float = 12f,
        muted: Boolean = false,
    ): TextView =
        TextView(context).apply {
            text = value
            setTextColor(if (muted) MUTED else TEXT)
            textSize = sizeSp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            includeFontPadding = false
        }

    fun badge(context: Context, value: Int): TextView =
        TextView(context).apply {
            text = value.coerceAtMost(99).toString()
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = dp(context, 24)
            minHeight = dp(context, 24)
            setPadding(dp(context, 5), 0, dp(context, 5), 0)
            background = gradient(
                context = context,
                top = 0xFFFF4F54.toInt(),
                bottom = 0xFFD71724.toInt(),
                stroke = 0xFFFF7B7B.toInt(),
                strokeDp = 1,
                radiusDp = 12,
            )
        }

    fun input(context: Context, hintText: String): EditText =
        EditText(context).apply {
            hint = hintText
            setHintTextColor(0xFF7899CA.toInt())
            setTextColor(TEXT)
            textSize = 16f
            setSingleLine(true)
            background = inputSurface(context)
            setPadding(
                dp(context, 16),
                0,
                dp(context, 16),
                0,
            )
        }

    fun button(
        context: Context,
        label: String,
        style: FriendsButtonStyle,
        icon: FriendsIcon? = null,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ): TextView =
        TextView(context).apply {
            text = buildString {
                if (icon != null) {
                    append(iconGlyph(icon))
                    append("  ")
                }
                append(label)
            }
            setTextColor(if (enabled) Color.WHITE else 0xFF9EB4D1.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(
                dp(context, 10),
                dp(context, 3),
                dp(context, 10),
                dp(context, 3),
            )
            background = buttonBackground(context, style, enabled)
            alpha = if (enabled) 1f else 0.6f
            isClickable = enabled
            isFocusable = enabled
            if (enabled) {
                setOnClickListener { onClick() }
            }
        }

    fun iconView(
        context: Context,
        icon: FriendsIcon,
        tint: Int = TEXT,
    ): FriendsIconView =
        FriendsIconView(context, icon, tint)

    fun avatar(
        context: Context,
        seed: String,
        online: Boolean,
    ): FriendAvatarView =
        FriendAvatarView(context, seed, online)

    fun sectionGap(context: Context, topDp: Int = 12): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(context, topDp)
        }

    private fun buttonBackground(
        context: Context,
        style: FriendsButtonStyle,
        enabled: Boolean,
    ): GradientDrawable {
        val colors =
            when (style) {
                FriendsButtonStyle.GREEN -> 0xFF39E73B.toInt() to 0xFF08A93B.toInt()
                FriendsButtonStyle.ORANGE -> 0xFFFFC12A.toInt() to 0xFFFF7900.toInt()
                FriendsButtonStyle.PURPLE -> 0xFFD24DFF.toInt() to 0xFF7128E8.toInt()
                FriendsButtonStyle.RED -> 0xFFFF5268.toInt() to 0xFFB81832.toInt()
                FriendsButtonStyle.NAVY -> 0xFF15539B.toInt() to 0xFF0A3473.toInt()
                FriendsButtonStyle.BLUE -> 0xFF1E9CFF.toInt() to 0xFF0067D8.toInt()
            }
        val top = if (enabled) colors.first else 0xFF47617F.toInt()
        val bottom = if (enabled) colors.second else 0xFF33475F.toInt()
        val stroke =
            when (style) {
                FriendsButtonStyle.GREEN -> 0xFFA5FF67.toInt()
                FriendsButtonStyle.ORANGE -> 0xFFFFEC72.toInt()
                FriendsButtonStyle.PURPLE -> 0xFFFF8DFF.toInt()
                FriendsButtonStyle.RED -> 0xFFFF96A2.toInt()
                FriendsButtonStyle.NAVY -> 0xFF5DBBFF.toInt()
                FriendsButtonStyle.BLUE -> 0xFF6BE4FF.toInt()
            }
        return gradient(
            context = context,
            top = top,
            bottom = bottom,
            stroke = stroke,
            strokeDp = 2,
            radiusDp = 22,
        )
    }

    private fun gradient(
        context: Context,
        top: Int,
        bottom: Int,
        stroke: Int,
        strokeDp: Int,
        radiusDp: Int,
    ): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(top, bottom),
        ).apply {
            cornerRadius = dp(context, radiusDp).toFloat()
            setStroke(dp(context, strokeDp), stroke)
        }

    private fun iconGlyph(icon: FriendsIcon): String =
        when (icon) {
            FriendsIcon.BACK -> "‹"
            FriendsIcon.PEOPLE -> "◉◉"
            FriendsIcon.ID_CARD -> "▣"
            FriendsIcon.ADD_FRIEND -> "+"
            FriendsIcon.HOUSE -> "⌂"
            FriendsIcon.REQUESTS -> "✉"
            FriendsIcon.INVITES -> "●●"
            FriendsIcon.SEARCH -> "⌕"
            FriendsIcon.COPY -> "▣"
            FriendsIcon.SHARE -> "⌯"
            FriendsIcon.PLAY -> "▶"
            FriendsIcon.INVITE -> "+"
            FriendsIcon.MESSAGE -> "●"
            FriendsIcon.CLOCK -> "◷"
            FriendsIcon.MORE -> "⋮"
        }
}

internal class FriendsIconView(
    context: Context,
    private val icon: FriendsIcon,
    private val tint: Int,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            strokeWidth = FriendsVisualKit.dp(context, 3).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)
        val cx = w / 2f
        val cy = h / 2f
        paint.color = tint
        when (icon) {
            FriendsIcon.BACK -> drawBack(canvas, cx, cy, s)
            FriendsIcon.PEOPLE,
            FriendsIcon.INVITES -> drawPeople(canvas, cx, cy, s)
            FriendsIcon.ID_CARD -> drawIdCard(canvas, cx, cy, s)
            FriendsIcon.ADD_FRIEND -> drawAddFriend(canvas, cx, cy, s)
            FriendsIcon.HOUSE -> drawHouse(canvas, cx, cy, s)
            FriendsIcon.REQUESTS -> drawEnvelope(canvas, cx, cy, s)
            FriendsIcon.SEARCH -> drawSearch(canvas, cx, cy, s)
            FriendsIcon.COPY -> drawCopy(canvas, cx, cy, s)
            FriendsIcon.SHARE -> drawShare(canvas, cx, cy, s)
            FriendsIcon.PLAY -> drawPlay(canvas, cx, cy, s)
            FriendsIcon.INVITE -> drawAddFriend(canvas, cx, cy, s)
            FriendsIcon.MESSAGE -> drawMessage(canvas, cx, cy, s)
            FriendsIcon.CLOCK -> drawClock(canvas, cx, cy, s)
            FriendsIcon.MORE -> drawMore(canvas, cx, cy, s)
        }
    }

    private fun drawBack(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        val r = s * 0.23f
        canvas.drawLine(cx + r, cy - r, cx - r, cy, paint)
        canvas.drawLine(cx - r, cy, cx + r, cy + r, paint)
    }

    private fun drawPeople(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx - s * 0.14f, cy - s * 0.14f, s * 0.12f, paint)
        canvas.drawCircle(cx + s * 0.14f, cy - s * 0.11f, s * 0.10f, paint)
        canvas.drawRoundRect(
            RectF(cx - s * 0.31f, cy + s * 0.02f, cx + s * 0.02f, cy + s * 0.27f),
            s * 0.08f,
            s * 0.08f,
            paint,
        )
        canvas.drawRoundRect(
            RectF(cx + s * 0.03f, cy + s * 0.04f, cx + s * 0.29f, cy + s * 0.25f),
            s * 0.07f,
            s * 0.07f,
            paint,
        )
    }

    private fun drawIdCard(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(
            RectF(cx - s * 0.34f, cy - s * 0.24f, cx + s * 0.34f, cy + s * 0.25f),
            s * 0.09f,
            s * 0.09f,
            paint,
        )
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx - s * 0.17f, cy - s * 0.06f, s * 0.08f, paint)
        canvas.drawRoundRect(
            RectF(cx - s * 0.27f, cy + s * 0.05f, cx - s * 0.07f, cy + s * 0.16f),
            s * 0.04f,
            s * 0.04f,
            paint,
        )
        canvas.drawRoundRect(
            RectF(cx + s * 0.03f, cy - s * 0.10f, cx + s * 0.25f, cy - s * 0.04f),
            s * 0.03f,
            s * 0.03f,
            paint,
        )
        canvas.drawRoundRect(
            RectF(cx + s * 0.03f, cy + s * 0.03f, cx + s * 0.25f, cy + s * 0.09f),
            s * 0.03f,
            s * 0.03f,
            paint,
        )
    }

    private fun drawAddFriend(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx - s * 0.11f, cy - s * 0.13f, s * 0.12f, paint)
        canvas.drawRoundRect(
            RectF(cx - s * 0.30f, cy + s * 0.02f, cx + s * 0.08f, cy + s * 0.26f),
            s * 0.08f,
            s * 0.08f,
            paint,
        )
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx + s * 0.19f, cy - s * 0.02f, cx + s * 0.19f, cy + s * 0.23f, paint)
        canvas.drawLine(cx + s * 0.07f, cy + s * 0.10f, cx + s * 0.31f, cy + s * 0.10f, paint)
    }

    private fun drawHouse(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        val path = Path().apply {
            moveTo(cx - s * 0.31f, cy - s * 0.02f)
            lineTo(cx, cy - s * 0.28f)
            lineTo(cx + s * 0.31f, cy - s * 0.02f)
            moveTo(cx - s * 0.23f, cy - s * 0.08f)
            lineTo(cx - s * 0.23f, cy + s * 0.27f)
            lineTo(cx + s * 0.23f, cy + s * 0.27f)
            lineTo(cx + s * 0.23f, cy - s * 0.08f)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawEnvelope(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        val rect = RectF(cx - s * 0.31f, cy - s * 0.20f, cx + s * 0.31f, cy + s * 0.22f)
        canvas.drawRoundRect(rect, s * 0.07f, s * 0.07f, paint)
        canvas.drawLine(rect.left, rect.top, cx, cy + s * 0.03f, paint)
        canvas.drawLine(cx, cy + s * 0.03f, rect.right, rect.top, paint)
    }

    private fun drawSearch(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx - s * 0.06f, cy - s * 0.06f, s * 0.20f, paint)
        canvas.drawLine(cx + s * 0.08f, cy + s * 0.08f, cx + s * 0.27f, cy + s * 0.27f, paint)
    }

    private fun drawCopy(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(
            RectF(cx - s * 0.16f, cy - s * 0.22f, cx + s * 0.22f, cy + s * 0.22f),
            s * 0.06f,
            s * 0.06f,
            paint,
        )
        canvas.drawRoundRect(
            RectF(cx - s * 0.28f, cy - s * 0.10f, cx + s * 0.10f, cy + s * 0.32f),
            s * 0.06f,
            s * 0.06f,
            paint,
        )
    }

    private fun drawShare(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        val left = cx - s * 0.20f
        val right = cx + s * 0.22f
        canvas.drawLine(left, cy, right - s * 0.08f, cy - s * 0.19f, paint)
        canvas.drawLine(left, cy, right - s * 0.08f, cy + s * 0.19f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(left, cy, s * 0.07f, paint)
        canvas.drawCircle(right, cy - s * 0.22f, s * 0.07f, paint)
        canvas.drawCircle(right, cy + s * 0.22f, s * 0.07f, paint)
    }

    private fun drawPlay(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.FILL
        val path = Path().apply {
            moveTo(cx - s * 0.16f, cy - s * 0.24f)
            lineTo(cx + s * 0.24f, cy)
            lineTo(cx - s * 0.16f, cy + s * 0.24f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawMessage(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        val rect = RectF(cx - s * 0.30f, cy - s * 0.22f, cx + s * 0.30f, cy + s * 0.17f)
        canvas.drawRoundRect(rect, s * 0.10f, s * 0.10f, paint)
        val path = Path().apply {
            moveTo(cx - s * 0.08f, rect.bottom)
            lineTo(cx - s * 0.18f, cy + s * 0.31f)
            lineTo(cx + s * 0.02f, rect.bottom)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawClock(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, s * 0.28f, paint)
        canvas.drawLine(cx, cy, cx, cy - s * 0.15f, paint)
        canvas.drawLine(cx, cy, cx + s * 0.12f, cy + s * 0.08f, paint)
    }

    private fun drawMore(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.style = Paint.Style.FILL
        val r = s * 0.04f
        canvas.drawCircle(cx, cy - s * 0.16f, r, paint)
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawCircle(cx, cy + s * 0.16f, r, paint)
    }
}

internal class FriendAvatarView(
    context: Context,
    seed: String,
    private var online: Boolean,
) : View(context) {
    private val seedValue = seed.fold(0) { acc, char -> acc * 31 + char.code }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setOnline(value: Boolean) {
        online = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val radius = s * 0.42f
        val palette = intArrayOf(
            0xFF1F96FF.toInt(),
            0xFFDD4EAE.toInt(),
            0xFF6A63FF.toInt(),
            0xFFFF9D24.toInt(),
        )
        val ring = palette[(seedValue and Int.MAX_VALUE) % palette.size]
        paint.style = Paint.Style.FILL
        paint.color = 0xFF10295F.toInt()
        canvas.drawCircle(cx, cy, radius + s * 0.06f, paint)
        paint.color = ring
        canvas.drawCircle(cx, cy, radius + s * 0.035f, paint)
        paint.color = 0xFFFFC49A.toInt()
        canvas.drawCircle(cx, cy - s * 0.05f, radius * 0.58f, paint)
        paint.color = if ((seedValue and 1) == 0) 0xFF3A1E16.toInt() else 0xFF171A25.toInt()
        canvas.drawArc(
            RectF(cx - radius * 0.62f, cy - radius * 0.72f, cx + radius * 0.62f, cy + radius * 0.12f),
            185f,
            170f,
            true,
            paint,
        )
        paint.color = Color.WHITE
        canvas.drawCircle(cx - radius * 0.20f, cy - radius * 0.04f, radius * 0.08f, paint)
        canvas.drawCircle(cx + radius * 0.20f, cy - radius * 0.04f, radius * 0.08f, paint)
        paint.color = 0xFF17223F.toInt()
        canvas.drawCircle(cx - radius * 0.20f, cy - radius * 0.04f, radius * 0.04f, paint)
        canvas.drawCircle(cx + radius * 0.20f, cy - radius * 0.04f, radius * 0.04f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.025f
        paint.color = 0xFFA34D45.toInt()
        canvas.drawArc(
            RectF(cx - radius * 0.19f, cy + radius * 0.04f, cx + radius * 0.19f, cy + radius * 0.30f),
            10f,
            160f,
            false,
            paint,
        )
        paint.style = Paint.Style.FILL
        paint.color = if (online) 0xFF23E55A.toInt() else 0xFFFF4658.toInt()
        canvas.drawCircle(cx + radius * 0.72f, cy + radius * 0.68f, radius * 0.18f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.025f
        paint.color = Color.WHITE
        canvas.drawCircle(cx + radius * 0.72f, cy + radius * 0.68f, radius * 0.18f, paint)
    }
}

internal class FriendsHeaderArtView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val base = min(w * 0.28f, h * 0.95f)
        drawPawn(canvas, w * 0.20f, h * 0.48f, base, 0xFFE93444.toInt())
        drawPawn(canvas, w * 0.78f, h * 0.48f, base * 0.90f, 0xFF1685F5.toInt())
        drawPawn(canvas, w * 0.93f, h * 0.72f, base * 0.62f, 0xFFFFBF1F.toInt())
        drawDie(canvas, w * 0.50f, h * 0.49f, base * 0.82f)
    }

    private fun drawPawn(canvas: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy - s * 0.25f, s * 0.18f, paint)
        val path = Path().apply {
            moveTo(cx - s * 0.14f, cy - s * 0.05f)
            lineTo(cx + s * 0.14f, cy - s * 0.05f)
            lineTo(cx + s * 0.25f, cy + s * 0.30f)
            lineTo(cx - s * 0.25f, cy + s * 0.30f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.color = Color.argb(90, 255, 255, 255)
        canvas.drawCircle(cx - s * 0.06f, cy - s * 0.31f, s * 0.05f, paint)
    }

    private fun drawDie(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        paint.color = 0xFFF3F6FF.toInt()
        paint.style = Paint.Style.FILL
        val rect = RectF(cx - s * 0.34f, cy - s * 0.34f, cx + s * 0.34f, cy + s * 0.34f)
        canvas.save()
        canvas.rotate(12f, cx, cy)
        canvas.drawRoundRect(rect, s * 0.10f, s * 0.10f, paint)
        paint.color = 0xFF1B2D66.toInt()
        val r = s * 0.045f
        val offsets = arrayOf(
            -0.17f to -0.17f,
            0.17f to -0.17f,
            0f to 0f,
            -0.17f to 0.17f,
            0.17f to 0.17f,
        )
        for ((dx, dy) in offsets) {
            canvas.drawCircle(cx + s * dx, cy + s * dy, r, paint)
        }
        canvas.restore()
    }
}
