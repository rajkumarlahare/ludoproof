package com.ludoproof.game

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object LudoProofTheme {
    const val BLUE_DARK = 0xFF053A91.toInt()
    const val BLUE = 0xFF0754C8.toInt()
    const val BLUE_CARD = 0xFF0A49AD.toInt()
    const val BLUE_CARD_ALT = 0xFF0B4097.toInt()
    const val ORANGE = 0xFFFFA000.toInt()
    const val GREEN = 0xFF39B54A.toInt()
    const val YELLOW = 0xFFFFC107.toInt()
    const val WHITE = Color.WHITE
    const val TEXT_MUTED = 0xFFC9D9F6.toInt()
    const val RED = 0xFFDC3545.toInt()

    fun screen(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BLUE_DARK)
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 28))
        }

    fun title(view: TextView, size: Float = 30f) {
        view.textSize = size
        view.setTextColor(WHITE)
        view.gravity = Gravity.CENTER_HORIZONTAL
    }

    fun body(view: TextView, size: Float = 14f, centered: Boolean = false) {
        view.textSize = size
        view.setTextColor(TEXT_MUTED)
        if (centered) view.gravity = Gravity.CENTER_HORIZONTAL
    }

    fun input(view: EditText) {
        view.setTextColor(WHITE)
        view.setHintTextColor(0xFF9DB9EA.toInt())
        view.background = rounded(BLUE_CARD_ALT, 12f, 0xFF2D6ED1.toInt(), 1f, view.context)
        view.setPadding(dp(view.context, 14), dp(view.context, 12), dp(view.context, 14), dp(view.context, 12))
    }

    fun primary(button: Button) =
        buttonStyle(button, ORANGE)

    fun secondary(button: Button) =
        buttonStyle(button, BLUE)

    fun positive(button: Button) =
        buttonStyle(button, GREEN)

    fun danger(button: Button) =
        buttonStyle(button, RED)

    private fun buttonStyle(button: Button, color: Int) {
        button.isAllCaps = false
        button.setTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf(android.R.attr.state_enabled),
                ),
                intArrayOf(0xFF8FA9D6.toInt(), WHITE),
            ),
        )
        button.background = rounded(color, 18f, 0x66FFFFFF, 1f, button.context)
        button.minHeight = dp(button.context, 50)
        button.setPadding(dp(button.context, 14), dp(button.context, 8), dp(button.context, 14), dp(button.context, 8))
    }

    fun card(view: View, alternate: Boolean = false) {
        view.background = rounded(
            if (alternate) BLUE_CARD_ALT else BLUE_CARD,
            16f,
            0x554D8BEA,
            1f,
            view.context,
        )
    }

    fun chip(view: View, color: Int = BLUE) {
        view.background = rounded(color, 999f, 0x66FFFFFF, 1f, view.context)
    }

    fun rounded(
        fill: Int,
        radiusDp: Float,
        stroke: Int = Color.TRANSPARENT,
        strokeDp: Float = 0f,
        context: Context,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(context, radiusDp).toFloat()
            if (strokeDp > 0f) {
                setStroke(dp(context, strokeDp), stroke)
            }
        }

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
