package com.ludoproof.game.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.*

internal fun showSettingsDialog(
    context: Context,
) {
    val dialog =
        baseDialog(
            context,
        )
    val panel =
        dialogPanel(
            context,
            "SETTINGS",
            "CURRENT CONFIGURATION",
            dialog,
        )

    panel.addView(
        statusChip(
            context,
            "READ-ONLY IN THIS BUILD",
            0xFF70E7FF.toInt(),
        ),
        fullWidthParams(
            context,
            topDp = 8,
        ),
    )

    listOf(
        Triple(
            "Online sync",
            "AUTO",
            "Reconnects and refreshes the current verified match.",
        ),
        Triple(
            "Proof mode",
            "ENTRONEX V4",
            "Online rolls use server commitments and attestations.",
        ),
        Triple(
            "Offline rolls",
            "V4 LOCAL",
            "Same v4 derivation recomputed on this device.",
        ),
        Triple(
            "Game speed",
            "NORMAL",
            "Standard animation and interaction timing.",
        ),
        Triple(
            "Board",
            "CLASSIC",
            "Classic Ludo board presentation.",
        ),
        Triple(
            "Dice",
            "CLASSIC",
            "Standard six-sided dice presentation.",
        ),
    ).forEach {
            item ->
        panel.addView(
            settingsRow(
                context,
                item.first,
                item.second,
                item.third,
            ),
            fullWidthParams(
                context,
                topDp = 9,
            ),
        )
    }

    panel.addView(
        trustStrip(
            context,
            "VERIFICATION MODEL",
            "Online uses remote EntroNex authority. Offline uses the same v4 derivation locally and does not claim remote attestation.",
        ),
        fullWidthParams(
            context,
            topDp = 12,
        ),
    )

    dialog.setContentView(
        panel,
    )
    sizeDialog(
        dialog,
        .92f,
    )
    dialog.show()
}
