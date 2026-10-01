package com.ludoproof.game

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class OfflineGameActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val engine by lazy { OfflineGameEngine(this) }
    private val preferences by lazy { GamePreferences(this) }
    private var playerCount = 2
    private var selectedColor = "BLUE"
    private var playerNames = MutableList(4) { "Player ${it + 1}" }
    private val nameFields = mutableListOf<EditText>()
    private var rollInProgress = false
    private var showingGame = false
    private lateinit var board: LudoBoardView
    private lateinit var dice: DiceView
    private lateinit var turn: TextView
    private lateinit var status: TextView
    private lateinit var roll: Button
    private lateinit var players: LinearLayout
    private lateinit var moves: LinearLayout
    private lateinit var result: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        if (savedInstanceState != null) {
            playerCount = savedInstanceState.getInt("players", 2).coerceIn(2, 4)
            selectedColor = savedInstanceState.getString("color", "BLUE").takeIf { it in COLORS } ?: "BLUE"
            savedInstanceState.getStringArrayList("names")?.takeIf { it.size == 4 }?.let { playerNames = it.toMutableList() }
        }
        val snapshot = engine.snapshot()
        if (snapshot != null) showGame(snapshot) else showSetup()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        captureNames()
        outState.putInt("players", playerCount)
        outState.putString("color", selectedColor)
        outState.putStringArrayList("names", ArrayList(playerNames))
        super.onSaveInstanceState(outState)
    }

    private fun showSetup() {
        showingGame = false
        nameFields.clear()
        val start = ArcadeUi.button(this, "LET'S PLAY  ›", primary = true) { startLocalGame() }
        val page = ArcadeUi.page(this, start)
        ArcadeUi.add(page, ArcadeUi.header(this, "LOCAL PLAY") { finish() }, 0)

        val game = LudoProofTheme.panel(this)
        val summary = ArcadeUi.row(this)
        summary.addView(ArcadeUi.text(this, "Classic\nRace your friends home", 16f).apply { setCompoundDrawablesWithIntrinsicBounds(GameGlyphDrawable("dice", android.graphics.Color.WHITE, dp(54)), null, null, null); compoundDrawablePadding = dp(14) },
            LinearLayout.LayoutParams(0, -2, 1f))
        summary.addView(ArcadeUi.button(this, "Rules") {
            ArcadeDialogs.showInfo(this, "CLASSIC RULES", HomeActivity.RULES)
        }, LinearLayout.LayoutParams(dp(88), -2))
        game.addView(summary)
        ArcadeUi.add(page, game, 18)

        val count = ArcadeUi.section(this, "SELECT PLAYERS")
        val countRow = ArcadeUi.row(this)
        (2..4).forEach { number ->
            val choice = ArcadeUi.button(this, "$number P") {
                captureNames()
                playerCount = number
                showSetup()
            }
            choice.setCompoundDrawablesWithIntrinsicBounds(null, GameGlyphDrawable("players$number", android.graphics.Color.WHITE, dp(32)), null, null)
            choice.compoundDrawablePadding = dp(4)
            styleChoice(choice, playerCount == number)
            choice.contentDescription = "$number players${if (playerCount == number) ", selected" else ""}"
            countRow.addView(choice, choiceParams())
        }
        ArcadeUi.add(count, countRow, 12)
        ArcadeUi.add(page, count)

        val colors = ArcadeUi.section(this, "PLAYER 1 · CHOOSE YOUR COLOR")
        val colorRow = ArcadeUi.row(this)
        COLORS.forEach { color ->
            val choice = ArcadeUi.button(this, color.lowercase().replaceFirstChar { it.uppercase() }) {
                captureNames()
                selectedColor = color
                showSetup()
            }.apply { textSize = 12f; setPadding(dp(2), dp(8), dp(2), dp(8)) }
            choice.setCompoundDrawablesWithIntrinsicBounds(null, GameGlyphDrawable("pawn", pawnColor(color), dp(42)), null, null)
            styleChoice(choice, color == selectedColor)
            colorRow.addView(choice, choiceParams())
        }
        ArcadeUi.add(colors, colorRow, 12)
        ArcadeUi.add(page, colors)

        val names = ArcadeUi.section(this, "WHO'S PLAYING?")
        val colorOrder = listOf(selectedColor) + COLORS.filter { it != selectedColor }
        repeat(playerCount) { index ->
            val label = ArcadeUi.text(this, "${index + 1}  ·  ${colorOrder[index]}", 12f)
            ArcadeUi.add(names, label, 12)
            val input = EditText(this).apply {
                id = View.generateViewId()
                contentDescription = "Player ${index + 1} name"
                hint = "Player ${index + 1}"
                setText(playerNames[index])
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setSingleLine()
                filters = arrayOf(InputFilter.LengthFilter(24))
                LudoProofTheme.input(this)
                minimumHeight = dp(52)
            }
            label.labelFor = input.id
            nameFields += input
            ArcadeUi.add(names, input, 5)
        }
        ArcadeUi.add(page, names)
        ArcadeUi.add(page, ArcadeUi.text(this, "Local verification · both seeds generated on this device", 12f, true), 12)
    }

    private fun startLocalGame() {
        captureNames()
        val invalid = nameFields.indexOfFirst { it.text.toString().trim().length !in 2..24 }
        if (invalid >= 0) {
            nameFields[invalid].error = "Use 2–24 characters"
            nameFields[invalid].requestFocus()
        } else {
            val keyboard = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            currentFocus?.let { keyboard.hideSoftInputFromWindow(it.windowToken, 0) }
            showGame(engine.start(playerCount, selectedColor, playerNames.take(playerCount)))
        }
    }

    private fun showGame(state: MatchSnapshot) {
        showingGame = true
        nameFields.clear()
        val page = ArcadeUi.page(this)
        ArcadeUi.add(page, ArcadeUi.header(this, "LOCAL MATCH") { finish() }, 0)
        turn = ArcadeUi.title(this, "", 20f).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        ArcadeUi.add(page, turn, 16)
        players = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ArcadeUi.add(page, players, 12)
        board = LudoBoardView(this).apply {
            onTokenSelected = { token -> move(token) }
        }
        val frame = LudoProofTheme.boardFrame(this)
        frame.addView(board, android.widget.FrameLayout.LayoutParams(-1, -2))
        ArcadeUi.add(page, frame, 14)

        val action = LudoProofTheme.panel(this)
        val diceRow = ArcadeUi.row(this)
        dice = DiceView(this)
        diceRow.addView(dice, LinearLayout.LayoutParams(dp(68), dp(68)))
        status = ArcadeUi.text(this, "", 14f).apply {
            setPadding(dp(14), 0, 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        diceRow.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        action.addView(diceRow)
        roll = ArcadeUi.button(this, "ROLL DICE", primary = true) { rollOffline() }
        ArcadeUi.add(action, roll, 12)
        moves = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ArcadeUi.add(action, moves, 0)
        ArcadeUi.add(page, action, 12)

        result = LudoProofTheme.panel(this)
        ArcadeUi.add(page, result)
        val tools = ArcadeUi.row(this)
        tools.addView(ArcadeUi.button(this, "History") { showHistory() }, choiceParams())
        tools.addView(ArcadeUi.button(this, "Engine map") {
            ArcadeDialogs.showNaturalWorldAudit(this, engine.lastRandomnessAudit())
        }, choiceParams())
        ArcadeUi.add(page, tools, 12)
        ArcadeUi.add(page, ArcadeUi.button(this, "New game") { newGame() }, 8)
        ArcadeUi.add(page, ArcadeUi.text(this, "Your game saves automatically.\nLocal verification · no remote attestation", 12f, true), 14)
        render(state)
    }

    private fun render(state: MatchSnapshot) {
        val active = state.players.find { it.seat == state.turnSeat }
        val finished = state.status == "FINISHED"
        val winner = state.players.find { it.playerId == state.winnerPlayerId }
        turn.text = if (finished) "${winner?.displayName ?: "Player"} wins!" else "${active?.displayName ?: "Player"}'s turn"
        players.removeAllViews()
        state.players.chunked(2).forEach { pair ->
            val row = ArcadeUi.row(this)
            pair.forEach { player ->
                val selected = !finished && player.seat == state.turnSeat
                val label = ArcadeUi.text(this,
                    "${if (selected) "› " else ""}${player.displayName}\n${player.color} · ${player.tokens.count { it == 57 }}/4 home", 12f).apply {
                    setPadding(dp(10), dp(9), dp(10), dp(9))
                    setCompoundDrawablesWithIntrinsicBounds(GameGlyphDrawable("pawn", pawnColor(player.color), dp(34)), null, null, null)
                    compoundDrawablePadding = dp(6)
                    background = LudoProofTheme.darkPanelDrawable(this@OfflineGameActivity, selected)
                    contentDescription = "${player.displayName}, ${player.color}, ${player.tokens.count { it == 57 }} of four home${if (selected) ", current turn" else ""}"
                }
                row.addView(label, choiceParams())
            }
            ArcadeUi.add(players, row, 4)
        }
        board.bind(state, engine.activePlayerId())
        board.isEnabled = !rollInProgress
        val outcome = state.pendingRoll?.outcome ?: state.history.lastOrNull()?.outcome
        if (outcome != null) dice.showOutcome(outcome)
        roll.isEnabled = !rollInProgress && state.status == "ACTIVE" && state.pendingRoll == null
        roll.visibility = if (finished) View.GONE else View.VISIBLE
        roll.text = if (state.pendingRoll != null) "CHOOSE A TOKEN" else "ROLL DICE"
        val latest = state.history.lastOrNull()
        status.text = when {
            finished -> "All four tokens are home. Well played!"
            state.pendingRoll != null -> "Rolled ${state.pendingRoll.outcome}. Tap a highlighted token or choose below."
            latest != null && latest.moveTokenIndex == null ->
                "Rolled ${latest.outcome}. No move available. ${active?.displayName ?: "Next player"}, roll again."
            else -> "${active?.displayName ?: "Player"}, roll when you're ready."
        }
        moves.removeAllViews()
        val legal = selectableTokenIndexes(state, engine.activePlayerId())
        if (legal.isNotEmpty()) {
            ArcadeUi.add(moves, ArcadeUi.text(this, "Choose your token", 12f, true), 10)
            legal.sorted().chunked(2).forEach { tokens ->
                val row = ArcadeUi.row(this)
                tokens.forEach { token ->
                    row.addView(ArcadeUi.button(this, "Token ${token + 1}") { move(token) }, choiceParams())
                }
                ArcadeUi.add(moves, row, 6)
            }
        }
        result.visibility = if (finished) View.VISIBLE else View.GONE
        result.removeAllViews()
        if (finished) {
            ArcadeUi.add(result, ArcadeUi.title(this, "★  ${winner?.displayName ?: "Player"}  ★", 22f), 0)
            ArcadeUi.add(result, ArcadeUi.text(this, "First to bring every token home.", 14f, true), 8)
            ArcadeUi.add(result, ArcadeUi.button(this, "Play again", primary = true) { newGame() }, 12)
        }
    }

    private fun move(token: Int) {
        if (rollInProgress) return
        val state = engine.snapshot() ?: return
        if (token !in selectableTokenIndexes(state, engine.activePlayerId())) return
        runCatching { engine.move(token) }.onSuccess {
            preferences.feedback(board)
            render(it)
        }.onFailure { status.text = it.message ?: "Could not move. Please try again." }
    }

    private fun rollOffline() {
        if (rollInProgress || !roll.isEnabled) return
        rollInProgress = true
        roll.isEnabled = false
        board.isEnabled = false
        // Save the authoritative local result before any presentation delay.
        val state = runCatching { engine.roll() }.getOrElse {
            rollInProgress = false
            engine.snapshot()?.let(::render)
            status.text = it.message ?: "Roll failed. Please try again."
            return
        }
        preferences.feedback(roll)
        dice.startRolling()
        roll.text = "ROLLING…"
        status.text = "Rolling for the current player…"
        handler.postDelayed({
            rollInProgress = false
            render(state)
        }, preferences.rollDurationMs)
    }

    private fun newGame() {
        if (rollInProgress) return
        ArcadeDialogs.confirm(this, "START A NEW GAME?",
            "This replaces your saved local match. Your current game stays available if you keep playing.", "New game") {
            engine.clear()
            showSetup()
        }
    }

    private fun showHistory() {
        val history = engine.snapshot()?.history.orEmpty()
        val details = if (history.isEmpty()) "No rolls yet. Roll the dice to begin." else history.takeLast(24).reversed().joinToString("\n\n") {
            "Roll #${it.eventIndex} · dice ${it.outcome ?: "?"}" +
                (it.moveTokenIndex?.let { index -> " · token ${index + 1}" } ?: "") +
                (if (it.captures > 0) " · ${it.captures} captured" else "") +
                "\nProof: ${it.proofDigest ?: "Unavailable"}"
        }
        ArcadeDialogs.showProofHistory(this, "LOCAL HISTORY", details)
    }

    private fun captureNames() {
        nameFields.forEachIndexed { index, input -> playerNames[index] = input.text.toString().trim() }
    }

    private fun styleChoice(button: Button, selected: Boolean) {
        button.isSelected = selected
        if (selected) LudoProofTheme.selectedTile(button) else LudoProofTheme.normalTile(button)
        button.contentDescription = "${button.text}${if (selected) ", selected" else ""}"
    }

    private fun choiceParams() = LinearLayout.LayoutParams(0, -2, 1f).apply {
        marginStart = dp(3)
        marginEnd = dp(3)
    }

    override fun onStop() {
        if (rollInProgress && showingGame) {
            handler.removeCallbacksAndMessages(null)
            rollInProgress = false
            engine.snapshot()?.let(::render)
        }
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun pawnColor(color: String) = when (color) {
        "RED" -> 0xFFFF4055.toInt(); "GREEN" -> 0xFF21D46A.toInt(); "YELLOW" -> 0xFFFFD431.toInt(); else -> 0xFF25A8FF.toInt()
    }
    private fun dp(value: Int) = LudoProofTheme.dp(this, value)
    companion object { private val COLORS = listOf("RED", "GREEN", "YELLOW", "BLUE") }
}
