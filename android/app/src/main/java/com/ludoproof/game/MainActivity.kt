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
    private val api = GameApi()
    private val executor =
        Executors.newSingleThreadExecutor()
    private val mainHandler =
        Handler(Looper.getMainLooper())

    private lateinit var lobbyPanel: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var matchInput: EditText
    private lateinit var matchInfoText: TextView
    private lateinit var turnText: TextView
    private lateinit var playersText: TextView
    private lateinit var verificationText: TextView
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
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)

        secureSessionStore.load()?.let { session ->
            matchId = session.matchId
            playerToken = session.playerToken
            playerId = session.playerId
        }
        pendingSecret =
            pendingRollStore.load()

        val (root, host) =
            LudoProofTheme.arcadeRoot(this)

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }
        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(28),
                )
            }
        scroll.addView(content)
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val topBar =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        topBar.addView(
            Button(this).apply {
                LudoProofTheme.circularAction(
                    this,
                    "‹",
                )
                setOnClickListener {
                    finish()
                }
            },
            LinearLayout.LayoutParams(
                dp(52),
                dp(52),
            ),
        )

        connectionText =
            TextView(this).apply {
                text = "● CHECKING"
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                    bright = true,
                )
                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8),
                )
                background =
                    LudoProofTheme.darkPanelDrawable(
                        this@MainActivity,
                        goldBorder = true,
                    )
            }
        topBar.addView(
            connectionText,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                setMargins(
                    dp(12),
                    0,
                    dp(12),
                    0,
                )
            },
        )

        topBar.addView(
            Button(this).apply {
                LudoProofTheme.circularAction(
                    this,
                    "⚙",
                )
                setOnClickListener {
                    ArcadeDialogs.showSettings(
                        this@MainActivity,
                    )
                }
            },
            LinearLayout.LayoutParams(
                dp(52),
                dp(52),
            ),
        )
        content.addView(topBar)

        lobbyPanel =
            LudoProofTheme.panel(this).apply {
                addView(
                    TextView(
                        this@MainActivity,
                    ).apply {
                        text =
                            "SELECT ONLINE MATCH"
                        LudoProofTheme.title(
                            this,
                            21f,
                        )
                        setPadding(
                            0,
                            0,
                            0,
                            dp(12),
                        )
                    },
                )

                nameInput =
                    EditText(
                        this@MainActivity,
                    ).apply {
                        hint =
                            "Player name"
                        setText(
                            "Player",
                        )
                        inputType =
                            InputType
                                .TYPE_CLASS_TEXT
                        LudoProofTheme.input(
                            this,
                        )
                    }
                addView(
                    nameInput,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin =
                            dp(10)
                    },
                )

                matchInput =
                    EditText(
                        this@MainActivity,
                    ).apply {
                        hint =
                            "Match code for Join"
                        setText(
                            matchId
                                .orEmpty(),
                        )
                        inputType =
                            InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                        LudoProofTheme.input(
                            this,
                        )
                    }
                addView(
                    matchInput,
                )

                val actions =
                    LinearLayout(
                        this@MainActivity,
                    ).apply {
                        orientation =
                            LinearLayout.HORIZONTAL
                    }

                createButton =
                    Button(
                        this@MainActivity,
                    ).apply {
                        text =
                            "＋\nCREATE"
                        LudoProofTheme.selectedTile(
                            this,
                        )
                        setTextColor(
                            LudoProofTheme.WHITE,
                        )
                        textSize = 17f
                        setOnClickListener {
                            createMatch()
                        }
                    }
                joinButton =
                    Button(
                        this@MainActivity,
                    ).apply {
                        text =
                            "⇥\nJOIN"
                        LudoProofTheme.normalTile(
                            this,
                        )
                        setTextColor(
                            LudoProofTheme.WHITE,
                        )
                        textSize = 17f
                        setOnClickListener {
                            joinMatch()
                        }
                    }
                actions.addView(
                    createButton,
                    LinearLayout.LayoutParams(
                        0,
                        dp(96),
                        1f,
                    ).apply {
                        setMargins(
                            dp(6),
                            dp(14),
                            dp(6),
                            0,
                        )
                    },
                )
                actions.addView(
                    joinButton,
                    LinearLayout.LayoutParams(
                        0,
                        dp(96),
                        1f,
                    ).apply {
                        setMargins(
                            dp(6),
                            dp(14),
                            dp(6),
                            0,
                        )
                    },
                )
                addView(actions)
            }
        content.addView(
            lobbyPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(
                    0,
                    dp(18),
                    0,
                    dp(16),
                )
            },
        )

        matchInfoText =
            infoText(
                "No active match",
                14f,
            ).apply {
                gravity =
                    Gravity.CENTER
            }
        content.addView(matchInfoText)

        playersText =
            infoText(
                "Players will appear here.",
                13f,
            ).apply {
                gravity =
                    Gravity.CENTER
            }
        content.addView(playersText)

        turnText =
            infoText(
                "Waiting for a match.",
                22f,
            ).apply {
                gravity =
                    Gravity.CENTER
                setTextColor(
                    LudoProofTheme.GOLD,
                )
                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(10),
                )
            }
        content.addView(turnText)

        boardView =
            LudoBoardView(this).apply {
                onTokenSelected = {
                        tokenIndex ->
                    moveToken(
                        tokenIndex,
                    )
                }
            }
        val boardFrame =
            LudoProofTheme.boardFrame(
                this,
            )
        boardFrame.addView(
            boardView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        content.addView(
            boardFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(
                    dp(2),
                    0,
                    dp(2),
                    dp(14),
                )
            },
        )

        val actionPanel =
            LudoProofTheme.panel(
                this,
            )

        diceView =
            DiceView(this)
        actionPanel.addView(
            diceView,
            LinearLayout.LayoutParams(
                dp(102),
                dp(102),
            ).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
            },
        )

        verificationText =
            infoText(
                "No verified roll yet.",
                14f,
            ).apply {
                gravity =
                    Gravity.CENTER
                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(10),
                )
            }
        actionPanel.addView(
            verificationText,
        )

        rollButton =
            button(
                "ROLL VERIFIED DICE",
            ) {
                rollVerifiedDice()
            }.apply {
                textSize =
                    20f
            }
        LudoProofTheme.primary(
            rollButton,
        )
        actionPanel.addView(
            rollButton,
        )

        startButton =
            button(
                "START MATCH",
            ) {
                withSession {
                        code,
                        token,
                    ->
                    runNetwork(
                        action = {
                            api.start(
                                code,
                                token,
                            )
                        },
                    )
                }
            }
        LudoProofTheme.primary(
            startButton,
        )

        refreshButton =
            button(
                "REFRESH",
            ) {
                refreshState()
            }

        actionPanel.addView(
            row(
                startButton,
                refreshButton,
            ),
        )

        shareButton =
            button(
                "SHARE CODE",
            ) {
                shareMatch()
            }
        proofButton =
            button(
                "PROOF / HISTORY",
            ) {
                toggleProofDetails()
            }
        actionPanel.addView(
            row(
                shareButton,
                proofButton,
            ),
        )

        proofDetailsText =
            TextView(this).apply {
                visibility =
                    View.GONE
            }

        statusText =
            TextView(this).apply {
                text =
                    "Create or join a match to begin."
                LudoProofTheme.body(
                    this,
                    12f,
                    centered = true,
                )
                setPadding(
                    dp(8),
                    dp(10),
                    dp(8),
                    0,
                )
            }
        actionPanel.addView(
            statusText,
        )

        content.addView(
            actionPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(
                    dp(6),
                    0,
                    dp(6),
                    0,
                )
            },
        )

        setContentView(root)
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

    private fun createMatch() {
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
                .uppercase()

        if (
            !Regex(
                "^LP[A-Z2-9]{8}$",
            ).matches(code)
        ) {
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

        envelope.playerId?.let { resolvedPlayerId ->
            playerId = resolvedPlayerId
            val code = matchId
            val token = playerToken
            if (code != null && token != null) {
                persistSessionSecurely(
                    code = code,
                    id = resolvedPlayerId,
                    token = token,
                )
            }
        }

        val state =
            envelope.state
        if (state == null) {
            showStatus(
                response.toString(2),
            )
            updateRollButton()
            return
        }

        currentState = state
        matchId = state.matchId

        response.optJSONObject(
            "state",
        )?.let {
            safeState ->
            cachedMatchStore.save(
                envelope.playerId
                    ?: playerId,
                safeState,
            )
        }
        reconcilePendingSecret(state)
        boardView.bind(
            state,
            playerId,
        )

        matchInfoText.text =
            buildString {
                append("Match: ")
                append(state.matchId)
                append("  •  ")
                append(state.status)
                if (
                    state.rulesetId
                        .isNotBlank()
                ) {
                    append("\nRules: ")
                    append(
                        state.rulesetId,
                    )
                }
            }

        playersText.text =
            state.players
                .joinToString(
                    separator = "   ",
                ) {
                    player ->
                    val marker =
                        if (
                            player.playerId ==
                            playerId
                        ) {
                            " (You)"
                        } else {
                            ""
                        }
                    player.color +
                        ": " +
                        player.displayName +
                        marker
                }

        updateTurnBanner(state)
        updateVerification(state)
        proofDetailsText.text =
            proofDetails(state)
        updateControls(state)
        updateRollButton()

        if (announce) {
            showStatus(
                "State revision updated. " +
                    "Event index: " +
                    state.randomEventIndex,
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
        when (state.status) {
            "WAITING" -> {
                turnText.text =
                    if (
                        state.players.size < 2
                    ) {
                        "Waiting for another player"
                    } else {
                        "Ready — host can start"
                    }
            }

            "FINISHED" -> {
                val winner =
                    state.players.find {
                        it.playerId ==
                            state.winnerPlayerId
                    }
                turnText.text =
                    "Winner: " +
                        (
                            winner
                                ?.displayName
                                ?: "Player"
                            )
            }

            else -> {
                val active =
                    state.players.getOrNull(
                        state.turnSeat,
                    )
                val mine =
                    active?.playerId ==
                        playerId
                turnText.text =
                    if (mine) {
                        "Your turn"
                    } else {
                        "Turn: " +
                            (
                                active
                                    ?.displayName
                                    ?: "Player"
                                )
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
                pending?.eventIndex != null &&
                pending.eventIndex >= 0
            ) {
                pending.eventIndex
            } else {
                latest?.eventIndex
            }

        verificationText.text =
            if (
                outcome != null &&
                digest != null
            ) {
                diceView.showOutcome(outcome)
                buildString {
                    append(
                        "Dice: ",
                    )
                    append(outcome)
                    append(
                        "   ✓ EntroNex v4 proof verified locally",
                    )
                    if (
                        eventIndex != null
                    ) {
                        append(
                            "\nEvent: ",
                        )
                        append(eventIndex)
                    }
                    append(
                        "  •  Proof: ",
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
                        "Preparing EntroNex commitment…"

                    "COMMITTED" ->
                        "Server commitment received — reveal can resume safely."

                    "RESOLVING" ->
                        "Resolving the committed EntroNex round…"

                    else ->
                        "No verified roll yet."
                }
            }
    }

    private fun updateControls(
        state: MatchSnapshot,
    ) {
        val mySeat =
            state.players.indexOfFirst {
                it.playerId == playerId
            }
        val myTurn =
            state.status == "ACTIVE" &&
                mySeat >= 0 &&
                state.turnSeat == mySeat

        val canEnterAnotherMatch =
            state.status == "FINISHED"

        lobbyPanel.visibility =
            if (canEnterAnotherMatch) {
                View.VISIBLE
            } else {
                View.GONE
            }

        nameInput.visibility =
            if (canEnterAnotherMatch) {
                View.VISIBLE
            } else {
                View.GONE
            }
        matchInput.visibility =
            if (canEnterAnotherMatch) {
                View.VISIBLE
            } else {
                View.GONE
            }
        createButton.visibility =
            if (canEnterAnotherMatch) {
                View.VISIBLE
            } else {
                View.GONE
            }
        joinButton.visibility =
            if (canEnterAnotherMatch) {
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

        startButton.visibility =
            if (state.status == "WAITING") {
                View.VISIBLE
            } else {
                View.GONE
            }
        startButton.isEnabled =
            isOnline &&
                state.status == "WAITING" &&
                state.players.size >= 2 &&
                state.hostPlayerId == playerId

        rollButton.visibility =
            if (state.status == "ACTIVE") {
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

        refreshButton.isEnabled =
            isOnline
        shareButton.isEnabled =
            state.matchId.isNotBlank()
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
        require(
            value.length in 2..24,
        ) {
            "Player name must contain 2 to 24 characters"
        }
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

    private fun row(
        vararg views: View,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            for (view in views) {
                addView(
                    view,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams
                            .WRAP_CONTENT,
                        1f,
                    ),
                )
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
