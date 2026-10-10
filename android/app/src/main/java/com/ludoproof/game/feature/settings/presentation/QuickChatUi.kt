package com.ludoproof.game.ui.quickchat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import kotlin.math.min
import kotlin.math.roundToInt

/** Emoji order follows the four rows in the approved Quick Chat reference. */
internal object QuickChatEmojiCatalog {
    const val COOLDOWN_MS = 700L

    val EMOJIS: List<String> = listOf(
        "👍", "😂", "😮", "😭", "😠", "🥳",
        "😅", "😛", "😎", "🤔", "😘", "🤭",
        "🤦", "😈", "🔥", "💪", "🤡", "👑",
        "😵‍💫", "😞", "🤣", "😆", "😤", "😡",
    )
}

/** Small circular speech-bubble control drawn to match the supplied red/gold icon. */
internal class QuickChatButtonView(
    context: Context,
) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Open Quick Chat"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = dp(36)
        setMeasuredDimension(
            resolveSize(desired, widthMeasureSpec),
            resolveSize(desired, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = size * .465f

        fill.shader = null
        fill.color = 0xFF51170D.toInt()
        fill.setShadowLayer(size * .10f, 0f, size * .035f, 0xAA000000.toInt())
        canvas.drawCircle(cx, cy, radius, fill)
        fill.clearShadowLayer()

        fill.shader = LinearGradient(
            cx, cy - radius, cx, cy + radius,
            intArrayOf(0xFFFFD85B.toInt(), 0xFFE9A000.toInt(), 0xFFB93716.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius * .91f, fill)
        fill.shader = null

        stroke.color = 0xFF8B2617.toInt()
        stroke.strokeWidth = size * .055f
        canvas.drawCircle(cx, cy, radius * .82f, stroke)

        fill.shader = LinearGradient(
            cx, cy - radius * .62f, cx, cy + radius * .62f,
            intArrayOf(0xFFFFFFFF.toInt(), 0xFFF1F5F7.toInt(), 0xFFD5E0E8.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius * .68f, fill)
        fill.shader = null
        stroke.color = 0xFFB67827.toInt()
        stroke.strokeWidth = size * .025f
        canvas.drawCircle(cx, cy, radius * .68f, stroke)

        val bubble = Path().apply {
            moveTo(cx - radius * .31f, cy + radius * .30f)
            lineTo(cx - radius * .38f, cy + radius * .48f)
            lineTo(cx - radius * .06f, cy + radius * .34f)
            close()
        }
        fill.color = 0xFFF5F7FA.toInt()
        canvas.drawPath(bubble, fill)

        fill.color = 0xFF233448.toInt()
        val dotRadius = size * .055f
        val dotY = cy
        canvas.drawCircle(cx - radius * .28f, dotY, dotRadius, fill)
        canvas.drawCircle(cx, dotY, dotRadius, fill)
        canvas.drawCircle(cx + radius * .28f, dotY, dotRadius, fill)

        if (isPressed) {
            stroke.color = 0xAAFFFFFF.toInt()
            stroke.strokeWidth = size * .05f
            canvas.drawCircle(cx, cy, radius * .76f, stroke)
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}

/**
 * Shows a compact, anchored 6x4 emoji palette. It flips above/below the profile
 * icon according to available screen space and dismisses before dispatching.
 */
internal fun showQuickChatPopup(
    anchor: View,
    onEmojiSelected: (String) -> Unit,
) {
    if (!anchor.isAttachedToWindow || !anchor.isEnabled) return
    val context = anchor.context
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).roundToInt()

    val screenWidth = context.resources.displayMetrics.widthPixels
    val popupWidth = min(dp(360), screenWidth - dp(24)).coerceAtLeast(dp(280))

    val panel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(9), dp(8), dp(9), dp(9))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xF52870CB.toInt(), 0xF1174DAB.toInt(), 0xF00B347C.toInt()),
        ).apply {
            cornerRadius = dp(14).toFloat()
            setStroke(dp(3), 0xFFFFCD38.toInt())
        }
        elevation = dp(10).toFloat()
    }

    panel.addView(
        TextView(context).apply {
            text = "QUICK CHAT"
            textSize = 19f
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setTextColor(0xFFFFD84C.toInt())
            gravity = Gravity.CENTER
            includeFontPadding = false
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), 0xA5000000.toInt())
            setPadding(0, dp(6), 0, dp(9))
            contentDescription = "Quick Chat emoji picker"
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(38),
        ),
    )

    QuickChatEmojiCatalog.EMOJIS.chunked(6).forEach { rowEmojis ->
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        rowEmojis.forEach { emoji ->
            val cell = TextView(context).apply {
                text = emoji
                textSize = 24f
                gravity = Gravity.CENTER
                includeFontPadding = false
                isClickable = true
                isFocusable = true
                contentDescription = "Send $emoji"
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0x1CFFFFFF)
                }
                setOnClickListener(null)
            }
            row.addView(
                cell,
                LinearLayout.LayoutParams(
                    0,
                    dp(45),
                    1f,
                ).apply {
                    setMargins(dp(1), dp(1), dp(1), dp(1))
                },
            )
        }
        panel.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(47),
            ),
        )
    }

    val wrapper = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false
        clipToPadding = false
    }
    val arrow = TextView(context).apply {
        textSize = 18f
        setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        setTextColor(0xFFFFCD38.toInt())
        gravity = Gravity.CENTER
        includeFontPadding = false
        setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), 0x99000000.toInt())
    }

    val location = IntArray(2)
    anchor.getLocationOnScreen(location)
    val visibleFrame = Rect()
    anchor.rootView.getWindowVisibleDisplayFrame(visibleFrame)
    val estimatedHeight = dp(38 + 8 + 9 + 4 * 47 + 12)
    val spaceBelow = visibleFrame.bottom - (location[1] + anchor.height)
    val spaceAbove = location[1] - visibleFrame.top
    val openAbove = spaceBelow < estimatedHeight && spaceAbove > spaceBelow

    if (openAbove) {
        wrapper.addView(
            panel,
            LinearLayout.LayoutParams(popupWidth, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        arrow.text = "▼"
        wrapper.addView(arrow, LinearLayout.LayoutParams(dp(24), dp(16)))
    } else {
        arrow.text = "▲"
        wrapper.addView(arrow, LinearLayout.LayoutParams(dp(24), dp(16)))
        wrapper.addView(
            panel,
            LinearLayout.LayoutParams(popupWidth, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
    }

    val popup = PopupWindow(
        wrapper,
        popupWidth,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true,
    ).apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = true
        isClippingEnabled = true
        elevation = dp(12).toFloat()
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        animationStyle = android.R.style.Animation_Dialog
    }

    wrapper.measure(
        View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )
    val measuredHeight = wrapper.measuredHeight
    popup.setOnDismissListener { anchor.isPressed = false }
    for (row in (panel.childCount - 4) until panel.childCount) {
        // Keep rows reachable for TalkBack; click handlers dismiss via the shared listener below.
        val rowView = panel.getChildAt(row)
        if (rowView is ViewGroup) {
            for (i in 0 until rowView.childCount) {
                val cell = rowView.getChildAt(i)
                cell.setOnClickListener {
                    val emoji = (cell as? TextView)?.text?.toString() ?: return@setOnClickListener
                    popup.dismiss()
                    onEmojiSelected(emoji)
                }
            }
        }
    }

    anchor.post {
        if (!anchor.isAttachedToWindow || !anchor.isEnabled) return@post
        val yOffset = if (openAbove) -measuredHeight - anchor.height else 0
        popup.showAsDropDown(
            anchor,
            0,
            yOffset,
            Gravity.CENTER_HORIZONTAL,
        )
    }
}

/** Animated non-authoritative reaction bubble layered over the board. */
internal fun animateQuickChatReaction(
    parent: FrameLayout,
    previous: View?,
    emoji: String,
    displayName: String,
): View {
    previous?.animate()?.cancel()
    (previous?.parent as? ViewGroup)?.removeView(previous)
    val context = parent.context
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).roundToInt()

    val bubble = TextView(context).apply {
        text = "$emoji  $displayName"
        gravity = Gravity.CENTER
        textSize = 20f
        setTextColor(0xFFF8FBFF.toInt())
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xF20B2E70.toInt(), 0xF2071D4C.toInt()),
        ).apply {
            cornerRadius = dp(18).toFloat()
            setStroke(dp(2), 0xFFFFD34E.toInt())
        }
        elevation = dp(10).toFloat()
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        alpha = 0f
        scaleX = .78f
        scaleY = .78f
        translationY = dp(12).toFloat()
    }
    parent.addView(
        bubble,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        ).apply {
            topMargin = dp(12)
        },
    )
    bubble.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .translationY(0f)
        .setDuration(170L)
        .withEndAction {
            bubble.animate()
                .alpha(0f)
                .translationY(-dp(44).toFloat())
                .setStartDelay(720L)
                .setDuration(420L)
                .withEndAction {
                    (bubble.parent as? ViewGroup)?.removeView(bubble)
                }
                .start()
        }
        .start()
    return bubble
}
