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
