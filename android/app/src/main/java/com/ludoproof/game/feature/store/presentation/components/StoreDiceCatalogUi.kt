package com.ludoproof.game.feature.store.presentation.components

import android.widget.LinearLayout
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.diceCatalogContent():
    LinearLayout =
    cosmeticGrid(
        items =
            StoreCosmeticCatalog
                .dice,
        borderColor =
            0xFF54E866.toInt(),
    )
