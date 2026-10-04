package com.ludoproof.game.feature.store.presentation.components

import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.feature.store.domain.StoreCosmeticCatalog
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun StoreActivity.pawsCatalogContent():
    LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(
            TextView(this@pawsCatalogContent).apply {
                text = "ANIMAL PACKS"
                textSize = 17f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFFFFFFFF.toInt(),
                )
            },
        )
        addView(
            TextView(this@pawsCatalogContent).apply {
                text =
                    "One pack includes its animals, reactions, animation set and voice set. " +
                        "Only complete content-ready packs can spend gems or become equipped."
                textSize = 11.5f
                setTextColor(
                    0xFFCFE4FF.toInt(),
                )
                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(10),
                )
            },
        )
        addView(
            cosmeticGrid(
                items = StoreCosmeticCatalog.characterPacks,
                borderColor = 0xFFFFD45C.toInt(),
            ),
        )
    }
