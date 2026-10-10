package com.ludoproof.game

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView

/**
 * Shared full-screen backdrop used by every screen, including active gameplay.
 *
 * Both legacy entry points intentionally resolve to the same optimized WebP
 * so switching between UI states cannot switch the visual theme.
 */
class ArcadeBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ImageView(context, attrs) {
    init {
        scaleType = ScaleType.CENTER_CROP
        adjustViewBounds = false
        isClickable = false
        isFocusable = false
        contentDescription = null
        importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_NO
        applySharedBackdrop()
    }

    /** Compatibility entry point used by gameplay callers. */
    fun useGameplayBackground() {
        applySharedBackdrop()
    }

    /** Compatibility entry point used by setup and the rest of the UI. */
    fun useUiBackground() {
        applySharedBackdrop()
    }

    @Suppress("DiscouragedApi")
    private fun applySharedBackdrop() {
        val drawableId =
            resources.getIdentifier(
                "ludo_paws_game_background",
                "drawable",
                context.packageName,
            )
        if (drawableId != 0) {
            setBackgroundColor(0x00000000)
            setImageResource(drawableId)
        } else {
            setImageDrawable(null)
            setBackgroundColor(0xFF17261B.toInt())
        }
    }
}
