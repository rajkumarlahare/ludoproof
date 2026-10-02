package com.ludoproof.game.feature.store.presentation.components

import android.widget.LinearLayout
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.boardCatalogContent():
    LinearLayout =
    cosmeticGrid(
        items =
            StoreCosmeticCatalog
                .boards,
        borderColor =
            0xFFFF9D81.toInt(),
    )
