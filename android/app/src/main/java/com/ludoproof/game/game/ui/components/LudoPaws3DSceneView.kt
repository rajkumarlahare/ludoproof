package com.ludoproof.game

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import kotlin.math.max

internal data class LudoPaws3DPawnKey(
    val playerId: String,
    val tokenIndex: Int,
)

internal data class LudoPaws3DSceneState(
    val snapshot: MatchSnapshot? = null,
    val localPlayerId: String? = null,
    val perspectiveColor: String? = null,
    val characterIdsBySeat: List<String> = emptyList(),
    val forwardMotion: LudoPawsPawnMotion? = null,
    val forwardStartedAtMillis: Long = 0L,
    val forwardDurationMillis: Long = 0L,
    val captureHiddenUntilMillis: Map<LudoPaws3DPawnKey, Long> = emptyMap(),
    val reducedMotion: Boolean = false,
)

/**
 * One production OpenGL surface for every visible Ludo pawn.
 *
 * TextureView is deliberate here: unlike one GLSurfaceView per pawn, it keeps a
 * single GL context and still participates in normal Android view compositing.
 * The authoritative LudoBoardView remains underneath for board geometry and
 * touch hit-testing; this layer is visual only and never mutates game state.
 *
 * If ES 3.0/EGL initialization fails, [onOperationalChanged] reports false and
 * the authoritative classic board remains the safe pawn fallback. No retired
 * 2D animal drawable renderer is restored.
 */
internal class LudoPaws3DSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextureView(context, attrs), TextureView.SurfaceTextureListener {
    private val settingsStore =
        GameSettingsStore(context)

    @Volatile
    private var sceneState =
        LudoPaws3DSceneState()

    @Volatile
    private var renderLoop: RenderLoop? = null

    @Volatile
    private var operational = false

    var onOperationalChanged: ((Boolean) -> Unit)? = null

    init {
        isOpaque = false
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        surfaceTextureListener = this
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String?,
        characterIdsBySeat: List<String> = emptyList(),
    ) {
        val now =
            SystemClock.uptimeMillis()
        val previousState =
            sceneState
        val previousSnapshot =
            previousState.snapshot
        val sameMatch =
            previousSnapshot != null &&
                state != null &&
                previousSnapshot.matchId == state.matchId
        val settings =
            settingsStore.snapshot()
        val motions =
            LudoPawsPawnAnimationPolicy
                .plans(
                    previous = previousSnapshot,
                    current = state,
                )
        val newForward =
            motions.firstOrNull {
                it.kind == LudoPawsPawnMotionKind.FORWARD
            }

        val retainedForward =
            if (
                newForward == null &&
                sameMatch &&
                previousState.forwardMotion != null &&
                now <
                previousState.forwardStartedAtMillis +
                    previousState.forwardDurationMillis +
                    HOME_CELEBRATION_TAIL_MILLIS
            ) {
                previousState.forwardMotion
            } else {
                null
            }
        val forward =
            newForward ?: retainedForward
        val forwardStartedAtMillis =
            if (newForward != null) {
                now
            } else if (forward != null) {
                previousState.forwardStartedAtMillis
            } else {
                0L
            }
        val forwardDurationMillis =
            if (newForward != null) {
                max(
                    settings.gameSpeed.moveStepMs,
                    newForward.visualSteps.toLong() *
                        settings.gameSpeed.moveStepMs,
                )
            } else if (forward != null) {
                previousState.forwardDurationMillis
            } else {
                0L
            }

        val retainedCaptureHides =
            if (sameMatch) {
                previousState.captureHiddenUntilMillis
                    .filterValues { hideUntil ->
                        hideUntil > now
                    }
            } else {
                emptyMap()
            }
        val newCaptureHides =
            motions
                .asSequence()
                .filter {
                    it.kind == LudoPawsPawnMotionKind.CAPTURE_RETURN
                }
                .associate {
                    motion ->
                    val duration =
                        (
                            motion.visualSteps.toLong() *
                                settings.gameSpeed.moveStepMs
                            )
                            .coerceIn(
                                MIN_CAPTURE_DURATION_MILLIS,
                                MAX_CAPTURE_DURATION_MILLIS,
                            )
                    LudoPaws3DPawnKey(
                        playerId = motion.playerId,
                        tokenIndex = motion.tokenIndex,
                    ) to
                        (now + duration)
                }

        sceneState =
            LudoPaws3DSceneState(
                snapshot = state,
                localPlayerId = playerId,
                perspectiveColor =
                    perspectiveColor
                        ?.takeIf {
                            it in OfflinePlayerLayout.COLORS
                        },
                characterIdsBySeat = characterIdsBySeat.take(4),
                forwardMotion = forward,
                forwardStartedAtMillis = forwardStartedAtMillis,
                forwardDurationMillis = forwardDurationMillis,
                captureHiddenUntilMillis =
                    retainedCaptureHides + newCaptureHides,
                reducedMotion = settings.reducedMotionEnabled,
            )
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        startRenderLoop(
            surface = surface,
            width = width,
            height = height,
        )
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        renderLoop
            ?.resize(
                width = width,
                height = height,
            )
    }

    override fun onSurfaceTextureDestroyed(
        surface: SurfaceTexture,
    ): Boolean {
        stopRenderLoop()
        return true
    }

    override fun onSurfaceTextureUpdated(
        surface: SurfaceTexture,
    ) = Unit

    override fun onDetachedFromWindow() {
        stopRenderLoop()
        sceneState =
            LudoPaws3DSceneState()
        super.onDetachedFromWindow()
    }

