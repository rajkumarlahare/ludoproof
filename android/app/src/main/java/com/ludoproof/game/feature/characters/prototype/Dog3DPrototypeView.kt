package com.ludoproof.game.feature.characters.prototype

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class Dog3DPrototypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val dogRenderer = Dog3DRenderer()

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(dogRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isClickable = true
        isFocusable = true
        contentDescription = "Code-generated 3D dog pawn prototype"
        setOnClickListener {
            playHop()
        }
    }

    fun playIdle() {
        dogRenderer.play(Dog3DMotion.IDLE)
    }

    fun playHop() {
        dogRenderer.play(Dog3DMotion.HOP)
    }

    fun playHomeCelebration() {
        dogRenderer.play(Dog3DMotion.HOME)
    }
}
