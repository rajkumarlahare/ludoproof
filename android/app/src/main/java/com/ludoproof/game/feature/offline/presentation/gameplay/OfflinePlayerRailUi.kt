package com.ludoproof.game.ui.offline.gameplay

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.ludoproof.game.*
import com.ludoproof.game.feature.offline.*
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*

internal fun OfflineGameActivity.playerRail():
    LinearLayout =
    LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        minimumHeight =
            dp(
                if (isCompactSetup()) {
                    82
                } else {
                    94
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

    diceView
        ?.stopRolling()
    diceView =
        null
    diceHost =
        null

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
        OfflinePlayerLayout.Slot.TOP_LEFT,
        alignEnd = false,
        activePlayer = activePlayer,
    )
    addPlayerSlot(
        top,
        state,
        OfflinePlayerLayout.Slot.TOP_RIGHT,
        alignEnd = true,
        activePlayer = activePlayer,
    )
    addPlayerSlot(
        bottom,
        state,
        OfflinePlayerLayout.Slot.BOTTOM_LEFT,
        alignEnd = false,
        activePlayer = activePlayer,
    )
    addPlayerSlot(
        bottom,
        state,
        OfflinePlayerLayout.Slot.BOTTOM_RIGHT,
        alignEnd = true,
        activePlayer = activePlayer,
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
                        color = it.color,
                        preferredBottomLeftColor =
                            state.players
                                .firstOrNull()
                                ?.color
                                ?: "BLUE",
                    ) == slot
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
            clipChildren = false
            clipToPadding = false
        }

    if (player != null) {
        val active =
            player.playerId == activePlayer?.playerId &&
                state.status == "ACTIVE"
        val characterId =
            activeCharacterIdsBySeat
                .getOrNull(
                    player.seat,
                )

        if (
            alignEnd &&
            active
        ) {
            host.addView(
                activeDiceControl(player),
            )
        }

        host.addView(
            LudoPawsPlayerCardView(this).apply {
                bind(
                    player = player,
                    characterId = characterId,
                    active = active,
                    computer =
                        engine.isComputerPlayer(
                            player.playerId,
                        ),
                    compact = isCompactSetup(),
                    portraitOnEnd = alignEnd,
                )
            },
            LinearLayout.LayoutParams(
                dp(
                    if (isCompactSetup()) {
                        108
                    } else {
                        124
                    },
                ),
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        if (
            !alignEnd &&
            active
        ) {
            host.addView(
                activeDiceControl(player),
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

private fun OfflineGameActivity.activeDiceControl(
    player: PlayerSnapshot,
): FrameLayout {
    val control =
        FrameLayout(this).apply {
            isClickable =
                true
            isFocusable =
                true
            // The dice itself is the visual control. Keep this host transparent so
            // no extra "safe" frame surrounds or clips the face/shadow.
            background = null
            elevation = 0f
            clipChildren = false
            clipToPadding = false
            setOnClickListener {
                rollOffline()
            }

            val newDice =
                DiceView(
                    this@activeDiceControl,
                )
            diceView =
                newDice
            addView(
                newDice,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                ),
            )
        }
    diceHost =
        control

    val cpuTurn =
        engine.isComputerPlayer(
            player.playerId,
        )
    control.contentDescription =
        if (cpuTurn) {
            player.displayName +
                " is thinking"
        } else {
            "Roll dice for " +
                player.displayName
        }
    control.isEnabled =
        !cpuTurn
    control.alpha =
        if (cpuTurn) {
            .58f
        } else {
            1f
        }
    control.layoutParams =
        LinearLayout.LayoutParams(
            dp(
                if (isCompactSetup()) {
                    58
                } else {
                    64
                },
            ),
            dp(
                if (isCompactSetup()) {
                    58
                } else {
                    64
                },
            ),
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            setMargins(
                dp(2),
                0,
                dp(2),
                0,
            )
        }

    return control
}
