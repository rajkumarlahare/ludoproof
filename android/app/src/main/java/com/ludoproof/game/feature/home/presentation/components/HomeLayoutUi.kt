package com.ludoproof.game.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*

internal fun HomeActivity.fullWidthSection(
    topMarginDp: Int,
): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin =
            dp(topMarginDp)
    }

internal fun HomeActivity.weighted():
    LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        0,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        1f,
    )

internal fun HomeActivity.isCompact():
    Boolean =
    LudoProofTheme
        .isCompactWidth(this)

internal fun HomeActivity.dp(
    value: Int,
): Int =
    LudoProofTheme.dp(
        this,
        value,
    )
