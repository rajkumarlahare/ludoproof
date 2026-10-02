package com.ludoproof.game.ui.home

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import com.ludoproof.game.HomeActivity
import com.ludoproof.game.ui.dialogs.showSettingsDialog

internal fun HomeActivity.homeBottomActions():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            bottomActionButton(
                symbol = "★",
                description =
                    "Favorites",
            ) {
                // Favorites logic will be connected later.
            },
            bottomActionParams(),
        )

        addView(
            bottomActionButton(
                symbol = "↗",
                description =
                    "Share",
            ) {
                shareLudoProof()
            },
            bottomActionParams(),
        )

        addView(
            bottomActionButton(
                symbol = "⚙",
                description =
                    "Settings",
            ) {
                showSettingsDialog(
                    this@homeBottomActions,
                )
            },
            bottomActionParams(),
        )
    }

private fun HomeActivity.bottomActionParams():
    LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        0,
        dp(if (isCompact()) 50 else 56),
        1f,
    ).apply {
        leftMargin =
            dp(if (isCompact()) 18 else 28)
        rightMargin =
            dp(if (isCompact()) 18 else 28)
    }

private fun HomeActivity.bottomActionButton(
    symbol: String,
    description: String,
    action: () -> Unit,
): Button =
    Button(this).apply {
        text =
            symbol
        textSize =
            if (isCompact()) {
                23f
            } else {
                26f
            }
        setTypeface(
            Typeface.DEFAULT_BOLD,
        )
        setTextColor(
            Color.WHITE,
        )
        gravity =
            Gravity.CENTER
        minWidth =
            0
        minHeight =
            0
        setPadding(
            0,
            0,
            0,
            0,
        )
        contentDescription =
            description
        background =
            bottomActionDrawable()
        elevation =
            dp(6).toFloat()
        setOnClickListener {
            action()
        }
    }

private fun HomeActivity.bottomActionDrawable():
    StateListDrawable =
    StateListDrawable().apply {
        addState(
            intArrayOf(
                android.R.attr.state_pressed,
            ),
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFFFFB71B.toInt(),
                    0xFFF58400.toInt(),
                    0xFFD96100.toInt(),
                ),
            ).apply {
                shape =
                    GradientDrawable.OVAL
                setStroke(
                    dp(2),
                    0xFFFFD765.toInt(),
                )
            },
        )
        addState(
            intArrayOf(),
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFFFFD23A.toInt(),
                    0xFFFF9800.toInt(),
                    0xFFE76B00.toInt(),
                ),
            ).apply {
                shape =
                    GradientDrawable.OVAL
                setStroke(
                    dp(2),
                    0xFFFFE88A.toInt(),
                )
            },
        )
    }
