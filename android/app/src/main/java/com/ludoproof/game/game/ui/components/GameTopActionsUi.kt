package com.ludoproof.game.ui.game

import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.ui.home.HomeGlassShape
import com.ludoproof.game.ui.home.homeGlassBackground
import com.ludoproof.game.ui.home.HomeIconKind
import com.ludoproof.game.ui.home.HomeIconView

/**
 * Fixed top-level game controls.
 *
 * These controls deliberately live in the activity viewport rather than the
 * gameplay vertical flow, so board placement cannot move when the game layout
 * changes. The visual chrome reuses the Home glass treatment.
 */
internal fun addGameTopActions(
    host: FrameLayout,
    context: android.content.Context,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    val bar =
        FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

    bar.addView(
        gameTopButton(
            context = context,
            contentDescription = "Back",
            icon = GameChevronIconView(context),
            onClick = onBack,
        ),
        FrameLayout.LayoutParams(
            LudoProofTheme.dp(context, 52),
            LudoProofTheme.dp(context, 52),
            Gravity.TOP or Gravity.START,
        ).apply {
            leftMargin = LudoProofTheme.dp(context, 16)
            topMargin = LudoProofTheme.dp(context, 14)
        },
    )

    bar.addView(
        gameTopButton(
            context = context,
            contentDescription = "Game settings",
            icon =
                HomeIconView(context).apply {
                    kind = HomeIconKind.SETTINGS
                    iconColor = android.graphics.Color.WHITE
                    importantForAccessibility =
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
            onClick = onSettings,
        ),
        FrameLayout.LayoutParams(
            LudoProofTheme.dp(context, 52),
            LudoProofTheme.dp(context, 52),
            Gravity.TOP or Gravity.END,
        ).apply {
            rightMargin = LudoProofTheme.dp(context, 16)
            topMargin = LudoProofTheme.dp(context, 14)
        },
    )

    host.addView(
        bar,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ),
    )
}

private fun gameTopButton(
    context: android.content.Context,
    contentDescription: String,
    icon: View,
    onClick: () -> Unit,
): FrameLayout =
    FrameLayout(context).apply {
        isClickable = true
        isFocusable = true
        this.contentDescription = contentDescription
        background =
            homeGlassBackground(
                context = context,
                shape = HomeGlassShape.CIRCLE,
            )
        elevation = LudoProofTheme.dp(context, 7).toFloat()
        setOnClickListener { onClick() }

        addView(
            icon,
            FrameLayout.LayoutParams(
                LudoProofTheme.dp(context, 34),
                LudoProofTheme.dp(context, 34),
                Gravity.CENTER,
            ),
        )
    }

private class GameChevronIconView(
    context: android.content.Context,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
            strokeWidth = LudoProofTheme.dp(context, 4f).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val span = size * 0.24f
        canvas.drawLine(cx + span * 0.55f, cy - span, cx - span * 0.55f, cy, paint)
        canvas.drawLine(cx - span * 0.55f, cy, cx + span * 0.55f, cy + span, paint)
    }
}
