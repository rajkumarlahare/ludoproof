package com.ludoproof.game

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.feature.characters.domain.catalog.LudoPawsCharacterCatalog
import kotlin.math.roundToInt

/**
 * Compact character panel used by offline and remote gameplay rails.
 *
 * The card is presentation-only: token progress is read from PlayerSnapshot and
 * the selected animal is cosmetic metadata supplied by the caller.
 */
class LudoPawsPlayerCardView(
    context: Context,
) : LinearLayout(context) {
    private val portrait =
        ImageView(context)
    private val nameText =
        TextView(context)
    private val stateText =
        TextView(context)
    private val progressText =
        TextView(context)
    private val copy =
        LinearLayout(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(7), dp(6), dp(8), dp(6))

        addView(
            portrait,
            LayoutParams(
                dp(48),
                dp(48),
            ).apply {
                marginEnd = dp(7)
            },
        )

        copy.apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        nameText.apply {
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.WHITE)
            textSize = 11.5f
            maxLines = 1
        }
        stateText.apply {
            setTypeface(Typeface.DEFAULT_BOLD)
            textSize = 9.5f
            maxLines = 1
        }
        progressText.apply {
            setTextColor(0xFFDCEBFF.toInt())
            textSize = 9f
            maxLines = 1
        }

        copy.addView(nameText)
        copy.addView(stateText)
        copy.addView(progressText)
        addView(
            copy,
            LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
    }

    fun bind(
        player: PlayerSnapshot,
        characterId: String?,
        active: Boolean,
        computer: Boolean,
        compact: Boolean,
        localPlayer: Boolean = true,
        portraitOnEnd: Boolean = false,
    ) {
        layoutDirection =
            if (portraitOnEnd) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
        copy.layoutDirection =
            View.LAYOUT_DIRECTION_LTR
        nameText.textDirection =
            View.TEXT_DIRECTION_LTR
        stateText.textDirection =
            View.TEXT_DIRECTION_LTR
        progressText.textDirection =
            View.TEXT_DIRECTION_LTR
        setPadding(
            dp(if (portraitOnEnd) 8 else 2),
            dp(6),
            dp(if (portraitOnEnd) 2 else 8),
            dp(6),
        )

        val character =
            characterId
                ?.let(LudoPawsCharacterCatalog::character)
                ?: LudoPawsCharacterCatalog
                    .character(
                        LudoPawsCharacterCatalog.DEFAULT_CHARACTER_ID,
                    )

        val drawableId =
            character
                ?.fallbackDrawableName
                ?.let {
                    resources.getIdentifier(
                        it,
                        "drawable",
                        context.packageName,
                    )
                }
                ?: 0
        if (drawableId != 0) {
            portrait.setImageResource(drawableId)
        } else {
            portrait.setImageDrawable(null)
        }
        portrait.scaleType = ImageView.ScaleType.CENTER_CROP
        portrait.background =
            portraitBackground(
                player.color,
                active,
            )
        portrait.setPadding(dp(4), dp(4), dp(4), dp(4))

        val characterName =
            character
                ?.displayName
                ?: "Paw"
        nameText.text =
            "$characterName • ${player.displayName}"
        nameText.textSize =
            if (compact) 10.5f else 11.5f

        stateText.text =
            when {
                active && computer -> "CPU TURN"
                active && localPlayer -> "YOUR TURN"
                active && player.teamId != null -> "TEAM ${player.teamId} • TURN"
                active -> "TURN"
                player.teamId != null -> "TEAM ${player.teamId} • WAITING"
                else -> "WAITING"
            }
        stateText.setTextColor(
            if (active) {
                0xFFFFD54F.toInt()
            } else {
                0xFF76E4F7.toInt()
            },
        )

        val home =
            player.tokens.count {
                it == 57
            }
        val yard =
            player.tokens.count {
                it == -1
            }
        val racing =
            player.tokens.size - home - yard
        progressText.text =
            "$home HOME • $racing RACING • $yard YARD"
        progressText.textSize =
            if (compact) 8.2f else 9f

        background =
            cardBackground(
                player.color,
                active,
            )
        elevation =
            dp(
                if (active) 6 else 2,
            ).toFloat()

        contentDescription =
            buildString {
                append(characterName)
                append(", ")
                append(player.displayName)
                append(", ")
                append(player.color.lowercase())
                append(", ")
                append(home)
                append(" home, ")
                append(racing)
                append(" racing, ")
                append(yard)
                append(" in yard")
                if (active) {
                    append(", active turn")
                }
            }
        importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun portraitBackground(
        colorName: String,
        active: Boolean,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFFF9FBFF.toInt())
            setStroke(
                dp(if (active) 4 else 3),
                playerColor(colorName),
            )
        }

    private fun cardBackground(
        colorName: String,
        active: Boolean,
    ): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(
                if (active) {
                    0xF22B3550.toInt()
                } else {
                    0xD91A2337.toInt()
                },
            )
            setStroke(
                dp(if (active) 2 else 1),
                if (active) {
                    0xFFFFD54F.toInt()
                } else {
                    playerColor(colorName)
                },
            )
        }

    private fun playerColor(
        colorName: String,
    ): Int =
        when (colorName) {
            "RED" -> 0xFFF1252F.toInt()
            "GREEN" -> 0xFF00A950.toInt()
            "YELLOW" -> 0xFFFFD81B.toInt()
            "BLUE" -> 0xFF3097D7.toInt()
            else -> 0xFF6C757D.toInt()
        }

    private fun dp(
        value: Int,
    ): Int =
        (
            value *
                resources.displayMetrics.density
            )
            .roundToInt()
}
