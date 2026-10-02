package com.ludoproof.game.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.HomeActivity
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.ui.dialogs.baseDialog
import com.ludoproof.game.ui.dialogs.sizeDialog
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun HomeActivity.showHomeRatingDialog() {
    if (
        isFinishing ||
        isDestroyed
    ) {
        return
    }

    val dialog =
        baseDialog(
            this,
        )
    val compact =
        LudoProofTheme
            .isCompactWidth(
                this,
            )

    val panel =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            gravity =
                Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(
                    if (compact) 16 else 22,
                ),
                dp(16),
                dp(
                    if (compact) 16 else 22,
                ),
                dp(22),
            )
            background =
                LudoProofTheme
                    .hudPanelDrawable(
                        this@showHomeRatingDialog,
                        goldBorder =
                            true,
                    )
        }

    val closeRow =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.END or
                    Gravity.CENTER_VERTICAL
        }
    closeRow.addView(
        Button(this).apply {
            text =
                "×"
            textSize =
                24f
            setTextColor(
                Color.WHITE,
            )
            gravity =
                Gravity.CENTER
            minWidth =
                0
            minHeight =
                0
            setPadding(
                0,
                0,
                0,
                0,
            )
            contentDescription =
                "Close rating"
            background =
                ratingCloseDrawable(
                    this@showHomeRatingDialog,
                )
            setOnClickListener {
                dialog.dismiss()
            }
        },
        LinearLayout.LayoutParams(
            dp(42),
            dp(42),
        ),
    )
    panel.addView(
        closeRow,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    panel.addView(
        TextView(this).apply {
            text =
                "RATE LUDOPROOF"
            LudoProofTheme.title(
                this,
                if (compact) 22f else 26f,
                gold = true,
            )
            gravity =
                Gravity.CENTER
            setPadding(
                0,
                dp(4),
                0,
                0,
            )
        },
    )

    panel.addView(
        TextView(this).apply {
            text =
                "Loving LudoProof?"
            LudoProofTheme.title(
                this,
                if (compact) 17f else 19f,
                gold = true,
            )
            gravity =
                Gravity.CENTER
            setPadding(
                0,
                dp(8),
                0,
                0,
            )
        },
    )

    panel.addView(
        TextView(this).apply {
            text =
                "Choose your stars, then continue to the Play Store."
            LudoProofTheme.body(
                this,
                if (compact) 11f else 12f,
                centered = true,
                bright = true,
            )
            gravity =
                Gravity.CENTER
            setPadding(
                dp(8),
                dp(7),
                dp(8),
                0,
            )
        },
    )

    var selectedRating =
        5
    lateinit var rateButton:
        Button

    val stars =
        mutableListOf<
            RatingStarView
            >()
    val starRow =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER
            setPadding(
                0,
                dp(
                    if (compact) 22 else 26,
                ),
                0,
                dp(
                    if (compact) 18 else 22,
                ),
            )
        }

    fun refreshStars() {
        stars.forEachIndexed {
                index,
                star ->
            star.isSelected =
                index <
                selectedRating
        }
        if (
            ::rateButton
                .isInitialized
        ) {
            rateButton.text =
                if (
                    selectedRating ==
                    1
                ) {
                    "Rate 1 Star on Play Store"
                } else {
                    "Rate " +
                        selectedRating +
                        " Stars on Play Store"
                }
        }
    }

    repeat(
        5,
    ) {
            index ->
        val star =
            RatingStarView(
                this,
            ).apply {
                isSelected =
                    index <
                    selectedRating
                contentDescription =
                    (
                        index +
                            1
                        )
                        .toString() +
                        " star rating"
                isClickable =
                    true
                isFocusable =
                    true
                setOnClickListener {
                    selectedRating =
                        index +
                        1
                    refreshStars()
                }
            }
        stars +=
            star
        starRow.addView(
            star,
            LinearLayout.LayoutParams(
                0,
                dp(
                    if (compact) 54 else 62,
                ),
                1f,
            ).apply {
                marginStart =
                    dp(2)
                marginEnd =
                    dp(2)
            },
        )
    }
    panel.addView(
        starRow,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    rateButton =
        Button(this).apply {
            text =
                "Rate 5 Stars on Play Store"
            textSize =
                if (compact) 14f else 15f
            setTextColor(
                Color.WHITE,
            )
            setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD,
            )
            gravity =
                Gravity.CENTER
            minHeight =
                0
            minWidth =
                0
            setPadding(
                dp(14),
                0,
                dp(14),
                0,
            )
            background =
                ratingPrimaryButtonDrawable(
                    this@showHomeRatingDialog,
                )
            setOnClickListener {
                dialog.dismiss()
                openPlayStoreRating(
                    this@showHomeRatingDialog,
                )
            }
        }
    panel.addView(
        rateButton,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(
                if (compact) 52 else 56,
            ),
        ).apply {
            leftMargin =
                dp(
                    if (compact) 20 else 34,
                )
            rightMargin =
                dp(
                    if (compact) 20 else 34,
                )
        },
    )

    panel.addView(
        TextView(this).apply {
            text =
                "Not now"
            LudoProofTheme.body(
                this,
                if (compact) 13f else 14f,
                centered = true,
                bright = true,
            )
            gravity =
                Gravity.CENTER
            isClickable =
                true
            isFocusable =
                true
            setPadding(
                dp(12),
                dp(18),
                dp(12),
                dp(8),
            )
            contentDescription =
                "Not now"
            setOnClickListener {
                dialog.dismiss()
            }
        },
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    refreshStars()

    dialog.setContentView(
        panel,
    )
    sizeDialog(
        dialog,
        .94f,
    )
    dialog.show()
}

