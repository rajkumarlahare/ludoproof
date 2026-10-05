package com.ludoproof.game

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView

/**
 * Lightweight board-level quick reaction bubble shared by local and online play.
 *
 * This view is presentation-only. Reactions never mutate match state and can be
 * freely replaced or restyled without touching Ludo rules/network authority.
 */
class LudoPawsQuickReactionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextView(context, attrs) {
    private var reducedMotion = false

    private val hideRunnable =
        Runnable {
            if (reducedMotion) {
                resetHidden()
                return@Runnable
            }
            animate()
                .cancel()
            animate()
                .alpha(0f)
                .scaleX(.86f)
                .scaleY(.86f)
                .translationY(-density(22f))
                .setDuration(EXIT_DURATION_MILLIS)
                .withEndAction(::resetHidden)
                .start()
        }

    init {
        gravity = Gravity.CENTER
        textSize = 38f
        setTextColor(Color.WHITE)
        setPadding(
            density(13f).toInt(),
            density(8f).toInt(),
            density(13f).toInt(),
            density(8f).toInt(),
        )
        background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = density(22f)
                setColor(0xCC111827.toInt())
                setStroke(
                    density(1f).toInt().coerceAtLeast(1),
                    0x66FFFFFF,
                )
            }
        elevation = density(10f)
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        resetHidden()
    }

    fun showReaction(
        emoji: String,
        reducedMotion: Boolean,
    ) {
        if (emoji.isBlank()) return

        this.reducedMotion = reducedMotion
        removeCallbacks(hideRunnable)
        animate().cancel()
        text = emoji
        visibility = View.VISIBLE
        translationY = 0f

        if (reducedMotion) {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
            postDelayed(
                hideRunnable,
                HOLD_DURATION_MILLIS,
            )
            return
        }

        alpha = 0f
        scaleX = .72f
        scaleY = .72f
        translationY = density(14f)
        animate()
            .alpha(1f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .translationY(0f)
            .setDuration(ENTER_DURATION_MILLIS)
            .withEndAction {
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(SETTLE_DURATION_MILLIS)
                    .start()
                postDelayed(
                    hideRunnable,
                    HOLD_DURATION_MILLIS,
                )
            }
            .start()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideRunnable)
        animate().cancel()
        resetHidden()
        super.onDetachedFromWindow()
    }

    private fun resetHidden() {
        animate().cancel()
        visibility = View.GONE
        alpha = 0f
        scaleX = 1f
        scaleY = 1f
        translationY = 0f
    }

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density

    companion object {
        private const val ENTER_DURATION_MILLIS = 150L
        private const val SETTLE_DURATION_MILLIS = 90L
        private const val HOLD_DURATION_MILLIS = 650L
        private const val EXIT_DURATION_MILLIS = 220L
    }
}
