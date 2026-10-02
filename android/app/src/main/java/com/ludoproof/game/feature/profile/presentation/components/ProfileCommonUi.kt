package com.ludoproof.game.feature.profile.presentation.components

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.profile.presentation.ProfileActivity

internal fun ProfileActivity.profilePanel():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        setPadding(
            dp(14),
            dp(14),
            dp(14),
            dp(14),
        )
        background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xE81D72D1.toInt(),
                    0xE50D4CAA.toInt(),
                ),
            ).apply {
                cornerRadius =
                    dp(15)
                        .toFloat()
                setStroke(
                    dp(1),
                    0x8856D9FF.toInt(),
                )
            }
        elevation =
            dp(5)
                .toFloat()
    }

internal fun ProfileActivity.profileTitle(
    text: String,
):
    TextView =
    TextView(this).apply {
        this.text =
            text
        textSize =
            18f
        setTypeface(
            Typeface.DEFAULT_BOLD,
        )
        setTextColor(
            Color.WHITE,
        )
    }

internal fun ProfileActivity.profileBody(
    text: String,
    size: Float = 13f,
):
    TextView =
    TextView(this).apply {
        this.text =
            text
        textSize =
            size
        setTextColor(
            0xFFE8F5FF.toInt(),
        )
    }

internal fun ProfileActivity.showNameEditor() {
    val current =
        profileStore
            .snapshot()
    val input =
        EditText(this).apply {
            setText(
                current.displayName,
            )
            selectAll()
            isSingleLine =
                true
            setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8),
            )
        }

    AlertDialog
        .Builder(this)
        .setTitle(
            "Profile name",
        )
        .setView(
            input,
        )
        .setNegativeButton(
            "CANCEL",
            null,
        )
        .setPositiveButton(
            "SAVE",
        ) {
                _,
                _ ->
            profileStore
                .updateDisplayName(
                    input.text
                        .toString(),
                )
            renderProfile()
        }
        .show()
}

internal fun ProfileActivity.showGoogleSetupInfo() {
    AlertDialog
        .Builder(this)
        .setTitle(
            "Google sign-in",
        )
        .setMessage(
            "The profile is fully usable as a guest. Google account sign-in needs the production OAuth client configuration before it can authenticate safely.",
        )
        .setPositiveButton(
            "OK",
            null,
        )
        .show()
}

internal fun ProfileActivity.viewAllBadges(
    body: String,
) {
    AlertDialog
        .Builder(this)
        .setTitle(
            "Badges",
        )
        .setMessage(
            body,
        )
        .setPositiveButton(
            "CLOSE",
            null,
        )
        .show()
}

internal fun ProfileActivity.viewPurchases(
    body: String,
) {
    AlertDialog
        .Builder(this)
        .setTitle(
            "Purchased Products",
        )
        .setMessage(
            body,
        )
        .setPositiveButton(
            "CLOSE",
            null,
        )
        .show()
}
