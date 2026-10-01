package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var introPanel: LinearLayout
    private lateinit var tokenActions: LinearLayout
    private var networkBusy = false
    private val api = GameApi()
    private val executor =
        Executors.newSingleThreadExecutor()
    private val mainHandler =
        Handler(Looper.getMainLooper())

    private lateinit var lobbyPanel: LinearLayout
    private lateinit var matchStatusPanel: LinearLayout
    private lateinit var resultPanel: FrameLayout
    private lateinit var resultTitleText: TextView
    private lateinit var resultSubtitleText: TextView
    private lateinit var boardFrame: FrameLayout
    private lateinit var actionPanel: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var matchInput: EditText
    private lateinit var matchInfoText: TextView
    private lateinit var turnText: TextView
    private lateinit var playersText: TextView
    private lateinit var verificationText: TextView
    private lateinit var verificationPanel: LinearLayout
    private lateinit var proofDetailsText: TextView
    private lateinit var statusText: TextView
    private lateinit var connectionText: TextView
    private lateinit var boardView: LudoBoardView
    private lateinit var diceView: DiceView
    private lateinit var createButton: Button
    private lateinit var joinButton: Button
    private lateinit var startButton: Button
    private lateinit var refreshButton: Button
    private lateinit var shareButton: Button
    private lateinit var rollButton: Button
    private lateinit var proofButton: Button

    private var matchId: String? = null
    private var playerToken: String? = null
    private var playerId: String? = null
    private var currentState: MatchSnapshot? = null
    private var pendingSecret: PendingRollSecret? = null
    private var isOnline: Boolean = false

    private val statePollRunnable =
        object : Runnable {
            override fun run() {
                val state = currentState
                if (
                    isOnline &&
                    playerToken != null &&
                    ::refreshButton.isInitialized &&
                    refreshButton.isEnabled &&
                    state?.status != "FINISHED"
                ) {
                    refreshState(
                        silent = true,
                    )
                }
                mainHandler.postDelayed(
                    this,
                    STATE_POLL_MS,
                )
            }
        }

    private val secureSessionStore by lazy {
        SecureSessionStore(this)
    }

    private val pendingRollStore by lazy {
        PendingRollStore(this)
    }

    private val pendingOperationStore by lazy {
        PendingOperationStore(this)
    }

    private val cachedMatchStore by lazy {
        CachedMatchStore(this)
    }

    private val connectivityMonitor by lazy {
        ConnectivityMonitor(this) { online ->
            mainHandler.post {
                if (isDestroyed) return@post
                val changed =
                    isOnline != online
                isOnline = online

                if (
                    ::connectionText.isInitialized
                ) {
                    connectionText.text =
                        if (online) {
                            "● ONLINE"
                        } else {
                            "● OFFLINE"
                        }
                    connectionText.setTextColor(
                        if (online) {
                            0xFF65EF55.toInt()
                        } else {
                            LudoProofTheme.GOLD
                        },
                    )
                }

                currentState?.let {
                    updateControls(it)
                } ?: setNetworkControls(
                    enabled = online,
                )

                if (!online) {
                    showStatus(
                        "Offline — last online state stays visible. Verified rolls and moves resume when internet returns.",
                    )
                } else if (
                    changed &&
                    playerToken != null
                ) {
                    refreshState(
                        silent = true,
                    )
                }
            }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )
        LudoProofTheme.configureWindow(
            this,
        )

        secureSessionStore
            .load()
            ?.let {
                    session ->
                matchId =
                    session.matchId
                playerToken =
                    session.playerToken
                playerId =
                    session.playerId
            }
        pendingSecret =
            pendingRollStore.load()

        val page = ArcadeUi.page(this)
        ArcadeUi.add(page, onlineTopBar(), 0)
        introPanel = onlineHero()
        ArcadeUi.add(page, introPanel, 20)
        lobbyPanel = onlineLobbyPanel()
        ArcadeUi.add(page, lobbyPanel, 18)
        statusText = ArcadeUi.text(this, "Create a room or join your friends.", 13f, true).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        ArcadeUi.add(page, statusText, 12)
        matchStatusPanel = onlineMatchStatusPanel().apply { visibility = View.GONE }
        ArcadeUi.add(page, matchStatusPanel, 12)
        resultPanel = onlineResultPanel().apply { visibility = View.GONE }
        ArcadeUi.add(page, resultPanel, 12)
        boardView = LudoBoardView(this).apply { onTokenSelected = { moveToken(it) } }
        boardFrame = LudoProofTheme.boardFrame(this).apply {
            visibility = View.GONE
            addView(boardView, FrameLayout.LayoutParams(-1, -2))
        }
        ArcadeUi.add(page, boardFrame, 12)
        actionPanel = onlineActionPanel().apply { visibility = View.GONE }
        ArcadeUi.add(page, actionPanel, 12)
        nameInput.setText(savedInstanceState?.getString("draftName") ?: getSharedPreferences("profile", MODE_PRIVATE).getString("name", "Player"))
        savedInstanceState?.getString("draftCode")?.let { matchInput.setText(it) }
        updateRollButton()

        if (
            matchId != null &&
            playerToken != null
        ) {
            matchInput.setText(
                matchId,
            )
            cachedMatchStore
                .load()
                ?.let {
                    applyResponse(
                        it,
                        announce = false,
                    )
                }
        }
    }

    private fun onlineTopBar(): LinearLayout = ArcadeUi.header(this, "ONLINE PLAY") { finish() }

    private fun onlineHero(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(ArcadeUi.title(context, "A seat for your friends.", 25f))
        ArcadeUi.add(this, ArcadeUi.text(context, "Create a private room. Share the code. Play together.", 14f, true), 8)
        connectionText = ArcadeUi.text(context, "Checking connection…", 12f, true)
        ArcadeUi.add(this, connectionText, 10)
    }

    private fun onlineLobbyPanel(): LinearLayout = ArcadeUi.section(this, "CREATE OR JOIN").apply {
        val nameLabel = ArcadeUi.text(context, "Your name", 13f)
        ArcadeUi.add(this, nameLabel, 16)
        nameInput = EditText(context).apply {
            id = View.generateViewId()
            hint = "Your display name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSingleLine()
            minimumHeight = dp(52)
            LudoProofTheme.input(this)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }
        nameLabel.labelFor = nameInput.id
        ArcadeUi.add(this, nameInput, 6)
        createButton = ArcadeUi.button(context, "Create a room", primary = true) { createMatch() }
        ArcadeUi.add(this, createButton, 14)
        ArcadeUi.add(this, ArcadeUi.text(context, "—  or join a friend's room  —", 12f, true), 18)
        val codeLabel = ArcadeUi.text(context, "Match code", 13f)
        ArcadeUi.add(this, codeLabel, 14)
        matchInput = EditText(context).apply {
            id = View.generateViewId()
            hint = "LPXXXXXXXX"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine()
            minimumHeight = dp(52)
            LudoProofTheme.input(this)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == android.view.inputmethod.EditorInfo.IME_ACTION_GO && joinButton.isEnabled) {
                    joinMatch()
                    true
                } else false
            }
        }
        codeLabel.labelFor = matchInput.id
        ArcadeUi.add(this, matchInput, 6)
        joinButton = ArcadeUi.button(context, "Join a room") { joinMatch() }
        ArcadeUi.add(this, joinButton, 12)
        ArcadeUi.add(this, ArcadeUi.text(context, "2–4 players · an internet connection is required", 12f, true), 12)
    }

    private fun onlineMatchStatusPanel(): LinearLayout = LudoProofTheme.panel(this).apply {
        matchInfoText = ArcadeUi.text(context, "", 12f, true).apply { setTextIsSelectable(true) }
        turnText = ArcadeUi.title(context, "Waiting for players", 20f).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        playersText = ArcadeUi.text(context, "", 13f, true)
        addView(matchInfoText)
        ArcadeUi.add(this, turnText, 8)
        ArcadeUi.add(this, playersText, 8)
    }

    private fun onlineResultPanel(): FrameLayout = FrameLayout(this).apply {
        val panel = LudoProofTheme.panel(context)
        resultTitleText = ArcadeUi.title(context, "MATCH COMPLETE", 22f)
        resultSubtitleText = ArcadeUi.text(context, "", 14f, true)
        panel.addView(resultTitleText)
        ArcadeUi.add(panel, resultSubtitleText, 8)
        addView(panel, FrameLayout.LayoutParams(-1, -2))
    }

    private fun onlineActionPanel(): LinearLayout = LudoProofTheme.panel(this).apply {
        verificationPanel = ArcadeUi.row(context)
        diceView = DiceView(context)
        verificationPanel.addView(diceView, LinearLayout.LayoutParams(dp(68), dp(68)))
        verificationText = ArcadeUi.text(context, "No verified roll yet.", 13f).apply { setPadding(dp(12), 0, 0, 0) }
        verificationPanel.addView(verificationText, LinearLayout.LayoutParams(0, -2, 1f))
        addView(verificationPanel)
        rollButton = ArcadeUi.button(context, "ROLL DICE", primary = true) { rollVerifiedDice() }
        ArcadeUi.add(this, rollButton, 12)
        tokenActions = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        ArcadeUi.add(this, tokenActions, 0)
        startButton = ArcadeUi.button(context, "Start match", primary = true) {
            withSession { code, token -> runNetwork(action = { api.start(code, token) }) }
        }
        ArcadeUi.add(this, startButton, 10)
        refreshButton = ArcadeUi.button(context, "Refresh") { refreshState() }
        shareButton = ArcadeUi.button(context, "Share code") { shareMatch() }
        val tools = ArcadeUi.row(context)
        tools.addView(shareButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) })
        tools.addView(refreshButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(4) })
        ArcadeUi.add(this, tools, 8)
        proofButton = ArcadeUi.button(context, "Verified history") { toggleProofDetails() }
        ArcadeUi.add(this, proofButton, 8)
        proofDetailsText = TextView(context).apply { visibility = View.GONE }
        ArcadeUi.add(this, ArcadeUi.text(context, "Remotely verified dice · retries keep the same roll", 12f, true), 12)
    }

    private fun updateTokenActions(state: MatchSnapshot) {
        tokenActions.removeAllViews()
        val legal = if (isOnline && !networkBusy) selectableTokenIndexes(state, playerId) else emptySet()
        legal.sorted().chunked(2).forEach { tokens ->
            val row = ArcadeUi.row(this)
            tokens.forEach { index ->
                row.addView(ArcadeUi.button(this, "Token ${index + 1}") { moveToken(index) },
                    LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
            }
            ArcadeUi.add(tokenActions, row, 8)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("draftName", nameInput.text.toString())
        outState.putString("draftCode", matchInput.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun isCompactOnline(): Boolean = LudoProofTheme.isCompactWidth(this)
    private fun createMatch() {
        if (networkBusy) return
        val displayName =
            runCatching {
                playerName()
            }.getOrElse {
                showStatus(
                    it.message
                        ?: "Invalid player name",
                )
                return
            }
        val operationKey =
            "create:" + displayName
        val requestId =
            runCatching {
                pendingOperationStore
                    .getOrCreate(
                        "create",
                        operationKey,
                    )
            }.getOrElse {
                showStatus(
                    it.message
                        ?: "Could not prepare match request",
                )
                return
            }

        runNetwork(
            action = {
                api.createMatch(
                    displayName,
                    requestId,
                )
            },
            onSuccess = {
                pendingOperationStore.clear(
                    "create",
                    operationKey,
                )
                captureSession(it)
                applyResponse(it)
            },
        )
    }

    private fun joinMatch() {
        if (networkBusy) return
        val displayName =
            runCatching {
                playerName()
            }.getOrElse {
                showStatus(
                    it.message
                        ?: "Invalid player name",
                )
                return
            }

        val code =
            matchInput.text
                .toString()
                .trim()
                .uppercase(java.util.Locale.ROOT)

        if (
            !Regex(
                "^LP[A-Z2-9]{8}$",
            ).matches(code)
        ) {
            matchInput.error = "Use the 10-character code beginning with LP"
            matchInput.requestFocus()
            showStatus(
                "Enter a valid LudoProof match code.",
            )
            return
        }

        val operationKey =
            "join:" +
                code +
                ":" +
                displayName
        val requestId =
            runCatching {
                pendingOperationStore
                    .getOrCreate(
                        "join",
                        operationKey,
                    )
            }.getOrElse {
                showStatus(
                    it.message
                        ?: "Could not prepare join request",
                )
                return
            }

        runNetwork(
            action = {
                api.joinMatch(
                    code,
                    displayName,
                    requestId,
                )
            },
            onSuccess = {
                pendingOperationStore.clear(
                    "join",
                    operationKey,
                )
                captureSession(it)
                applyResponse(it)
            },
        )
    }

    private fun refreshState(
        silent: Boolean = false,
    ) {
        withSession {
                code,
                token,
            ->
            runNetwork(
                action = {
                    api.state(
                        code,
                        token,
                    )
                },
                onSuccess = {
                    applyResponse(
                        it,
                        announce = !silent,
                    )
                },
                showWorking = !silent,
            )
        }
    }

    private fun rollVerifiedDice() {
        if (networkBusy) return
        withSession {
                code,
                token,
            ->
            diceView.startRolling()
            verificationText.text =
                "Verifying committed EntroNex roll…"
            runNetwork(
                action = {
                    var secret =
                        pendingSecret
                        ?.takeIf {
                            it.matchId == code
                        }

                if (secret == null) {
                    pendingSecret
                        ?.let {
                            pendingRollStore
                                .clear()
                        }

                    val prepared =
                        SeedCommitment.prepare()
                    secret =
                        PendingRollSecret(
                            matchId = code,
                            clientSeed =
                                prepared.clientSeed,
                            clientCommitment =
                                prepared.clientCommitment,
                        )
                    pendingRollStore.save(
                        secret,
                    )
                    pendingSecret = secret
                }

                api.commitRoll(
                    code,
                    token,
                    secret.clientCommitment,
                )

                val revealed =
                    api.revealRoll(
                        code,
                        token,
                        secret.clientSeed,
                    )

                pendingRollStore.clear()
                    pendingSecret = null
                    revealed
                },
            )
        }
    }

    private fun moveToken(
        index: Int,
    ) {
        if (networkBusy || index !in selectableTokenIndexes(currentState, playerId)) return
        val eventIndex =
            currentState
                ?.pendingRoll
                ?.eventIndex
                ?.takeIf {
                    it >= 0
                }
                ?: run {
                    showStatus(
                        "No verified roll is available for this move.",
                    )
                    return
                }

        withSession {
                code,
                token,
            ->
            runNetwork(
                action = {
                    api.move(
                        code,
                        token,
                        index,
                        eventIndex,
                    )
                },
            )
        }
    }

    private fun captureSession(
        response: JSONObject,
    ) {
        val code =
            response.getString(
                "matchId",
            )
        val token =
            response.getString(
                "playerToken",
            )
        val id =
            response.getString(
                "playerId",
            )

        if (
            pendingSecret != null &&
            pendingSecret?.matchId != code
        ) {
            pendingRollStore.clear()
            pendingSecret = null
        }

        matchId = code
        playerToken = token
        playerId = id
        matchInput.setText(code)

        persistSessionSecurely(
            code = code,
            id = id,
            token = token,
        )
    }

    private fun applyResponse(
        response: JSONObject,
        announce: Boolean = true,
    ) {
        val envelope =
            GameJson.envelope(
                response,
            )

        envelope.playerId
            ?.let {
                    resolvedPlayerId ->
                playerId =
                    resolvedPlayerId
                val code =
                    matchId
                val token =
                    playerToken
                if (
                    code != null &&
                    token != null
                ) {
                    persistSessionSecurely(
                        code =
                            code,
                        id =
                            resolvedPlayerId,
                        token =
                            token,
                    )
                }
            }

        val state =
            envelope.state
        if (
            state ==
            null
        ) {
            showStatus(
                response
                    .toString(2),
            )
            updateRollButton()
            return
        }

        currentState =
            state
        matchId =
            state.matchId

        response
            .optJSONObject(
                "state",
            )
            ?.let {
                    safeState ->
                cachedMatchStore
                    .save(
                        envelope.playerId
                            ?: playerId,
                        safeState,
                    )
            }

        reconcilePendingSecret(
            state,
        )
        boardView.bind(
            state,
            playerId,
        )

        matchInfoText.text =
            buildString {
                append(
                    "MATCH • ",
                )
                append(
                    state.matchId,
                )
                append(
                    "   •   ",
                )
                append(
                    state.status,
                )
                if (
                    state.rulesetId
                        .isNotBlank()
                ) {
                    append(
                        "   •   ",
                    )
                    append(
                        state.rulesetId,
                    )
                }
            }

        playersText.text =
            state.players
                .joinToString(
                    separator =
                        "\n",
                ) {
                    player ->
                    val marker =
                        if (
                            player.playerId ==
                            playerId
                        ) {
                            "  •  YOU"
                        } else {
                            ""
                        }
                    player.color +
                        "  •  " +
                        player.displayName +
                        marker
                }

        updateTurnBanner(
            state,
        )
        updateVerification(
            state,
        )
        proofDetailsText.text =
            proofDetails(
                state,
            )
        updateControls(
            state,
        )
        updateRollButton()

        if (
            announce
        ) {
            showStatus(
                if (
                    state.status ==
                    "WAITING"
                ) {
                    "Room synced • share the code and wait for players."
                } else {
                    "State revision updated. Event index: " +
                        state.randomEventIndex
                },
            )
        }
    }

    private fun reconcilePendingSecret(
        state: MatchSnapshot,
    ) {
        val secret =
            pendingSecret
                ?: return
        if (secret.matchId != state.matchId) {
            pendingRollStore.clear()
            pendingSecret = null
            return
        }

        val remoteCommitment =
            state.pendingRoll
                ?.clientCommitment
        if (
            remoteCommitment == null ||
            remoteCommitment !=
            secret.clientCommitment
        ) {
            pendingRollStore.clear()
            pendingSecret = null
        }
    }

    private fun updateTurnBanner(
        state: MatchSnapshot,
    ) {
        when (
            state.status
        ) {
            "WAITING" -> {
                turnText.text =
                    if (
                        state.players.size <
                        2
                    ) {
                        "WAITING FOR PLAYER"
                    } else if (
                        state.hostPlayerId ==
                        playerId
                    ) {
                        "READY • START THE MATCH"
                    } else {
                        "READY • WAITING FOR HOST"
                    }
            }

            "FINISHED" -> {
                val winner =
                    state.players
                        .find {
                            it.playerId ==
                                state.winnerPlayerId
                        }
                val winnerName =
                    winner
                        ?.displayName
                        ?: "Player"

                turnText.text =
                    "MATCH COMPLETE"

                resultTitleText.text =
                    if (
                        winner?.playerId ==
                        playerId
                    ) {
                        "YOU WON"
                    } else {
                        "WINNER • " +
                            winnerName
                    }

                resultSubtitleText.text =
                    if (
                        winner?.playerId ==
                        playerId
                    ) {
                        winnerName +
                            " • server-authoritative result • proof history available"
                    } else {
                        "Server-authoritative result • verified history available"
                    }
            }

            else -> {
                val active =
                    state.players
                        .getOrNull(
                            state.turnSeat,
                        )
                val mine =
                    active?.playerId ==
                        playerId
                turnText.text =
                    if (
                        mine
                    ) {
                        "YOUR TURN • ROLL OR MOVE"
                    } else {
                        "WAITING • " +
                            (
                                active
                                    ?.displayName
                                    ?: "Player"
                                ) +
                            "'S TURN"
                    }
            }
        }
    }

    private fun updateVerification(
        state: MatchSnapshot,
    ) {
        val pending =
            state.pendingRoll
        val latest =
            state.history
                .lastOrNull()

        val outcome =
            pending?.outcome
                ?: latest?.outcome
        val digest =
            pending?.proofDigest
                ?: latest?.proofDigest
        val eventIndex =
            if (
                pending?.eventIndex !=
                null &&
                pending.eventIndex >=
                0
            ) {
                pending.eventIndex
            } else {
                latest?.eventIndex
            }

        verificationText.text =
            if (
                outcome !=
                    null &&
                digest !=
                    null
            ) {
                diceView.showOutcome(
                    outcome,
                )
                buildString {
                    append(
                        "✓ VERIFIED • Dice ",
                    )
                    append(
                        outcome,
                    )
                    if (
                        eventIndex !=
                        null
                    ) {
                        append(
                            "\nEvent ",
                        )
                        append(
                            eventIndex,
                        )
                    }
                    append(
                        "  •  Proof ",
                    )
                    append(
                        shortDigest(
                            digest,
                        ),
                    )
                }
            } else {
                when (
                    pending?.status
                ) {
                    "CREATING" ->
                        "COMMITTING • Preparing server commitment…"

                    "COMMITTED" ->
                        "COMMITTED • Server commitment secured. Reveal can resume safely."

                    "RESOLVING" ->
                        "VERIFYING • Resolving committed EntroNex round…"

                    else ->
                        "No verified roll yet."
                }
            }
    }

    private fun updateControls(
        state: MatchSnapshot,
    ) {
        introPanel.visibility = View.GONE
        val mySeat =
            state.players
                .indexOfFirst {
                    it.playerId ==
                        playerId
                }
        val myTurn =
            state.status ==
                "ACTIVE" &&
                mySeat >=
                0 &&
                state.turnSeat ==
                mySeat

        val waiting =
            state.status ==
                "WAITING"
        val active =
            state.status ==
                "ACTIVE"
        val finished =
            state.status ==
                "FINISHED"
        val canEnterAnotherMatch =
            finished

        lobbyPanel.visibility =
            if (
                canEnterAnotherMatch
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        nameInput.visibility =
            if (
                canEnterAnotherMatch
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        matchInput.visibility =
            if (
                canEnterAnotherMatch
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        createButton.visibility =
            if (
                canEnterAnotherMatch
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        joinButton.visibility =
            if (
                canEnterAnotherMatch
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        createButton.isEnabled =
            canEnterAnotherMatch &&
                isOnline
        joinButton.isEnabled =
            canEnterAnotherMatch &&
                isOnline
        nameInput.isEnabled =
            canEnterAnotherMatch
        matchInput.isEnabled =
            canEnterAnotherMatch

        matchStatusPanel.visibility =
            View.VISIBLE
        resultPanel.visibility =
            if (
                finished
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        boardFrame.visibility =
            if (
                active ||
                finished
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        actionPanel.visibility =
            View.VISIBLE

        verificationPanel.visibility =
            if (
                active ||
                finished
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        diceView.visibility =
            View.VISIBLE
        verificationText.visibility =
            View.VISIBLE

        startButton.visibility =
            if (
                waiting
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        startButton.isEnabled =
            isOnline &&
                waiting &&
                state.players.size >=
                2 &&
                state.hostPlayerId ==
                playerId

        refreshButton.visibility =
            if (
                finished
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
        refreshButton.isEnabled =
            isOnline &&
                !finished

        shareButton.visibility =
            View.VISIBLE
        shareButton.isEnabled =
            state.matchId
                .isNotBlank()

        proofButton.visibility =
            if (
                active ||
                finished
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        rollButton.visibility =
            if (
                active
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
        rollButton.isEnabled =
            isOnline &&
                myTurn &&
                state.pendingRoll
                    ?.status !=
                "RESOLVED"
        boardView.isEnabled = isOnline && !networkBusy
        updateTokenActions(state)
        if (networkBusy) setNetworkControls(false)
    }

    private fun updateRollButton() {
        val secret =
            pendingSecret
        rollButton.text =
            if (
                secret != null &&
                secret.matchId ==
                matchId
            ) {
                "RESUME VERIFIED ROLL"
            } else {
                "ROLL VERIFIED DICE"
            }
    }

    private fun proofDetails(
        state: MatchSnapshot,
    ): String {
        val latest =
            state.history
                .takeLast(8)
                .reversed()

        if (latest.isEmpty()) {
            return "No proof history yet."
        }

        return buildString {
            append(
                "Recent verified events\n",
            )
            latest.forEach {
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
                append(
                    "  proof=",
                )
                append(
                    shortDigest(
                        event.proofDigest,
                    ),
                )
                append(
                    "  round=",
                )
                append(
                    shortDigest(
                        event.roundId,
                    ),
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
        }.trimEnd()
    }

    private fun toggleProofDetails() {
        ArcadeDialogs.showProofHistory(
            this,
            "VERIFIED HISTORY",
            proofDetailsText.text
                .toString()
                .ifBlank {
                    "No proof history yet."
                },
        )
    }

    private fun shareMatch() {
        val code =
            matchId
                ?: run {
                    showStatus(
                        "Create or join a match first.",
                    )
                    return
                }

        val intent =
            Intent(
                Intent.ACTION_SEND,
            ).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Join my LudoProof match: $code",
                )
            }
        startActivity(
            Intent.createChooser(
                intent,
                "Share LudoProof match",
            ),
        )
    }

    private fun playerName(): String {
        val value =
            nameInput.text
                .toString()
                .trim()
        if (value.length !in 2..24) {
            nameInput.error = "Use 2–24 characters"
            nameInput.requestFocus()
            throw IllegalArgumentException("Player name must contain 2 to 24 characters")
        }
        nameInput.error = null
        getSharedPreferences("profile", MODE_PRIVATE).edit().putString("name", value).apply()
        return value
    }

    private fun persistSessionSecurely(
        code: String,
        id: String,
        token: String,
    ) {
        try {
            secureSessionStore.save(
                PlayerSession(
                    matchId = code,
                    playerId = id,
                    playerToken = token,
                ),
            )
        } catch (_: Exception) {
            secureSessionStore.clear()
        }
    }

    private fun withSession(
        action: (
            String,
            String,
        ) -> Unit,
    ) {
        val code = matchId
        val token = playerToken

        if (
            code == null ||
            token == null
        ) {
            showStatus(
                "Create or join a match first.",
            )
            return
        }

        action(
            code,
            token,
        )
    }

    private fun runNetwork(
        action: () -> JSONObject,
        onSuccess: (
            JSONObject,
        ) -> Unit = {
            applyResponse(it)
        },
        showWorking: Boolean = true,
    ) {
        if (networkBusy || isDestroyed) return
        if (!isOnline) {
            diceView.stopRolling()
            showStatus(
                "Offline — this online match is read-only until internet returns.",
            )
            currentState?.let {
                updateControls(it)
            }
            return
        }

        networkBusy = true
        if (showWorking) {
            showStatus("Working…")
        }
        setNetworkControls(
            enabled = false,
        )

        executor.execute {
            try {
                val response =
                    action()
                mainHandler.post {
                    if (isDestroyed) return@post
                    networkBusy = false
                    setNetworkControls(
                        enabled = true,
                    )
                    onSuccess(
                        response,
                    )
                    updateRollButton()
                }
            } catch (
                error: Exception,
            ) {
                mainHandler.post {
                    if (isDestroyed) return@post
                    networkBusy = false
                    diceView.stopRolling()
                    val sessionReset =
                        resetInvalidSessionIfNeeded(
                            error,
                        )
                    if (!sessionReset) {
                        setNetworkControls(
                            enabled = true,
                        )
                        showStatus(
                            "Error: " +
                                (
                                    error.message
                                        ?: error
                                            .toString()
                                    ),
                        )
                        currentState
                            ?.let {
                                updateControls(
                                    it,
                                )
                            }
                    }
                    updateRollButton()
                }
            }
        }
    }

    private fun resetInvalidSessionIfNeeded(
        error: Exception,
    ): Boolean {
        val code =
            (error as? GameApiException)
                ?.code
                ?: return false
        if (
            code != "MATCH_NOT_FOUND" &&
            code != "AUTH_INVALID"
        ) {
            return false
        }

        secureSessionStore.clear()
        pendingRollStore.clear()
        pendingSecret = null
        matchId = null
        playerToken = null
        playerId = null
        currentState = null
        introPanel.visibility = View.VISIBLE
        tokenActions.removeAllViews()
        cachedMatchStore.clear()

        matchInput.setText("")
        boardView.bind(
            null,
            null,
        )
        diceView.stopRolling()
        matchInfoText.text =
            "No active match"
        playersText.text =
            "Players will appear here."
        turnText.text =
            "Create or join a new match."
        verificationText.text =
            "No verified roll yet."
        proofDetailsText.text = ""
        proofDetailsText.visibility =
            View.GONE

        lobbyPanel.visibility =
            View.VISIBLE
        nameInput.visibility =
            View.VISIBLE
        matchInput.visibility =
            View.VISIBLE
        createButton.visibility =
            View.VISIBLE
        joinButton.visibility =
            View.VISIBLE
        startButton.visibility =
            View.GONE
        rollButton.visibility =
            View.GONE

        nameInput.isEnabled = true
        matchInput.isEnabled = true
        createButton.isEnabled = true
        joinButton.isEnabled = true
        refreshButton.isEnabled = false
        shareButton.isEnabled = false

        showStatus(
            "Previous match session is no longer available. Create or join a new match.",
        )
        return true
    }

    private fun setNetworkControls(
        enabled: Boolean,
    ) {
        if (!enabled) {
            boardView.isEnabled = false
            tokenActions.removeAllViews()
            nameInput.isEnabled = false
            matchInput.isEnabled = false
            createButton.isEnabled = false
            joinButton.isEnabled = false
            refreshButton.isEnabled = false
            startButton.isEnabled = false
            rollButton.isEnabled = false
            return
        }

        val state = currentState
        if (state != null) {
            updateControls(state)
        } else {
            createButton.isEnabled =
                isOnline
            joinButton.isEnabled =
                isOnline
            nameInput.isEnabled = true
            matchInput.isEnabled = true
            refreshButton.isEnabled =
                isOnline &&
                    playerToken != null
            startButton.isEnabled = false
            rollButton.isEnabled = false
        }
    }

    private fun showStatus(
        value: String,
    ) {
        statusText.text = value
    }

    private fun infoText(
        value: String,
        size: Float,
    ): TextView =
        TextView(this).apply {
            text = value
            LudoProofTheme.body(
                this,
                size,
                centered = false,
            )
            setPadding(
                dp(4),
                dp(6),
                dp(4),
                dp(6),
            )
        }

    private fun button(
        label: String,
        action: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            LudoProofTheme.secondary(
                this,
            )
            setOnClickListener {
                action()
            }
        }



    private fun shortDigest(
        value: String?,
    ): String {
        if (
            value.isNullOrBlank()
        ) {
            return "—"
        }
        return if (
            value.length <= 16
        ) {
            value
        } else {
            value.take(8) +
                "…" +
                value.takeLast(6)
        }
    }

    private fun dp(
        value: Int,
    ): Int =
        (
            value *
                resources
                    .displayMetrics
                    .density
            ).toInt()

    override fun onStart() {
        super.onStart()
        connectivityMonitor.start()
        mainHandler.removeCallbacks(
            statePollRunnable,
        )
        mainHandler.postDelayed(
            statePollRunnable,
            STATE_POLL_MS,
        )
    }

    override fun onStop() {
        connectivityMonitor.stop()
        mainHandler.removeCallbacks(
            statePollRunnable,
        )
        super.onStop()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(
            statePollRunnable,
        )
        executor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val STATE_POLL_MS =
            3_000L
    }
}
