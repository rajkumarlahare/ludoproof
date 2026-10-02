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

internal fun metricCard(
    context: Context,
    label: String,
    value: String,
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
                5,
            ),
            dp(
                context,
                8,
            ),
            dp(
                context,
                5,
            ),
            dp(
                context,
                8,
            ),
        )
        background =
            LudoProofTheme
                .rounded(
                    0xE807204E.toInt(),
                    14f,
                    0x6655E3FF,
                    1f,
                    context,
                )

        addView(
            TextView(
                context,
            ).apply {
                text =
                    value
                LudoProofTheme.title(
                    this,
                    15f,
                    gold = true,
                )
            },
        )
        addView(
            TextView(
                context,
            ).apply {
                text =
                    label
                LudoProofTheme.body(
                    this,
                    9f,
                    centered = true,
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
    }

internal fun metricParams(
    context: Context,
):
    LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f,
    ).apply {
        setMargins(
            dp(
                context,
                4,
            ),
            0,
            dp(
                context,
                4,
            ),
            0,
        )
    }

internal fun statusChip(
    context: Context,
    value: String,
    color: Int,
): TextView =
    TextView(
        context,
    ).apply {
        text =
            value
        LudoProofTheme.body(
            this,
            10.5f,
            centered = true,
            bright = true,
        )
        setTextColor(
            color,
        )
        gravity =
            Gravity.CENTER
        setPadding(
            dp(
                context,
                12,
            ),
            dp(
                context,
                7,
            ),
            dp(
                context,
                12,
            ),
            dp(
                context,
                7,
            ),
        )
        background =
            LudoProofTheme
                .rounded(
                    0xDD061B42.toInt(),
                    999f,
                    color,
                    1f,
                    context,
                )
    }

internal fun shortDigest(
    value: String,
): String =
    if (
        value.length <=
        20
    ) {
        value
    } else {
        value.take(
            10,
        ) +
            "…" +
            value.takeLast(
                10,
            )
    }

internal fun dialogPanel(
    context: Context,
    title: String,
    subtitle: String,
    dialog: Dialog,
): LinearLayout =
    LinearLayout(
        context,
    ).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(
                context,
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            context,
                        )
                ) {
                    12
                } else {
                    16
                },
            ),
            dp(
                context,
                12,
            ),
            dp(
                context,
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            context,
                        )
                ) {
                    12
                } else {
                    16
                },
            ),
            dp(
                context,
                16,
            ),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(
                    context,
                    goldBorder =
                        true,
                )

        val header =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
            TextView(
                context,
            ).apply {
                text =
                    "LP"
                LudoProofTheme.title(
                    this,
                    13f,
                    gold = true,
                )
                gravity =
                    Gravity.CENTER
                background =
                    LudoProofTheme
                        .brandBadgeDrawable(
                            context,
                        )
            },
            LinearLayout.LayoutParams(
                dp(
                    context,
                    44,
                ),
                dp(
                    context,
                    44,
                ),
            ),
        )

        header.addView(
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL

                addView(
                    TextView(
                        context,
                    ).apply {
                        text =
                            title
                        LudoProofTheme.title(
                            this,
                            if (
                                LudoProofTheme
                                    .isCompactWidth(
                                        context,
                                    )
                            ) {
                                18f
                            } else {
                                21f
                            },
                            gold = true,
                        )
                        gravity =
                            Gravity.START or
                                Gravity.CENTER_VERTICAL
                    },
                )
                addView(
                    TextView(
                        context,
                    ).apply {
                        text =
                            subtitle
                        LudoProofTheme.body(
                            this,
                            9.5f,
                            bright = true,
                        )
                        setTextColor(
                            0xFF70E7FF.toInt(),
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
                        10,
                    )
            },
        )

        header.addView(
            Button(
                context,
            ).apply {
                LudoProofTheme
                    .homeCircularAction(
                        this,
                        "×",
                    )
                contentDescription =
                    "Close"
                setOnClickListener {
                    dialog.dismiss()
                }
            },
            LinearLayout.LayoutParams(
                dp(
                    context,
                    44,
                ),
                dp(
                    context,
                    44,
                ),
            ),
        )
        addView(
            header,
        )
    }

internal fun settingsRow(
    context: Context,
    name: String,
    value: String,
    detail: String,
): LinearLayout =
    LinearLayout(
        context,
    ).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(
                context,
                12,
            ),
            dp(
                context,
                10,
            ),
            dp(
                context,
                12,
            ),
            dp(
                context,
                10,
            ),
        )
        background =
            LudoProofTheme
                .rounded(
                    0xE807204E.toInt(),
                    16f,
                    0x6655E3FF,
                    1f,
                    context,
                )

        val top =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        top.addView(
            TextView(
                context,
            ).apply {
                text =
                    name
                LudoProofTheme.body(
                    this,
                    14f,
                    bright = true,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        top.addView(
            TextView(
                context,
            ).apply {
                text =
                    value
                LudoProofTheme.body(
                    this,
                    10.5f,
                    bright = true,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                setPadding(
                    dp(
                        context,
                        10,
                    ),
                    dp(
                        context,
                        5,
                    ),
                    dp(
                        context,
                        10,
                    ),
                    dp(
                        context,
                        5,
                    ),
                )
                background =
                    LudoProofTheme
                        .rounded(
                            0xDD07183D.toInt(),
                            999f,
                            0x88FFD45E.toInt(),
                            1f,
                            context,
                        )
            },
        )
        addView(
            top,
        )

        addView(
            TextView(
                context,
            ).apply {
                text =
                    detail
                LudoProofTheme.body(
                    this,
                    10f,
                    bright = true,
                )
                setPadding(
                    0,
                    dp(
                        context,
                        5,
                    ),
                    0,
                    0,
                )
            },
        )
    }

internal fun fullWidthParams(
    context: Context,
    topDp: Int,
):
    LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(
                context,
                topDp,
            )
    }

internal fun baseDialog(
    context: Context,
): Dialog =
    Dialog(
        context,
    ).apply {
        requestWindowFeature(
            Window.FEATURE_NO_TITLE,
        )
        window
            ?.setBackgroundDrawable(
                ColorDrawable(
                    Color.TRANSPARENT,
                ),
            )
        window
            ?.addFlags(
                WindowManager.LayoutParams
                    .FLAG_DIM_BEHIND,
            )
        window
            ?.attributes =
            window
                ?.attributes
                ?.apply {
                    dimAmount =
                        .78f
                }
    }

internal fun sizeDialog(
    dialog: Dialog,
    widthFraction: Float,
) {
    dialog.setOnShowListener {
        val context =
            dialog.context
        val metrics =
            context.resources
                .displayMetrics
        val desired =
            (
                metrics.widthPixels *
                    widthFraction
                ).toInt()
        val maxWidth =
            dp(
                context,
                LudoProofTheme
                    .pageMaxContentWidthDp(
                        context,
                    ),
            )

        dialog.window
            ?.setLayout(
                minOf(
                    desired,
                    maxWidth,
                ),
                WindowManager
                    .LayoutParams
                    .WRAP_CONTENT,
            )
    }
}

internal fun dp(
    context: Context,
    value: Int,
): Int =
    LudoProofTheme.dp(
        context,
        value,
    )