private fun openPlayStoreRating(
    activity: Activity,
) {
    val packageName =
        activity.packageName
    val marketIntent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "market://details?id=" +
                    packageName,
            ),
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
            )
        }

    val openedMarket =
        runCatching {
            activity.startActivity(
                marketIntent,
            )
        }
            .isSuccess

    if (
        openedMarket
    ) {
        return
    }

    runCatching {
        activity.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://play.google.com/store/apps/details?id=" +
                        packageName,
                ),
            ),
        )
    }
}

private class RatingStarView(
    context: Context,
) : View(context) {
    var isSelected:
        Boolean =
        false
        set(value) {
            field = value
            invalidate()
        }

    private val fill =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.FILL
            color =
                0xFFFFD31F.toInt()
        }

    private val outline =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.STROKE
            strokeWidth =
                5f
            strokeJoin =
                Paint.Join.ROUND
            color =
                0xFFFFC84A.toInt()
        }

    private val inner =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        ).apply {
            style =
                Paint.Style.FILL
            color =
                0x33000000
        }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(
            canvas,
        )

        val size =
            min(
                width,
                height,
            )
                .toFloat()
        if (
            size <=
            0f
        ) {
            return
        }

        val left =
            (
                width -
                    size
                ) /
                2f
        val top =
            (
                height -
                    size
                ) /
                2f
        val scale =
            size /
                100f

        canvas.save()
        canvas.translate(
            left,
            top,
        )
        canvas.scale(
            scale,
            scale,
        )

        val star =
            starPath()
        if (
            isSelected
        ) {
            canvas.drawPath(
                star,
                fill,
            )
            canvas.drawPath(
                star,
                outline,
            )
        } else {
            canvas.drawPath(
                star,
                inner,
            )
            canvas.drawPath(
                star,
                outline,
            )
        }

        canvas.restore()
    }

    private fun starPath():
        Path {
        val path =
            Path()
        for (
            index in
            0 until 10
        ) {
            val radius =
                if (
                    index %
                    2 ==
                    0
                ) {
                    37f
                } else {
                    16f
                }
            val angle =
                Math.toRadians(
                    -90.0 +
                        index *
                        36.0,
                )
            val x =
                50f +
                    cos(
                        angle,
                    )
                        .toFloat() *
                    radius
            val y =
                50f +
                    sin(
                        angle,
                    )
                        .toFloat() *
                    radius
            if (
                index ==
                0
            ) {
                path.moveTo(
                    x,
                    y,
                )
            } else {
                path.lineTo(
                    x,
                    y,
                )
            }
        }
        path.close()
        return path
    }
}

private fun ratingPrimaryButtonDrawable(
    context: Context,
):
    StateListDrawable =
    StateListDrawable().apply {
        addState(
            intArrayOf(
                android.R.attr.state_pressed,
            ),
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFFFFB21E.toInt(),
                    0xFFF17A00.toInt(),
                    0xFFD65C00.toInt(),
                ),
            ).apply {
                cornerRadius =
                    LudoProofTheme.dp(
                        context,
                        999,
                    )
                        .toFloat()
                setStroke(
                    LudoProofTheme.dp(
                        context,
                        2,
                    ),
                    0xFFFFE173.toInt(),
                )
            },
        )
        addState(
            intArrayOf(),
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xFFFFD142.toInt(),
                    0xFFFF9800.toInt(),
                    0xFFE66B00.toInt(),
                ),
            ).apply {
                cornerRadius =
                    LudoProofTheme.dp(
                        context,
                        999,
                    )
                        .toFloat()
                setStroke(
                    LudoProofTheme.dp(
                        context,
                        2,
                    ),
                    0xFFFFEFA2.toInt(),
                )
            },
        )
    }

private fun ratingCloseDrawable(
    context: Context,
):
    StateListDrawable =
    StateListDrawable().apply {
        addState(
            intArrayOf(
                android.R.attr.state_pressed,
            ),
            GradientDrawable().apply {
                shape =
                    GradientDrawable.OVAL
                setColor(
                    0xFFE87300.toInt(),
                )
                setStroke(
                    LudoProofTheme.dp(
                        context,
                        2,
                    ),
                    0xFFFFE173.toInt(),
                )
            },
        )
        addState(
            intArrayOf(),
            GradientDrawable().apply {
                shape =
                    GradientDrawable.OVAL
                setColor(
                    0xFFFF9800.toInt(),
                )
                setStroke(
                    LudoProofTheme.dp(
                        context,
                        2,
                    ),
                    0xFFFFEFA2.toInt(),
                )
            },
        )
    }
