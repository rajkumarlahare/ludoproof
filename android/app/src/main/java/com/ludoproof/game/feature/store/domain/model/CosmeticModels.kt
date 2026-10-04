package com.ludoproof.game.feature.store.domain.model

enum class CosmeticCategory {
    BOARD,
    DICE,
    AVATAR,
    CHARACTER_PACK,
}

enum class CosmeticUnlockKind {
    FREE,
    GEMS,
    LEVEL,
    REWARDED_ADS,
    EVENT,
}

data class StoreCosmetic(
    val id: String,
    val title: String,
    val category: CosmeticCategory,
    val unlockKind: CosmeticUnlockKind,
    val gemPrice: Int = 0,
    val requiredLevel: Int = 0,
    val requiredAdViews: Int = 0,
    val previewSymbol: String,
    val previewPrimary: Int,
    val previewSecondary: Int,
    val characterPackId: String? = null,
    val eventKey: String? = null,
    val contentAvailable: Boolean = true,
)

enum class CosmeticAcquireResult {
    SELECTED,
    ACQUIRED_AND_SELECTED,
    NEED_LEVEL,
    NEED_GEMS,
    NEED_REWARDED_ADS,
    NEED_EVENT,
    CONTENT_UNAVAILABLE,
    INVALID,
}
