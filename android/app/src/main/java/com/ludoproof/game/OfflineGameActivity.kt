package com.ludoproof.game

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class OfflineGameActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val engine by lazy { OfflineGameEngine(this) }

    private var selectedPlayers = 2
    private var selectedColor = "BLUE"
    private lateinit var playerButtons: Map<Int, Button>
    private lateinit var colorButtons: Map<String, Button>

    private var boardView: LudoBoardView? = null
    private var diceView: DiceView? = null
    private var turnText: TextView? = null
    private var infoText: TextView? = null
    private var statusText: TextView? = null
    private var rollButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        if (engine.hasSavedGame()) showGame(engine.snapshot()) else showSetup()
    }

    private fun showSetup() {
        val (root, host) = LudoProofTheme.arcadeRoot(this)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(26))
        }
        scroll.addView(content)
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(backHeader("LOCAL GAME"))

        content.addView(
            selectionPanel("SELECT GAME").apply {
                val row = LinearLayout(this@OfflineGameActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                row.addView(
                    choiceTile("▦", "Classic", "Standard play", true, true) {},
                    tileParams(),
                )
                row.addView(
                    choiceTile("⚡", "Rush", "Fast-paced", false, false) {},
                    tileParams(),
                )
                addView(row)
                addView(
                    TextView(this@OfflineGameActivity).apply {
                        text = "Theme:   CLASSIC                         ▼"
                        LudoProofTheme.body(this, 16f, bright = true)
                        setPadding(dp(16), dp(13), dp(16), dp(13))
                        background =
                            LudoProofTheme.darkPanelDrawable(
                                this@OfflineGameActivity,
                                goldBorder = true,
                            )
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = dp(14) },
                )
            },
            sectionParams(),
        )

        val playersPanel = selectionPanel("SELECT PLAYERS")
        val playersRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val playerMap = linkedMapOf<Int, Button>()
        listOf(
            2 to "♟♟",
            3 to "♟♟♟",
            4 to "♟♟♟♟",
        ).forEach { entry ->
            val button = tileButton(
                entry.second + "\n" + entry.first + "P",
            ) {
                selectedPlayers = entry.first
                refreshSetupSelections()
            }
            playerMap[entry.first] = button
            playersRow.addView(button, tileParams())
        }
        playerButtons = playerMap
        playersPanel.addView(playersRow)
        content.addView(playersPanel, sectionParams())

        val colorPanel = selectionPanel("CHOOSE YOUR COLOR")
        val colorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val colors = linkedMapOf(
            "BLUE" to 0xFF298DFF.toInt(),
            "RED" to 0xFFF22E35.toInt(),
            "GREEN" to 0xFF1FB257.toInt(),
            "YELLOW" to 0xFFFFD324.toInt(),
        )
        val colorMap = linkedMapOf<String, Button>()
        colors.forEach { entry ->
            val button = Button(this).apply {
                text = "●"
                textSize = 34f
                setTextColor(entry.value)
                setOnClickListener {
                    selectedColor = entry.key
                    refreshSetupSelections()
                }
            }
            colorMap[entry.key] = button
            colorRow.addView(button, tileParams())
        }
        colorButtons = colorMap
        colorPanel.addView(colorRow)
        content.addView(colorPanel, sectionParams())

        refreshSetupSelections()

        content.addView(
            TextView(this).apply {
                text =
                    "Offline mode works without internet and uses the same EntroNex v4 outcome + Natural World derivation locally. It has no remote server attestation."
                LudoProofTheme.body(this, 12f, centered = true)
                setPadding(dp(18), dp(12), dp(18), dp(4))
            },
        )

        content.addView(
            Button(this).apply {
                text = "PLAY"
                textSize = 23f
                LudoProofTheme.primary(this)
                setOnClickListener {
                    showGame(engine.start(selectedPlayers, selectedColor))
                }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68),
            ).apply {
                setMargins(dp(78), dp(36), dp(78), 0)
            },
        )

        setContentView(root)
    }

    private fun showGame(snapshot: MatchSnapshot?) {
        val state = snapshot ?: run {
            showSetup()
            return
        }

        val (root, host) = LudoProofTheme.arcadeRoot(this)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(26))
        }
        scroll.addView(content)
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(backHeader("LOCAL • CLASSIC"))

        infoText = TextView(this).apply {
            LudoProofTheme.body(this, 13f, centered = true, bright = true)
            setPadding(dp(8), dp(10), dp(8), dp(4))
        }
        content.addView(requireNotNull(infoText))

        turnText = TextView(this).apply {
            LudoProofTheme.title(this, 23f, gold = true)
            setPadding(dp(8), dp(4), dp(8), dp(10))
        }
        content.addView(requireNotNull(turnText))

        boardView = LudoBoardView(this).apply {
            onTokenSelected = { tokenIndex ->
                runCatching { engine.move(tokenIndex) }
                    .onSuccess { next ->
                        renderGame(next)
                        showStatus("Move accepted.")
                    }
                    .onFailure { error ->
                        showStatus(error.message ?: "Move failed")
                    }
            }
        }

        val frame = LudoProofTheme.boardFrame(this)
        frame.addView(
            boardView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        content.addView(
            frame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(dp(2), 0, dp(2), dp(14))
            },
        )

        val actionPanel = LudoProofTheme.panel(this)
        diceView = DiceView(this)
        actionPanel.addView(
            diceView,
            LinearLayout.LayoutParams(dp(102), dp(102)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        statusText = TextView(this).apply {
            text = "Offline v4 local engine ready"
            LudoProofTheme.body(this, 13f, centered = true, bright = true)
            setPadding(0, dp(4), 0, dp(10))
        }
        actionPanel.addView(requireNotNull(statusText))

        rollButton = Button(this).apply {
            text = "ROLL DICE"
            textSize = 20f
            LudoProofTheme.primary(this)
            setOnClickListener { rollOffline() }
        }
        actionPanel.addView(requireNotNull(rollButton))

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        tools.addView(
            Button(this).apply {
                text = "HISTORY"
                LudoProofTheme.secondary(this)
                setOnClickListener {
                    ArcadeDialogs.showProofHistory(
                        this@OfflineGameActivity,
                        "OFFLINE HISTORY",
                        offlineHistory(engine.snapshot()),
                    )
                }
            },
            toolParams(),
        )
        tools.addView(
            Button(this).apply {
                text = "ENGINE MAP"
                LudoProofTheme.secondary(this)
                setOnClickListener {
                    ArcadeDialogs.showNaturalWorldAudit(
                        this@OfflineGameActivity,
                        engine.lastRandomnessAudit(),
                    )
                }
            },
            toolParams(),
        )
        tools.addView(
            Button(this).apply {
                text = "NEW GAME"
                LudoProofTheme.positive(this)
                setOnClickListener {
                    engine.clear()
                    showSetup()
                }
            },
            toolParams(),
        )
        actionPanel.addView(tools)

        actionPanel.addView(
            TextView(this).apply {
                text = "LOCAL V4 • Same EntroNex derivation • No remote attestation"
                LudoProofTheme.body(this, 11f, centered = true)
                setPadding(0, dp(10), 0, 0)
            },
        )
        content.addView(
            actionPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(dp(6), 0, dp(6), 0)
            },
        )

        setContentView(root)
        renderGame(state)
    }

    private fun renderGame(state: MatchSnapshot) {
        val active = state.players.getOrNull(state.turnSeat)
        val winner = state.players.find { it.playerId == state.winnerPlayerId }

        infoText?.text =
            state.players.joinToString(separator = "   ") {
                it.color + ": " + it.displayName
            }

        turnText?.text =
            if (state.status == "FINISHED") {
                "Winner: " + (winner?.displayName ?: "Player")
            } else {
                "Turn: " + (active?.displayName ?: "Player")
            }

        boardView?.bind(state, engine.activePlayerId())

        val pending = state.pendingRoll
        val latest = state.history.lastOrNull()
        val outcome = pending?.outcome ?: latest?.outcome
        if (outcome != null) diceView?.showOutcome(outcome)

        rollButton?.isEnabled =
            state.status == "ACTIVE" && pending == null
        rollButton?.text =
            when {
                state.status == "FINISHED" -> "GAME FINISHED"
                pending != null -> "MOVE HIGHLIGHTED TOKEN"
                else -> "ROLL DICE"
            }

        if (pending != null) {
            showStatus(
                "Dice " + pending.outcome + " • move a highlighted token",
            )
        }
    }

    private fun rollOffline() {
        val button = rollButton ?: return
        button.isEnabled = false
        diceView?.startRolling()
        showStatus("Rolling locally…")

        handler.postDelayed(
            {
                runCatching { engine.roll() }
                    .onSuccess { state ->
                        renderGame(state)
                        showStatus(
                            if (state.pendingRoll != null) {
                                "Move a highlighted token."
                            } else {
                                "Turn updated."
                            },
                        )
                    }
                    .onFailure { error ->
                        diceView?.stopRolling()
                        button.isEnabled = true
                        showStatus(error.message ?: "Roll failed")
                    }
            },
            430L,
        )
    }

    private fun selectionPanel(title: String): LinearLayout =
        LudoProofTheme.panel(this).apply {
            addView(
                TextView(this@OfflineGameActivity).apply {
                    text = title
                    LudoProofTheme.title(this, 20f)
                    setPadding(0, 0, 0, dp(12))
                },
            )
        }

    private fun choiceTile(
        icon: String,
        title: String,
        subtitle: String,
        selected: Boolean,
        enabled: Boolean,
        action: () -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isEnabled = enabled
            alpha = if (enabled) 1f else .48f
            if (selected) LudoProofTheme.selectedTile(this)
            else LudoProofTheme.normalTile(this)
            setPadding(dp(10), dp(10), dp(10), dp(10))

            addView(
                TextView(this@OfflineGameActivity).apply {
                    text = icon
                    textSize = 30f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                },
            )
            addView(
                TextView(this@OfflineGameActivity).apply {
                    text = title
                    LudoProofTheme.body(this, 17f, centered = true, bright = true)
                },
            )
            addView(
                TextView(this@OfflineGameActivity).apply {
                    text = subtitle
                    LudoProofTheme.body(this, 11f, centered = true, bright = true)
                },
            )
            setOnClickListener { if (enabled) action() }
        }

    private fun tileButton(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setOnClickListener { action() }
        }

    private fun refreshSetupSelections() {
        if (!::playerButtons.isInitialized || !::colorButtons.isInitialized) return

        playerButtons.forEach { entry ->
            if (entry.key == selectedPlayers) {
                LudoProofTheme.selectedTile(entry.value)
            } else {
                LudoProofTheme.normalTile(entry.value)
            }
        }
        colorButtons.forEach { entry ->
            if (entry.key == selectedColor) {
                LudoProofTheme.selectedTile(entry.value)
            } else {
                LudoProofTheme.normalTile(entry.value)
            }
        }
    }

    private fun backHeader(label: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                Button(this@OfflineGameActivity).apply {
                    LudoProofTheme.circularAction(this, "‹")
                    setOnClickListener { finish() }
                },
                LinearLayout.LayoutParams(dp(52), dp(52)),
            )
            addView(
                TextView(this@OfflineGameActivity).apply {
                    text = label
                    LudoProofTheme.body(this, 14f, centered = true, bright = true)
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    background =
                        LudoProofTheme.darkPanelDrawable(
                            this@OfflineGameActivity,
                            goldBorder = true,
                        )
                },
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply { marginStart = dp(12) },
            )
        }

    private fun offlineHistory(state: MatchSnapshot?): String {
        if (state == null || state.history.isEmpty()) {
            return "No offline rolls yet."
        }
        return buildString {
            append("Recent offline rolls\n\n")
            state.history.takeLast(16).reversed().forEach { event ->
                append("#")
                append(event.eventIndex)
                append("  dice=")
                append(event.outcome ?: "?")
                if (event.proofDigest != null) {
                    append("  localV4=")
                    append(event.proofDigest.take(8))
                    append("…")
                }
                if (event.moveTokenIndex != null) {
                    append("  token=")
                    append(event.moveTokenIndex + 1)
                }
                if (event.captures > 0) {
                    append("  captures=")
                    append(event.captures)
                }
                append("\n")
            }
        }.trimEnd()
    }

    private fun showStatus(value: String) {
        statusText?.text = value
    }

    private fun sectionParams() =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, dp(18), 0, 0) }

    private fun tileParams() =
        LinearLayout.LayoutParams(
            0,
            dp(118),
            1f,
        ).apply { setMargins(dp(7), 0, dp(7), 0) }

    private fun toolParams() =
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        ).apply { setMargins(dp(5), dp(12), dp(5), 0) }

    private fun dp(value: Int): Int =
        LudoProofTheme.dp(this, value)

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
