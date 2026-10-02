package com.ludoproof.game.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*

internal fun evidenceScroll(
    context: Context,
    body: String,
    heightDp: Int,
): ScrollView =
    ScrollView(
        context,
    ).apply {
        isVerticalScrollBarEnabled =
            true
        overScrollMode =
            View.OVER_SCROLL_IF_CONTENT_SCROLLS
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    context,
                    goldBorder =
                        false,
                )

        addView(
            TextView(
                context,
            ).apply {
                text =
                    body
                setTextIsSelectable(
                    true,
                )
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = false,
                    bright = true,
                )
                setPadding(
                    dp(
                        context,
                        14,
                    ),
                    dp(
                        context,
                        14,
                    ),
                    dp(
                        context,
                        14,
                    ),
                    dp(
                        context,
                        14,
                    ),
                )
                setLineSpacing(
                    0f,
                    1.12f,
                )
            },
        )

        layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(
                    context,
                    heightDp,
                ),
            )
    }

internal fun emptyState(
    context: Context,
    icon: String,
    title: String,
    body: String,
): LinearLayout =
    LinearLayout(
        context,
    ).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(
                context,
                16,
            ),
            dp(
                context,
                22,
            ),
            dp(
                context,
                16,
            ),
            dp(
                context,
                22,
            ),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    context,
                    goldBorder =
                        false,
                )

        addView(
            TextView(
                context,
            ).apply {
                text =
                    icon
                textSize =
                    32f
                gravity =
                    Gravity.CENTER
                setTextColor(
                    0xFF70E7FF.toInt(),
                )
            },
        )
        addView(
            TextView(
                context,
            ).apply {
                text =
                    title
                LudoProofTheme.title(
                    this,
                    17f,
                    gold = true,
                )
                setPadding(
                    0,
                    dp(
                        context,
                        6,
                    ),
                    0,
                    0,
                )
            },
        )
        addView(
            TextView(
                context,
            ).apply {
                text =
                    body
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(
                        context,
                        8,
                    ),
                    dp(
                        context,
                        6,
                    ),
                    dp(
                        context,
                        8,
                    ),
                    0,
                )
            },
        )
    }

internal fun trustStrip(
    context: Context,
    title: String,
    body: String,
): LinearLayout =
    LinearLayout(
        context,
    ).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        setPadding(
            dp(
                context,
                12,
            ),
            dp(
                context,
                11,
            ),
            dp(
                context,
                12,
            ),
            dp(
                context,
                11,
            ),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    context,
                    goldBorder =
                        true,
                )

        addView(
            TextView(
                context,
            ).apply {
                text =
                    "✓"
                textSize =
                    22f
                gravity =
                    Gravity.CENTER
                setTextColor(
                    0xFF68F053.toInt(),
                )
            },
            LinearLayout.LayoutParams(
                dp(
                    context,
                    34,
                ),
                dp(
                    context,
                    34,
                ),
            ),
        )

        addView(
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL

                addView(
                    TextView(
                        context,
                    ).apply {
                        text =
                            title
                        LudoProofTheme.body(
                            this,
                            11f,
                            bright = true,
                        )
                        setTextColor(
                            LudoProofTheme.GOLD,
                        )
                    },
                )
                addView(
                    TextView(
                        context,
                    ).apply {
                        text =
                            body
                        LudoProofTheme.body(
                            this,
                            10.5f,
                            bright = true,
                        )
                        setPadding(
                            0,
                            dp(
                                context,
                                2,
                            ),
                            0,
                            0,
                        )
                    },
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart =
                    dp(
                        context,
                        8,
                    )
            },
        )
    }
