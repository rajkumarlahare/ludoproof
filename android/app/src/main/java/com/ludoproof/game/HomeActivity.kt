package com.ludoproof.game

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class HomeActivity : Activity() {
    private lateinit var connectivity: TextView
    private lateinit var monitor: ConnectivityMonitor
    private lateinit var continueHost: LinearLayout
    private var openingGame = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LudoProofTheme.configureWindow(this)
        val footer = ArcadeUi.row(this).apply { setPadding(0, dp(6), 0, dp(6)) }
        listOf(
            Triple("★", "Rules", { ArcadeDialogs.showInfo(this, "HOW TO PLAY", RULES) }),
            Triple("↗", "Share", { shareApp() }),
            Triple("⚙", "Settings", { ArcadeDialogs.showSettings(this) }),
        ).forEach { (symbol, label, action) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(ArcadeUi.icon(context, symbol, label, action), LinearLayout.LayoutParams(dp(48), dp(48)))
                addView(ArcadeUi.text(context, label, 11f, true), ArcadeUi.params(context, 4))
            }
            footer.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val page = ArcadeUi.page(this, footer)
        val header = ArcadeUi.row(this)
        header.addView(android.widget.ImageView(this).apply {
            setImageDrawable(GameGlyphDrawable("dice", LudoProofTheme.WHITE, dp(42)))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(dp(5),dp(5),dp(5),dp(5))
            background = LudoProofTheme.brandBadgeDrawable(context)
        }, LinearLayout.LayoutParams(dp(48),dp(48)))
        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10),0,0,0)
            addView(ArcadeUi.text(context, "LudoProof", 18f).apply { setTypeface(null, android.graphics.Typeface.BOLD) })
            connectivity = ArcadeUi.text(context, "Checking connection…", 11f)
            addView(connectivity)
        }
        header.addView(identity, LinearLayout.LayoutParams(0,-2,1f))
        header.addView(ArcadeUi.icon(this,"✓","Fair play") { ArcadeDialogs.showInfo(this,"FAIR PLAY",TRUST) },
            LinearLayout.LayoutParams(dp(44),dp(44)))
        ArcadeUi.add(page,header,0)
        page.addView(LudoWordmarkView(this),LinearLayout.LayoutParams(-1,dp(100)).apply { topMargin=dp(24) })
        val modes = ArcadeUi.row(this)
        modes.addView(modeCard(R.drawable.online_mode_art_v2,"Online",true) {
            openGame(MainActivity::class.java)
        },LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(6) })
        modes.addView(modeCard(R.drawable.local_mode_art_v2,"Local",false) {
            openGame(OfflineGameActivity::class.java)
        },LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(6) })
        ArcadeUi.add(page,modes,64)
        ArcadeUi.add(page,ArcadeUi.text(this,"Play with friends        •        Pass & play",12f,true),9)
        val fairPlay = ArcadeUi.button(this,"✓  Every roll has a proof") {
            ArcadeDialogs.showInfo(this,"FAIR PLAY",TRUST)
        }.apply { LudoProofTheme.positive(this); textSize=16f }
        ArcadeUi.add(page,fairPlay,24)
        continueHost=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        ArcadeUi.add(page,continueHost,0)
        monitor=ConnectivityMonitor(this) { online ->
            runOnUiThread {
                connectivity.text=if(online) "● Ready to play online" else "● Local play available"
                connectivity.setTextColor(if(online) 0xFF9FF0B4.toInt() else LudoProofTheme.GOLD)
            }
        }
    }

    private fun modeCard(asset: Int, label: String, online: Boolean, action: () -> Unit): android.widget.FrameLayout =
        object : android.widget.FrameLayout(this) {
            override fun onMeasure(widthMeasureSpec: Int,heightMeasureSpec: Int) {
                val width=View.MeasureSpec.getSize(widthMeasureSpec)
                super.onMeasure(widthMeasureSpec,View.MeasureSpec.makeMeasureSpec((width*1.24f).toInt(),View.MeasureSpec.EXACTLY))
            }
        }.apply {
            background=LudoProofTheme.panelDrawable(context)
            clipToOutline=true
            elevation=dp(6).toFloat()
            setPadding(dp(3),dp(3),dp(3),dp(3))
            val options=android.graphics.BitmapFactory.Options().apply { inSampleSize=2 }
            val picture=android.widget.ImageView(context).apply {
                setImageBitmap(android.graphics.BitmapFactory.decodeResource(resources,asset,options))
                scaleType=android.widget.ImageView.ScaleType.CENTER_CROP
                importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setOnClickListener { action() }
            }
            addView(picture,android.widget.FrameLayout.LayoutParams(-1,-1).apply { bottomMargin=dp(38) })
            addView(ArcadeUi.button(context,label,primary=!online,action=action).apply {
                if(online) LudoProofTheme.positive(this)
                background = GameButtonDrawable(if (online) intArrayOf(0xFFAFE81E.toInt(), 0xFF52C50C.toInt(), 0xFF168D06.toInt()) else intArrayOf(0xFFFFDA52.toInt(), 0xFFFFB600.toInt(), 0xFFFF8A00.toInt()), 12f)
                textSize=21f
                contentDescription=if(online) "Play online with friends" else "Play local with two to four players"
            },android.widget.FrameLayout.LayoutParams(-1,dp(50),Gravity.BOTTOM))
        }
    override fun onStart() {
        super.onStart()
        openingGame = false
        continueHost.removeAllViews()
        if (SecureSessionStore(this).load() != null) {
            ArcadeUi.add(continueHost, ArcadeUi.button(this, "Continue online match  ›") {
                openGame(MainActivity::class.java)
            }, 14)
        }
        if (OfflineGameEngine(this).hasSavedGame()) {
            ArcadeUi.add(continueHost, ArcadeUi.button(this, "Continue local game  ›") {
                openGame(OfflineGameActivity::class.java)
            }, 10)
        }
        monitor.start()
    }

    override fun onStop() {
        monitor.stop()
        super.onStop()
    }

    private fun openGame(target: Class<out Activity>) {
        if (openingGame) return
        openingGame = true
        startActivity(Intent(this, target))
    }

    private fun shareApp() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "LudoProof — play Ludo with verifiable dice. https://github.com/rajkumarlahare/ludoproof")
        }, "Share LudoProof"))
    }

    private fun dp(value: Int) = LudoProofTheme.dp(this, value)

    companion object {
        const val RULES = "2–4 players · four tokens each.\n\nRoll a six to leave the yard. Tap a highlighted token to move. Reach home with an exact roll.\n\nA six or a capture gives another turn. Three consecutive sixes forfeit the turn. Tokens on safe cells cannot be captured.\n\nBring all four tokens home to win. Blockades are not part of this ruleset."
        const val TRUST = "Online rolls are verified by the game server using EntroNex v4 commitments and attestations. Retries reuse the same roll.\n\nLocal games use the same derivation on this device. Both seeds are generated locally; there is no remote attestation.\n\nOpen History or Engine map during a game to inspect the evidence. EntroNex v4 is pending independent review."
    }
}
