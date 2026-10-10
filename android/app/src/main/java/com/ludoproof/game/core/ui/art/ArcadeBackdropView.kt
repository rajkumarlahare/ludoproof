package com.ludoproof.game

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView

/**
 * Shared full-screen nature backdrop used across the regular UI and gameplay.
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
        useUiBackground()
    }

    /** Applies the dedicated art for an active match only. */
    fun useGameplayBackground() {
        applyBackdrop(
            drawableName = "ludo_paws_game_background",
            fallbackColor = 0xFF17261B.toInt(),
        )
    }

    /** Restores the shared nature backdrop used by non-gameplay screens. */
    fun useUiBackground() {
        applyBackdrop(
            drawableName = "ludo_paws_ui_background",
            fallbackColor = 0xFF275B19.toInt(),
        )
    }

    @Suppress("DiscouragedApi")
    private fun applyBackdrop(
        drawableName: String,
        fallbackColor: Int,
    ) {
        val drawableId =
            resources.getIdentifier(
                drawableName,
                "drawable",
                context.packageName,
            )
        if (drawableId != 0) {
            setBackgroundColor(0x00000000)
            setImageResource(drawableId)
        } else {
            setImageDrawable(null)
            setBackgroundColor(fallbackColor)
        }
    }
}
