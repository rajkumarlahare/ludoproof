package com.ludoproof.game.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.HomeActivity
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.profile.presentation.ProfileActivity
import com.ludoproof.game.feature.leaderboard.presentation.LeaderboardActivity
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory

internal fun HomeActivity.profileHud():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(if (isCompact()) 4 else 6),
            dp(4),
            dp(if (isCompact()) 4 else 6),
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
                topMargin =
                    dp(if (isCompact()) 18 else 22)
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

private fun HomeActivity.homeIdentityRow():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        minimumHeight =
            dp(if (isCompact()) 72 else 80)

        addView(
            homeAvatarPlaceholder(),
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 58 else 64),
                dp(if (isCompact()) 58 else 64),
            ),
        )

        addView(
            homeIdentityBlock(),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart =
                    dp(if (isCompact()) 10 else 12)
                marginEnd =
                    dp(if (isCompact()) 8 else 12)
            },
        )

        addView(
            homeCurrencyPill(),
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 112 else 126),
                dp(if (isCompact()) 44 else 48),
            ),
        )
    }

private fun HomeActivity.homeAvatarPlaceholder():
    TextView =
    TextView(this).apply {
        homeAvatarText = this
        val avatarId =
            CosmeticInventoryStore(
                this@homeAvatarPlaceholder,
            )
                .selectedId(
                    CosmeticCategory.AVATAR,
                )
        text =
            StoreCosmeticCatalog
                .find(
                    avatarId,
                )
                ?.previewSymbol
                ?: "GU"
        textSize =
            if (isCompact()) 17f else 19f
        setTypeface(
            Typeface.DEFAULT_BOLD,
        )
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        contentDescription =
            "Guest user profile"
        isClickable = true
        isFocusable = true
        setOnClickListener {
            startActivity(
                Intent(
                    this@homeAvatarPlaceholder,
                    ProfileActivity::class.java,
                ),
            )
        }
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF1679DD.toInt(),
                    0xFF0A4BA8.toInt(),
                    0xFF062A72.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(12).toFloat()
                setStroke(
                    dp(2),
                    0xFF53DFFF.toInt(),
                )
            }
        elevation =
            dp(5).toFloat()
    }

private fun HomeActivity.homeIdentityBlock():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            TextView(
                this@homeIdentityBlock,
            ).apply {
                homeProfileNameText = this
                text =
                    ProfileStore(
                        this@homeIdentityBlock,
                    )
                        .snapshot()
                        .displayName
                textSize =
                    if (isCompact()) 18f else 21f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(Color.WHITE)
                gravity =
                    Gravity.START or
                        Gravity.CENTER_VERTICAL
                setShadowLayer(
                    2f,
                    0f,
                    dp(1).toFloat(),
                    0x99000000.toInt(),
                )
            },
        )

        addView(
            homeLevelProgress(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(if (isCompact()) 30 else 34),
            ).apply {
                topMargin = dp(3)
            },
        )
    }

private fun HomeActivity.homeLevelProgress():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL

        addView(
            TextView(
                this@homeLevelProgress,
            ).apply {
                homeProfileLevelText = this
                val profile =
                    ProfileStore(
                        this@homeLevelProgress,
                    )
                        .snapshot()
                val progress =
                    ProfileProgression
                        .levelProgress(
                            profile.totalXp,
                        )
                text =
                    "★" +
                        progress.level
                textSize =
                    if (isCompact()) 21f else 24f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                gravity = Gravity.CENTER
                contentDescription =
                    "Level 0"
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 38 else 42),
                dp(if (isCompact()) 30 else 34),
            ),
        )

        val track =
            FrameLayout(
                this@homeLevelProgress,
            ).apply {
                background =
                    LudoProofTheme.rounded(
                        0xD908245B.toInt(),
                        999f,
                        0xFF4A91D9.toInt(),
                        1f,
                        this@homeLevelProgress,
                    )
            }

        val trackWidthDp =
            if (isCompact()) 118 else 138
        val currentProgress =
            ProfileProgression
                .levelProgress(
                    ProfileStore(
                        this@homeLevelProgress,
                    )
                        .snapshot()
                        .totalXp,
                )

        track.addView(
            View(
                this@homeLevelProgress,
            ).apply {
                homeProfileXpFill = this
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(
                            0xFFFFD43B.toInt(),
                            0xFFFFB20F.toInt(),
                        ),
                    ).apply {
                        cornerRadius =
                            dp(999).toFloat()
                    }
            },
            FrameLayout.LayoutParams(
                dp(
                    (trackWidthDp *
                        currentProgress.fraction)
                        .toInt()
                        .coerceAtLeast(3),
                ),
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            track,
            LinearLayout.LayoutParams(
                dp(trackWidthDp),
                dp(10),
            ).apply {
                marginStart = dp(2)
            },
        )
    }

