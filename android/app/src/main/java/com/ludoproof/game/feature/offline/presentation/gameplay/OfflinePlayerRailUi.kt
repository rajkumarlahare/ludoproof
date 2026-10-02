package com.ludoproof.game.ui.offline.gameplay

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ludoproof.game.*

internal fun OfflineGameActivity.playerRail():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        minimumHeight =
            dp(
                if (isCompactSetup()) {
                    72
                } else {
                    82
                },
            )
    }

internal fun OfflineGameActivity.renderPlayerRails(
    state: MatchSnapshot,
) {
    val top =
        topPlayerRail
            ?: return
    val bottom =
        bottomPlayerRail
            ?: return

    top.removeAllViews()
    bottom.removeAllViews()

    val activePlayer =
        state.players
            .getOrNull(
                state.turnSeat,
            )

    addPlayerSlot(
        top,
        state,
        OfflinePlayerLayout
            .Slot
            .TOP_LEFT,
        alignEnd =
            false,
        activePlayer =
            activePlayer,
    )
    addPlayerSlot(
        top,
        state,
        OfflinePlayerLayout
            .Slot
            .TOP_RIGHT,
        alignEnd =
            true,
        activePlayer =
            activePlayer,
    )
    addPlayerSlot(
        bottom,
        state,
        OfflinePlayerLayout
            .Slot
            .BOTTOM_LEFT,
        alignEnd =
            false,
        activePlayer =
            activePlayer,
    )
    addPlayerSlot(
        bottom,
        state,
        OfflinePlayerLayout
            .Slot
            .BOTTOM_RIGHT,
        alignEnd =
            true,
        activePlayer =
            activePlayer,
    )
}

private fun OfflineGameActivity.addPlayerSlot(
    rail: LinearLayout,
    state: MatchSnapshot,
    slot: OfflinePlayerLayout.Slot,
    alignEnd: Boolean,
    activePlayer: PlayerSnapshot?,
) {
    val player =
        state.players
            .firstOrNull {
                OfflinePlayerLayout
                    .slotForColor(
                        it.color,
                    ) ==
                    slot
            }

    val host =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                (
                    if (alignEnd) {
                        Gravity.END
                    } else {
                        Gravity.START
                    }
                    ) or
                    Gravity.CENTER_VERTICAL
        }

    if (player != null) {
        val active =
            player.playerId ==
                activePlayer
                    ?.playerId &&
                state.status ==
                    "ACTIVE"

        if (
            alignEnd &&
            active
        ) {
            host.addView(
                activeDiceControl(
                    player,
                ),
            )
        }

        host.addView(
            playerProfile(
                player,
                active,
            ),
        )

        if (
            !alignEnd &&
            active
        ) {
            host.addView(
                activeDiceControl(
                    player,
                ),
            )
        }
    }

    rail.addView(
        host,
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        ),
    )
}

private fun OfflineGameActivity.playerProfile(
    player: PlayerSnapshot,
    active: Boolean,
): LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
        gravity =
            Gravity.CENTER
        setPadding(
            dp(2),
            dp(2),
            dp(2),
            dp(2),
        )

        addView(
            TextView(
                this@playerProfile,
            ).apply {
                text =
                    "P" +
                        (
                            player.seat +
                                1
                            )
                textSize =
                    if (isCompactSetup()) {
                        16f
                    } else {
                        18f
                    }
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    Color.WHITE,
                )
                gravity =
                    Gravity.CENTER
                background =
                    playerAvatarBackground(
                        player.color,
                        active,
                    )
                elevation =
                    dp(
                        if (active) {
                            7
                        } else {
                            3
                        },
                    ).toFloat()
                contentDescription =
                    player.displayName +
                        ", " +
                        player.color.lowercase() +
                        (
                            if (active) {
                                ", active turn"
                            } else {
                                ""
                            }
                            )
            },
            LinearLayout.LayoutParams(
                dp(
                    if (isCompactSetup()) {
                        54
                    } else {
                        60
                    },
                ),
                dp(
                    if (isCompactSetup()) {
                        54
                    } else {
                        60
                    },
                ),
            ),
        )

        addView(
            TextView(
                this@playerProfile,
            ).apply {
                text =
                    player.displayName
                textSize =
                    if (isCompactSetup()) {
                        9.5f
                    } else {
                        10.5f
                    }
                setTypeface(
                    Typeface.DEFAULT_BOLD,
                )
                setTextColor(
                    if (active) {
                        LudoProofTheme.GOLD
                    } else {
                        Color.WHITE
                    },
                )
                gravity =
                    Gravity.CENTER
                maxLines =
                    1
                setPadding(
                    0,
                    dp(3),
                    0,
                    0,
                )
            },
            LinearLayout.LayoutParams(
                dp(
                    if (isCompactSetup()) {
                        70
                    } else {
                        78
                    },
                ),
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

private fun OfflineGameActivity.activeDiceControl(
    player: PlayerSnapshot,
): FrameLayout {
    var control =
        diceHost
    if (
        control ==
        null
    ) {
        control =
            FrameLayout(this).apply {
                isClickable =
                    true
                isFocusable =
                    true
                background =
                    LudoProofTheme
                        .rounded(
                            0xECF8FAFF.toInt(),
                            12f,
                            0xFF5FE1FF.toInt(),
                            2f,
                            this@activeDiceControl,
                        )
                elevation =
                    dp(7).toFloat()
                setOnClickListener {
                    rollOffline()
                }
            }

        diceView =
            DiceView(this)
        control.addView(
            diceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ),
        )
        diceHost =
            control
    }

    control.contentDescription =
        "Roll dice for " +
            player.displayName
    control.isEnabled =
        true
    control.alpha =
        1f

    val params =
        LinearLayout.LayoutParams(
            dp(
                if (isCompactSetup()) {
                    56
                } else {
                    62
                },
            ),
            dp(
                if (isCompactSetup()) {
                    56
                } else {
                    62
                },
            ),
        ).apply {
            setMargins(
                dp(7),
                0,
                dp(7),
                dp(14),
            )
        }

    control.layoutParams =
        params
    return control
}

private fun OfflineGameActivity.playerAvatarBackground(
    colorName: String,
    active: Boolean,
): GradientDrawable {
    val color =
        when (colorName) {
            "RED" ->
                0xFFF1252F.toInt()
            "GREEN" ->
                0xFF00A950.toInt()
            "YELLOW" ->
                0xFFFFD81B.toInt()
            "BLUE" ->
                0xFF3097D7.toInt()
            else ->
                0xFF6C757D.toInt()
        }

    return GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            lightenPlayerColor(
                color,
            ),
            color,
        ),
    ).apply {
        cornerRadius =
            dp(12).toFloat()
        setStroke(
            dp(
                if (active) {
                    3
                } else {
                    2
                },
            ),
            if (active) {
                LudoProofTheme.GOLD
            } else {
                0xFF62E6FF.toInt()
            },
        )
    }
}

private fun lightenPlayerColor(
    color: Int,
): Int =
    Color.rgb(
        (
            Color.red(color) +
                (
                    255 -
                        Color.red(color)
                    ) *
                    .18f
            ).toInt(),
        (
            Color.green(color) +
                (
                    255 -
                        Color.green(color)
                    ) *
                    .18f
            ).toInt(),
        (
            Color.blue(color) +
                (
                    255 -
                        Color.blue(color)
                    ) *
                    .18f
            ).toInt(),
    )
