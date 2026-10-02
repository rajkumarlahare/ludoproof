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

internal fun HomeActivity.profileHud():
    LinearLayout =
    if (isCompact()) {
        compactProfileHud()
    } else {
        regularProfileHud()
    }

internal fun HomeActivity.compactProfileHud():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(14),
            dp(14),
            dp(14),
            dp(12),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(this@compactProfileHud)
        elevation =
            dp(5).toFloat()

        val identityRow =
            LinearLayout(this@compactProfileHud).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }
        identityRow.addView(
            avatar(),
            LinearLayout.LayoutParams(
                dp(52),
                dp(52),
            ),
        )
        identityRow.addView(
            identityBlock(),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart =
                    dp(11)
            },
        )
        addView(identityRow)

        addView(
            connectivityChip(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42),
            ).apply {
                topMargin =
                    dp(10)
            },
        )
    }

internal fun HomeActivity.regularProfileHud():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14),
        )
        background =
            LudoProofTheme
                .hudPanelDrawable(this@regularProfileHud)
        elevation =
            dp(5).toFloat()

        addView(
            avatar(),
            LinearLayout.LayoutParams(
                dp(58),
                dp(58),
            ),
        )
        addView(
            identityBlock(),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart =
                    dp(12)
            },
        )
        addView(
            connectivityChip(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(42),
            ),
        )
    }

internal fun HomeActivity.avatar():
    TextView =
    TextView(this).apply {
        text = "LP"
        LudoProofTheme.title(
            this,
            17f,
            gold = true,
        )
        gravity =
            Gravity.CENTER
        contentDescription =
            "LudoProof"
        background =
            LudoProofTheme
                .brandBadgeDrawable(
                    this@avatar,
                )
        elevation =
            dp(7).toFloat()
    }

internal fun HomeActivity.identityBlock():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER_VERTICAL

        val brandRow =
            LinearLayout(
                this@identityBlock,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        brandRow.addView(
            TextView(this@identityBlock).apply {
                text =
                    "LUDO"
                LudoProofTheme.title(
                    this,
                    if (isCompact()) 18f else 21f,
                )
                gravity =
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
            },
        )
        brandRow.addView(
            TextView(this@identityBlock).apply {
                text =
                    "PROOF"
                LudoProofTheme.title(
                    this,
                    if (isCompact()) 18f else 21f,
                    gold = true,
                )
                gravity =
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
            },
        )
        addView(brandRow)

        addView(
            TextView(this@identityBlock).apply {
                text =
                    "VERIFIABLE PLAY • ENTRONEX V4"
                LudoProofTheme.body(
                    this,
                    if (isCompact()) 9.5f else 11f,
                    bright = true,
                )
                setTextColor(
                    0xFF5FE4FF.toInt(),
                )
                setPadding(
                    0,
                    dp(2),
                    0,
                    0,
                )
            },
        )
    }

internal fun HomeActivity.connectivityChip():
    TextView =
    TextView(this).apply {
        connectivityText =
            this
        text =
            "● CHECKING"
        LudoProofTheme.body(
            this,
            11f,
            centered = true,
            bright = true,
        )
        gravity =
            Gravity.CENTER
        setPadding(
            dp(13),
            0,
            dp(13),
            0,
        )
        background =
            LudoProofTheme
                .rounded(
                    0xE4071739.toInt(),
                    18f,
                    0x6647D7FF,
                    1f,
                    this@connectivityChip,
                )
        accessibilityLiveRegion =
            View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
