package com.ludoproof.game.feature.profile.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.domain.model.ProfileSnapshot
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profilePurchasesPanel(
    profile: ProfileSnapshot,
):
    LinearLayout =
    profilePanel().apply {
        val heading =
            LinearLayout(
                this@profilePurchasesPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        heading.addView(
            profileTitle(
                "Purchased Products",
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        heading.addView(
            Button(
                this@profilePurchasesPanel,
            ).apply {
                text =
                    "View All"
                textSize =
                    12f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                minWidth =
                    0
                minHeight =
                    0
                background =
                    null
                setOnClickListener {
                    viewPurchases(
                        if (
                            profile.purchasedProducts
                                .isEmpty()
                        ) {
                            "No purchases yet."
                        } else {
                            profile.purchasedProducts
                                .joinToString(
                                    "\n",
                                ) {
                                    "• $it"
                                }
                        },
                    )
                }
            },
        )
        addView(
            heading,
        )

        addView(
            TextView(
                this@profilePurchasesPanel,
            ).apply {
                text =
                    if (
                        profile.purchasedProducts
                            .isEmpty()
                    ) {
                        "No purchases yet"
                    } else {
                        profile.purchasedProducts
                            .joinToString(
                                "  •  ",
                            )
                    }
                textSize =
                    14f
                setTextColor(
                    0xFFE8F5FF.toInt(),
                )
                gravity =
                    Gravity.CENTER
                setPadding(
                    0,
                    dp(36),
                    0,
                    dp(32),
                )
            },
        )
    }
