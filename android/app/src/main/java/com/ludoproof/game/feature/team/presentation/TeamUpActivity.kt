package com.ludoproof.game.feature.team.presentation

import android.app.Activity
import android.content.Intent
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
import com.ludoproof.game.CachedMatchStore
import com.ludoproof.game.GameMode
import com.ludoproof.game.GameModeIntent
import com.ludoproof.game.LudoProofTheme
import com.ludoproof.game.MainActivity
import com.ludoproof.game.PlayerSession
import com.ludoproof.game.SecureSessionStore
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCredential
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.feature.leaderboard.data.remote.LeaderboardApi
import com.ludoproof.game.feature.profile.data.local.ProfileStore
import com.ludoproof.game.feature.team.data.local.TeamMatchmakingStore
import com.ludoproof.game.feature.team.data.local.TeamMatchmakingTicket
import com.ludoproof.game.feature.team.data.remote.TeamMatchmakingApi
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class TeamUpActivity : Activity() {
    private val api by lazy { TeamMatchmakingApi() }
    private val ticketStore by lazy { TeamMatchmakingStore(this) }
    private val identityStore by lazy { LeaderboardIdentityStore(this) }
    private val profileStore by lazy { ProfileStore(this) }
    private val sessionStore by lazy { SecureSessionStore(this) }
    private val cachedMatchStore by lazy { CachedMatchStore(this) }
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var requestInFlight = false
    private var stopped = false

    private lateinit var quickTeamButton: Button
    private lateinit var cancelButton: Button
    private lateinit var searchPanel: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var slotsText: TextView
    private lateinit var timerText: TextView

    private val pollRunnable =
        object : Runnable {
            override fun run() {
                if (!stopped && ticketStore.load() != null) {
                    pollSearch()
                    updateTimer()
                }
                if (!stopped) {
                    handler.postDelayed(this, POLL_MS)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        buildUi()
        renderSearchState()
    }

    override fun onStart() {
        super.onStart()
        stopped = false
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    override fun onStop() {
        stopped = true
        handler.removeCallbacks(pollRunnable)
        super.onStop()
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val (root, host) = LudoProofTheme.arcadeRoot(this)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(30))
        }

        content.addView(
            Button(this).apply {
                text = "‹  BACK"
                LudoProofTheme.secondary(this)
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50),
            ),
        )

        content.addView(
            TextView(this).apply {
                text = "TEAM UP"
                LudoProofTheme.title(this, 30f, gold = true)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(28), dp(8), dp(6))
            },
        )
        content.addView(
            TextView(this).apply {
                text = "2 VS 2 ONLINE"
                LudoProofTheme.body(this, 15f, centered = true, bright = true)
                setTextColor(0xFF69E8FF.toInt())
            },
        )

        content.addView(
            teamRulesPanel(),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(22) },
        )

        quickTeamButton = Button(this).apply {
            text = "QUICK TEAM"
            LudoProofTheme.primary(this)
            setOnClickListener { beginSearch() }
        }
        content.addView(
            quickTeamButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60),
            ).apply { topMargin = dp(18) },
        )

        content.addView(
            TextView(this).apply {
                text = "Find 3 real players. Red + Yellow play against Green + Blue."
                LudoProofTheme.body(this, 11.5f, centered = true, bright = true)
                setPadding(dp(12), dp(10), dp(12), dp(4))
            },
        )

        searchPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = LudoProofTheme.hudPanelDrawable(
                this@TeamUpActivity,
                goldBorder = true,
            )
            visibility = View.GONE

            addView(
                TextView(this@TeamUpActivity).apply {
                    text = "FINDING YOUR TEAM MATCH"
                    LudoProofTheme.title(this, 20f, gold = true)
                    gravity = Gravity.CENTER
                },
            )

            slotsText = TextView(this@TeamUpActivity).apply {
                text = "●  ○  ○  ○\n1 / 4 PLAYERS"
                LudoProofTheme.body(this, 16f, centered = true, bright = true)
                setTextColor(0xFF69E8FF.toInt())
                setPadding(dp(8), dp(14), dp(8), dp(8))
            }
            addView(slotsText)

            timerText = TextView(this@TeamUpActivity).apply {
                text = "SEARCH  00:00"
                LudoProofTheme.body(this, 12f, centered = true, bright = true)
            }
            addView(timerText)

            cancelButton = Button(this@TeamUpActivity).apply {
                text = "CANCEL SEARCH"
                LudoProofTheme.secondary(this)
                setOnClickListener { cancelSearch() }
            }
            addView(
                cancelButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52),
                ).apply { topMargin = dp(12) },
            )
        }
        content.addView(
            searchPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(16) },
        )

        statusText = TextView(this).apply {
            text = "Quick Team is ready."
            LudoProofTheme.body(this, 11.5f, centered = true, bright = true)
            setPadding(dp(10), dp(16), dp(10), dp(8))
        }
        content.addView(statusText)

        content.addView(
            TextView(this).apply {
                text = "SERVER-AUTHORITATIVE • ENTRONEX V4 VERIFIED DICE • TEAM RULESET V1"
                LudoProofTheme.body(this, 9.5f, centered = true)
                setPadding(dp(8), dp(8), dp(8), 0)
            },
        )

        scroll.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(root)
    }

    private fun teamRulesPanel(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = LudoProofTheme.hudPanelDrawable(
                this@TeamUpActivity,
                goldBorder = true,
            )

            listOf(
                "RED  +  YELLOW   •   TEAM A",
                "GREEN  +  BLUE   •   TEAM B",
                "NO FRIENDLY CAPTURE",
                "BOTH PARTNERS FINISH TO WIN",
            ).forEach { line ->
                addView(
                    TextView(this@TeamUpActivity).apply {
                        text = line
                        LudoProofTheme.body(this, 13f, centered = true, bright = true)
                        setPadding(dp(6), dp(6), dp(6), dp(6))
                    },
                )
            }
        }

    private fun beginSearch() {
        if (requestInFlight) return
        ticketStore.load()?.let {
            renderSearchState()
            pollSearch()
            return
        }

        val displayName =
            profileStore.snapshot().displayName
                .trim()
                .replace(Regex("\\s+"), " ")
                .take(24)
                .ifBlank { "Guest User" }
        if (displayName.length < 2) {
            showStatus("Set a valid profile name before Team Up.")
            return
        }

        val ticket = TeamMatchmakingTicket(
            requestId = UUID.randomUUID().toString(),
            displayName = displayName,
            startedAt = System.currentTimeMillis(),
        )
        runCatching { ticketStore.save(ticket) }
            .onFailure {
                showStatus("Could not save Team Up search. Try again.")
                return
            }

        renderSearchState()
        showStatus("Searching for 3 real players…")
        runRequest(
            action = {
                val credential = ensureLeaderboardCredential()
                api.search(
                    profileToken = credential.profileToken,
                    displayName = ticket.displayName,
                    clientRequestId = ticket.requestId,
                )
            },
            onSuccess = ::applyMatchmakingResponse,
        )
    }

    private fun pollSearch() {
        val ticket = ticketStore.load() ?: return
        runRequest(
            quietFailure = true,
            action = {
                val credential = ensureLeaderboardCredential()
                api.status(
                    profileToken = credential.profileToken,
                    clientRequestId = ticket.requestId,
                )
            },
            onSuccess = ::applyMatchmakingResponse,
        )
    }

    private fun cancelSearch() {
        val ticket = ticketStore.load()
            ?: run {
                renderSearchState()
                return
            }
        runRequest(
            action = {
                val credential = ensureLeaderboardCredential()
                api.cancel(
                    profileToken = credential.profileToken,
                    clientRequestId = ticket.requestId,
                )
            },
            onSuccess = { response ->
                if (response.optString("status") == "MATCHED") {
                    showStatus("Match was found while cancelling. Opening it…")
                    pollSearch()
                } else {
                    ticketStore.clear()
                    renderSearchState()
                    showStatus("Team search cancelled.")
                }
            },
        )
    }

    private fun applyMatchmakingResponse(response: JSONObject) {
        when (response.optString("status")) {
            "MATCHED" -> openMatchedGame(response)
            "SEARCHING" -> {
                val queued = response.optInt("queuedPlayers", 1).coerceIn(1, 4)
                renderQueuedPlayers(queued)
            }
            "IDLE" -> {
                ticketStore.clear()
                renderSearchState()
                showStatus("Search expired. Tap QUICK TEAM to search again.")
            }
            else -> showStatus("Unexpected Team Up matchmaking response.")
        }
    }

    private fun openMatchedGame(response: JSONObject) {
        val matchId = response.optString("matchId").trim().uppercase(Locale.ROOT)
        val playerId = response.optString("playerId").trim()
        val playerToken = response.optString("playerToken").trim()
        val matchMode = response.optString("matchMode")
        if (matchMode != "TEAM_UP") {
            showStatus("Team match validation failed. Search again.")
            return
        }

        val saved = runCatching {
            sessionStore.save(
                PlayerSession(
                    matchId = matchId,
                    playerId = playerId,
                    playerToken = playerToken,
                    modeWire = GameMode.TEAM_UP.wireValue,
                ),
            )
            cachedMatchStore.save(
                playerId,
                response.optJSONObject("state"),
            )
        }
        if (saved.isFailure) {
            sessionStore.clear()
            cachedMatchStore.clear()
            showStatus("Could not securely save the Team Up match.")
            return
        }

        ticketStore.clear()
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(
                    GameModeIntent.EXTRA_GAME_MODE,
                    GameMode.TEAM_UP.wireValue,
                )
                .putExtra(
                    GameModeIntent.EXTRA_RESUME_SAVED_MATCH,
                    true,
                ),
        )
        finish()
    }

    private fun ensureLeaderboardCredential(): LeaderboardCredential {
        identityStore.load()?.let { return it }
        val requestId = identityStore.registrationRequestId()
        val response = LeaderboardApi().registerProfile(requestId)
        val credential = LeaderboardCredential(
            profileId = response.getString("profileId"),
            profileToken = response.getString("profileToken"),
            registrationRequestId = requestId,
        )
        identityStore.save(credential)
        return credential
    }

    private fun runRequest(
        action: () -> JSONObject,
        onSuccess: (JSONObject) -> Unit,
        quietFailure: Boolean = false,
    ) {
        if (requestInFlight || executor.isShutdown || isFinishing || isDestroyed) return
        requestInFlight = true
        quickTeamButton.isEnabled = false
        if (::cancelButton.isInitialized) cancelButton.isEnabled = false

        val submitted = runCatching {
            executor.execute {
                try {
                    val response = action()
                    handler.post {
                        requestInFlight = false
                        if (isFinishing || isDestroyed) return@post
                        updateButtons()
                        onSuccess(response)
                    }
                } catch (error: Exception) {
                    handler.post {
                        requestInFlight = false
                        if (isFinishing || isDestroyed) return@post
                        updateButtons()
                        if (!quietFailure) {
                            showStatus(
                                "Team search error: " +
                                    (error.message ?: "connection failed"),
                            )
                        }
                    }
                }
            }
        }.isSuccess
        if (!submitted) {
            requestInFlight = false
            updateButtons()
        }
    }

    private fun renderSearchState() {
        val searching = ticketStore.load() != null
        quickTeamButton.visibility = if (searching) View.GONE else View.VISIBLE
        searchPanel.visibility = if (searching) View.VISIBLE else View.GONE
        if (searching) {
            renderQueuedPlayers(1)
            updateTimer()
        }
        updateButtons()
    }

    private fun renderQueuedPlayers(queued: Int) {
        val safe = queued.coerceIn(1, 4)
        val dots = buildString {
            repeat(4) { index ->
                if (index > 0) append("   ")
                append(if (index < safe) "●" else "○")
            }
        }
        slotsText.text = "$dots\n$safe / 4 PLAYERS"
        if (safe >= 4) {
            showStatus("Match found • assigning opposite-color teams…")
        } else {
            showStatus("Searching • ${4 - safe} more player" + if (4 - safe == 1) "" else "s" + " needed.")
        }
    }

    private fun updateTimer() {
        if (!::timerText.isInitialized) return
        val startedAt = ticketStore.load()?.startedAt ?: return
        val elapsed = ((System.currentTimeMillis() - startedAt) / 1_000L).coerceAtLeast(0L)
        timerText.text = String.format(
            Locale.US,
            "SEARCH  %02d:%02d",
            elapsed / 60L,
            elapsed % 60L,
        )
    }

    private fun updateButtons() {
        val searching = ticketStore.load() != null
        quickTeamButton.isEnabled = !requestInFlight && !searching
        if (::cancelButton.isInitialized) {
            cancelButton.isEnabled = !requestInFlight && searching
        }
    }

    private fun showStatus(value: String) {
        statusText.text = value
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val POLL_MS = 2_000L
    }
}
