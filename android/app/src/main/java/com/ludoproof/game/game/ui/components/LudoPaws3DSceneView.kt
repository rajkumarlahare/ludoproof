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
import android.view.View
import com.ludoproof.game.feature.characters.domain.model.AnimationCue
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

internal data class LudoPaws3DPawnKey(
    val playerId: String,
    val tokenIndex: Int,
)

internal data class LudoPaws3DCaptureReturnState(
    val motion: LudoPawsPawnMotion,
    val startedAtMillis: Long,
    val durationMillis: Long,
)

internal data class LudoPaws3DSceneState(
    val snapshot: MatchSnapshot? = null,
    val localPlayerId: String? = null,
    val perspectiveColor: String? = null,
    val characterIdsBySeat: List<String> = emptyList(),
    val forwardMotion: LudoPawsPawnMotion? = null,
    val forwardStartedAtMillis: Long = 0L,
    val forwardDurationMillis: Long = 0L,
    val captureReturns: Map<LudoPaws3DPawnKey, LudoPaws3DCaptureReturnState> = emptyMap(),
    val activeReactions: Map<LudoPaws3DPawnKey, LudoPaws3DActiveReaction> = emptyMap(),
)

/**
 * One production OpenGL surface for every visible Ludo pawn.
 *
 * TextureView keeps a single GL context and participates in Android compositing.
 * The authoritative board remains underneath for geometry/touch/game state.
 * This layer is presentation-only and never mutates gameplay or proof state.
 */
internal class LudoPaws3DSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextureView(context, attrs), TextureView.SurfaceTextureListener {
    private val settingsStore = GameSettingsStore(context)

