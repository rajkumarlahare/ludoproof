package com.ludoproof.game.feature.store.data.local

import android.content.Context

class GemWalletStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            StorePreferences.PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun balance(): Int =
        prefs.getInt(
            StorePreferences.KEY_BALANCE,
            0,
        )
            .coerceAtLeast(
                0,
            )

    fun adsRemoved(): Boolean =
        prefs.getBoolean(
            StorePreferences.KEY_ADS_REMOVED,
            false,
        )

    @Synchronized
    fun applyVerifiedPurchase(
        productId: String,
        purchaseToken: String,
        gemAmount: Int,
        removeAds: Boolean,
    ): Boolean {
        if (
            purchaseToken.isBlank()
        ) {
            return false
        }

        val applied =
            prefs.getStringSet(
                StorePreferences.KEY_APPLIED_TOKENS,
                emptySet(),
            )
                ?.toMutableSet()
                ?: mutableSetOf()

        if (
            purchaseToken in
                applied
        ) {
            return false
        }

        val nextBalance =
            (
                balance()
                    .toLong() +
                    gemAmount
                        .coerceAtLeast(
                            0,
                        )
                )
                .coerceAtMost(
                    Int.MAX_VALUE
                        .toLong(),
                )
                .toInt()

        applied +=
            purchaseToken

        return prefs
            .edit()
            .putInt(
                StorePreferences.KEY_BALANCE,
                nextBalance,
            )
            .putBoolean(
                StorePreferences.KEY_ADS_REMOVED,
                adsRemoved() ||
                    removeAds,
            )
            .putStringSet(
                StorePreferences.KEY_APPLIED_TOKENS,
                applied,
            )
            .commit()
    }

}
