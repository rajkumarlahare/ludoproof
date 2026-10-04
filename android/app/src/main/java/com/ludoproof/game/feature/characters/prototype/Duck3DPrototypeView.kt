package com.ludoproof.game.feature.characters.prototype

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class Duck3DPrototypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val duckRenderer = Duck3DRenderer()

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(duckRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isClickable = true
        isFocusable = true
        contentDescription = "Code-generated 3D duck pawn prototype"
        setOnClickListener {
            playHop()
        }
    }

    fun playIdle() {
        duckRenderer.play(Duck3DMotion.IDLE)
    }

    fun playHop() {
        duckRenderer.play(Duck3DMotion.HOP)
    }

    fun playHomeCelebration() {
        duckRenderer.play(Duck3DMotion.HOME)
    }
}
