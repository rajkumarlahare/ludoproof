package com.ludoproof.game.feature.store.domain

import com.ludoproof.game.feature.store.domain.model.StoreProductKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCatalogTest {
    @Test
    fun productIdsAreUnique() {
        val ids =
            StoreCatalog
                .products
                .map {
                    it.productId
                }

        assertEquals(
            ids.size,
            ids.toSet()
                .size,
        )
    }

    @Test
    fun gemPacksHavePositiveAmounts() {
        val gemPacks =
            StoreCatalog
                .products
                .filter {
                    it.kind ==
                        StoreProductKind
                            .CONSUMABLE_GEMS
                }

        assertTrue(
            gemPacks.isNotEmpty(),
        )
        assertTrue(
            gemPacks.all {
                it.gemAmount >
                    0 &&
                    it.fallbackPrice
                        .isNotBlank()
            },
        )
    }

    @Test
    fun removeAdsIsNonConsumable() {
        val product =
            requireNotNull(
                StoreCatalog
                    .find(
                        StoreCatalog
                            .REMOVE_ADS,
                    ),
            )

        assertEquals(
            StoreProductKind
                .NON_CONSUMABLE_REMOVE_ADS,
            product.kind,
        )
        assertTrue(
            product.featured,
        )
    }
}
