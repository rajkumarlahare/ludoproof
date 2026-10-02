package com.ludoproof.game.feature.store.domain

import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCosmeticCatalogTest {
    @Test
    fun cosmeticIdsAreUnique() {
        val ids =
            StoreCosmeticCatalog
                .all
                .map {
                    it.id
                }

        assertEquals(
            ids.size,
            ids.toSet()
                .size,
        )
    }

    @Test
    fun everyCategoryHasAFreeDefault() {
        CosmeticCategory
            .entries
            .forEach {
                    category ->
                val defaultId =
                    StoreCosmeticCatalog
                        .defaultId(
                            category,
                        )
                val item =
                    requireNotNull(
                        StoreCosmeticCatalog
                            .find(
                                defaultId,
                            ),
                    )

                assertEquals(
                    category,
                    item.category,
                )
                assertEquals(
                    CosmeticUnlockKind.FREE,
                    item.unlockKind,
                )
                assertTrue(
                    defaultId in
                        StoreCosmeticCatalog
                            .defaultOwnedIds,
                )
            }
    }

    @Test
    fun paidCosmeticsHavePositiveGemPrices() {
        val gemItems =
            StoreCosmeticCatalog
                .all
                .filter {
                    it.unlockKind ==
                        CosmeticUnlockKind.GEMS
                }

        assertTrue(
            gemItems.isNotEmpty(),
        )
        assertTrue(
            gemItems.all {
                it.gemPrice >
                    0
            },
        )
    }

    @Test
    fun rewardedItemsHavePositiveTargets() {
        val adItems =
            StoreCosmeticCatalog
                .all
                .filter {
                    it.unlockKind ==
                        CosmeticUnlockKind
                            .REWARDED_ADS
                }

        assertTrue(
            adItems.isNotEmpty(),
        )
        assertTrue(
            adItems.all {
                it.requiredAdViews >
                    0
            },
        )
    }
}
