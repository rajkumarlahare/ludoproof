package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.storeTabs():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        StoreTab
            .entries
            .forEach {
                    tab ->
                val selected =
                    tab ==
                        selectedTab
                addView(
                    TextView(
                        this@storeTabs,
                    ).apply {
                        text =
                            tab.label
                        textSize =
                            13f
                        setTypeface(
                            Typeface.DEFAULT_BOLD,
                        )
                        setTextColor(
                            if (selected) {
                                Color.WHITE
                            } else {
                                0xFFC4D4E9.toInt()
                            },
                        )
                        gravity =
                            Gravity.CENTER
                        isClickable =
                            true
                        isFocusable =
                            true
                        contentDescription =
                            tab.label
                        background =
                            GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                if (selected) {
                                    intArrayOf(
                                        0xFF9D3BB0.toInt(),
                                        0xFF6E237D.toInt(),
                                    )
                                } else {
                                    intArrayOf(
                                        0xFF1B64BE.toInt(),
                                        0xFF0B438F.toInt(),
                                    )
                                },
                            ).apply {
                                cornerRadii =
                                    floatArrayOf(
                                        dp(10).toFloat(),
                                        dp(10).toFloat(),
                                        dp(10).toFloat(),
                                        dp(10).toFloat(),
                                        0f,
                                        0f,
                                        0f,
                                        0f,
                                    )
                                setStroke(
                                    dp(1),
                                    if (selected) {
                                        0xFFFF76C9.toInt()
                                    } else {
                                        0xFF4BA8F0.toInt()
                                    },
                                )
                            }
                        setOnClickListener {
                            selectTab(
                                tab,
                            )
                        }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f,
                    ).apply {
                        setMargins(
                            dp(2),
                            0,
                            dp(2),
                            0,
                        )
                    },
                )
            }
    }
