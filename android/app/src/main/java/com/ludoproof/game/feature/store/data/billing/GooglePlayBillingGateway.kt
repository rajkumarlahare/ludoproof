package com.ludoproof.game.feature.store.data.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.ludoproof.game.feature.store.domain.StoreCatalog
import com.ludoproof.game.feature.store.domain.model.StoreBillingProduct

class GooglePlayBillingGateway(
    private val activity: Activity,
    private val enabled: Boolean,
    private val listener: Listener,
) : PurchasesUpdatedListener {
    private val productDetails =
        linkedMapOf<
            String,
            ProductDetails,
            >()

    private val billingClient =
        BillingClient
            .newBuilder(
                activity.applicationContext,
            )
            .setListener(
                this,
            )
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .enableAutoServiceReconnection()
            .build()

    fun connect() {
        if (!enabled) {
            listener
                .onBillingUnavailable(
                    "Google Play purchases are not enabled for this build yet.",
                )
            return
        }

        if (
            billingClient
                .isReady
        ) {
            queryProducts()
            return
        }

        billingClient
            .startConnection(
                object :
                    BillingClientStateListener {
                    override fun onBillingSetupFinished(
                        billingResult:
                            BillingResult,
                    ) {
                        if (
                            billingResult.responseCode ==
                            BillingClient
                                .BillingResponseCode
                                .OK
                        ) {
                            queryProducts()
                        } else {
                            listener
                                .onBillingUnavailable(
                                    billingResult
                                        .debugMessage
                                        .ifBlank {
                                            "Google Play Billing is unavailable."
                                        },
                                )
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        listener
                            .onBillingUnavailable(
                                "Google Play Billing connection was interrupted.",
                            )
                    }
                },
            )
    }

    fun launchPurchase(
        productId: String,
    ) {
        if (!enabled) {
            listener
                .onBillingUnavailable(
                    "Purchases are disabled until Play Console products and secure verification are configured.",
                )
            return
        }

        val details =
            productDetails[
                productId
            ]
        if (details == null) {
            listener
                .onBillingUnavailable(
                    "This product is not available from Google Play yet.",
                )
            queryProducts()
            return
        }

        val offer =
            details
                .oneTimePurchaseOfferDetailsList
                ?.firstOrNull()

        if (offer == null) {
            listener
                .onBillingUnavailable(
                    "No eligible Google Play offer is available for this product.",
                )
            return
        }

        val offerToken =
            offer.offerToken
        if (
            offerToken ==
            null
        ) {
            listener
                .onBillingUnavailable(
                    "Google Play did not provide a usable offer token for this product.",
                )
            return
        }

        val productParams =
            BillingFlowParams
                .ProductDetailsParams
                .newBuilder()
                .setProductDetails(
                    details,
                )
                .setOfferToken(
                    offerToken,
                )
                .build()

        val result =
            billingClient
                .launchBillingFlow(
                    activity,
                    BillingFlowParams
                        .newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                productParams,
                            ),
                        )
                        .build(),
                )

        if (
            result.responseCode !=
            BillingClient
                .BillingResponseCode
                .OK
        ) {
            listener
                .onBillingUnavailable(
                    result.debugMessage
                        .ifBlank {
                            "Google Play purchase screen could not be opened."
                        },
                )
        }
    }

    fun close() {
        if (
            billingClient
                .isReady
        ) {
            billingClient
                .endConnection()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (
            billingResult.responseCode
        ) {
            BillingClient
                .BillingResponseCode
                .OK -> {
                purchases
                    .orEmpty()
                    .forEach {
                            purchase ->
                        when (
                            purchase.purchaseState
                        ) {
                            Purchase
                                .PurchaseState
                                .PURCHASED ->
                                purchase.products
                                    .forEach {
                                            productId ->
                                        listener
                                            .onPurchaseRequiresVerification(
                                                productId =
                                                    productId,
                                                purchaseToken =
                                                    purchase.purchaseToken,
                                            )
                                    }

                            Purchase
                                .PurchaseState
                                .PENDING ->
                                purchase.products
                                    .forEach {
                                            productId ->
                                        listener
                                            .onPurchasePending(
                                                productId,
                                            )
                                    }

                            else ->
                                Unit
                        }
                    }
            }

            BillingClient
                .BillingResponseCode
                .USER_CANCELED ->
                Unit

            else ->
                listener
                    .onBillingUnavailable(
                        billingResult
                            .debugMessage
                            .ifBlank {
                                "Google Play purchase failed."
                            },
                    )
        }
    }

    private fun queryProducts() {
        val products =
            StoreCatalog
                .playProductIds
                .map {
                        productId ->
                    QueryProductDetailsParams
                        .Product
                        .newBuilder()
                        .setProductId(
                            productId,
                        )
                        .setProductType(
                            BillingClient
                                .ProductType
                                .INAPP,
                        )
                        .build()
                }

        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    products,
                )
                .build()

        billingClient
            .queryProductDetailsAsync(
                params,
            ) {
                    billingResult,
                    result ->
                if (
                    billingResult.responseCode !=
                    BillingClient
                        .BillingResponseCode
                        .OK
                ) {
                    listener
                        .onBillingUnavailable(
                            billingResult
                                .debugMessage
                                .ifBlank {
                                    "Google Play catalog could not be loaded."
                                },
                        )
                    return@queryProductDetailsAsync
                }

                productDetails
                    .clear()

                val mapped =
                    linkedMapOf<
                        String,
                        StoreBillingProduct,
                        >()

                result
                    .productDetailsList
                    .forEach {
                            details ->
                        val offer =
                            details
                                .oneTimePurchaseOfferDetailsList
                                ?.firstOrNull()
                                ?: return@forEach
                        productDetails[
                            details.productId
                        ] =
                            details
                        mapped[
                            details.productId
                        ] =
                            StoreBillingProduct(
                                productId =
                                    details.productId,
                                formattedPrice =
                                    offer.formattedPrice,
                            )
                    }

                listener
                    .onCatalogLoaded(
                        mapped,
                    )
            }
    }

    interface Listener {
        fun onCatalogLoaded(
            products:
                Map<
                    String,
                    StoreBillingProduct,
                    >,
        )

        fun onPurchaseRequiresVerification(
            productId: String,
            purchaseToken: String,
        )

        fun onPurchasePending(
            productId: String,
        )

        fun onBillingUnavailable(
            message: String,
        )
    }
}
