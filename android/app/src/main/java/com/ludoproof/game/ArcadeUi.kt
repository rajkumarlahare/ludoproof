package com.ludoproof.game

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Shared, scrollable native layouts; controls can grow with the system font. */
object ArcadeUi {
    fun page(activity: Activity, footer: View? = null): LinearLayout {
        val (root, host) = LudoProofTheme.arcadeRoot(activity)
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        val container = FrameLayout(activity)
        val content = object : LinearLayout(activity) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val width = minOf(MeasureSpec.getSize(widthMeasureSpec), dp(context, 560))
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 24))
        }
        container.addView(content, FrameLayout.LayoutParams(-1, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        scroll.addView(container, FrameLayout.LayoutParams(-1, -2))
        if (footer == null) {
            host.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        } else {
            val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            column.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            val dock = FrameLayout(activity).apply {
                setPadding(dp(context, 20), dp(context, 10), dp(context, 20), dp(context, 12))
                setBackgroundColor(0x6004144C)
                addView(footer, FrameLayout.LayoutParams(-1, -2))
            }
            column.addView(dock, LinearLayout.LayoutParams(-1, -2))
            host.addView(column, FrameLayout.LayoutParams(-1, -1))
        }
        activity.setContentView(root)
        return content
    }

    fun text(context: Context, value: String, size: Float = 14f, centered: Boolean = false): TextView =
        TextView(context).apply {
            text = value
            LudoProofTheme.body(this, size, centered, bright = true)
            setLineSpacing(dp(context, 2).toFloat(), 1f)
        }

    fun title(context: Context, value: String, size: Float = 20f): TextView =
        text(context, value, size, true).apply { LudoProofTheme.title(this, size, gold = true) }

    fun button(context: Context, label: String, primary: Boolean = false, action: () -> Unit): Button =
        Button(context).apply {
            text = label
            if (primary) LudoProofTheme.primary(this) else LudoProofTheme.secondary(this)
            textSize = 14f
            isAllCaps = false
            setOnClickListener { action() }
        }

    fun icon(context: Context, symbol: String, label: String, action: () -> Unit): Button =
        Button(context).apply {
            LudoProofTheme.circularAction(this, symbol)
            setPadding(0, 0, 0, 0)
            contentDescription = label
            setOnClickListener { action() }
        }

    fun header(activity: Activity, label: String, back: () -> Unit): LinearLayout =
        row(activity).apply {
            addView(icon(activity, "‹", "Back", back), LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)))
            addView(title(activity, label, 18f), LinearLayout.LayoutParams(0, -2, 1f))
            addView(icon(activity, "⚙", "Settings") { ArcadeDialogs.showSettings(activity) },
                LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)))
        }

    fun row(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun section(context: Context, heading: String): LinearLayout = LudoProofTheme.panel(context).apply {
        addView(title(context, heading, 16f).apply { setTextColor(LudoProofTheme.WHITE) }, params(context, 0))
    }

    fun params(context: Context, top: Int = 12): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, top) }

    fun add(parent: LinearLayout, view: View, top: Int = 12) {
        parent.addView(view, params(parent.context, top))
    }

    fun dp(context: Context, value: Int): Int = LudoProofTheme.dp(context, value)
}
