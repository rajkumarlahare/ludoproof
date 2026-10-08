package com.ludoproof.game.ui.offline.gameplay

import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.OfflineGameActivity
import com.ludoproof.game.feature.settings.data.local.GameSoundFeedback
import com.ludoproof.game.ui.offline.common.*

/**
 * Presents one lightweight, non-authoritative emoji reaction above the board.
 * Gameplay status copy is intentionally left untouched.
 */
internal fun OfflineGameActivity.presentQuickReaction(
    emoji: String,
): Boolean {
    val now = SystemClock.elapsedRealtime()
    val presentation =
        OfflineQuickReactionPolicy.resolve(
            state = session.snapshot(),
            emoji = emoji,
            nowMs = now,
            lastShownAtMs = quickReactionLastShownAtMs,
        ) ?: return false
    val board = boardView ?: return false

    quickReactionView
        ?.animate()
        ?.cancel()
    (quickReactionView?.parent as? ViewGroup)
        ?.removeView(quickReactionView)

    val bubble =
        TextView(this).apply {
            text = "${presentation.emoji}  ${presentation.displayName}"
            gravity = Gravity.CENTER
            textSize = if (isCompactSetup()) 18f else 20f
            setTextColor(0xFFF8FBFF.toInt())
            setPadding(
                dp(if (isCompactSetup()) 12 else 14),
                dp(7),
                dp(if (isCompactSetup()) 12 else 14),
                dp(7),
            )
            background =
                LudoProofTheme.hudPanelDrawable(
                    this@presentQuickReaction,
                    goldBorder = true,
                )
            elevation = dp(10).toFloat()
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            alpha = 0f
            scaleX = 0.84f
            scaleY = 0.84f
            translationY = dp(12).toFloat()
        }

    board.addView(
        bubble,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        ).apply {
            topMargin = dp(if (isCompactSetup()) 10 else 14)
        },
    )

    quickReactionView = bubble
    quickReactionLastShownAtMs = now
    GameSoundFeedback.click(this)

    fun removeBubble() {
        if (quickReactionView === bubble) {
            quickReactionView = null
        }
        (bubble.parent as? ViewGroup)?.removeView(bubble)
    }

    bubble.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .translationY(0f)
        .setDuration(170L)
        .withEndAction {
            bubble.animate()
                .alpha(0f)
                .translationY(-dp(presentation.riseDp).toFloat())
                .setStartDelay(260L)
                .setDuration((presentation.durationMs - 430L).coerceAtLeast(500L))
                .withEndAction(::removeBubble)
                .start()
        }
        .start()


    return true
}
