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
                "LOCAL",
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
                    "Online rolls stay server-authoritative. Offline mode never claims an EntroNex proof."
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