    private fun startRenderLoop(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        stopRenderLoop()
        RenderLoop(
            surfaceTexture = surface,
            initialWidth = width,
            initialHeight = height,
        )
            .also {
                renderLoop = it
                it.start()
            }
    }

    private fun stopRenderLoop() {
        val loop =
            renderLoop
                ?: run {
                    setOperational(false)
                    return
                }
        renderLoop = null
        loop.shutdown()
        runCatching {
            loop.join(RENDER_THREAD_JOIN_MILLIS)
        }
        setOperational(false)
    }

    private fun setOperational(
        value: Boolean,
    ) {
        if (operational == value) {
            return
        }
        operational = value
        post {
            onOperationalChanged?.invoke(value)
        }
    }

    private inner class RenderLoop(
        private val surfaceTexture: SurfaceTexture,
        initialWidth: Int,
        initialHeight: Int,
    ) : Thread("LudoPaws3D") {
        @Volatile
        private var running = true

        @Volatile
        private var targetWidth = initialWidth

        @Volatile
        private var targetHeight = initialHeight

        fun resize(
            width: Int,
            height: Int,
        ) {
            targetWidth = width
            targetHeight = height
        }

        fun shutdown() {
            running = false
            interrupt()
        }

        override fun run() {
            var egl: EglWindow? = null
            try {
                egl =
                    EglWindow(
                        surfaceTexture,
                    )
                val renderer =
                    LudoPaws3DSceneRenderer()
                renderer.onSurfaceCreated()
                var renderedWidth = -1
                var renderedHeight = -1
                setOperational(true)

                while (running) {
                    val width =
                        targetWidth.coerceAtLeast(1)
                    val height =
                        targetHeight.coerceAtLeast(1)
                    if (
                        width != renderedWidth ||
                        height != renderedHeight
                    ) {
                        renderer.onSurfaceChanged(
                            width = width,
                            height = height,
                        )
                        renderedWidth = width
                        renderedHeight = height
                    }

                    renderer.drawFrame(
                        state = sceneState,
                        nowMillis = SystemClock.uptimeMillis(),
                    )
                    if (!egl.swapBuffers()) {
                        error("EGL swapBuffers failed")
                    }
                    SystemClock.sleep(FRAME_DELAY_MILLIS)
                }
            } catch (interrupted: InterruptedException) {
                // Normal shutdown path.
            } catch (error: Throwable) {
                Log.w(
                    TAG,
                    "3D pawn runtime unavailable; keeping authoritative board fallback",
                    error,
                )
            } finally {
                runCatching {
                    egl?.release()
                }
                setOperational(false)
            }
        }
    }

    private class EglWindow(
        surfaceTexture: SurfaceTexture,
    ) {
        private val display: EGLDisplay
        private val context: EGLContext
        private val surface: EGLSurface

        init {
            display =
                EGL14.eglGetDisplay(
                    EGL14.EGL_DEFAULT_DISPLAY,
                )
            check(display != EGL14.EGL_NO_DISPLAY) {
                "No EGL display"
            }
            val versions =
                IntArray(2)
            check(
                EGL14.eglInitialize(
                    display,
                    versions,
                    0,
                    versions,
                    1,
                ),
            ) {
                "EGL initialize failed"
            }

            val configAttributes =
                intArrayOf(
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_RED_SIZE,
                    8,
                    EGL14.EGL_GREEN_SIZE,
                    8,
                    EGL14.EGL_BLUE_SIZE,
                    8,
                    EGL14.EGL_ALPHA_SIZE,
                    8,
                    EGL14.EGL_DEPTH_SIZE,
                    16,
                    EGL14.EGL_NONE,
                )
            val configs =
                arrayOfNulls<EGLConfig>(1)
            val configCount =
                IntArray(1)
            check(
                EGL14.eglChooseConfig(
                    display,
                    configAttributes,
                    0,
                    configs,
                    0,
                    configs.size,
                    configCount,
                    0,
                ) &&
                    configCount[0] > 0,
            ) {
                "No ES3 RGBA EGL config"
            }
            val config =
                checkNotNull(configs[0])

            context =
                EGL14.eglCreateContext(
                    display,
                    config,
                    EGL14.EGL_NO_CONTEXT,
                    intArrayOf(
                        EGL14.EGL_CONTEXT_CLIENT_VERSION,
                        3,
                        EGL14.EGL_NONE,
                    ),
                    0,
                )
            check(context != EGL14.EGL_NO_CONTEXT) {
                "ES3 EGL context creation failed"
            }

            surface =
                EGL14.eglCreateWindowSurface(
                    display,
                    config,
                    surfaceTexture,
                    intArrayOf(EGL14.EGL_NONE),
                    0,
                )
            check(surface != EGL14.EGL_NO_SURFACE) {
                "EGL window surface creation failed"
            }
            check(
                EGL14.eglMakeCurrent(
                    display,
                    surface,
                    surface,
                    context,
                ),
            ) {
                "EGL makeCurrent failed"
            }
        }

        fun swapBuffers(): Boolean =
            EGL14.eglSwapBuffers(
                display,
                surface,
            )

        fun release() {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroySurface(
                display,
                surface,
            )
            EGL14.eglDestroyContext(
                display,
                context,
            )
            EGL14.eglTerminate(display)
        }
    }

    private companion object {
        const val TAG = "LudoPaws3D"
        const val FRAME_DELAY_MILLIS = 16L
        const val RENDER_THREAD_JOIN_MILLIS = 250L
        const val HOME_CELEBRATION_TAIL_MILLIS = 1_400L
        const val MIN_CAPTURE_DURATION_MILLIS = 520L
        const val MAX_CAPTURE_DURATION_MILLIS = 1_200L
    }
}
