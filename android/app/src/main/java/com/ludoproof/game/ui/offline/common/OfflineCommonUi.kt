package com.ludoproof.game.ui.offline.common

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*

internal fun OfflineGameActivity.backHeader(label: String): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(
            Button(this@backHeader).apply {
                LudoProofTheme.homeCircularAction(this, "‹")
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(dp(52), dp(52)),
        )
        addView(
            TextView(this@backHeader).apply {
                text = label
                LudoProofTheme.body(this, 14f, centered = true, bright = true)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                background =
                    LudoProofTheme.darkPanelDrawable(
                        this@backHeader,
                        goldBorder = true,
                    )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply { marginStart = dp(12) },
        )
    }

internal fun OfflineGameActivity.isCompactSetup():
    Boolean =
    LudoProofTheme
        .isCompactWidth(this)

internal fun OfflineGameActivity.dp(value: Int): Int =
    LudoProofTheme.dp(this, value)
