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
    internal val gameMode:
        GameMode =
        GameMode.ONLINE

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
    internal lateinit var findMatchButton: Button
    internal lateinit var cancelMatchmakingButton: Button
    internal lateinit var twoPlayerButton: Button
    internal lateinit var fourPlayerButton: Button
    internal lateinit var matchmakingStatusText: TextView
    internal lateinit var createButton: Button
    internal lateinit var joinButton: Button
    internal lateinit var startButton: Button
    internal lateinit var refreshButton: Button
    internal lateinit var shareButton: Button
    internal lateinit var rollButton: Button
    internal lateinit var proofButton: Button

    internal val uiStateHolder = OnlineGameStateHolder()

    internal val session by lazy {
        RemoteMatchSession(
            mode =
                gameMode,
            snapshotProvider = {
                currentState
            },
            playerIdProvider = {
                playerId
            },
        )
    }

    internal var matchId: String?
        get() = uiStateHolder.value.matchId
        set(value) { uiStateHolder.update { it.copy(matchId = value) } }

    internal var playerToken: String?
        get() = uiStateHolder.value.playerToken
        set(value) { uiStateHolder.update { it.copy(playerToken = value) } }

    internal var playerId: String?
        get() = uiStateHolder.value.playerId
        set(value) { uiStateHolder.update { it.copy(playerId = value) } }

    internal var currentState: MatchSnapshot?
        get() = uiStateHolder.value.currentState
        set(value) { uiStateHolder.update { it.copy(currentState = value) } }

    internal var pendingSecret: PendingRollSecret?
        get() = uiStateHolder.value.pendingSecret
        set(value) { uiStateHolder.update { it.copy(pendingSecret = value) } }

    internal var isOnline: Boolean
        get() = uiStateHolder.value.isOnline
        set(value) { uiStateHolder.update { it.copy(isOnline = value) } }

    internal var selectedPublicPlayerCount =
        2

    @Volatile
    internal var matchmakingRequestInFlight =
        false

    @Volatile
    internal var realtimeConnected =
        false

    internal val realtimeRefreshRunnable =
        Runnable {
            if (
                canRenderUi() &&
                isOnline &&
                playerToken != null
            ) {
                refreshState(
                    silent = true,
                )
            }
        }

    internal val matchmakingPollRunnable =
        object : Runnable {
            override fun run() {
                if (
                    canRenderUi() &&
                    isOnline &&
                    matchId == null &&
                    publicMatchmakingStore.load() !=
                    null
                ) {
                    pollPublicMatchmaking()
                }
                mainHandler.postDelayed(
                    this,
                    MATCHMAKING_POLL_MS,
                )
            }
        }

    internal val realtimeReconnectRunnable =
        object : Runnable {
            override fun run() {
                if (
                    canRenderUi() &&
                    isOnline &&
                    playerToken != null &&
                    !realtimeConnected
                ) {
                    connectRealtimeIfPossible()
                }
                mainHandler.postDelayed(
                    this,
                    REALTIME_RECONNECT_MS,
                )
            }
        }

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

    internal val publicMatchmakingStore by lazy {
        PublicMatchmakingStore(this)
    }

    internal val realtimeClient by lazy {
        MatchRealtimeClient(
            onRevision = {
                    revision,
                    _,
                ->
                mainHandler.post {
                    if (
                        !canRenderUi()
                    ) {
                        return@post
                    }

                    val localRevision =
                        currentState
                            ?.revision
                            ?: -1
                    if (
                        revision >
                        localRevision
                    ) {
                        mainHandler.removeCallbacks(
                            realtimeRefreshRunnable,
                        )
                        mainHandler.postDelayed(
                            realtimeRefreshRunnable,
                            120L,
                        )
                    }
                }
            },
            onConnectionChanged = {
                    connected ->
                mainHandler.post {
                    if (
                        !canRenderUi()
                    ) {
                        return@post
                    }
                    realtimeConnected =
                        connected
                    updateConnectionLabel()
                }
            },
        )
    }

    internal val connectivityMonitor by lazy {
        ConnectivityMonitor(this) { online ->
            mainHandler.post {
                if (
                    !canRenderUi()
                ) {
                    return@post
                }

                val changed =
                    isOnline != online
                isOnline = online

                if (!online) {
                    realtimeConnected =
                        false
                    realtimeClient.disconnect()
                }
                updateConnectionLabel()

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
                    connectRealtimeIfPossible()
                } else if (
                    changed &&
                    publicMatchmakingStore.load() !=
                    null
                ) {
                    pollPublicMatchmaking()
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
        val isExpandedWidth =
            LudoProofTheme
                .isExpandedWidth(this)
        val contentWidth =
            if (isExpandedWidth) {
                minOf(
                    resources.displayMetrics.widthPixels,
                    dp(
                        LudoProofTheme
                            .pageMaxContentWidthDp(this),
                    ),
                )
            } else {
                resources.displayMetrics.widthPixels
            }
        val sectionSideMargin =
            if (isExpandedWidth) {
                0
            } else {
                dp(horizontalPaddingDp)
            }

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
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                leftMargin =
                    sectionSideMargin
                rightMargin =
                    sectionSideMargin
            },
        )

        content.addView(
            onlineHero(),
            onlineSectionParams(
                if (isCompactOnline()) 12 else 16,
            ).apply {
                leftMargin =
                    sectionSideMargin
                rightMargin =
                    sectionSideMargin
            },
        )

        lobbyPanel =
            onlineLobbyPanel()
        content.addView(
            lobbyPanel,
            onlineSectionParams(
                if (isCompactOnline()) 12 else 16,
            ).apply {
                leftMargin =
                    sectionSideMargin
                rightMargin =
                    sectionSideMargin
            },
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
            ).apply {
                leftMargin =
                    sectionSideMargin
                rightMargin =
                    sectionSideMargin
            },
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
            ).apply {
                leftMargin =
                    sectionSideMargin
                rightMargin =
                    sectionSideMargin
            },
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
            FrameLayout(this).apply {
                visibility =
                    View.GONE
                clipChildren =
                    false
                clipToPadding =
                    false
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
                    sectionSideMargin
                rightMargin =
                    sectionSideMargin
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
        restorePublicMatchmakingUi()

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
        mainHandler.removeCallbacks(
            matchmakingPollRunnable,
        )
        mainHandler.post(
            matchmakingPollRunnable,
        )
        mainHandler.removeCallbacks(
            realtimeReconnectRunnable,
        )
        mainHandler.post(
            realtimeReconnectRunnable,
        )
        connectRealtimeIfPossible()
    }

    override fun onStop() {
        connectivityMonitor.stop()
        mainHandler.removeCallbacks(
            statePollRunnable,
        )
        mainHandler.removeCallbacks(
            matchmakingPollRunnable,
        )
        mainHandler.removeCallbacks(
            realtimeReconnectRunnable,
        )
        mainHandler.removeCallbacks(
            realtimeRefreshRunnable,
        )
        realtimeClient.disconnect()
        super.onStop()
    }

    internal fun canRenderUi(): Boolean =
        !isFinishing &&
            !isDestroyed

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(
            null,
        )
        realtimeClient.shutdown()
        executor.shutdownNow()
        super.onDestroy()
    }

    internal fun updateConnectionLabel() {
        if (
            !::connectionText
                .isInitialized
        ) {
            return
        }

        when {
            !isOnline -> {
                connectionText.text =
                    "● OFFLINE"
                connectionText.setTextColor(
                    LudoProofTheme.GOLD,
                )
            }

            realtimeConnected -> {
                connectionText.text =
                    "● LIVE"
                connectionText.setTextColor(
                    0xFF65EF55.toInt(),
                )
            }

            else -> {
                connectionText.text =
                    "● ONLINE"
                connectionText.setTextColor(
                    0xFF6EE7FF.toInt(),
                )
            }
        }
    }

    private companion object {
        const val STATE_POLL_MS =
            3_000L
        const val MATCHMAKING_POLL_MS =
            1_500L
        const val REALTIME_RECONNECT_MS =
            10_000L
    }
}
