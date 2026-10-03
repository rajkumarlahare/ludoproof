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
        val topHudInsetReductionDp =
            if (isCompact()) 6 else 8
        val availableWidth =
            (
                resources.displayMetrics.widthPixels -
                    dp(horizontalPaddingDp * 2)
                ).coerceAtLeast(1)
        val lowerContentWidth =
            minOf(
                availableWidth,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(this),
                ),
            )
        val topHudHorizontalPaddingDp =
            (
                horizontalPaddingDp -
                    topHudInsetReductionDp
                )
                .coerceAtLeast(6)
        val topHudWidth =
            minOf(
                resources.displayMetrics.widthPixels -
                    dp(
                        topHudHorizontalPaddingDp *
                            2,
                    ),
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(this) +
                        topHudInsetReductionDp *
                        2,
                ),
            )
        val modeSectionWidth =
            (
                lowerContentWidth *
                    0.92f
                )
                .toInt()
        val continueButtonWidth =
            (
                lowerContentWidth *
                    0.70f
                )
                .toInt()

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(26),
                )
            }
        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                topHudWidth,
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
            LinearLayout.LayoutParams(
                modeSectionWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
                topMargin =
                    dp(
                        if (isCompact()) 62 else 72,
                    )
            },
        )

        continueButton()
            ?.let { button ->
                content.addView(
                    button,
                    LinearLayout.LayoutParams(
                        continueButtonWidth,
                        dp(
                            if (isCompact()) 42 else 44,
                        ),
                    ).apply {
                        gravity =
                            Gravity.CENTER_HORIZONTAL
                        topMargin =
                            dp(16)
                    },
                )
            }

        setContentView(root)

        connectivityMonitor =
            ConnectivityMonitor(this) { online ->
                runOnUiThread {
                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }

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

        // Remote matches are never resumable from Home. Returning Home means
        // the remote match was abandoned, so stale remote credentials and
        // cached state must not survive into a future ONLINE/FRIENDS/TEAM_UP
        // entry. Local CPU and Pass & Play snapshots are intentionally kept.
        SecureSessionStore(this).clear()
        PendingRollStore(this).clear()
        CachedMatchStore(this).clear()
        PublicMatchmakingStore(this).clear()

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

    override fun onDestroy() {
        if (
            ::connectivityMonitor
                .isInitialized
        ) {
            connectivityMonitor.stop()
        }
        super.onDestroy()
    }
}
