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
import com.ludoproof.game.ui.online.*
import com.ludoproof.game.feature.online.*

class MainActivity : Activity() {
    internal val api = GameApi()
    internal val executor =
        Executors.newSingleThreadExecutor()
    internal val mainHandler =
        Handler(Looper.getMainLooper())

    internal lateinit var lobbyPanel: LinearLayout
    internal lateinit var matchStatusPanel: LinearLayout
    internal lateinit var resultPanel: FrameLayout
    internal lateinit var resultTitleText: TextView
    internal lateinit var resultSubtitleText: TextView
    internal lateinit var boardFrame: FrameLayout
    internal lateinit var actionPanel: LinearLayout
    internal lateinit var nameInput: EditText
    internal lateinit var matchInput: EditText
    internal lateinit var matchInfoText: TextView
    internal lateinit var turnText: TextView
    internal lateinit var playersText: TextView
    internal lateinit var verificationText: TextView
    internal lateinit var verificationPanel: LinearLayout
    internal lateinit var proofDetailsText: TextView
    internal lateinit var statusText: TextView
    internal lateinit var connectionText: TextView
    internal lateinit var boardView: LudoBoardView
    internal lateinit var diceView: DiceView
    internal lateinit var createButton: Button
    internal lateinit var joinButton: Button
    internal lateinit var startButton: Button
    internal lateinit var refreshButton: Button
    internal lateinit var shareButton: Button
    internal lateinit var rollButton: Button
    internal lateinit var proofButton: Button

    internal var matchId: String? = null
    internal var playerToken: String? = null
    internal var playerId: String? = null
    internal var currentState: MatchSnapshot? = null
    internal var pendingSecret: PendingRollSecret? = null
    internal var isOnline: Boolean = false

    internal val statePollRunnable =
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

    internal val secureSessionStore by lazy {
        SecureSessionStore(this)
    }

    internal val pendingRollStore by lazy {
        PendingRollStore(this)
    }

    internal val pendingOperationStore by lazy {
        PendingOperationStore(this)
    }

    internal val cachedMatchStore by lazy {
        CachedMatchStore(this)
    }

    internal val connectivityMonitor by lazy {
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

        val (root, host) =
            LudoProofTheme
                .arcadeRoot(this)

        val scroll =
            ScrollView(this).apply {
                isFillViewport =
                    true
                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }

        val contentHost =
            FrameLayout(this)
        scroll.addView(
            contentHost,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val horizontalPaddingDp =
            LudoProofTheme
                .pageHorizontalPaddingDp(this)
        val availableWidth =
            (
                resources.displayMetrics.widthPixels -
                    dp(horizontalPaddingDp * 2)
                ).coerceAtLeast(1)
        val contentWidth =
            minOf(
                availableWidth,
                dp(
                    LudoProofTheme
                        .pageMaxContentWidthDp(this),
                ),
            )

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(14),
                    0,
                    dp(28),
                )
            }

        contentHost.addView(
            content,
            FrameLayout.LayoutParams(
                contentWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL,
            ),
        )

        host.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        content.addView(
            onlineTopBar(),
        )

        content.addView(
            onlineHero(),
            onlineSectionParams(
                if (isCompactOnline()) 12 else 16,
            ),
        )

        lobbyPanel =
            onlineLobbyPanel()
        content.addView(
            lobbyPanel,
            onlineSectionParams(
                if (isCompactOnline()) 12 else 16,
            ),
        )

        matchStatusPanel =
            onlineMatchStatusPanel()
                .apply {
                    visibility =
                        View.GONE
                }
        content.addView(
            matchStatusPanel,
            onlineSectionParams(
                if (isCompactOnline()) 12 else 16,
            ),
        )

        resultPanel =
            onlineResultPanel()
                .apply {
                    visibility =
                        View.GONE
                }
        content.addView(
            resultPanel,
            onlineSectionParams(
                if (isCompactOnline()) 12 else 16,
            ),
        )

        boardView =
            LudoBoardView(this).apply {
                onTokenSelected = {
                        tokenIndex ->
                    moveToken(
                        tokenIndex,
                    )
                }
            }

        boardFrame =
            LudoProofTheme
                .boardFrame(this)
                .apply {
                    visibility =
                        View.GONE
                }
        boardFrame.addView(
            boardView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        content.addView(
            boardFrame,
            onlineSectionParams(
                if (isCompactOnline()) 12 else 14,
            ).apply {
                leftMargin =
                    dp(2)
                rightMargin =
                    dp(2)
            },
        )

        actionPanel =
            onlineActionPanel()
                .apply {
                    visibility =
                        View.GONE
                }
        content.addView(
            actionPanel,
            onlineSectionParams(
                if (isCompactOnline()) 12 else 14,
            ),
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
