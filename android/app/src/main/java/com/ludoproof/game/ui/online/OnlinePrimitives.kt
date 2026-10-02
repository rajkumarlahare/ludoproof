package com.ludoproof.game.ui.online

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.concurrent.Executors
import com.ludoproof.game.*

internal fun MainActivity.infoText(
    value: String,
    size: Float,
): TextView =
    TextView(this).apply {
        text = value
        LudoProofTheme.body(
            this,
            size,
            centered = false,
        )
        setPadding(
            dp(4),
            dp(6),
            dp(4),
            dp(6),
        )
    }

internal fun MainActivity.button(
    label: String,
    action: () -> Unit,
): Button =
    Button(this).apply {
        text = label
        LudoProofTheme.secondary(
            this,
        )
        setOnClickListener {
            action()
        }
    }

internal fun MainActivity.dp(
    value: Int,
): Int =
    (
        value *
            resources
                .displayMetrics
                .density
        ).toInt()
