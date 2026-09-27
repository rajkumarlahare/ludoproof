package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    private lateinit var nameInput: EditText
    private lateinit var matchInput: EditText
    private lateinit var matchInfoText: TextView
    private lateinit var turnText: TextView
    private lateinit var playersText: TextView
    private lateinit var verificationText: TextView
    private lateinit var proofDetailsText: TextView
    private lateinit var statusText: TextView
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

    private val statePollRunnable =
        object : Runnable {
            override fun run() {
                val state = currentState
                if (
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

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        secureSessionStore.load()?.let { session ->
            matchId = session.matchId
            playerToken = session.playerToken
            playerId = session.playerId
        }
        pendingSecret = pendingRollStore.load()

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(16),
                    dp(18),
                    dp(16),
                    dp(28),
                )
            }

        content.addView(
            TextView(this).apply {
                text = "LudoProof"
                textSize = 30f
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setTextColor(
                    Color.rgb(
                        24,
                        28,
                        35,
                    ),
                )
            },
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Fair multiplayer Ludo with locally verified EntroNex v4 dice proofs."
                textSize = 14f
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    0,
                    0,
                    0,
                    dp(12),
                )
            },
        )

        nameInput =
            EditText(this).apply {
                hint = "Player name"
                setText("Player")
                inputType =
                    InputType.TYPE_CLASS_TEXT
            }
        content.addView(nameInput)

        matchInput =
            EditText(this).apply {
                hint =
                    "Match code, e.g. LPABCDEFGH"
                setText(matchId.orEmpty())
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }
        content.addView(matchInput)

        createButton =
            button("Create Match") {
                createMatch()
            }
        joinButton =
            button("Join Match") {
                joinMatch()
            }
        content.addView(
            row(
                createButton,
                joinButton,
            ),
        )

        matchInfoText =
            infoText(
                "No active match",
                16f,
            )
        content.addView(matchInfoText)

        playersText =
            infoText(
                "Players will appear here.",
                14f,
            )
        content.addView(playersText)

        turnText =
            infoText(
                "Waiting for a match.",
                19f,
            ).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(8),
                    dp(10),
                    dp(8),
                    dp(10),
                )
            }
        content.addView(turnText)

        boardView =
            LudoBoardView(this).apply {
                onTokenSelected = {
                    tokenIndex ->
                    moveToken(tokenIndex)
                }
            }
        content.addView(
            boardView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        diceView = DiceView(this)
        content.addView(
            diceView,
            LinearLayout.LayoutParams(
                dp(92),
                dp(92),
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        verificationText =
            infoText(
                "No verified roll yet.",
                16f,
            ).apply {
                gravity =
                    Gravity.CENTER_HORIZONTAL
                setPadding(
                    dp(8),
                    dp(10),
                    dp(8),
                    dp(6),
                )
            }
        content.addView(verificationText)

        rollButton =
            button(
                "ROLL VERIFIED DICE",
            ) {
                rollVerifiedDice()
            }.apply {
                textSize = 19f
                minHeight = dp(64)
            }
        content.addView(rollButton)

        startButton =
            button("Start Match") {
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

        refreshButton =
            button("Refresh") {
                refreshState()
            }

        content.addView(
            row(
                startButton,
                refreshButton,
            ),
        )

        shareButton =
            button("Share Match Code") {
                shareMatch()
            }
        proofButton =
            button("Proof / History") {
                toggleProofDetails()
            }
        content.addView(
            row(
                shareButton,
                proofButton,
            ),
        )

        proofDetailsText =
            TextView(this).apply {
                textSize = 12f
                setTextIsSelectable(true)
                visibility = View.GONE
                setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10),
                )
                setBackgroundColor(
                    Color.rgb(
                        245,
                        246,
                        248,
                    ),
                )
            }
        content.addView(proofDetailsText)

        statusText =
            TextView(this).apply {
                textSize = 12f
                setTextIsSelectable(true)
                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(24),
                )
                text =
                    "Create or join a match to begin."
            }
        content.addView(statusText)

        setContentView(
            ScrollView(this).apply {
                addView(content)
            },
        )

        updateRollButton()

        if (
            matchId != null &&
            playerToken != null
        ) {
            matchInput.setText(matchId)
            refreshState()
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
            canEnterAnotherMatch
        joinButton.isEnabled =
            canEnterAnotherMatch
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
            myTurn &&
                state.pendingRoll
                    ?.status !=
                "RESOLVED"

        refreshButton.isEnabled = true
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
        proofDetailsText.visibility =
            if (
                proofDetailsText.visibility ==
                View.VISIBLE
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
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
            createButton.isEnabled = true
            joinButton.isEnabled = true
            nameInput.isEnabled = true
            matchInput.isEnabled = true
            refreshButton.isEnabled =
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
            textSize = size
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
            isAllCaps = false
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
        mainHandler.removeCallbacks(
            statePollRunnable,
        )
        mainHandler.postDelayed(
            statePollRunnable,
            STATE_POLL_MS,
        )
    }

    override fun onStop() {
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
