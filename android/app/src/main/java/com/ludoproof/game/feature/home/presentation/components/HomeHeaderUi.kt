package com.ludoproof.game.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.HomeActivity
import com.ludoproof.game.feature.leaderboard.presentation.LeaderboardActivity
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.presentation.ProfileActivity
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun HomeActivity.profileHud(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            dp(if (isCompact()) 2 else 4),
            dp(2),
            dp(if (isCompact()) 2 else 4),
            dp(4),
        )

        addView(
            homeIdentityRow(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        addView(
            homeShortcutRow(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(if (isCompact()) 8 else 10)
            },
        )

        connectivityText =
            TextView(this@profileHud).apply {
                text = "● CHECKING"
                visibility = View.GONE
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        addView(connectivityText)
    }

private fun HomeActivity.homeIdentityRow(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(if (isCompact()) 48 else 52)

        addView(
            homeCurrencyPill(),
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 128 else 142),
                dp(if (isCompact()) 46 else 50),
            ),
        )

        addView(
            View(this@homeIdentityRow),
            LinearLayout.LayoutParams(
                0,
                1,
                1f,
            ),
        )

        addView(
            homeProfilePill(),
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 174 else 192),
                dp(if (isCompact()) 46 else 50),
            ),
        )
    }

private fun HomeActivity.homeCurrencyPill(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        contentDescription = "Open gem store"
        background =
            homeGlassBackground(
                context = this@homeCurrencyPill,
                shape = HomeGlassShape.PILL,
            )
        elevation = dp(7).toFloat()
        setPadding(
            dp(5),
            dp(4),
            dp(4),
            dp(4),
        )
        setOnClickListener {
            startActivity(
                Intent(
                    this@homeCurrencyPill,
                    StoreActivity::class.java,
                ),
            )
        }

        addView(
            HomeIconView(this@homeCurrencyPill).apply {
                kind = HomeIconKind.GEM
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 34 else 38),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            TextView(this@homeCurrencyPill).apply {
                homeGemBalanceText = this
                text =
                    GemWalletStore(this@homeCurrencyPill)
                        .balance()
                        .toString()
                textSize = if (isCompact()) 18f else 20f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setShadowLayer(
                    2.4f,
                    0f,
                    dp(1).toFloat(),
                    0xB0000000.toInt(),
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            ),
        )

        addView(
            HomeIconView(this@homeCurrencyPill).apply {
                kind = HomeIconKind.ADD
                iconColor = Color.WHITE
                isClickable = true
                isFocusable = true
                contentDescription = "Add currency"
                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8),
                )
                background =
                    homeGlassBackground(
                        context = this@homeCurrencyPill,
                        shape = HomeGlassShape.CIRCLE,
                        tone = HomeGlassTone.BLUE,
                    )
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@homeCurrencyPill,
                            StoreActivity::class.java,
                        ),
                    )
                }
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 34 else 38),
                dp(if (isCompact()) 34 else 38),
            ),
        )
    }

private fun HomeActivity.homeProfilePill(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        contentDescription = "Guest user profile"
        background =
            homeGlassBackground(
                context = this@homeProfilePill,
                shape = HomeGlassShape.PILL,
            )
        elevation = dp(7).toFloat()
        setPadding(
            dp(4),
            dp(4),
            dp(7),
            dp(4),
        )
        setOnClickListener {
            startActivity(
                Intent(
                    this@homeProfilePill,
                    ProfileActivity::class.java,
                ),
            )
        }

        addView(
            HomeIconView(this@homeProfilePill).apply {
                kind = HomeIconKind.PROFILE
                iconColor = Color.WHITE
                setPadding(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6),
                )
                background =
                    homeGlassBackground(
                        context = this@homeProfilePill,
                        shape = HomeGlassShape.CIRCLE,
                        tone = HomeGlassTone.PURPLE,
                    )
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 38 else 42),
                dp(if (isCompact()) 38 else 42),
            ),
        )

        addView(
            TextView(this@homeProfilePill).apply {
                homeProfileNameText = this
                text = homeDisplayName()
                textSize = if (isCompact()) 14f else 16f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                maxLines = 1
                setShadowLayer(
                    2.4f,
                    0f,
                    dp(1).toFloat(),
                    0xB0000000.toInt(),
                )
                setPadding(dp(6), 0, dp(2), 0)
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            ),
        )

        addView(
            HomeIconView(this@homeProfilePill).apply {
                kind = HomeIconKind.EDIT
                iconColor = Color.WHITE
                setPadding(
                    dp(5),
                    dp(5),
                    dp(5),
                    dp(5),
                )
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 28 else 30),
                dp(if (isCompact()) 28 else 30),
            ),
        )
    }

private fun HomeActivity.homeShortcutRow(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER

        listOf(
            HeaderShortcut(
                label = "Shop",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.SHOP,
            ),
            HeaderShortcut(
                label = "Badge",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.BADGE,
            ),
            HeaderShortcut(
                label = "Leaderboard",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.LEADERBOARD,
            ),
            HeaderShortcut(
                label = "Remove Ads",
                tint = 0xFFFF3B30.toInt(),
                iconKind = HomeIconKind.AD_BLOCKER,
            ),
        ).forEach { item ->
            addView(
                homeShortcut(item),
                LinearLayout.LayoutParams(
                    0,
                    dp(if (isCompact()) 84 else 92),
                    1f,
                ).apply {
                    leftMargin = dp(2)
                    rightMargin = dp(2)
                },
            )
        }
    }

private fun HomeActivity.homeShortcut(
    item: HeaderShortcut,
): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = item.label
        background =
            homeGlassBackground(
                context = this@homeShortcut,
                shape = HomeGlassShape.TILE,
            )
        elevation = dp(7).toFloat()
        setPadding(
            dp(3),
            dp(if (isCompact()) 5 else 6),
            dp(3),
            dp(5),
        )
        setOnClickListener {
            when (item.iconKind) {
                HomeIconKind.SHOP ->
                    startActivity(
                        Intent(
                            this@homeShortcut,
                            StoreActivity::class.java,
                        ),
                    )

                HomeIconKind.LEADERBOARD ->
                    startActivity(
                        Intent(
                            this@homeShortcut,
                            LeaderboardActivity::class.java,
                        ),
                    )

                else -> Unit
            }
        }

        addView(
            HomeIconView(this@homeShortcut).apply {
                kind = item.iconKind
                iconColor = item.tint
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 48 else 54),
                dp(if (isCompact()) 48 else 54),
            ),
        )

        addView(
            TextView(this@homeShortcut).apply {
                text = item.label
                textSize =
                    when {
                        isCompact() && item.label.length > 10 -> 8.5f
                        isCompact() -> 10f
                        item.label.length > 10 -> 9.5f
                        else -> 11f
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
                0,
                1f,
            ),
        )
    }

private fun HomeActivity.homeDisplayName(): String {
    val name =
        ProfileStore(this)
            .snapshot()
            .displayName
            .trim()
    return when {
        name.isBlank() -> "Guest User"
        name.equals("Guest", ignoreCase = true) -> "Guest User"
        else -> name
    }
}

internal fun HomeActivity.refreshHomeProfileSummary() {
    homeProfileNameText?.text = homeDisplayName()
}

internal fun HomeActivity.refreshHomeGemBalance() {
    homeGemBalanceText?.text =
        GemWalletStore(this)
            .balance()
            .toString()
}

private data class HeaderShortcut(
    val label: String,
    val tint: Int,
    val iconKind: HomeIconKind,
)
