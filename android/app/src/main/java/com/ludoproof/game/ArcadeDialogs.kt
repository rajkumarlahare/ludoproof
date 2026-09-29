package com.ludoproof.game

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

object ArcadeDialogs {
    fun showSettings(
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
                dialog,
            )

        panel.addView(
            settingsRow(
                context,
                "Online sync",
                "AUTO",
            ),
        )
        panel.addView(
            divider(
                context,
            ),
        )
        panel.addView(
            settingsRow(
                context,
                "Proof mode",
                "ENTRONEX V4",
            ),
        )
        panel.addView(
            divider(
                context,
            ),
        )
        panel.addView(
            settingsRow(
                context,
                "Offline rolls",
                "V4 LOCAL",
            ),
        )
        panel.addView(
            divider(
                context,
            ),
        )
        panel.addView(
            settingsRow(
                context,
                "Game speed",
                "NORMAL",
            ),
        )
        panel.addView(
            divider(
                context,
            ),
        )
        panel.addView(
            settingsRow(
                context,
                "Board",
                "CLASSIC",
            ),
        )
        panel.addView(
            divider(
                context,
            ),
        )
        panel.addView(
            settingsRow(
                context,
                "Dice",
                "CLASSIC",
            ),
        )

        panel.addView(
            TextView(
                context,
            ).apply {
                text =
                    "Online uses EntroNex server commitments and attestations. Offline uses the same v4 derivation and Natural World logic locally, but has no remote server attestation."
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                )
                setPadding(
                    14,
                    22,
                    14,
                    4,
                )
            },
        )

        dialog.setContentView(
            panel,
        )
        sizeDialog(
            dialog,
            .90f,
        )
        dialog.show()
    }

    fun showNaturalWorldAudit(
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
                dialog,
            )

        if (
            audit == null
        ) {
            panel.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        "Roll the offline dice once to generate a local EntroNex v4 Natural World."
                    LudoProofTheme.body(
                        this,
                        14f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        LudoProofTheme.dp(
                            context,
                            16,
                        ),
                        LudoProofTheme.dp(
                            context,
                            24,
                        ),
                        LudoProofTheme.dp(
                            context,
                            16,
                        ),
                        LudoProofTheme.dp(
                            context,
                            24,
                        ),
                    )
                },
            )
        } else {
            val verified =
                audit.verify()

            panel.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        if (
                            verified
                        ) {
                            "✓ LOCAL V4 RECOMPUTATION VALID"
                        } else {
                            "✕ LOCAL V4 RECOMPUTATION FAILED"
                        }
                    setTextColor(
                        if (
                            verified
                        ) {
                            LudoProofTheme.GREEN
                        } else {
                            LudoProofTheme.RED
                        },
                    )
                    textSize =
                        14f
                    gravity =
                        Gravity.CENTER
                    setPadding(
                        0,
                        LudoProofTheme.dp(
                            context,
                            10,
                        ),
                        0,
                        LudoProofTheme.dp(
                            context,
                            10,
                        ),
                    )
                },
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
                            .darkPanelDrawable(
                                context,
                                goldBorder =
                                    false,
                            )
                }
            panel.addView(
                map,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LudoProofTheme.dp(
                        context,
                        240,
                    ),
                ),
            )

            val detail =
                buildString {
                    append(
                        "Outcome: ",
                    )
                    append(
                        audit.outcome,
                    )
                    append(
                        "   Outcome index: ",
                    )
                    append(
                        audit.outcomeIndex,
                    )
                    append(
                        "\nWorld: ",
                    )
                    append(
                        audit.width,
                    )
                    append(
                        "×",
                    )
                    append(
                        audit.height,
                    )
                    append(
                        " = ",
                    )
                    append(
                        audit.field.size,
                    )
                    append(
                        " cells (16 of each 1–6)",
                    )
                    append(
                        "\nSample cell: ",
                    )
                    append(
                        audit.sampleIndex,
                    )
                    append(
                        "   Tick: ",
                    )
                    append(
                        audit.sampleTick,
                    )
                    append(
                        "   Epoch: ",
                    )
                    append(
                        audit.layoutEpoch,
                    )
                    append(
                        "\nMotion: ",
                    )
                    append(
                        audit.motionProfile,
                    )
                    append(
                        "   Probe: ",
                    )
                    append(
                        audit.selectedProbe,
                    )
                    append(
                        "\nWitness swap: ",
                    )
                    append(
                        audit.witnessSwapped,
                    )
                    append(
                        " (",
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
                        ")",
                    )
                    append(
                        "\n\nProof: ",
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
                    append(
                        "\n\nOffline uses the same v4 outcome/map derivation, but both seeds are generated on this device. It is not a remote EntroNex attestation."
                    )
                }

            val scroll =
                ScrollView(
                    context,
                )
            scroll.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        detail
                    setTextIsSelectable(
                        true,
                    )
                    LudoProofTheme.body(
                        this,
                        12f,
                        bright = true,
                    )
                    setPadding(
                        LudoProofTheme.dp(
                            context,
                            12,
                        ),
                        LudoProofTheme.dp(
                            context,
                            12,
                        ),
                        LudoProofTheme.dp(
                            context,
                            12,
                        ),
                        LudoProofTheme.dp(
                            context,
                            12,
                        ),
                    )
                },
            )
            panel.addView(
                scroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LudoProofTheme.dp(
                        context,
                        260,
                    ),
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

    fun showProofHistory(
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
                dialog,
            )

        val scroll =
            ScrollView(
                context,
            )
        scroll.addView(
            TextView(
                context,
            ).apply {
                text =
                    body
                setTextIsSelectable(
                    true,
                )
                LudoProofTheme.body(
                    this,
                    13f,
                    centered = false,
                    bright = true,
                )
                setPadding(
                    LudoProofTheme.dp(
                        context,
                        12,
                    ),
                    LudoProofTheme.dp(
                        context,
                        12,
                    ),
                    LudoProofTheme.dp(
                        context,
                        12,
                    ),
                    LudoProofTheme.dp(
                        context,
                        12,
                    ),
                )
                background =
                    LudoProofTheme
                        .darkPanelDrawable(
                            context,
                            goldBorder =
                                false,
                        )
            },
        )
        panel.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams
                    .MATCH_PARENT,
                LudoProofTheme.dp(
                    context,
                    360,
                ),
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

    private fun shortDigest(
        value: String,
    ): String =
        if (
            value.length <=
                20
        ) {
            value
        } else {
            value.take(
                10,
            ) +
                "…" +
                value.takeLast(
                    10,
                )
        }

    private fun dialogPanel(
        context: Context,
        title: String,
        dialog: Dialog,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                LudoProofTheme.dp(
                    context,
                    16,
                ),
                LudoProofTheme.dp(
                    context,
                    12,
                ),
                LudoProofTheme.dp(
                    context,
                    16,
                ),
                LudoProofTheme.dp(
                    context,
                    18,
                ),
            )
            background =
                LudoProofTheme
                    .darkPanelDrawable(
                        context,
                        goldBorder = true,
                    )

            val header =
                LinearLayout(
                    context,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            header.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        title
                    LudoProofTheme.title(
                        this,
                        22f,
                        gold = true,
                    )
                    gravity =
                        Gravity.START or
                            Gravity.CENTER_VERTICAL
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                    1f,
                ),
            )

            header.addView(
                Button(
                    context,
                ).apply {
                    LudoProofTheme
                        .circularAction(
                            this,
                            "×",
                        )
                    setOnClickListener {
                        dialog.dismiss()
                    }
                },
                LinearLayout.LayoutParams(
                    LudoProofTheme.dp(
                        context,
                        48,
                    ),
                    LudoProofTheme.dp(
                        context,
                        48,
                    ),
                ),
            )
            addView(header)
        }

    private fun settingsRow(
        context: Context,
        name: String,
        value: String,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            setPadding(
                LudoProofTheme.dp(
                    context,
                    4,
                ),
                LudoProofTheme.dp(
                    context,
                    13,
                ),
                LudoProofTheme.dp(
                    context,
                    4,
                ),
                LudoProofTheme.dp(
                    context,
                    13,
                ),
            )

            addView(
                TextView(
                    context,
                ).apply {
                    text = name
                    LudoProofTheme.body(
                        this,
                        16f,
                        bright = true,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                    1f,
                ),
            )
            addView(
                TextView(
                    context,
                ).apply {
                    text = value
                    LudoProofTheme.body(
                        this,
                        13f,
                        bright = true,
                    )
                    setPadding(
                        LudoProofTheme.dp(
                            context,
                            13,
                        ),
                        LudoProofTheme.dp(
                            context,
                            8,
                        ),
                        LudoProofTheme.dp(
                            context,
                            13,
                        ),
                        LudoProofTheme.dp(
                            context,
                            8,
                        ),
                    )
                    background =
                        LudoProofTheme
                            .darkPanelDrawable(
                                context,
                                goldBorder =
                                    true,
                            )
                },
            )
        }

    private fun divider(
        context: Context,
    ): View =
        View(context).apply {
            setBackgroundColor(
                0x304FA1FF,
            )
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams
                        .MATCH_PARENT,
                    LudoProofTheme.dp(
                        context,
                        1,
                    ),
                )
        }

    private fun baseDialog(
        context: Context,
    ): Dialog =
        Dialog(context).apply {
            requestWindowFeature(
                Window.FEATURE_NO_TITLE,
            )
            window?.setBackgroundDrawable(
                ColorDrawable(
                    Color.TRANSPARENT,
                ),
            )
            window?.addFlags(
                WindowManager.LayoutParams
                    .FLAG_DIM_BEHIND,
            )
            window?.attributes =
                window?.attributes
                    ?.apply {
                        dimAmount = .74f
                    }
        }

    private fun sizeDialog(
        dialog: Dialog,
        widthFraction: Float,
    ) {
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (
                    dialog
                        .context
                        .resources
                        .displayMetrics
                        .widthPixels *
                        widthFraction
                    ).toInt(),
                WindowManager
                    .LayoutParams
                    .WRAP_CONTENT,
            )
        }
    }
}
