package com.ludoproof.game.feature.store.presentation

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.ludoproof.game.BuildConfig
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.store.data.billing.GooglePlayBillingGateway
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.data.local.CosmeticInventoryStore
import com.ludoproof.game.feature.store.domain.model.StoreBillingProduct
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.components.storeContent
import com.ludoproof.game.feature.store.presentation.components.storeHeader
import com.ludoproof.game.feature.store.presentation.components.storeTabs
import com.ludoproof.game.feature.store.presentation.components.storefrontAwning

class StoreActivity :
    Activity(),
    GooglePlayBillingGateway.Listener {
    internal val wallet by lazy {
        GemWalletStore(this)
    }
    internal val cosmetics by lazy {
        CosmeticInventoryStore(this)
    }

    internal var selectedTab =
        StoreTab.GEMS
    internal var liveProducts:
        Map<
            String,
            StoreBillingProduct,
            > =
        emptyMap()

    private val billing by lazy {
        GooglePlayBillingGateway(
            activity =
                this,
            enabled =
                BuildConfig
                    .LUDOPROOF_BILLING_ENABLED,
            listener =
                this,
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme
            .configureWindow(
                this,
            )
        renderStore()
    }

    override fun onStart() {
        super.onStart()
        billing.connect()
    }

    override fun onDestroy() {
        billing.close()
        super.onDestroy()
    }

    internal fun selectTab(
        tab: StoreTab,
    ) {
        if (
            selectedTab ==
            tab
        ) {
            return
        }
        selectedTab =
            tab
        renderStore()
    }

    internal fun requestPurchase(
        productId: String,
    ) {
        if (
            !BuildConfig
                .LUDOPROOF_BILLING_ENABLED
        ) {
            showStoreMessage(
                "Purchases are prepared but intentionally disabled in this build. Enable them only after the Play Console products and secure purchase-verification backend are configured.",
            )
            return
        }
        billing
            .launchPurchase(
                productId,
            )
    }

    internal fun requestRewardedAd() {
        showStoreMessage(
            "Rewarded ads are not connected yet, so no gems are granted. The button is reserved for the future rewarded-ad integration.",
        )
    }

    override fun onCatalogLoaded(
        products:
            Map<
                String,
                StoreBillingProduct,
                >,
    ) {
        runOnUiThread {
            if (
                !canRenderStoreUi()
            ) {
                return@runOnUiThread
            }
            liveProducts =
                products
            renderStore()
        }
    }

    override fun onPurchaseRequiresVerification(
        productId: String,
        purchaseToken: String,
    ) {
        runOnUiThread {
            if (
                !canRenderStoreUi()
            ) {
                return@runOnUiThread
            }
            showStoreMessage(
                "Google Play returned a completed purchase for $productId. Ludo Paws will not grant gems or remove ads until the purchase token is verified by the secure backend.",
            )
        }
    }

    override fun onPurchasePending(
        productId: String,
    ) {
        runOnUiThread {
            if (
                !canRenderStoreUi()
            ) {
                return@runOnUiThread
            }
            showStoreMessage(
                "Your Google Play purchase for $productId is pending. Content will only be granted after Google confirms payment and the backend verifies it.",
            )
        }
    }

    override fun onBillingUnavailable(
        message: String,
    ) {
        // Keep the visual catalog usable during development.
    }

    internal fun priceFor(
        productId: String,
        fallback: String,
    ): String =
        liveProducts[
            productId
        ]
            ?.formattedPrice
            ?: fallback

    internal fun dp(
        value: Int,
    ): Int =
        LudoProofTheme
            .dp(
                this,
                value,
            )

    private fun canRenderStoreUi(): Boolean =
        !isFinishing &&
            !isDestroyed

    internal fun renderStore() {
        if (
            !canRenderStoreUi()
        ) {
            return
        }

        val (root, host) =
            LudoProofTheme
                .arcadeRoot(
                    this,
                )

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }
        val contentHost =
            FrameLayout(this)
        scroll.addView(
            contentHost,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val horizontalPadding =
            dp(
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            this,
                        )
                ) {
                    10
                } else {
                    14
                },
            )
        val contentWidth =
            minOf(
                resources
                    .displayMetrics
                    .widthPixels -
                    horizontalPadding *
                        2,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(
                            this,
                        ),
                ),
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(30),
                )
            }

        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                contentWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL,
            ),
        )
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(
            storeHeader(),
        )
        content.addView(
            storefrontAwning(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(82),
            ).apply {
                topMargin =
                    dp(8)
            },
        )
        content.addView(
            storeTabs(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58),
            ),
        )
        content.addView(
            storeContent(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(12)
            },
        )

        setContentView(
            root,
        )
    }

    internal fun showStoreMessage(
        message: String,
    ) {
        if (
            !canRenderStoreUi()
        ) {
            return
        }

        AlertDialog
            .Builder(this)
            .setTitle(
                "Ludo Paws Store",
            )
            .setMessage(
                message,
            )
            .setPositiveButton(
                "OK",
                null,
            )
            .show()
    }
}
