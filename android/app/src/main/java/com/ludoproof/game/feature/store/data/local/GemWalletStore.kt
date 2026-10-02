package com.ludoproof.game.feature.store.data.local

import android.content.Context

class GemWalletStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    fun balance(): Int =
        prefs.getInt(
            KEY_BALANCE,
            0,
        )
            .coerceAtLeast(
                0,
            )

    fun adsRemoved(): Boolean =
        prefs.getBoolean(
            KEY_ADS_REMOVED,
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
                KEY_APPLIED_TOKENS,
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
                KEY_BALANCE,
                nextBalance,
            )
            .putBoolean(
                KEY_ADS_REMOVED,
                adsRemoved() ||
                    removeAds,
            )
            .putStringSet(
                KEY_APPLIED_TOKENS,
                applied,
            )
            .commit()
    }

    private companion object {
        const val PREFS_NAME =
            "ludoproof_wallet"
        const val KEY_BALANCE =
            "gem_balance"
        const val KEY_ADS_REMOVED =
            "ads_removed"
        const val KEY_APPLIED_TOKENS =
            "verified_purchase_tokens"
    }
}