private fun HomeActivity.homeCurrencyPill():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        isClickable =
            true
        isFocusable =
            true
        contentDescription =
            "Open gem store"
        setOnClickListener {
            startActivity(
                Intent(
                    this@homeCurrencyPill,
                    StoreActivity::class.java,
                ),
            )
        }
        background =
            LudoProofTheme.rounded(
                0xF0071538.toInt(),
                999f,
                0x33000000,
                1f,
                this@homeCurrencyPill,
            )
        elevation =
            dp(4).toFloat()

        addView(
            TextView(
                this@homeCurrencyPill,
            ).apply {
                text = "◆"
                textSize =
                    if (isCompact()) 23f else 26f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF43F36B.toInt(),
                )
                gravity = Gravity.CENTER
                contentDescription =
                    "Currency"
            },
            LinearLayout.LayoutParams(
                dp(if (isCompact()) 38 else 42),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        addView(
            TextView(
                this@homeCurrencyPill,
            ).apply {
                homeGemBalanceText = this
                text =
                    GemWalletStore(
                        this@homeCurrencyPill,
                    )
                        .balance()
                        .toString()
                textSize =
                    if (isCompact()) 19f else 21f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f,
            ),
        )

        addView(
            Button(
                this@homeCurrencyPill,
            ).apply {
                text = "+"
                textSize =
                    if (isCompact()) 25f else 28f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF7B5510.toInt(),
                )
                gravity = Gravity.CENTER
                minWidth = 0
                minHeight = 0
                setPadding(0, 0, 0, 0)
                background =
                    headerPressDrawable(
                        0xFFFFD03B.toInt(),
                        0xFFFFB81F.toInt(),
                        10f,
                    )
                contentDescription =
                    "Add currency"
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
                dp(if (isCompact()) 38 else 42),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

private fun HomeActivity.homeShortcutRow():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER

        listOf(
            HeaderShortcut(
                symbol = "★",
                label = "LEADERBOARD",
                tint = 0xFFFFC928.toInt(),
            ),
            HeaderShortcut(
                symbol = "",
                label = "SHOP",
                tint = 0xFFFFC928.toInt(),
                iconKind = HomeIconKind.SHOP,
            ),
            HeaderShortcut(
                symbol = "",
                label = "BADGES",
                tint = 0xFFFFB51E.toInt(),
                iconKind = HomeIconKind.BADGE,
            ),
            HeaderShortcut(
                symbol = "AD",
                label = "REMOVE ADS",
                tint = 0xFF72E8FF.toInt(),
            ),
        ).forEach {
                item ->
            addView(
                homeShortcut(item),
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )
        }
    }

private fun HomeActivity.homeShortcut(
    item: HeaderShortcut,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription =
            item.label.lowercase()
        background =
            headerPressDrawable(
                Color.TRANSPARENT,
                0x2217A4FF,
                12f,
            )
        setPadding(
            dp(2),
            dp(5),
            dp(2),
            dp(5),
        )
        setOnClickListener {
            when (
                item.label
            ) {
                "LEADERBOARD" ->
                    startActivity(
                        Intent(
                            this@homeShortcut,
                            LeaderboardActivity::class.java,
                        ),
                    )

                "SHOP" ->
                    startActivity(
                        Intent(
                            this@homeShortcut,
                            StoreActivity::class.java,
                        ),
                    )
            }
        }

        val iconSize =
            dp(
                if (
                    isCompact()
                ) {
                    50
                } else {
                    56
                },
            )
        val iconView =
            if (
                item.iconKind !=
                null
            ) {
                HomeIconView(
                    this@homeShortcut,
                ).apply {
                    kind =
                        item.iconKind
                    iconColor =
                        item.tint
                    setPadding(
                        dp(8),
                        dp(8),
                        dp(8),
                        dp(8),
                    )
                    background =
                        LudoProofTheme.rounded(
                            0xA80A2D72.toInt(),
                            13f,
                            0x665ED8FF,
                            1f,
                            this@homeShortcut,
                        )
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            } else {
                TextView(
                    this@homeShortcut,
                ).apply {
                    text = item.symbol
                    textSize =
                        if (
                            item.symbol.length >
                            1
                        ) {
                            if (isCompact()) 17f else 19f
                        } else {
                            if (isCompact()) 30f else 34f
                        }
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        item.tint,
                    )
                    gravity =
                        Gravity.CENTER
                    background =
                        LudoProofTheme.rounded(
                            0xA80A2D72.toInt(),
                            13f,
                            0x665ED8FF,
                            1f,
                            this@homeShortcut,
                        )
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            }

        addView(
            iconView,
            LinearLayout.LayoutParams(
                iconSize,
                iconSize,
            ),
        )

        addView(
            TextView(
                this@homeShortcut,
            ).apply {
                text = item.label
                textSize =
                    if (isCompact()) 8.5f else 9.5f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(
                    0,
                    dp(5),
                    0,
                    0,
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

private fun HomeActivity.headerPressDrawable(
    normalColor: Int,
    pressedColor: Int,
    radiusDp: Float,
): StateListDrawable =
    StateListDrawable().apply {
        addState(
            intArrayOf(
                android.R.attr.state_pressed,
            ),
            GradientDrawable().apply {
                setColor(pressedColor)
                cornerRadius =
                    dp(radiusDp.toInt())
                        .toFloat()
            },
        )
        addState(
            intArrayOf(),
            GradientDrawable().apply {
                setColor(normalColor)
                cornerRadius =
                    dp(radiusDp.toInt())
                        .toFloat()
            },
        )
    }

internal fun HomeActivity.refreshHomeProfileSummary() {
    val avatarId =
        CosmeticInventoryStore(this)
            .selectedId(
                CosmeticCategory.AVATAR,
            )
    homeAvatarText
        ?.text =
        StoreCosmeticCatalog
            .find(
                avatarId,
            )
            ?.previewSymbol
            ?: "GU"

    val profile =
        ProfileStore(this)
            .snapshot()
    val progress =
        ProfileProgression
            .levelProgress(
                profile.totalXp,
            )

    homeProfileNameText
        ?.text =
        profile.displayName
    homeProfileLevelText
        ?.text =
        "★" +
            progress.level

    val trackWidthDp =
        if (isCompact()) {
            118
        } else {
            138
        }
    homeProfileXpFill
        ?.layoutParams =
        homeProfileXpFill
            ?.layoutParams
            ?.apply {
                width =
                    dp(
                        (
                            trackWidthDp *
                                progress.fraction
                            )
                            .toInt()
                            .coerceAtLeast(
                                3,
                            ),
                    )
            }
}

internal fun HomeActivity.refreshHomeGemBalance() {
    homeGemBalanceText
        ?.text =
        GemWalletStore(this)
            .balance()
            .toString()
}

private data class HeaderShortcut(
    val symbol: String,
    val label: String,
    val tint: Int,
    val iconKind: HomeIconKind? = null,
)
