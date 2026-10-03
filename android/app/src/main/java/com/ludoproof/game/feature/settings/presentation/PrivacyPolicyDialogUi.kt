package com.ludoproof.game.ui.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

internal fun showPrivacyPolicyDialog(
    context: Context,
) {
    val dialog = baseDialog(context)
    val panel =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                settingsDp(context, 16),
                settingsDp(context, 14),
                settingsDp(context, 16),
                settingsDp(context, 14),
            )
            background =
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        0xFF08378F.toInt(),
                        0xFF031B50.toInt(),
                    ),
                ).apply {
                    cornerRadius = settingsDp(context, 18).toFloat()
                    setStroke(
                        settingsDp(context, 2),
                        0xFFFFC735.toInt(),
                    )
                }

            addView(
                TextView(context).apply {
                    text = "PRIVACY POLICY"
                    textSize = 20f
                    setTypeface(Typeface.DEFAULT_BOLD)
                    setTextColor(0xFFFFD34E.toInt())
                    gravity = Gravity.CENTER
                },
            )
        }

    val scroll =
        ScrollView(context).apply {
            isFillViewport = false
            overScrollMode = ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
    val body =
        TextView(context).apply {
            text = PRIVACY_POLICY_TEXT
            textSize = 13.5f
            setTextColor(Color.WHITE)
            setLineSpacing(0f, 1.18f)
            setPadding(
                settingsDp(context, 4),
                settingsDp(context, 14),
                settingsDp(context, 4),
                settingsDp(context, 14),
            )
        }
    scroll.addView(body)
    panel.addView(
        scroll,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            settingsDp(context, 430),
        ),
    )

    panel.addView(
        TextView(context).apply {
            text = "CLOSE"
            textSize = 14f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(0xFFFFD34E.toInt())
            gravity = Gravity.CENTER
            setPadding(
                0,
                settingsDp(context, 12),
                0,
                settingsDp(context, 8),
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dialog.dismiss()
            }
        },
    )

    dialog.setContentView(panel)
    sizeDialog(dialog, .88f)
    dialog.show()
}

private const val PRIVACY_POLICY_TEXT =
    """Last updated: October 3, 2026

Ludo Paws uses information that is necessary to provide its game features. Depending on the features you use, this can include your in-game display/profile information, Friend ID and friend relationships, match and proof data, leaderboard/progression information, and game settings or cosmetic selections.

ONLINE FEATURES
Online matchmaking, Team Up, Friends, private rooms, leaderboards and proof verification communicate with Ludo Paws game services. The app is configured to use encrypted network connections for these services.

ON-DEVICE DATA
Gameplay preferences such as music, sound, quick chat and game speed are stored on the device. Cosmetic selections and local game state can also be stored locally so the app can render the selected experience and operate supported local features.

ACCOUNT AND SESSION SECURITY
Public Friend IDs are designed to be shareable. Private friend credentials and match/session credentials are separate from the public Friend ID and are handled by the app's protected local-storage and authenticated service flows.

MATCH INTEGRITY
Ludo Paws can retain match and proof information needed to operate multiplayer features, verify game integrity, resolve match state and support match history. Settings that change audio, appearance or animation timing do not change dice outcomes or Ludo rules.

THIRD-PARTY PLATFORM SERVICES
Android, Google Play features when enabled, and infrastructure/network providers can process technical information needed to deliver their respective services under their own terms and privacy practices.

RETENTION AND CHANGES
Service data can be retained for as long as reasonably needed to operate game, friend, integrity and security features. This policy may be updated as Ludo Paws features and data flows change. The policy shown in the app should be reviewed together with the current store listing before a public release."""
