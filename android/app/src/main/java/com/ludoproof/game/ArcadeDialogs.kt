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
    fun showSettings(context: Context) {
        val preferences = GamePreferences(context)
        val dialog = baseDialog(context)
        val panel = dialogPanel(context, "SETTINGS", "MAKE YOURSELF AT HOME", dialog)
        val speedGroup = android.widget.RadioGroup(context)
        GamePreferences.SPEEDS.forEach { speed ->
            speedGroup.addView(android.widget.RadioButton(context).apply {
                id = View.generateViewId()
                text = speed
                textSize = 15f
                setTextColor(LudoProofTheme.WHITE)
                buttonTintList = android.content.res.ColorStateList.valueOf(LudoProofTheme.GOLD)
                minHeight = dp(context, 48)
                isChecked = preferences.speed == speed
                setOnCheckedChangeListener { _, checked -> if (checked) preferences.speed = speed }
            })
        }
        // Restore selection after the buttons belong to the RadioGroup.
        val selectedSpeed = GamePreferences.SPEEDS.indexOf(preferences.speed)
        speedGroup.check(speedGroup.getChildAt(selectedSpeed).id)
        ArcadeUi.add(panel, ArcadeUi.text(context, "Dice animation speed", 15f), 14)
        ArcadeUi.add(panel, speedGroup, 2)
        fun toggle(label: String, checked: Boolean, save: (Boolean) -> Unit) {
            ArcadeUi.add(panel, android.widget.Switch(context).apply {
                text = label
                textSize = 15f
                setTextColor(LudoProofTheme.WHITE)
                minHeight = dp(context, 56)
                isChecked = checked
                thumbTintList = android.content.res.ColorStateList.valueOf(LudoProofTheme.GOLD)
                setOnCheckedChangeListener { _, value -> save(value) }
            }, 4)
        }
        toggle("Dice animation", preferences.animations) { preferences.animations = it }
        toggle("Touch feedback", preferences.haptics) { preferences.haptics = it }
        ArcadeUi.add(panel, ArcadeUi.text(context,
            "Saved automatically. Speed changes presentation only; dice outcomes and game rules stay the same. System motion and vibration settings are respected.", 12f), 14)
        ArcadeUi.add(panel, ArcadeUi.button(context, "Done", primary = true) { dialog.dismiss() }, 16)
        dialog.setContentView(scrollablePanel(context, panel))
        sizeDialog(dialog, .92f)
        dialog.show()
    }

    fun showInfo(context: Context, title: String, body: String) {
        val dialog = baseDialog(context)
        val panel = dialogPanel(context, title, "LUDOPROOF", dialog)
        ArcadeUi.add(panel, ArcadeUi.text(context, body, 15f), 16)
        ArcadeUi.add(panel, ArcadeUi.button(context, "Got it", primary = true) { dialog.dismiss() }, 20)
        dialog.setContentView(scrollablePanel(context, panel))
        sizeDialog(dialog, .92f)
        dialog.show()
    }

    fun confirm(context: Context, title: String, body: String, actionLabel: String, action: () -> Unit) {
        val dialog = baseDialog(context)
        val panel = dialogPanel(context, title, "LOCAL GAME", dialog)
        ArcadeUi.add(panel, ArcadeUi.text(context, body, 15f), 14)
        ArcadeUi.add(panel, ArcadeUi.button(context, actionLabel, primary = true) {
            dialog.dismiss()
            action()
        }, 18)
        ArcadeUi.add(panel, ArcadeUi.button(context, "Keep playing") { dialog.dismiss() }, 8)
        dialog.setContentView(scrollablePanel(context, panel))
        sizeDialog(dialog, .92f)
        dialog.show()
    }

    private fun scrollablePanel(context: Context, panel: View): ScrollView =
        object : ScrollView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val limit = (resources.displayMetrics.heightPixels * .85f).toInt()
                val available = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) limit
                    else minOf(limit, MeasureSpec.getSize(heightMeasureSpec))
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST))
            }
        }.apply { addView(panel) }
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

        dialog.setContentView(scrollablePanel(context, panel))
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

        dialog.setContentView(scrollablePanel(context, panel))
        sizeDialog(
            dialog,
            .92f,
        )
        dialog.show()
    }

    private fun evidenceScroll(
        context: Context,
        body: String,
        heightDp: Int,
    ): ScrollView =
        ScrollView(
            context,
        ).apply {
            isVerticalScrollBarEnabled =
                true
            overScrollMode =
                View.OVER_SCROLL_IF_CONTENT_SCROLLS
            background =
                LudoProofTheme
                    .hudPanelDrawable(
                        context,
                        goldBorder =
                            false,
                    )

            addView(
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
                        12f,
                        centered = false,
                        bright = true,
                    )
                    setPadding(
                        dp(
                            context,
                            14,
                        ),
                        dp(
                            context,
                            14,
                        ),
                        dp(
                            context,
                            14,
                        ),
                        dp(
                            context,
                            14,
                        ),
                    )
                    setLineSpacing(
                        0f,
                        1.12f,
                    )
                },
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(
                        context,
                        heightDp,
                    ),
                )
        }

    private fun emptyState(
        context: Context,
        icon: String,
        title: String,
        body: String,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER
            setPadding(
                dp(
                    context,
                    16,
                ),
                dp(
                    context,
                    22,
                ),
                dp(
                    context,
                    16,
                ),
                dp(
                    context,
                    22,
                ),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(
                        context,
                        goldBorder =
                            false,
                    )

            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        icon
                    textSize =
                        32f
                    gravity =
                        Gravity.CENTER
                    setTextColor(
                        0xFF70E7FF.toInt(),
                    )
                },
            )
            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        title
                    LudoProofTheme.title(
                        this,
                        17f,
                        gold = true,
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
            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        body
                    LudoProofTheme.body(
                        this,
                        12f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        dp(
                            context,
                            8,
                        ),
                        dp(
                            context,
                            6,
                        ),
                        dp(
                            context,
                            8,
                        ),
                        0,
                    )
                },
            )
        }

    private fun trustStrip(
        context: Context,
        title: String,
        body: String,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            setPadding(
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    11,
                ),
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    11,
                ),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(
                        context,
                        goldBorder =
                            true,
                    )

            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        "✓"
                    textSize =
                        22f
                    gravity =
                        Gravity.CENTER
                    setTextColor(
                        0xFF68F053.toInt(),
                    )
                },
                LinearLayout.LayoutParams(
                    dp(
                        context,
                        34,
                    ),
                    dp(
                        context,
                        34,
                    ),
                ),
            )

            addView(
                LinearLayout(
                    context,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    addView(
                        TextView(
                            context,
                        ).apply {
                            text =
                                title
                            LudoProofTheme.body(
                                this,
                                11f,
                                bright = true,
                            )
                            setTextColor(
                                LudoProofTheme.GOLD,
                            )
                        },
                    )
                    addView(
                        TextView(
                            context,
                        ).apply {
                            text =
                                body
                            LudoProofTheme.body(
                                this,
                                10.5f,
                                bright = true,
                            )
                            setPadding(
                                0,
                                dp(
                                    context,
                                    2,
                                ),
                                0,
                                0,
                            )
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        dp(
                            context,
                            8,
                        )
                },
            )
        }

    private fun metricCard(
        context: Context,
        label: String,
        value: String,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER
            setPadding(
                dp(
                    context,
                    5,
                ),
                dp(
                    context,
                    8,
                ),
                dp(
                    context,
                    5,
                ),
                dp(
                    context,
                    8,
                ),
            )
            background =
                LudoProofTheme
                    .rounded(
                        0xE807204E.toInt(),
                        14f,
                        0x6655E3FF,
                        1f,
                        context,
                    )

            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        value
                    LudoProofTheme.title(
                        this,
                        15f,
                        gold = true,
                    )
                },
            )
            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        label
                    LudoProofTheme.body(
                        this,
                        9f,
                        centered = true,
                        bright = true,
                    )
                    setPadding(
                        0,
                        dp(
                            context,
                            2,
                        ),
                        0,
                        0,
                    )
                },
            )
        }

    private fun metricParams(
        context: Context,
    ):
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        ).apply {
            setMargins(
                dp(
                    context,
                    4,
                ),
                0,
                dp(
                    context,
                    4,
                ),
                0,
            )
        }

    private fun statusChip(
        context: Context,
        value: String,
        color: Int,
    ): TextView =
        TextView(
            context,
        ).apply {
            text =
                value
            LudoProofTheme.body(
                this,
                10.5f,
                centered = true,
                bright = true,
            )
            setTextColor(
                color,
            )
            gravity =
                Gravity.CENTER
            setPadding(
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    7,
                ),
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    7,
                ),
            )
            background =
                LudoProofTheme
                    .rounded(
                        0xDD061B42.toInt(),
                        999f,
                        color,
                        1f,
                        context,
                    )
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
        subtitle: String,
        dialog: Dialog,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(
                    context,
                    if (
                        LudoProofTheme
                            .isCompactWidth(
                                context,
                            )
                    ) {
                        12
                    } else {
                        16
                    },
                ),
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    if (
                        LudoProofTheme
                            .isCompactWidth(
                                context,
                            )
                    ) {
                        12
                    } else {
                        16
                    },
                ),
                dp(
                    context,
                    16,
                ),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(
                        context,
                        goldBorder =
                            true,
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
                        "LP"
                    LudoProofTheme.title(
                        this,
                        13f,
                        gold = true,
                    )
                    gravity =
                        Gravity.CENTER
                    background =
                        LudoProofTheme
                            .brandBadgeDrawable(
                                context,
                            )
                },
                LinearLayout.LayoutParams(
                    dp(
                        context,
                        44,
                    ),
                    dp(
                        context,
                        44,
                    ),
                ),
            )

            header.addView(
                LinearLayout(
                    context,
                ).apply {
                    orientation =
                        LinearLayout.VERTICAL
                    gravity =
                        Gravity.CENTER_VERTICAL

                    addView(
                        TextView(
                            context,
                        ).apply {
                            text =
                                title
                            LudoProofTheme.title(
                                this,
                                if (
                                    LudoProofTheme
                                        .isCompactWidth(
                                            context,
                                        )
                                ) {
                                    18f
                                } else {
                                    21f
                                },
                                gold = true,
                            )
                            gravity =
                                Gravity.START or
                                    Gravity.CENTER_VERTICAL
                        },
                    )
                    addView(
                        TextView(
                            context,
                        ).apply {
                            text =
                                subtitle
                            LudoProofTheme.body(
                                this,
                                9.5f,
                                bright = true,
                            )
                            setTextColor(
                                0xFF70E7FF.toInt(),
                            )
                            setPadding(
                                0,
                                dp(
                                    context,
                                    2,
                                ),
                                0,
                                0,
                            )
                        },
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    marginStart =
                        dp(
                            context,
                            10,
                        )
                },
            )

            header.addView(
                Button(
                    context,
                ).apply {
                    LudoProofTheme
                        .homeCircularAction(
                            this,
                            "×",
                        )
                    contentDescription =
                        "Close"
                    setOnClickListener {
                        dialog.dismiss()
                    }
                },
                LinearLayout.LayoutParams(
                    dp(
                        context,
                        44,
                    ),
                    dp(
                        context,
                        44,
                    ),
                ),
            )
            addView(
                header,
            )
        }

    private fun settingsRow(
        context: Context,
        name: String,
        value: String,
        detail: String,
    ): LinearLayout =
        LinearLayout(
            context,
        ).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    10,
                ),
                dp(
                    context,
                    12,
                ),
                dp(
                    context,
                    10,
                ),
            )
            background =
                LudoProofTheme
                    .rounded(
                        0xE807204E.toInt(),
                        16f,
                        0x6655E3FF,
                        1f,
                        context,
                    )

            val top =
                LinearLayout(
                    context,
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            top.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        name
                    LudoProofTheme.body(
                        this,
                        14f,
                        bright = true,
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ),
            )

            top.addView(
                TextView(
                    context,
                ).apply {
                    text =
                        value
                    LudoProofTheme.body(
                        this,
                        10.5f,
                        bright = true,
                    )
                    setTextColor(
                        LudoProofTheme.GOLD,
                    )
                    setPadding(
                        dp(
                            context,
                            10,
                        ),
                        dp(
                            context,
                            5,
                        ),
                        dp(
                            context,
                            10,
                        ),
                        dp(
                            context,
                            5,
                        ),
                    )
                    background =
                        LudoProofTheme
                            .rounded(
                                0xDD07183D.toInt(),
                                999f,
                                0x88FFD45E.toInt(),
                                1f,
                                context,
                            )
                },
            )
            addView(
                top,
            )

            addView(
                TextView(
                    context,
                ).apply {
                    text =
                        detail
                    LudoProofTheme.body(
                        this,
                        10f,
                        bright = true,
                    )
                    setPadding(
                        0,
                        dp(
                            context,
                            5,
                        ),
                        0,
                        0,
                    )
                },
            )
        }

    private fun fullWidthParams(
        context: Context,
        topDp: Int,
    ):
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(
                    context,
                    topDp,
                )
        }

    private fun baseDialog(
        context: Context,
    ): Dialog =
        Dialog(
            context,
        ).apply {
            requestWindowFeature(
                Window.FEATURE_NO_TITLE,
            )
            window
                ?.setBackgroundDrawable(
                    ColorDrawable(
                        Color.TRANSPARENT,
                    ),
                )
            window
                ?.addFlags(
                    WindowManager.LayoutParams
                        .FLAG_DIM_BEHIND,
                )
            window
                ?.attributes =
                window
                    ?.attributes
                    ?.apply {
                        dimAmount =
                            .78f
                    }
        }

    private fun sizeDialog(
        dialog: Dialog,
        widthFraction: Float,
    ) {
        dialog.setOnShowListener {
            val context =
                dialog.context
            val metrics =
                context.resources
                    .displayMetrics
            val desired =
                (
                    metrics.widthPixels *
                        widthFraction
                    ).toInt()
            val maxWidth =
                dp(
                    context,
                    LudoProofTheme
                        .pageMaxContentWidthDp(
                            context,
                        ),
                )

            dialog.window
                ?.setLayout(
                    minOf(
                        desired,
                        maxWidth,
                    ),
                    WindowManager
                        .LayoutParams
                        .WRAP_CONTENT,
                )
        }
    }

    private fun dp(
        context: Context,
        value: Int,
    ): Int =
        LudoProofTheme.dp(
            context,
            value,
        )
}
