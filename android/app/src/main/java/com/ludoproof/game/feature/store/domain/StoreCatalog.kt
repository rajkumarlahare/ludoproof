package com.ludoproof.game.feature.store.domain

import com.ludoproof.game.feature.store.domain.model.StoreProduct
import com.ludoproof.game.feature.store.domain.model.StoreProductKind

object StoreCatalog {
    const val REMOVE_ADS =
        "remove_ads"
    const val GEMS_300 =
        "gems_300"
    const val GEMS_1500 =
        "gems_1500"
    const val GEMS_3500 =
        "gems_3500"
    const val GEMS_20000 =
        "gems_20000"
    const val GEMS_40000 =
        "gems_40000"
    const val GEMS_200000 =
        "gems_200000"

    val products =
        listOf(
            StoreProduct(
                productId =
                    REMOVE_ADS,
                title =
                    "Remove Ads",
                gemAmount =
                    5,
                kind =
                    StoreProductKind.NON_CONSUMABLE_REMOVE_ADS,
                fallbackPrice =
                    "₹149.00",
                featured =
                    true,
            ),
            StoreProduct(
                productId =
                    GEMS_300,
                title =
                    "300 Gems",
                gemAmount =
                    300,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹80.00",
            ),
            StoreProduct(
                productId =
                    GEMS_1500,
                title =
                    "1,500 Gems",
                gemAmount =
                    1_500,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹240.00",
            ),
            StoreProduct(
                productId =
                    GEMS_3500,
                title =
                    "3,500 Gems",
                gemAmount =
                    3_500,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹400.00",
            ),
            StoreProduct(
                productId =
                    GEMS_20000,
                title =
                    "20K Gems",
                gemAmount =
                    20_000,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹850.00",
            ),
            StoreProduct(
                productId =
                    GEMS_40000,
                title =
                    "40K Gems",
                gemAmount =
                    40_000,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹1,650.00",
            ),
            StoreProduct(
                productId =
                    GEMS_200000,
                title =
                    "200K Gems",
                gemAmount =
                    200_000,
                kind =
                    StoreProductKind.CONSUMABLE_GEMS,
                fallbackPrice =
                    "₹4,200.00",
            ),
        )

    val playProductIds:
        List<String> =
        products.map {
            it.productId
        }

    fun find(
        productId: String,
    ): StoreProduct? =
        products.firstOrNull {
            it.productId ==
                productId
        }
}
