package com.ludoproof.game.feature.profile.presentation

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.profile.presentation.components.profileBadgesPanel
import com.ludoproof.game.feature.profile.presentation.components.profileIdentityPanel
import com.ludoproof.game.feature.profile.presentation.components.profileModeStatsPanel
import com.ludoproof.game.feature.profile.presentation.components.profilePurchasesPanel
import com.ludoproof.game.feature.profile.presentation.components.profileTopBar

class ProfileActivity : Activity() {
    internal val profileStore by lazy {
        ProfileStore(this)
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme
            .configureWindow(
                this,
            )
        renderProfile()
    }

    internal fun renderProfile() {
        val profile =
            profileStore
                .snapshot()

        val (root, host) =
            LudoProofTheme
                .arcadeRoot(
                    this,
                )

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
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

        val horizontalPadding =
            dp(
                if (
                    LudoProofTheme
                        .isCompactWidth(
                            this,
                        )
                ) {
                    12
                } else {
                    18
                },
            )
        val maxWidth =
            dp(
                LudoProofTheme
                    .pageMaxContentWidthDp(
                        this,
                    ),
            )
        val width =
            minOf(
                resources
                    .displayMetrics
                    .widthPixels -
                    horizontalPadding *
                        2,
                maxWidth,
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(34),
                )
            }
        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                width,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL,
            ),
        )

        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(
            profileTopBar(),
        )
        content.addView(
            profileIdentityPanel(
                profile,
            ),
            sectionParams(
                14,
            ),
        )
        content.addView(
            profileModeStatsPanel(
                profile,
            ),
            sectionParams(
                16,
            ),
        )
        content.addView(
            profileBadgesPanel(
                profile,
            ),
            sectionParams(
                16,
            ),
        )
        content.addView(
            profilePurchasesPanel(
                profile,
            ),
            sectionParams(
                16,
            ),
        )

        setContentView(
            root,
        )
    }

    internal fun dp(
        value: Int,
    ): Int =
        LudoProofTheme
            .dp(
                this,
                value,
            )

    private fun sectionParams(
        topDp: Int,
    ):
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin =
                dp(
                    topDp,
                )
        }
}