    @Volatile
    private var sceneState = LudoPaws3DSceneState()

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
        val now = SystemClock.uptimeMillis()
        val previousState = sceneState
        val previousSnapshot = previousState.snapshot
        val sameMatch =
            previousSnapshot != null &&
                state != null &&
                previousSnapshot.matchId == state.matchId
        val settings = settingsStore.snapshot()
        val motions =
            LudoPawsPawnAnimationPolicy.plans(
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
                    LudoPaws3DRenderCadencePolicy.HOME_CELEBRATION_TAIL_MILLIS
            ) {
                previousState.forwardMotion
            } else {
                null
            }
        val forward = newForward ?: retainedForward
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
                val travelDuration =
                    max(
                        settings.gameSpeed.moveStepMs,
                        newForward.visualSteps.toLong() * settings.gameSpeed.moveStepMs,
                    )
                travelDuration +
                    LudoPaws3DRenderCadencePolicy
                        .FORWARD_LANDING_SETTLE_MILLIS
            } else if (forward != null) {
                previousState.forwardDurationMillis
            } else {
                0L
            }

        val retainedCaptureReturns =
            if (sameMatch) {
                previousState.captureReturns.filterValues { capture ->
                    now < capture.startedAtMillis + capture.durationMillis
                }
            } else {
                emptyMap()
            }
        val newCaptureReturns =
            motions
                    .asSequence()
                    .filter {
                        it.kind == LudoPawsPawnMotionKind.CAPTURE_RETURN
                    }
                    .associate { motion ->
                        // The captured animal must remain on the occupied square
                        // until the attacker has visibly completed its full route.
                        // Its impact/pop/yard return begins only after contact.
                        val contactAtMillis =
                            now +
                                if (newForward != null) {
                                    LudoPaws3DRenderCadencePolicy
                                        .captureContactDelayMillis(
                                            forwardDurationMillis,
                                        )
                                } else {
                                    0L
                                }
                        LudoPaws3DPawnKey(
                            playerId = motion.playerId,
                            tokenIndex = motion.tokenIndex,
                        ) to
                            LudoPaws3DCaptureReturnState(
                                motion = motion,
                                startedAtMillis = contactAtMillis,
                                durationMillis =
                                    LudoPawsGameplayPacingPolicy
                                        .captureReturnDurationMillis(
                                            settings.gameSpeed,
                                        ),
                            )
                    }
        val retainedReactions =
            if (sameMatch) {
                previousState.activeReactions.filterValues { reaction ->
                    now < reaction.startedAtMillis + reaction.durationMillis
                }
            } else {
                emptyMap()
            }

        sceneState =
            LudoPaws3DSceneState(
                snapshot = state,
                localPlayerId = playerId,
                perspectiveColor =
                    perspectiveColor?.takeIf {
                        it in OfflinePlayerLayout.COLORS
                    },
                characterIdsBySeat = characterIdsBySeat.take(4),
                forwardMotion = forward,
                forwardStartedAtMillis = forwardStartedAtMillis,
                forwardDurationMillis = forwardDurationMillis,
                captureReturns = retainedCaptureReturns + newCaptureReturns,
                activeReactions = retainedReactions,
            )
    }

    /**
     * Adds short, pawn-local body language after the snapshot has been bound.
     * Higher-priority reactions replace lower-priority animation on the same
     * pawn; gameplay movement remains independent and still has precedence for
     * board position.
     */
    fun playReactions(
        reactions: List<LudoPawsReaction>,
    ) {
        if (reactions.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val current = sceneState
        val snapshot = current.snapshot ?: return

        val next =
            current.activeReactions
                .filterValues {
                    now < it.startedAtMillis + it.durationMillis
                }
                .toMutableMap()

        reactions
            .asSequence()
            .sortedByDescending(LudoPawsReaction::priority)
            .forEach { reaction ->
                val player =
                    snapshot.players.firstOrNull {
                        it.playerId == reaction.playerId
                    } ?: return@forEach
                val species =
                    LudoPaws3DCharacterPolicy.speciesForSeat(
                        characterIdsBySeat = current.characterIdsBySeat,
                        seat = player.seat,
                        fallbackColor = player.color,
                    ) ?: return@forEach
                val targets =
                    targetTokens(
                        player = player,
                        reaction = reaction,
                    )
                val duration =
                    LudoPaws3DReactionMotion.durationMillis(
                        species = species,
                        cue = reaction.animationCue,
                    )
                targets.forEach { tokenIndex ->
                    val key =
                        LudoPaws3DPawnKey(
                            playerId = player.playerId,
                            tokenIndex = tokenIndex,
                        )
                    val previous = next[key]
                    if (
                        previous == null ||
                        reaction.priority >= previous.priority
                    ) {
                        val captureStart =
                            if (reaction.animationCue == AnimationCue.CAPTURED) {
                                current.captureReturns[key]?.startedAtMillis
                            } else {
                                null
                            }
                        val attackerContact =
                            if (
                                reaction.animationCue == AnimationCue.CAPTURE &&
                                current.forwardMotion?.playerId == key.playerId &&
                                current.forwardMotion?.tokenIndex == key.tokenIndex
                            ) {
                                current.forwardStartedAtMillis +
                                    LudoPaws3DRenderCadencePolicy
                                        .captureContactDelayMillis(
                                            current.forwardDurationMillis,
                                        )
                            } else {
                                null
                            }
                        next[key] =
                            LudoPaws3DActiveReaction(
                                cue = reaction.animationCue,
                                startedAtMillis =
                                    captureStart
                                        ?: attackerContact
                                        ?: now,
                                durationMillis = duration,
                                priority = reaction.priority,
                            )
                    }
                }
            }

        sceneState = current.copy(activeReactions = next)
    }

    private fun targetTokens(
        player: PlayerSnapshot,
        reaction: LudoPawsReaction,
    ): List<Int> {
        reaction.tokenIndex
            ?.takeIf { it in player.tokens.indices }
            ?.let { return listOf(it) }

        if (
            reaction.animationCue == AnimationCue.VICTORY ||
            reaction.animationCue == AnimationCue.DEFEAT
        ) {
            return player.tokens.indices.toList()
        }

        val active =
            player.tokens.indices.firstOrNull { index ->
                player.tokens[index] in 0..56
            }
        val yard =
            player.tokens.indices.firstOrNull { index ->
                player.tokens[index] == -1
            }
        return listOf(active ?: yard ?: 0)
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        startRenderLoop(surface, width, height)
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        renderLoop?.resize(width, height)
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

    override fun onVisibilityChanged(
        changedView: View,
        visibility: Int,
    ) {
        super.onVisibilityChanged(changedView, visibility)
        updateRenderVisibility()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateRenderVisibility()
    }

    override fun onDetachedFromWindow() {
        stopRenderLoop()
        sceneState = LudoPaws3DSceneState()
        super.onDetachedFromWindow()
    }

    private fun updateRenderVisibility() {
        renderLoop?.setPresentationVisible(
            isShown &&
                visibility == VISIBLE &&
                windowVisibility == VISIBLE,
        )
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
            initialPresentationVisible =
                isShown &&
                    visibility == VISIBLE &&
                    windowVisibility == VISIBLE,
        ).also {
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

    private fun setOperational(value: Boolean) {
        if (operational == value) return
        operational = value
        post {
            onOperationalChanged?.invoke(value)
        }
    }

    private inner class RenderLoop(
        private val surfaceTexture: SurfaceTexture,
        initialWidth: Int,
        initialHeight: Int,
        initialPresentationVisible: Boolean,
    ) : Thread("LudoPaws3D") {
        private val visibilityLock = ReentrantLock()
        private val visibilityChanged = visibilityLock.newCondition()

        @Volatile
        private var running = true

        @Volatile
        private var targetWidth = initialWidth

        @Volatile
        private var targetHeight = initialHeight

        @Volatile
        private var presentationVisible = initialPresentationVisible

        fun resize(
            width: Int,
            height: Int,
        ) {
            targetWidth = width
            targetHeight = height
        }

        fun setPresentationVisible(visible: Boolean) {
            if (presentationVisible == visible) return
            presentationVisible = visible
            if (visible) {
                visibilityLock.withLock {
                    visibilityChanged.signalAll()
                }
            }
        }

        fun shutdown() {
            running = false
            visibilityLock.withLock {
                visibilityChanged.signalAll()
            }
            interrupt()
        }

        private fun awaitPresentationVisible(): Boolean {
            visibilityLock.withLock {
                while (running && !presentationVisible) {
                    visibilityChanged.await()
                }
                return running
            }
        }

        override fun run() {
            var egl: EglWindow? = null
            try {
                egl = EglWindow(surfaceTexture)
                val renderer = LudoPaws3DSceneRenderer()
                renderer.onSurfaceCreated()
                var renderedWidth = -1
                var renderedHeight = -1
                setOperational(true)

                while (running) {
                    if (!awaitPresentationVisible()) break

                    val frameStartedAtMillis = SystemClock.uptimeMillis()
                    val width = targetWidth.coerceAtLeast(1)
                    val height = targetHeight.coerceAtLeast(1)
                    if (
                        width != renderedWidth ||
                        height != renderedHeight
                    ) {
                        renderer.onSurfaceChanged(width, height)
                        renderedWidth = width
                        renderedHeight = height
                    }

                    val frameState = sceneState
                    LudoPaws3DCharacterPolicy.bindRenderAssignments(
                        snapshot = frameState.snapshot,
                        characterIdsBySeat = frameState.characterIdsBySeat,
                    )
                    renderer.drawFrame(
                        state = frameState,
                        nowMillis = frameStartedAtMillis,
                    )
                    if (!egl.swapBuffers()) error("EGL swapBuffers failed")

                    val frameDelayMillis =
                        LudoPaws3DRenderCadencePolicy.frameDelayMillis(
                            state = frameState,
                            nowMillis = frameStartedAtMillis,
                        )
                    val frameWorkMillis =
                        (SystemClock.uptimeMillis() - frameStartedAtMillis)
                            .coerceAtLeast(0L)
                    val sleepMillis = frameDelayMillis - frameWorkMillis
                    if (sleepMillis > 0L) {
                        SystemClock.sleep(sleepMillis)
                    }
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
                LudoPaws3DCharacterPolicy.clearRenderAssignments()
                runCatching { egl?.release() }
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
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "No EGL display" }
            val versions = IntArray(2)
            check(
                EGL14.eglInitialize(
                    display,
                    versions,
                    0,
                    versions,
                    1,
                ),
            ) { "EGL initialize failed" }

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
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
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
                ) && configCount[0] > 0,
            ) { "No ES3 RGBA EGL config" }
            val config = checkNotNull(configs[0])

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
            ) { "EGL makeCurrent failed" }
        }

        fun swapBuffers(): Boolean =
            EGL14.eglSwapBuffers(display, surface)

        fun release() {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
    }

    private companion object {
        const val TAG = "LudoPaws3D"
        const val RENDER_THREAD_JOIN_MILLIS = 250L
    }
}
