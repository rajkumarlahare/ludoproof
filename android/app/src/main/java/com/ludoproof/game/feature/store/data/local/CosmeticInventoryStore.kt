package com.ludoproof.game.feature.store.data.local

import android.content.Context
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticAcquireResult
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic

class CosmeticInventoryStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            StorePreferences.PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun isOwned(
        cosmeticId: String,
    ): Boolean =
        cosmeticId in
            ownedIds()

    fun selectedId(
        category: CosmeticCategory,
    ): String =
        prefs.getString(
            selectedKey(
                category,
            ),
            null,
        )
            ?.takeIf {
                StoreCosmeticCatalog
                    .find(
                        it,
                    )
                    ?.category ==
                    category
            }
            ?: StoreCosmeticCatalog
                .defaultId(
                    category,
                )

    fun rewardedAdProgress(
        cosmeticId: String,
    ): Int =
        prefs.getInt(
            StorePreferences
                .KEY_AD_PROGRESS_PREFIX +
                cosmeticId,
            0,
        )
            .coerceAtLeast(
                0,
            )

    @Synchronized
    fun acquireOrSelect(
        cosmetic: StoreCosmetic,
        currentLevel: Int,
    ): CosmeticAcquireResult {
        if (
            currentLevel <
            cosmetic.requiredLevel
        ) {
            return CosmeticAcquireResult
                .NEED_LEVEL
        }

        val owned =
            ownedIds()

        if (
            cosmetic.id in
            owned
        ) {
            return if (
                prefs.edit()
                    .putString(
                        selectedKey(
                            cosmetic.category,
                        ),
                        cosmetic.id,
                    )
                    .commit()
            ) {
                CosmeticAcquireResult
                    .SELECTED
            } else {
                CosmeticAcquireResult
                    .INVALID
            }
        }

        when (
            cosmetic.unlockKind
        ) {
            CosmeticUnlockKind.FREE,
            CosmeticUnlockKind.LEVEL -> {
                owned +=
                    cosmetic.id
                return if (
                    prefs.edit()
                        .putStringSet(
                            StorePreferences
                                .KEY_OWNED_COSMETICS,
                            owned,
                        )
                        .putString(
                            selectedKey(
                                cosmetic.category,
                            ),
                            cosmetic.id,
                        )
                        .commit()
                ) {
                    CosmeticAcquireResult
                        .ACQUIRED_AND_SELECTED
                } else {
                    CosmeticAcquireResult
                        .INVALID
                }
            }

            CosmeticUnlockKind.GEMS -> {
                val balance =
                    prefs.getInt(
                        StorePreferences
                            .KEY_BALANCE,
                        0,
                    )
                        .coerceAtLeast(
                            0,
                        )
                if (
                    balance <
                    cosmetic.gemPrice
                ) {
                    return CosmeticAcquireResult
                        .NEED_GEMS
                }

                owned +=
                    cosmetic.id
                return if (
                    prefs.edit()
                        .putInt(
                            StorePreferences
                                .KEY_BALANCE,
                            balance -
                                cosmetic.gemPrice,
                        )
                        .putStringSet(
                            StorePreferences
                                .KEY_OWNED_COSMETICS,
                            owned,
                        )
                        .putString(
                            selectedKey(
                                cosmetic.category,
                            ),
                            cosmetic.id,
                        )
                        .commit()
                ) {
                    CosmeticAcquireResult
                        .ACQUIRED_AND_SELECTED
                } else {
                    CosmeticAcquireResult
                        .INVALID
                }
            }

            CosmeticUnlockKind.REWARDED_ADS -> {
                if (
                    rewardedAdProgress(
                        cosmetic.id,
                    ) <
                    cosmetic.requiredAdViews
                ) {
                    return CosmeticAcquireResult
                        .NEED_REWARDED_ADS
                }

                owned +=
                    cosmetic.id
                return if (
                    prefs.edit()
                        .putStringSet(
                            StorePreferences
                                .KEY_OWNED_COSMETICS,
                            owned,
                        )
                        .putString(
                            selectedKey(
                                cosmetic.category,
                            ),
                            cosmetic.id,
                        )
                        .commit()
                ) {
                    CosmeticAcquireResult
                        .ACQUIRED_AND_SELECTED
                } else {
                    CosmeticAcquireResult
                        .INVALID
                }
            }
        }
    }

    @Synchronized
    fun recordVerifiedRewardedAd(
        cosmeticId: String,
    ): Int {
        val cosmetic =
            StoreCosmeticCatalog
                .find(
                    cosmeticId,
                )
                ?: return 0
        if (
            cosmetic.unlockKind !=
            CosmeticUnlockKind
                .REWARDED_ADS
        ) {
            return 0
        }

        val next =
            (
                rewardedAdProgress(
                    cosmeticId,
                ) +
                    1
                )
                .coerceAtMost(
                    cosmetic.requiredAdViews,
                )

        prefs.edit()
            .putInt(
                StorePreferences
                    .KEY_AD_PROGRESS_PREFIX +
                    cosmeticId,
                next,
            )
            .commit()

        return next
    }

    private fun ownedIds():
        MutableSet<String> =
        prefs.getStringSet(
            StorePreferences
                .KEY_OWNED_COSMETICS,
            emptySet(),
        )
            ?.toMutableSet()
            ?.apply {
                addAll(
                    StoreCosmeticCatalog
                        .defaultOwnedIds,
                )
            }
            ?: StoreCosmeticCatalog
                .defaultOwnedIds
                .toMutableSet()

    private fun selectedKey(
        category: CosmeticCategory,
    ): String =
        when (category) {
            CosmeticCategory.BOARD ->
                StorePreferences
                    .KEY_SELECTED_BOARD

            CosmeticCategory.DICE ->
                StorePreferences
                    .KEY_SELECTED_DICE

            CosmeticCategory.AVATAR ->
                StorePreferences
                    .KEY_SELECTED_AVATAR
        }
}
