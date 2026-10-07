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
        applyUiBackdrop()
    }

    @Suppress("DiscouragedApi")
    private fun applyUiBackdrop() {
        val drawableId =
            resources.getIdentifier(
                "ludo_paws_ui_background",
                "drawable",
                context.packageName,
            )
        if (drawableId != 0) {
            setBackgroundColor(0x00000000)
            setImageResource(drawableId)
        } else {
            setImageDrawable(null)
            setBackgroundColor(0xFF275B19.toInt())
        }
    }
}
