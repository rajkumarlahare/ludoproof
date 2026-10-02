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

internal fun showNaturalWorldAuditDialog(
    context: Context,
    audit: OfflineRandomnessAudit?,
) {
    val dialog =
        baseDialog(
            context,
        )
    val panel =
        dialogPanel(
            context,
            "V4 LOCAL ENGINE",
            "NATURAL WORLD AUDIT",
            dialog,
        )

    if (
        audit ==
        null
    ) {
        panel.addView(
            emptyState(
                context,
                "◎",
                "NO LOCAL WORLD YET",
                "Roll the offline dice once to generate a local EntroNex v4 Natural World.",
            ),
            fullWidthParams(
                context,
                topDp = 12,
            ),
        )
    } else {
        val verified =
            audit.verify()

        panel.addView(
            statusChip(
                context,
                if (
                    verified
                ) {
                    "✓ LOCAL V4 RECOMPUTATION VALID"
                } else {
                    "✕ LOCAL V4 RECOMPUTATION FAILED"
                },
                if (
                    verified
                ) {
                    LudoProofTheme.GREEN
                } else {
                    LudoProofTheme.RED
                },
            ),
            fullWidthParams(
                context,
                topDp = 10,
            ),
        )

        val metrics =
            LinearLayout(
                context,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
            }

        metrics.addView(
            metricCard(
                context,
                "OUTCOME",
                audit.outcome
                    .toString(),
            ),
            metricParams(
                context,
            ),
        )
        metrics.addView(
            metricCard(
                context,
                "SAMPLE",
                audit.sampleIndex
                    .toString(),
            ),
            metricParams(
                context,
            ),
        )
        metrics.addView(
            metricCard(
                context,
                "WORLD",
                audit.width
                    .toString() +
                    "×" +
                    audit.height,
            ),
            metricParams(
                context,
            ),
        )
        panel.addView(
            metrics,
            fullWidthParams(
                context,
                topDp = 10,
            ),
        )

        val map =
            NaturalWorldMapView(
                context,
            ).apply {
                bind(
                    audit,
                )
                background =
                    LudoProofTheme
                        .hudPanelDrawable(
                            context,
                            goldBorder =
                                false,
                        )
                setPadding(
                    dp(
                        context,
                        8,
                    ),
                    dp(
                        context,
                        8,
                    ),
                    dp(
                        context,
                        8,
                    ),
                    dp(
                        context,
                        8,
                    ),
                )
            }
        panel.addView(
            map,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(
                    context,
                    if (
                        LudoProofTheme
                            .isCompactWidth(
                                context,
                            )
                    ) {
                        205
                    } else {
                        240
                    },
                ),
            ).apply {
                topMargin =
                    dp(
                        context,
                        10,
                    )
            },
        )

        panel.addView(
            TextView(
                context,
            ).apply {
                text =
                    "SELECTED CELL IS RINGED IN GOLD"
                LudoProofTheme.body(
                    this,
                    9.5f,
                    centered = true,
                    bright = true,
                )
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                setPadding(
                    0,
                    dp(
                        context,
                        6,
                    ),
                    0,
                    0,
                )
            },
        )

        val detail =
            buildString {
                append(
                    "OUTCOME\n",
                )
                append(
                    "Dice: ",
                )
                append(
                    audit.outcome,
                )
                append(
                    "   •   Index: ",
                )
                append(
                    audit.outcomeIndex,
                )

                append(
                    "\n\nWORLD SAMPLE\n",
                )
                append(
                    "Cells: ",
                )
                append(
                    audit.field.size,
                )
                append(
                    "   •   Sample: ",
                )
                append(
                    audit.sampleIndex,
                )
                append(
                    "   •   Tick: ",
                )
                append(
                    audit.sampleTick,
                )
                append(
                    "\nEpoch: ",
                )
                append(
                    audit.layoutEpoch,
                )
                append(
                    "   •   Motion: ",
                )
                append(
                    audit.motionProfile,
                )
                append(
                    "   •   Probe: ",
                )
                append(
                    audit.selectedProbe,
                )

                append(
                    "\n\nWITNESS\n",
                )
                append(
                    "Swap: ",
                )
                append(
                    audit.witnessSwapped,
                )
                append(
                    "   •   ",
                )
                append(
                    audit.witnessSourceIndex,
                )
                append(
                    " → ",
                )
                append(
                    audit.witnessTargetIndex,
                )

                append(
                    "\n\nDIGESTS\n",
                )
                append(
                    "Proof: ",
                )
                append(
                    shortDigest(
                        audit.proofDigest,
                    ),
                )
                append(
                    "\nWorld: ",
                )
                append(
                    shortDigest(
                        audit.worldDigest,
                    ),
                )
                append(
                    "\nField: ",
                )
                append(
                    shortDigest(
                        audit.fieldDigest,
                    ),
                )
                append(
                    "\nTranscript: ",
                )
                append(
                    shortDigest(
                        audit.transcriptDigest,
                    ),
                )
                append(
                    "\nEvent binding: ",
                )
                append(
                    shortDigest(
                        audit.eventBindingDigest,
                    ),
                )
            }

        panel.addView(
            evidenceScroll(
                context,
                detail,
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            context,
                        )
                ) {
                    220
                } else {
                    260
                },
            ),
            fullWidthParams(
                context,
                topDp = 10,
            ),
        )

        panel.addView(
            trustStrip(
                context,
                "LOCAL AUDIT",
                "Both offline seeds are generated on this device. This verifies local recomputation; it is not a remote EntroNex attestation.",
            ),
            fullWidthParams(
                context,
                topDp = 10,
            ),
        )
    }

    dialog.setContentView(
        panel,
    )
    sizeDialog(
        dialog,
        .94f,
    )
    dialog.show()
}
