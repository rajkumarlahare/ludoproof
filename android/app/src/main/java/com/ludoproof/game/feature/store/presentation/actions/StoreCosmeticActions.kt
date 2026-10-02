package com.ludoproof.game.feature.store.presentation.actions

import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.ProfileProgression
import com.ludoproof.game.feature.store.domain.model.CosmeticAcquireResult
import com.ludoproof.game.feature.store.domain.model.StoreCosmetic
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.handleCosmeticTap(
    cosmetic: StoreCosmetic,
) {
    val level =
        ProfileProgression
            .levelProgress(
                ProfileStore(this)
                    .snapshot()
                    .totalXp,
            )
            .level

    when (
        cosmetics.acquireOrSelect(
            cosmetic =
                cosmetic,
            currentLevel =
                level,
        )
    ) {
        CosmeticAcquireResult.SELECTED,
        CosmeticAcquireResult.ACQUIRED_AND_SELECTED -> {
            renderStore()
        }

        CosmeticAcquireResult.NEED_LEVEL -> {
            showStoreMessage(
                "Reach level " +
                    cosmetic.requiredLevel +
                    " to unlock " +
                    cosmetic.title +
                    ".",
            )
        }

        CosmeticAcquireResult.NEED_GEMS -> {
            showStoreMessage(
                cosmetic.title +
                    " costs " +
                    cosmetic.gemPrice +
                    " gems. Your balance is " +
                    wallet.balance() +
                    ".",
            )
        }

        CosmeticAcquireResult.NEED_REWARDED_ADS -> {
            val progress =
                cosmetics
                    .rewardedAdProgress(
                        cosmetic.id,
                    )
            showStoreMessage(
                cosmetic.title +
                    " unlocks after " +
                    cosmetic.requiredAdViews +
                    " verified rewarded ads. Progress: " +
                    progress +
                    "/" +
                    cosmetic.requiredAdViews +
                    ". Rewarded ads are not connected yet, so this build does not fake progress.",
            )
        }

        CosmeticAcquireResult.INVALID -> {
            showStoreMessage(
                "This cosmetic could not be updated. Please try again.",
            )
        }
    }
}
