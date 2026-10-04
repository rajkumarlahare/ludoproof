package com.ludoproof.game.feature.characters.prototype

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class Goat3DPrototypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val goatRenderer = Goat3DRenderer()

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(goatRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isClickable = true
        isFocusable = true
        contentDescription = "Code-generated 3D goat pawn prototype"
        setOnClickListener {
            playHop()
        }
    }

    fun playIdle() {
        goatRenderer.play(Goat3DMotion.IDLE)
    }

    fun playHop() {
        goatRenderer.play(Goat3DMotion.HOP)
    }

    fun playHomeCelebration() {
        goatRenderer.play(Goat3DMotion.HOME)
    }
}
