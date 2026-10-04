package com.ludoproof.game.feature.profile.presentation.components

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.feature.characters.data.local.CharacterSelectionStore
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import com.ludoproof.game.feature.profile.presentation.ProfileActivity
import com.ludoproof.game.feature.store.domain.model.StoreTab
import com.ludoproof.game.feature.store.presentation.StoreActivity

internal fun ProfileActivity.profilePawIdentityPanel():
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
            LudoProofTheme
                .rounded(
                    0xE9153879.toInt(),
                    16f,
                    0xFF55C8FF.toInt(),
                    2f,
                    this@profilePawIdentityPanel,
                )

        val selection =
            CharacterSelectionStore(
                this@profilePawIdentityPanel,
            ).load()
        val character =
            LudoPawsCharacterCatalog
                .character(
                    selection.characterId,
                )
                ?: LudoPawsCharacterCatalog
                    .character(
                        LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID,
                    )
        val pack =
            LudoPawsCharacterCatalog
                .pack(
                    selection.packId,
                )

        val row =
            LinearLayout(
                this@profilePawIdentityPanel,
            ).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val portraitFrame =
            FrameLayout(
                this@profilePawIdentityPanel,
            ).apply {
                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.OVAL
                        setColor(
                            0xFFF9FBFF.toInt(),
                        )
                        setStroke(
                            dp(3),
                            0xFFFFC928.toInt(),
                        )
                    }
            }
        val portrait =
            ImageView(
                this@profilePawIdentityPanel,
            ).apply {
                scaleType =
                    ImageView.ScaleType.CENTER_CROP
                setPadding(
                    dp(5),
                    dp(5),
                    dp(5),
                    dp(5),
                )
                val drawableId =
                    character
                        ?.fallbackDrawableName
                        ?.let {
                            resources.getIdentifier(
                                it,
                                "drawable",
                                this@profilePawIdentityPanel.packageName,
                            )
                        }
                        ?: 0
                if (drawableId != 0) {
                    setImageResource(
                        drawableId,
                    )
                }
                contentDescription =
                    buildString {
                        append("Equipped Ludo Paws character")
                        character?.let {
                            append(", ")
                            append(it.displayName)
                            append(", ")
                            append(
                                it.species.name
                                    .lowercase(),
                            )
                            append(", ")
                            append(
                                it.personality.name
                                    .lowercase(),
                            )
                            append(" personality")
                        }
                    }
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        portraitFrame.addView(
            portrait,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        row.addView(
            portraitFrame,
            LinearLayout.LayoutParams(
                dp(88),
                dp(88),
            ),
        )

        val copy =
            LinearLayout(
                this@profilePawIdentityPanel,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(14),
                    0,
                    0,
                    0,
                )
            }
        copy.addView(
            TextView(
                this@profilePawIdentityPanel,
            ).apply {
                text =
                    "YOUR PAW"
                textSize =
                    12f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    0xFF8DE8FF.toInt(),
                )
            },
        )
        copy.addView(
            TextView(
                this@profilePawIdentityPanel,
            ).apply {
                text =
                    character
                        ?.displayName
                        ?: "Paw"
                textSize =
                    22f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                setPadding(
                    0,
                    dp(2),
                    0,
                    0,
                )
            },
        )
        copy.addView(
            TextView(
                this@profilePawIdentityPanel,
            ).apply {
                val species =
                    character
                        ?.species
                        ?.name
                        ?.lowercase()
                        ?.replaceFirstChar {
                            it.uppercase()
                        }
                        ?: "Animal"
                val personality =
                    character
                        ?.personality
                        ?.name
                        ?.lowercase()
                        ?.replaceFirstChar {
                            it.uppercase()
                        }
                        ?: "Friendly"
                text =
                    (pack?.displayName ?: "Starter Paws") +
                        " • " +
                        species +
                        " • " +
                        personality
                textSize =
                    11f
                setTextColor(
                    0xFFD9E9FF.toInt(),
                )
                setPadding(
                    0,
                    dp(4),
                    0,
                    0,
                )
            },
        )
        copy.addView(
            TextView(
                this@profilePawIdentityPanel,
            ).apply {
                text =
                    "This is your equipped animal identity for Ludo Paws matches. Character personality is presentation-only and never changes dice or legal moves."
                textSize =
                    10.5f
                setTextColor(
                    0xFFBFD6F3.toInt(),
                )
                setPadding(
                    0,
                    dp(6),
                    0,
                    0,
                )
            },
        )
        row.addView(
            copy,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        addView(
            row,
        )

        addView(
            Button(
                this@profilePawIdentityPanel,
            ).apply {
                text =
                    "OPEN PAWS STORE"
                textSize =
                    12f
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                minHeight =
                    0
                contentDescription =
                    "Open Paws character store"
                background =
                    LudoProofTheme
                        .rounded(
                            0xFF0AA54A.toInt(),
                            12f,
                            0xFF55E980.toInt(),
                            1f,
                            this@profilePawIdentityPanel,
                        )
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@profilePawIdentityPanel,
                            StoreActivity::class.java,
                        ).putExtra(
                            StoreActivity.EXTRA_INITIAL_TAB,
                            StoreTab.PAWS.name,
                        ),
                    )
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50),
            ).apply {
                topMargin =
                    dp(12)
            },
        )
    }
