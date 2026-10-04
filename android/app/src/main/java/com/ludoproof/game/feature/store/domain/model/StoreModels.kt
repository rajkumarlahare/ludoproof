package com.ludoproof.game.feature.store.domain.model

enum class StoreTab(
    val label: String,
) {
    GEMS("GEMS"),
    PAWS("PAWS"),
    BOARD("BOARD"),
    DICE("DICE"),
    AVATAR("AVATAR"),
}

enum class StoreProductKind {
    CONSUMABLE_GEMS,
    NON_CONSUMABLE_REMOVE_ADS,
}

data class StoreProduct(
    val productId: String,
    val title: String,
    val gemAmount: Int,
    val kind: StoreProductKind,
    val fallbackPrice: String,
    val featured: Boolean = false,
)

data class StoreBillingProduct(
    val productId: String,
    val formattedPrice: String,
)

data class VerifiedStorePurchase(
    val productId: String,
    val purchaseToken: String,
)
