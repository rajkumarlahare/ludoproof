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
    internal val engine by lazy { OfflineGameEngine(this) }

    internal var selectedPlayers = 2
    internal var selectedColor = "BLUE"
    internal lateinit var playerButtons: Map<Int, Button>
    internal lateinit var colorButtons: Map<String, Button>

    internal var boardView: LudoBoardView? = null
    internal var diceView: DiceView? = null
    internal var turnText: TextView? = null
    internal var infoText: TextView? = null
    internal var statusText: TextView? = null
    internal var rollButton: Button? = null
    internal lateinit var resultPanel: FrameLayout
    internal lateinit var resultTitleText: TextView
    internal lateinit var resultSubtitleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        if (engine.hasSavedGame()) showGame(engine.snapshot()) else showSetup()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
