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

internal fun showProofHistoryDialog(
    context: Context,
    title: String,
    body: String,
) {
    val dialog =
        baseDialog(
            context,
        )
    val panel =
        dialogPanel(
            context,
            title,
            "SELECTABLE AUDIT TEXT",
            dialog,
        )

    panel.addView(
        statusChip(
            context,
            "LONG-PRESS TO SELECT / COPY",
            0xFF70E7FF.toInt(),
        ),
        fullWidthParams(
            context,
            topDp = 8,
        ),
    )

    panel.addView(
        evidenceScroll(
            context,
            body,
            if (
                LudoProofTheme
                    .isCompactWidth(
                        context,
                    )
            ) {
                330
            } else {
                390
            },
        ),
        fullWidthParams(
            context,
            topDp = 10,
        ),
    )

    panel.addView(
        TextView(
            context,
        ).apply {
            text =
                "Evidence is shown exactly as stored by the current game flow."
            LudoProofTheme.body(
                this,
                10f,
                centered = true,
            )
            setPadding(
                dp(
                    context,
                    6,
                ),
                dp(
                    context,
                    8,
                ),
                dp(
                    context,
                    6,
                ),
                0,
            )
        },
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
