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
import com.ludoproof.game.ui.offline.common.*
import com.ludoproof.game.ui.offline.setup.*
import com.ludoproof.game.ui.offline.gameplay.*
import com.ludoproof.game.feature.offline.*

class OfflineGameActivity : Activity() {
    internal val handler = Handler(Looper.getMainLooper())

    internal val gameMode:
        GameMode by lazy {
        GameMode
            .fromWireValue(
                intent.getStringExtra(
                    EXTRA_GAME_MODE,
                )
                    ?: intent.getStringExtra(
                        EXTRA_PLAY_MODE,
                    ),
            )
            ?.takeIf {
                it.isLocal
            }
            ?: GameMode.PASS_AND_PLAY
    }

    internal val isComputerMode: Boolean
        get() =
            gameMode ==
                GameMode.COMPUTER

    internal val session by lazy {
        LocalMatchSession(
            context =
                this,
            mode =
                gameMode,
        )
    }

    internal val engine:
        OfflineGameEngine
        get() =
            session.engine

    internal val setupStateHolder = OfflineSetupStateHolder()

    internal var selectedPlayers: Int
        get() = setupStateHolder.value.selectedPlayers
        set(value) { setupStateHolder.update { it.copy(selectedPlayers = value) } }

    internal var selectedColor: String
        get() = setupStateHolder.value.selectedColor
        set(value) { setupStateHolder.update { it.copy(selectedColor = value) } }
    internal var playerButtons: Map<Int, Button> = emptyMap()
    internal var colorButtons: Map<String, Button> = emptyMap()

    internal var boardView: LudoBoardView? = null
    internal var diceView: DiceView? = null
    internal var diceHost: FrameLayout? = null
    internal var topPlayerRail: LinearLayout? = null
    internal var bottomPlayerRail: LinearLayout? = null
    internal var turnText: TextView? = null
    internal var infoText: TextView? = null
    internal var statusText: TextView? = null
    internal var computerActionRevision: Int? = null
    internal lateinit var resultPanel: FrameLayout
    internal lateinit var resultTitleText: TextView
    internal lateinit var resultSubtitleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        if (
            session.hasSavedGame()
        ) {
            showGame(
                session.snapshot(),
            )
        } else {
            showSetup()
        }
    }

    internal fun prepareOfflineUiTransition() {
        handler.removeCallbacksAndMessages(null)
        computerActionRevision =
            null
        diceView
            ?.stopRolling()
        boardView =
            null
        diceView =
            null
        diceHost =
            null
        topPlayerRail =
            null
        bottomPlayerRail =
            null
        turnText =
            null
        infoText =
            null
        statusText =
            null
    }

    override fun onDestroy() {
        prepareOfflineUiTransition()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_GAME_MODE =
            "ludoproof_game_mode_v1"

        // Legacy extras remain readable so installed builds and old intents
        // can continue an existing local or computer session after upgrade.
        const val EXTRA_PLAY_MODE =
            "ludoproof_play_mode"
        const val PLAY_MODE_LOCAL =
            "LOCAL"
        const val PLAY_MODE_COMPUTER =
            "COMPUTER"
    }
}
