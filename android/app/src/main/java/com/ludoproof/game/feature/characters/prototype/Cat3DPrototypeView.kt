package com.ludoproof.game.feature.characters.prototype

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class Cat3DPrototypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val catRenderer = Cat3DRenderer()

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(catRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isClickable = true
        isFocusable = true
        contentDescription = "Code-generated 3D cat pawn prototype"
        setOnClickListener {
            playHop()
        }
    }

    fun playIdle() {
        catRenderer.play(Cat3DMotion.IDLE)
    }

    fun playHop() {
        catRenderer.play(Cat3DMotion.HOP)
    }

    fun playHomeCelebration() {
        catRenderer.play(Cat3DMotion.HOME)
    }
}
