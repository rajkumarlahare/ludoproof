package com.ludoproof.game.feature.store.data.local

import android.content.Context
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.domain.model.CosmeticAcquireResult
import com.ludoproof.game.feature.store.domain.model.CosmeticCategory
import com.ludoproof.game.feature.store.domain.model.CosmeticUnlockKind
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic

class CosmeticInventoryStore(
    context: Context,
) {
    private val appContext =
        context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(
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
                    ?.let { item ->
                        item.category == category &&
                            item.contentAvailable
                    } == true
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

    fun hasVerifiedEventEntitlement(
        cosmeticId: String,
    ): Boolean =
        cosmeticId in
            verifiedEventEntitlements()

    @Synchronized
    fun acquireOrSelect(
        cosmetic: StoreCosmetic,
        currentLevel: Int,
    ): CosmeticAcquireResult {
        if (!cosmetic.contentAvailable) {
            return CosmeticAcquireResult
                .CONTENT_UNAVAILABLE
        }
        if (!isSelectableCharacterPack(cosmetic)) {
            return CosmeticAcquireResult
                .INVALID
        }
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
                selectOwned(
                    cosmetic,
                )
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
                return if (
                    grantAndSelect(
                        cosmetic = cosmetic,
                        owned = owned,
                    )
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
                val committed =
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
                if (committed) {
                    syncCharacterPackSelection(
                        cosmetic,
                    )
                    return CosmeticAcquireResult
                        .ACQUIRED_AND_SELECTED
                }
                return CosmeticAcquireResult
                    .INVALID
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

                return if (
                    grantAndSelect(
                        cosmetic = cosmetic,
                        owned = owned,
                    )
                ) {
                    CosmeticAcquireResult
                        .ACQUIRED_AND_SELECTED
                } else {
                    CosmeticAcquireResult
                        .INVALID
                }
            }

            CosmeticUnlockKind.EVENT -> {
                if (
                    !hasVerifiedEventEntitlement(
                        cosmetic.id,
                    )
                ) {
                    return CosmeticAcquireResult
                        .NEED_EVENT
                }
                return if (
                    grantAndSelect(
                        cosmetic = cosmetic,
                        owned = owned,
                    )
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
                .REWARDED_ADS ||
            !cosmetic.contentAvailable
        ) {
            return rewardedAdProgress(
                cosmeticId,
            )
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

    /**
     * Called only after a trusted event/reward source has verified entitlement.
     * This records entitlement separately from ownership; acquireOrSelect still
     * validates that the pack content is actually available before selecting it.
     */
    @Synchronized
    fun recordVerifiedEventEntitlement(
        cosmeticId: String,
    ): Boolean {
        val cosmetic =
            StoreCosmeticCatalog
                .find(
                    cosmeticId,
                )
                ?: return false
        if (
            cosmetic.unlockKind !=
            CosmeticUnlockKind.EVENT ||
            cosmetic.eventKey.isNullOrBlank()
        ) {
            return false
        }

        val entitlements =
            verifiedEventEntitlements()
        entitlements +=
            cosmetic.id
        return prefs.edit()
            .putStringSet(
                StorePreferences
                    .KEY_EVENT_ENTITLEMENTS,
                entitlements,
            )
            .commit()
    }

    private fun grantAndSelect(
        cosmetic: StoreCosmetic,
        owned: MutableSet<String>,
    ): Boolean {
        owned +=
            cosmetic.id
        val committed =
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
        if (committed) {
            syncCharacterPackSelection(
                cosmetic,
            )
        }
        return committed
    }

    private fun selectOwned(
        cosmetic: StoreCosmetic,
    ): Boolean {
        val committed =
            prefs.edit()
                .putString(
                    selectedKey(
                        cosmetic.category,
                    ),
                    cosmetic.id,
                )
                .commit()
        if (committed) {
            syncCharacterPackSelection(
                cosmetic,
            )
        }
        return committed
    }

    private fun isSelectableCharacterPack(
        cosmetic: StoreCosmetic,
    ): Boolean {
        if (
            cosmetic.category !=
            CosmeticCategory.CHARACTER_PACK
        ) {
            return true
        }
        val packId =
            cosmetic.characterPackId
                ?: return false
        return LudoPawsCharacterCatalog
            .pack(
                packId,
            ) != null
    }

    private fun syncCharacterPackSelection(
        cosmetic: StoreCosmetic,
    ) {
        if (
            cosmetic.category !=
            CosmeticCategory.CHARACTER_PACK
        ) {
            return
        }
        val packId =
            cosmetic.characterPackId
                ?: return
        val pack =
            LudoPawsCharacterCatalog
                .pack(
                    packId,
                )
                ?: return
        val selectionStore =
            CharacterSelectionStore(
                appContext,
            )
        val current =
            selectionStore.load()
        if (
            current.packId == pack.id &&
            current.characterId in pack.characterIds
        ) {
            return
        }
        val first =
            pack.characterIds
                .firstOrNull()
                ?: return
        selectionStore.select(
            packId = pack.id,
            characterId = first,
        )
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

    private fun verifiedEventEntitlements():
        MutableSet<String> =
        prefs.getStringSet(
            StorePreferences
                .KEY_EVENT_ENTITLEMENTS,
            emptySet(),
        )
            ?.toMutableSet()
            ?: mutableSetOf()

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

            CosmeticCategory.CHARACTER_PACK ->
                StorePreferences
                    .KEY_SELECTED_CHARACTER_PACK
        }
}
