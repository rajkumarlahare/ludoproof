package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ludoproof.game.ui.home.*

class HomeActivity : Activity() {
    internal lateinit var connectivityText:
        TextView
    internal var homeProfileNameText:
        TextView? = null
    internal var homeProfileLevelText:
        TextView? = null
    internal var homeProfileXpFill:
        View? = null
    internal var homeGemBalanceText:
        TextView? = null
    internal var homeAvatarText:
        TextView? = null
    internal lateinit var connectivityMonitor:
        ConnectivityMonitor

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)

        val (root, host) =
            LudoProofTheme.arcadeRoot(this)

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
                clipToPadding =
                    false
                setPadding(
                    0,
                    0,
                    0,
                    dp(if (isCompact()) 128 else 142),
                )
            }

        val contentHost =
            FrameLayout(this)
        scroll.addView(
            contentHost,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val horizontalPaddingDp =
            LudoProofTheme.pageHorizontalPaddingDp(this)
        val availableWidth =
            (
                resources.displayMetrics.widthPixels -
                    dp(horizontalPaddingDp * 2)
                ).coerceAtLeast(1)
        val contentWidth =
            minOf(
                availableWidth,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(this),
                ),
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(16),
                    0,
                    dp(26),
                )
            }
        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                contentWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ),
        )

        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        host.addView(
            homeBottomActions(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(if (isCompact()) 54 else 60),
                Gravity.BOTTOM or
                    Gravity.CENTER_HORIZONTAL,
            ).apply {
                leftMargin =
                    dp(if (isCompact()) 18 else 24)
                rightMargin =
                    dp(if (isCompact()) 18 else 24)
                bottomMargin =
                    dp(if (isCompact()) 46 else 54)
            },
        )

        content.addView(
            profileHud(),
        )

        content.addView(
            modeSection(),
            fullWidthSection(
                if (isCompact()) 20 else 24,
            ),
        )

        continueButton()
            ?.let { button ->
                content.addView(
                    button,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(if (isCompact()) 58 else 62),
                    ).apply {
                        setMargins(
                            if (isCompact()) dp(12) else dp(34),
                            dp(18),
                            if (isCompact()) dp(12) else dp(34),
                            0,
                        )
                    },
                )
            }

        setContentView(root)

        connectivityMonitor =
            ConnectivityMonitor(this) { online ->
                runOnUiThread {
                    connectivityText.text =
                        if (online) {
                            "● ONLINE"
                        } else {
                            "● OFFLINE"
                        }
                    connectivityText
                        .setTextColor(
                            if (online) {
                                0xFF68F053.toInt()
                            } else {
                                LudoProofTheme.GOLD
                            },
                        )
                    connectivityText.contentDescription =
                        if (online) {
                            "Online"
                        } else {
                            "Offline"
                        }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeProfileSummary()
        refreshHomeGemBalance()
    }

    override fun onStart() {
        super.onStart()
        connectivityMonitor.start()
    }

    override fun onStop() {
        connectivityMonitor.stop()
        super.onStop()
    }

}
