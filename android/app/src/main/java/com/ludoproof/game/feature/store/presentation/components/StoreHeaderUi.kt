package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.presentation.art.StoreAwningView

internal fun StoreActivity.storeHeader():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            Button(
                this@storeHeader,
            ).apply {
                text =
                    "‹"
                textSize =
                    30f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minWidth =
                    0
                minHeight =
                    0
                setPadding(
                    0,
                    0,
                    0,
                    dp(3),
                )
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            0xFFFFC928.toInt(),
                            0xFFF07A00.toInt(),
                        ),
                    ).apply {
                        shape =
                            GradientDrawable.OVAL
                        setStroke(
                            dp(2),
                            0xFFFFE47A.toInt(),
                        )
                    }
                contentDescription =
                    "Back"
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                dp(50),
                dp(50),
            ),
        )

        addView(
            TextView(
                this@storeHeader,
            ),
            LinearLayout.LayoutParams(
                0,
                1,
                1f,
            ),
        )

        val gemPill =
            LinearLayout(
                this@storeHeader,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                background =
                    LudoProofTheme
                        .rounded(
                            0xF307173C.toInt(),
                            999f,
                            0x44000000,
                            1f,
                            this@storeHeader,
                        )
            }

        gemPill.addView(
            TextView(
                this@storeHeader,
            ).apply {
                text =
                    "◆"
                textSize =
                    24f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF48F36B.toInt(),
                )
                gravity =
                    Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                dp(40),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        gemPill.addView(
            TextView(
                this@storeHeader,
            ).apply {
                text =
                    wallet
                        .balance()
                        .toString()
                textSize =
                    19f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            ),
        )

        gemPill.addView(
            TextView(
                this@storeHeader,
            ).apply {
                text =
                    "+"
                textSize =
                    27f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF7B5510.toInt(),
                )
                gravity =
                    Gravity.CENTER
                background =
                    LudoProofTheme
                        .rounded(
                            0xFFFFCA28.toInt(),
                            9f,
                            0xFFFFE37C.toInt(),
                            1f,
                            this@storeHeader,
                        )
            },
            LinearLayout.LayoutParams(
                dp(42),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            gemPill,
            LinearLayout.LayoutParams(
                dp(132),
                dp(48),
            ),
        )
    }

internal fun StoreActivity.storefrontAwning():
    StoreAwningView =
    StoreAwningView(this)
