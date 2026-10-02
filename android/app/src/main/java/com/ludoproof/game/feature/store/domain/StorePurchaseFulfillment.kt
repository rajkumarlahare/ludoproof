package com.ludoproof.game.feature.store.domain

import android.content.Context
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.store.data.local.GemWalletStore
import com.ludoproof.game.feature.store.domain.model.StoreProductKind
import com.ludoproof.game.feature.store.domain.model.VerifiedStorePurchase

class StorePurchaseFulfillment(
    context: Context,
) {
    private val wallet =
        GemWalletStore(
            context.applicationContext,
        )
    private val profile =
        ProfileStore(
            context.applicationContext,
        )

    fun applyVerifiedPurchase(
        purchase:
            VerifiedStorePurchase,
    ): Boolean {
        val product =
            StoreCatalog
                .find(
                    purchase.productId,
                )
                ?: return false

        val applied =
            wallet
                .applyVerifiedPurchase(
                    productId =
                        product.productId,
                    purchaseToken =
                        purchase.purchaseToken,
                    gemAmount =
                        product.gemAmount,
                    removeAds =
                        product.kind ==
                            StoreProductKind
                                .NON_CONSUMABLE_REMOVE_ADS,
                )

        if (applied) {
            profile
                .addPurchase(
                    product.productId,
                )
        }

        return applied
    }
}
