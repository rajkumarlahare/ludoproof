package com.ludoproof.game.ui.home

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.HomeActivity
import com.ludoproof.game.R
import com.ludoproof.game.ui.dialogs.showSettingsDialog

internal fun HomeActivity.homeBottomActions(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        addView(
            bottomActionButton(
                label = "Rate on Google",
                description =
                    getString(R.string.rate_accessibility_label),
                iconKind = HomeIconKind.RATING,
                secondaryText = "★★★★★",
            ) {
                showHomeRatingDialog()
            },
            bottomActionParams(),
        )

        addView(
            bottomActionButton(
                label = "Share",
                description = "Share",
                iconKind = HomeIconKind.SHARE,
            ) {
                shareLudoPaws()
            },
            bottomActionParams(),
        )

        addView(
            bottomActionButton(
                label = "Settings",
                description = "Settings",
                iconKind = HomeIconKind.SETTINGS,
            ) {
                showSettingsDialog(
                    this@homeBottomActions,
                )
            },
            bottomActionParams(),
        )
    }

private fun HomeActivity.bottomActionParams(): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(
        0,
        dp(if (isCompact()) 50 else 56),
        1f,
    ).apply {
        leftMargin = dp(if (isCompact()) 2 else 3)
        rightMargin = dp(if (isCompact()) 2 else 3)
    }

private fun HomeActivity.bottomActionButton(
    label: String,
    description: String,
    iconKind: HomeIconKind,
    secondaryText: String? = null,
    action: () -> Unit,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = description
        background =
            homeGlassBackground(
                context = this@bottomActionButton,
                shape = HomeGlassShape.PILL,
            )
        elevation = dp(7).toFloat()
        setPadding(
            dp(if (isCompact()) 6 else 8),
            dp(4),
            dp(if (isCompact()) 6 else 8),
            dp(4),
        )
        setOnClickListener {
            action()
        }

        addView(
            HomeIconView(this@bottomActionButton).apply {
                kind = iconKind
                iconColor =
                    if (iconKind == HomeIconKind.RATING) {
                        0xFFFFD13B.toInt()
                    } else {
                        Color.WHITE
                    }
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 31 else 36),
                dp(if (isCompact()) 31 else 36),
            ).apply {
                marginEnd = dp(if (isCompact()) 3 else 5)
            },
        )

        addView(
            LinearLayout(this@bottomActionButton).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                addView(
                    TextView(this@bottomActionButton).apply {
                        text = label
                        textSize =
                            when {
                                isCompact() && label.length > 8 -> 8.5f
                                label.length > 8 -> 9.5f
                                isCompact() -> 11f
                                else -> 12.5f
                            }
                        setTypeface(Typeface.DEFAULT_BOLD)
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        maxLines = 1
                        setShadowLayer(
                            2f,
                            0f,
                            dp(1).toFloat(),
                            0xC0000000.toInt(),
                        )
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )

                if (secondaryText != null) {
                    addView(
                        TextView(this@bottomActionButton).apply {
                            text = secondaryText
                            textSize = if (isCompact()) 8.5f else 9.5f
                            setTypeface(Typeface.DEFAULT_BOLD)
                            setTextColor(0xFFFFCF32.toInt())
                            letterSpacing = 0.01f
                            gravity = Gravity.CENTER
                            maxLines = 1
                            setShadowLayer(
                                1.5f,
                                0f,
                                dp(1).toFloat(),
                                0xB0000000.toInt(),
                            )
                        },
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                }
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
    }
