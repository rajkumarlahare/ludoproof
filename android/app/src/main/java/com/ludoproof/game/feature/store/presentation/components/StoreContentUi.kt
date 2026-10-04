package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.store.domain.StoreCatalog
import com.ludoproof.game.feature.store.domain.model.StoreProduct
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.storeContent():
    LinearLayout =
    when (selectedTab) {
        StoreTab.GEMS ->
            gemsContent()

        StoreTab.PAWS ->
            pawsCatalogContent()

        StoreTab.BOARD ->
            boardCatalogContent()

        StoreTab.DICE ->
            diceCatalogContent()

        StoreTab.AVATAR ->
            avatarCatalogContent()
    }

private fun StoreActivity.gemsContent():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL

        val removeAds =
            requireNotNull(
                StoreCatalog
                    .find(
                        StoreCatalog
                            .REMOVE_ADS,
                    ),
            )
        addView(
            featuredRemoveAdsCard(
                removeAds,
            ),
        )

        addView(
            rewardedAdCard(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(12)
            },
        )

        val packs =
            StoreCatalog
                .products
                .filter {
                    !it.featured
                }

        packs.chunked(
            3,
        ).forEach {
                rowProducts ->
            val row =
                LinearLayout(
                    this@gemsContent,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.TOP
                }

            rowProducts.forEach {
                    product ->
                row.addView(
                    gemProductCard(
                        product,
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
                    rowProducts.size,
            ) {
                row.addView(
                    TextView(
                        this@gemsContent,
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
                    topMargin =
                        dp(12)
                },
            )
        }

        addView(
            storeSafetyNote(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(16)
            },
        )
    }

private fun StoreActivity.featuredRemoveAdsCard(
    product: StoreProduct,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF4A156F.toInt(),
                    0xFF2A0C54.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(15)
                        .toFloat()
                setStroke(
                    dp(2),
                    0xFF8939A8.toInt(),
                )
            }

        val body =
            LinearLayout(
                this@featuredRemoveAdsCard,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12),
                )
            }

        body.addView(
            TextView(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    "AD\n×"
                textSize =
                    27f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                background =
                    LudoProofTheme
                        .rounded(
                            0xFF1067B8.toInt(),
                            16f,
                            0xFF64E0FF.toInt(),
                            2f,
                            this@featuredRemoveAdsCard,
                        )
            },
            LinearLayout.LayoutParams(
                dp(118),
                dp(104),
            ),
        )

        val copy =
            LinearLayout(
                this@featuredRemoveAdsCard,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(16),
                    0,
                    0,
                    0,
                )
            }
        copy.addView(
            TextView(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    "Remove Ads"
                textSize =
                    19f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
            },
        )
        copy.addView(
            TextView(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    "+" +
                        product.gemAmount +
                        " ◆"
                textSize =
                    17f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                setPadding(
                    0,
                    dp(8),
                    0,
                    0,
                )
            },
        )
        body.addView(
            copy,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        addView(
            body,
        )

        addView(
            Button(
                this@featuredRemoveAdsCard,
            ).apply {
                text =
                    priceFor(
                        product.productId,
                        product.fallbackPrice,
                    )
                textSize =
                    18f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minHeight =
                    0
                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            0xFF86E23C.toInt(),
                            0xFF4BAD1D.toInt(),
                        ),
                    ).apply {
                        cornerRadii =
                            floatArrayOf(
                                0f,
                                0f,
                                0f,
                                0f,
                                dp(15).toFloat(),
                                dp(15).toFloat(),
                                dp(15).toFloat(),
                                dp(15).toFloat(),
                            )
                    }
                setOnClickListener {
                    requestPurchase(
                        product.productId,
                    )
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52),
            ),
        )
    }

private fun StoreActivity.rewardedAdCard():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10),
        )
        background =
            LudoProofTheme
                .rounded(
                    0xE9144C9D.toInt(),
                    13f,
                    0xFFA7429B.toInt(),
                    2f,
                    this@rewardedAdCard,
                )

        addView(
            TextView(
                this@rewardedAdCard,
            ).apply {
                text =
                    "5 ◆"
                textSize =
                    18f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        addView(
            Button(
                this@rewardedAdCard,
            ).apply {
                text =
                    "WATCH AD"
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
                background =
                    LudoProofTheme
                        .rounded(
                            0xFFE93434.toInt(),
                            10f,
                            0xFFFF7373.toInt(),
                            1f,
                            this@rewardedAdCard,
                        )
                setOnClickListener {
                    requestRewardedAd()
                }
            },
            LinearLayout.LayoutParams(
                dp(112),
                dp(42),
            ),
        )
    }

private fun StoreActivity.gemProductCard(
    product: StoreProduct,
):
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(8),
            dp(12),
            dp(8),
            dp(10),
        )
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFF164FA3.toInt(),
                    0xFF0C397D.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(13)
                        .toFloat()
                setStroke(
                    dp(2),
                    0xFFA7429B.toInt(),
                )
            }

        addView(
            TextView(
                this@gemProductCard,
            ).apply {
                text =
                    formatGemAmount(
                        product.gemAmount,
                    ) +
                        " ◆"
                textSize =
                    18f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                gravity =
                    Gravity.CENTER
            },
        )

        addView(
            TextView(
                this@gemProductCard,
            ).apply {
                text =
                    if (
                        product.gemAmount >=
                            20_000
                    ) {
                        "▰\n◆◆◆"
                    } else {
                        "◆\n◆◆"
                    }
                textSize =
                    if (
                        product.gemAmount >=
                            20_000
                    ) {
                        24f
                    } else {
                        28f
                    }
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF43EF66.toInt(),
                )
                gravity =
                    Gravity.CENTER
                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(8),
                )
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(100),
            ),
        )

        addView(
            Button(
                this@gemProductCard,
            ).apply {
                text =
                    priceFor(
                        product.productId,
                        product.fallbackPrice,
                    )
                textSize =
                    13f
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
                            0xFF70D72F.toInt(),
                            10f,
                            0xFF9EFF57.toInt(),
                            1f,
                            this@gemProductCard,
                        )
                setOnClickListener {
                    requestPurchase(
                        product.productId,
                    )
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42),
            ),
        )
    }

private fun StoreActivity.storeSafetyNote():
    TextView =
    TextView(this).apply {
        text =
            "Payments are handled by Google Play. Gems and entitlements are granted only after purchase verification."
        textSize =
            10f
        setTextColor(
            0xFFC8D8EE.toInt(),
        )
        gravity =
            Gravity.CENTER
        setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(10),
        )
    }

private fun StoreActivity.formatGemAmount(
    amount: Int,
): String =
    when {
        amount >=
            1_000 &&
            amount %
                1_000 ==
                0 ->
            (
                amount /
                    1_000
                )
                .toString() +
                "K"

        else ->
            amount
                .toString()
    }
