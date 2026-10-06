package com.ludoproof.game

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.domain.model.ProfileGameMode
import com.ludoproof.game.feature.profile.domain.model.ProfileMatchSource
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

        val contentHost =
            FrameLayout(this)

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

        // Decorative scene sits in the existing hero gap. It is intentionally
        // independent from gameplay characters/dice and does not change the
        // approved mode-button or footer positions.
        val heroWidth =
            minOf(
                topHudWidth,
                dp(if (isCompact()) 350 else 388),
            )
        contentHost.addView(
            HomePetsHeroView(this),
            FrameLayout.LayoutParams(
                heroWidth,
                dp(if (isCompact()) 208 else 232),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                topMargin =
                    dp(if (isCompact()) 235 else 250)
            },
        )

        val brandWidth =
            minOf(
                topHudWidth,
                dp(if (isCompact()) 282 else 318),
            )
        contentHost.addView(
            HomeBrandLogoView(this),
            FrameLayout.LayoutParams(
                brandWidth,
                dp(if (isCompact()) 156 else 174),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                topMargin =
                    dp(if (isCompact()) 163 else 177)
            },
        )

        // Home is a fixed game menu, not a document-style scrolling screen.
        // Keeping the HUD/mode controls directly in the host prevents swipe
        // gestures from sliding the profile/shortcut controls into the status
        // bar/camera cutout while preserving their existing positions.
        host.addView(
            contentHost,
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
                        if (isCompact()) 302 else 312,
                    )
            },
        )

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

        recordAbandonedRemoteLossIfNeeded()

        // Remote matches are never resumable from Home. Returning Home means
        // the remote match was abandoned, so stale remote credentials and
        // cached state must not survive into a future ONLINE/FRIENDS/TEAM_UP
        // entry. Local modes start from their normal Computer/Pass & Play card.
        SecureSessionStore(this).clear()
        PendingRollStore(this).clear()
        CachedMatchStore(this).clear()
        PublicMatchmakingStore(this).clear()

        refreshHomeProfileSummary()
        refreshHomeGemBalance()
    }

    private fun recordAbandonedRemoteLossIfNeeded() {
        val session =
            SecureSessionStore(this)
                .load()
                ?: return
        val cached =
            CachedMatchStore(this)
                .load()
                ?.optJSONObject(
                    "state",
                )
                ?: return

        if (
            cached.optString(
                "matchId",
            ) !=
            session.matchId ||
            cached.optString(
                "status",
            ) !=
            "ACTIVE"
        ) {
            return
        }

        ProfileStore(this)
            .recordCompletedMatch(
                matchId =
                    session.matchId,
                mode =
                    ProfileGameMode.CLASSIC,
                source =
                    ProfileMatchSource.ONLINE,
                won =
                    false,
            )
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
