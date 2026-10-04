package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import com.ludoproof.game.feature.store.presentation.StoreActivity
import com.ludoproof.game.feature.store.presentation.actions.handleCosmeticTap

internal fun StoreActivity.cosmeticGrid(
    items: List<StoreCosmetic>,
    borderColor: Int,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL

        items.chunked(
            3,
        ).forEachIndexed {
                rowIndex,
                rowItems ->
            val row =
                LinearLayout(
                    this@cosmeticGrid,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.TOP
                }

            rowItems.forEach {
                    cosmetic ->
                row.addView(
                    cosmeticCard(
                        cosmetic =
                            cosmetic,
                        borderColor =
                            borderColor,
                    ),
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        setMargins(
                            dp(4),
                            0,
                            dp(4),
                            0,
                        )
                    },
                )
            }

            repeat(
                3 -
                    rowItems.size,
            ) {
                row.addView(
                    TextView(
                        this@cosmeticGrid,
                    ),
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f,
                    ),
                )
            }

            addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (
                        rowIndex >
                        0
                    ) {
                        topMargin =
                            dp(12)
                    }
                },
            )
        }
    }

private fun StoreActivity.cosmeticCard(
    cosmetic: StoreCosmetic,
    borderColor: Int,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER_HORIZONTAL
        setPadding(
            dp(8),
            dp(10),
            dp(8),
            dp(10),
        )

        val selected =
            cosmetics
                .selectedId(
                    cosmetic.category,
                ) ==
                cosmetic.id
        val owned =
            cosmetics
                .isOwned(
                    cosmetic.id,
                )
        val currentLevel =
            ProfileProgression
                .levelProgress(
                    ProfileStore(
                        this@cosmeticCard,
                    )
                        .snapshot()
                        .totalXp,
                )
                .level

        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (cosmetic.contentAvailable) {
                    intArrayOf(
                        0xFF164FA3.toInt(),
                        0xFF0C397D.toInt(),
                    )
                } else {
                    intArrayOf(
                        0xFF34445F.toInt(),
                        0xFF202D43.toInt(),
                    )
                },
            ).apply {
                cornerRadius =
                    dp(13)
                        .toFloat()
                setStroke(
                    dp(
                        if (selected) {
                            3
                        } else {
                            2
                        },
                    ),
                    if (selected) {
                        0xFF49F073.toInt()
                    } else {
                        borderColor
                    },
                )
            }

        addView(
            TextView(
                this@cosmeticCard,
            ).apply {
                text =
                    cosmetic.title
                textSize =
                    12.5f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                maxLines =
                    1
            },
        )

        addView(
            TextView(
                this@cosmeticCard,
            ).apply {
                text =
                    cosmetic.previewSymbol
                textSize =
                    if (
                        cosmetic.previewSymbol.length >
                        2
                    ) {
                        24f
                    } else {
                        38f
                    }
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            cosmetic.previewPrimary,
                            cosmetic.previewSecondary,
                        ),
                    ).apply {
                        cornerRadius =
                            dp(12)
                                .toFloat()
                        setStroke(
                            dp(1),
                            0x885FE1FF.toInt(),
                        )
                    }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(116),
            ).apply {
                topMargin =
                    dp(9)
            },
        )

        addView(
            Button(
                this@cosmeticCard,
            ).apply {
                text =
                    cosmeticActionText(
                        cosmetic =
                            cosmetic,
                        owned =
                            owned,
                        selected =
                            selected,
                    )
                textSize =
                    12f
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
                    dp(2),
                    0,
                    dp(2),
                    0,
                )
                background =
                    LudoProofTheme
                        .rounded(
                            when {
                                !cosmetic.contentAvailable ->
                                    0xFF52657E.toInt()
                                selected || owned ->
                                    0xFF08A849.toInt()
                                else ->
                                    0xFFE23434.toInt()
                            },
                            10f,
                            when {
                                !cosmetic.contentAvailable ->
                                    0xFF8195AE.toInt()
                                selected || owned ->
                                    0xFF40E47B.toInt()
                                else ->
                                    0xFFFF6D6D.toInt()
                            },
                            1f,
                            this@cosmeticCard,
                        )
                setOnClickListener {
                    handleCosmeticTap(
                        cosmetic,
                    )
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44),
            ).apply {
                topMargin =
                    dp(9)
            },
        )

        if (
            cosmetic.requiredLevel >
            0
        ) {
            addView(
                TextView(
                    this@cosmeticCard,
                ).apply {
                    text =
                        if (
                            currentLevel >=
                            cosmetic.requiredLevel
                        ) {
                            "Lv " +
                                cosmetic.requiredLevel +
                                " ✓"
                        } else {
                            "🔒 Lv " +
                                cosmetic.requiredLevel
                        }
                    textSize =
                        10f
                    setTypeface(
                        Typeface.DEFAULT_BOLD,
                    )
                    setTextColor(
                        if (
                            currentLevel >=
                            cosmetic.requiredLevel
                        ) {
                            0xFF74F28B.toInt()
                        } else {
                            LudoProofTheme.GOLD
                        },
                    )
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        0,
                        dp(7),
                        0,
                        0,
                    )
                },
            )
        }
    }

private fun StoreActivity.cosmeticActionText(
    cosmetic: StoreCosmetic,
    owned: Boolean,
    selected: Boolean,
): String =
    when {
        !cosmetic.contentAvailable ->
            "SOON"

        selected ->
            "✓"

        owned ->
            "USE"

        cosmetic.unlockKind ==
            CosmeticUnlockKind.GEMS ->
            cosmetic.gemPrice
                .toString() +
                " ◆"

        cosmetic.unlockKind ==
            CosmeticUnlockKind.REWARDED_ADS ->
            "AD " +
                cosmetics
                    .rewardedAdProgress(
                        cosmetic.id,
                    ) +
                "/" +
                cosmetic.requiredAdViews

        cosmetic.unlockKind ==
            CosmeticUnlockKind.LEVEL ->
            "UNLOCK"

        cosmetic.unlockKind ==
            CosmeticUnlockKind.EVENT ->
            "EVENT"

        else ->
            "FREE"
    }
