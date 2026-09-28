package com.ludoproof.game

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class OfflineGameActivity : Activity() {
    private val handler =
        Handler(
            Looper.getMainLooper(),
        )

    private val engine by lazy {
        OfflineGameEngine(this)
    }

    private lateinit var root:
        LinearLayout
    private var boardView:
        LudoBoardView? = null
    private var diceView:
        DiceView? = null
    private var turnText:
        TextView? = null
    private var infoText:
        TextView? = null
    private var historyText:
        TextView? = null
    private var rollButton:
        Button? = null

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        if (engine.hasSavedGame()) {
            showGame(
                engine.snapshot(),
            )
        } else {
            showSetup()
        }
    }

    private fun showSetup() {
        root =
            LudoProofTheme.screen(this)

        root.addView(
            topBar(
                "OFFLINE LOCAL",
            ),
        )

        root.addView(
            TextView(this).apply {
                text =
                    "Choose Players"
                LudoProofTheme.title(
                    this,
                    26f,
                )
                setPadding(
                    0,
                    14,
                    0,
                    6,
                )
            },
        )

        root.addView(
            TextView(this).apply {
                text =
                    "Pass-and-play on one phone. Internet is not required."
                LudoProofTheme.body(
                    this,
                    14f,
                    centered = true,
                )
                setPadding(
                    10,
                    0,
                    10,
                    14,
                )
            },
        )

        val selector =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    14,
                    14,
                    14,
                    14,
                )
                LudoProofTheme.card(
                    this,
                )
            }

        for (
            count in
            2..4
        ) {
            selector.addView(
                Button(this).apply {
                    text =
                        "$count Players"
                    LudoProofTheme.primary(
                        this,
                    )
                    setOnClickListener {
                        val snapshot =
                            engine.start(
                                count,
                            )
                        showGame(
                            snapshot,
                        )
                    }
                },
            )
        }

        root.addView(selector)

        root.addView(
            TextView(this).apply {
                text =
                    "Offline fairness note: rolls use Android SecureRandom on this device. They are not EntroNex-certified or server-verified."
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                )
                setPadding(
                    10,
                    18,
                    10,
                    4,
                )
            },
        )

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    LudoProofTheme.BLUE_DARK,
                )
                addView(root)
            },
        )
    }

    private fun showGame(
        snapshot: MatchSnapshot?,
    ) {
        val state =
            snapshot ?: run {
                showSetup()
                return
            }

        root =
            LudoProofTheme.screen(this)
        root.addView(
            topBar(
                "OFFLINE LOCAL",
            ),
        )

        infoText =
            TextView(this).apply {
                LudoProofTheme.body(
                    this,
                    13f,
                    centered = true,
                )
                setPadding(
                    8,
                    8,
                    8,
                    4,
                )
            }
        root.addView(
            requireNotNull(
                infoText,
            ),
        )

        turnText =
            TextView(this).apply {
                LudoProofTheme.title(
                    this,
                    22f,
                )
                setPadding(
                    8,
                    8,
                    8,
                    10,
                )
            }
        root.addView(
            requireNotNull(
                turnText,
            ),
        )

        boardView =
            LudoBoardView(this).apply {
                onTokenSelected = {
                    tokenIndex ->
                    runCatching {
                        engine.move(
                            tokenIndex,
                        )
                    }
                        .onSuccess {
                            next ->
                            renderGame(
                                next,
                            )
                        }
                        .onFailure {
                            error ->
                            showGameMessage(
                                error.message
                                    ?: "Move failed",
                            )
                        }
                }
            }

        val boardCard =
            LinearLayout(
                this,
            ).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    6,
                    6,
                    6,
                    6,
                )
                LudoProofTheme.card(
                    this,
                    alternate = true,
                )
                addView(
                    boardView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                            .MATCH_PARENT,
                        LinearLayout.LayoutParams
                            .WRAP_CONTENT,
                    ),
                )
            }
        root.addView(boardCard)

        diceView =
            DiceView(this)
        root.addView(
            diceView,
            LinearLayout.LayoutParams(
                LudoProofTheme.dp(
                    this,
                    104,
                ),
                LudoProofTheme.dp(
                    this,
                    104,
                ),
            ).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
            },
        )

        rollButton =
            Button(this).apply {
                text = "ROLL DICE"
                textSize = 18f
                LudoProofTheme.primary(
                    this,
                )
                setOnClickListener {
                    rollOffline()
                }
            }
        root.addView(
            requireNotNull(
                rollButton,
            ),
        )

        val trust =
            TextView(this).apply {
                text =
                    "OFFLINE • Device SecureRandom • No EntroNex proof"
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                )
                setPadding(
                    8,
                    10,
                    8,
                    10,
                )
            }
        root.addView(trust)

        historyText =
            TextView(this).apply {
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = false,
                )
                setPadding(
                    12,
                    12,
                    12,
                    12,
                )
                LudoProofTheme.card(
                    this,
                )
            }
        root.addView(
            requireNotNull(
                historyText,
            ),
        )

        val newButton =
            Button(this).apply {
                text =
                    "New Offline Game"
                LudoProofTheme.secondary(
                    this,
                )
                setOnClickListener {
                    engine.clear()
                    showSetup()
                }
            }
        root.addView(newButton)

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(
                    LudoProofTheme.BLUE_DARK,
                )
                addView(root)
            },
        )

        renderGame(state)
    }

    private fun renderGame(
        state: MatchSnapshot,
    ) {
        val active =
            state.players
                .getOrNull(
                    state.turnSeat,
                )
        val winner =
            state.players
                .find {
                    it.playerId ==
                        state.winnerPlayerId
                }

        infoText?.text =
            state.players
                .joinToString(
                    separator = "   ",
                ) {
                    it.color +
                        ": " +
                        it.displayName
                }

        turnText?.text =
            if (
                state.status ==
                "FINISHED"
            ) {
                "Winner: " +
                    (
                        winner
                            ?.displayName
                            ?: "Player"
                        )
            } else {
                "Turn: " +
                    (
                        active
                            ?.displayName
                            ?: "Player"
                        )
            }

        boardView?.bind(
            state,
            engine.activePlayerId(),
        )

        val pending =
            state.pendingRoll
        val latest =
            state.history
                .lastOrNull()
        val outcome =
            pending?.outcome
                ?: latest?.outcome
        if (outcome != null) {
            diceView?.showOutcome(
                outcome,
            )
        }

        rollButton?.isEnabled =
            state.status ==
                "ACTIVE" &&
                pending == null
        rollButton?.text =
            when {
                state.status ==
                    "FINISHED" ->
                    "GAME FINISHED"
                pending != null ->
                    "MOVE HIGHLIGHTED TOKEN"
                else ->
                    "ROLL DICE"
            }

        historyText?.text =
            buildString {
                append(
                    "Recent offline rolls\n",
                )
                val recent =
                    state.history
                        .takeLast(8)
                        .reversed()
                if (recent.isEmpty()) {
                    append(
                        "No rolls yet.",
                    )
                } else {
                    recent.forEach {
                        event ->
                        append(
                            "#",
                        )
                        append(
                            event.eventIndex,
                        )
                        append(
                            "  dice=",
                        )
                        append(
                            event.outcome
                                ?: "?",
                        )
                        if (
                            event.moveTokenIndex !=
                            null
                        ) {
                            append(
                                "  token=",
                            )
                            append(
                                event.moveTokenIndex +
                                    1,
                            )
                        }
                        if (
                            event.captures > 0
                        ) {
                            append(
                                "  captures=",
                            )
                            append(
                                event.captures,
                            )
                        }
                        append("\n")
                    }
                }
            }.trimEnd()
    }

    private fun rollOffline() {
        val button =
            rollButton ?: return
        button.isEnabled = false
        diceView?.startRolling()
        showGameMessage(
            "Rolling locally…",
        )

        handler.postDelayed(
            {
                runCatching {
                    engine.roll()
                }
                    .onSuccess {
                        state ->
                        renderGame(
                            state,
                        )
                        val pending =
                            state.pendingRoll
                        showGameMessage(
                            if (
                                pending !=
                                null
                            ) {
                                "Move a highlighted token."
                            } else {
                                "Turn updated."
                            },
                        )
                    }
                    .onFailure {
                        error ->
                        diceView?.stopRolling()
                        button.isEnabled =
                            true
                        showGameMessage(
                            error.message
                                ?: "Roll failed",
                        )
                    }
            },
            420L,
        )
    }

    private fun topBar(
        mode: String,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL

            addView(
                Button(
                    this@OfflineGameActivity,
                ).apply {
                    text = "‹ Home"
                    LudoProofTheme.secondary(
                        this,
                    )
                    setOnClickListener {
                        finish()
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams
                        .WRAP_CONTENT,
                    1f,
                ),
            )

            addView(
                TextView(
                    this@OfflineGameActivity,
                ).apply {
                    text = mode
                    setTextColor(
                        LudoProofTheme.WHITE,
                    )
                    textSize = 13f
                    gravity =
                        Gravity.CENTER
                    LudoProofTheme.chip(
                        this,
                        LudoProofTheme.GREEN,
                    )
                    setPadding(
                        12,
                        8,
                        12,
                        8,
                    )
                },
            )
        }

    private fun showGameMessage(
        value: String,
    ) {
        val text =
            infoText
        if (text != null) {
            text.contentDescription =
                value
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(
            null,
        )
        super.onDestroy()
    }
}
