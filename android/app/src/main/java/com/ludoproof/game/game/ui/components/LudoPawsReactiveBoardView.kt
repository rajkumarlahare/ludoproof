package com.ludoproof.game

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.ludoproof.game.feature.characters.data.audio.LudoPawsVoicePlayer
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsIdleReactionPolicy
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared Ludo Paws board shell.
 *
 * Authoritative gameplay remains inside the existing board/engine stack. Phase
 * 10 presentation layers add capture-return pawn motion, board particles/token
 * FX, timed idle personality, and larger character reactions without mutating
 * game state.
 *
 * The production 3D pawn runtime is composited above the authoritative board.
 * It is presentation-only and automatically falls back to the previous 2D pawn
 * overlay if ES3/EGL initialization is unavailable on a device.
 */
class LudoPawsReactiveBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val board =
        LudoPawsBoardView(context)
    private val pawn3DScene =
        LudoPaws3DSceneView(context)
    private val pawn3DLegalHalo =
        LudoPaws3DLegalHaloView(context)
    private val captureReturnOverlay =
        LudoPawsCaptureReturnOverlayView(context)
    private val gameFxOverlay =
        LudoPawsGameFxOverlayView(context)
    private val characterReactionOverlay =
        LudoPawsCharacterReactionOverlayView(context)
    private val voicePlayer =
        LudoPawsVoicePlayer(context)
    private val settingsStore =
        GameSettingsStore(context)

    private var previousSnapshot: MatchSnapshot? = null
    private var currentSnapshot: MatchSnapshot? = null
    private var currentCharacterIdsBySeat: List<String> = emptyList()
    private var meaningfulStateKey: String? = null
    private var lastMeaningfulChangeAtMillis: Long = monotonicMillis()
    private var lastIdleReactionKey: String? = null

    private val idleReactionRunnable =
        Runnable {
            playIdleReactionIfEligible()
        }

    var onTokenSelected: ((Int) -> Unit)?
        get() = board.onTokenSelected
        set(value) {
            board.onTokenSelected = value
        }

    init {
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO

        addView(
            board,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            pawn3DScene,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            pawn3DLegalHalo,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            captureReturnOverlay,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            gameFxOverlay,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            characterReactionOverlay,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )

        pawn3DLegalHalo.visibility = View.GONE
        pawn3DScene.onOperationalChanged =
            { available ->
                setLegacyPawnOverlayVisible(!available)
                pawn3DLegalHalo.visibility =
                    if (available) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String? = null,
        characterIdsBySeat: List<String> = emptyList(),
    ) {
        val nowMillis =
            monotonicMillis()
        updateIdleClock(
            state = state,
            nowMillis = nowMillis,
        )
        currentSnapshot = state
        currentCharacterIdsBySeat =
            characterIdsBySeat.take(4)

        val previous = previousSnapshot
        val reactions =
            LudoPawsReactionEngine.detect(
                previous = previous,
                current = state,
            )
        val reducedMotion =
            settingsStore
                .snapshot()
                .reducedMotionEnabled

        board.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        pawn3DScene.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
        )
        pawn3DLegalHalo.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
        )
        captureReturnOverlay.bind(
            previous = previous,
            current = state,
            perspectiveColor = perspectiveColor,
            characterIdsBySeat = characterIdsBySeat,
            reducedMotion = reducedMotion,
        )
        gameFxOverlay.bind(
            previous = previous,
            current = state,
            perspectiveColor = perspectiveColor,
            reducedMotion = reducedMotion,
        )
        characterReactionOverlay.bind(
            state = state,
            perspectiveColor = perspectiveColor,
            characterIdsBySeat = characterIdsBySeat,
        )
        previousSnapshot = state

        playReactions(
            reactions = reactions,
            characterIdsBySeat = characterIdsBySeat,
            reducedMotion = reducedMotion,
        )
        scheduleIdleReaction(
            state = state,
            nowMillis = nowMillis,
        )
    }

    fun reloadStyle() {
        board.reloadStyle()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(idleReactionRunnable)
        captureReturnOverlay.stop()
        gameFxOverlay.stop()
        characterReactionOverlay.stop()
        voicePlayer.shutdown()
        previousSnapshot = null
        currentSnapshot = null
        currentCharacterIdsBySeat = emptyList()
        meaningfulStateKey = null
        lastIdleReactionKey = null
        setLegacyPawnOverlayVisible(true)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val desired = density(380f).roundToInt()
        val resolvedWidth = resolveSize(desired, widthMeasureSpec)
        val resolvedHeight = resolveSize(resolvedWidth, heightMeasureSpec)
        val size = min(resolvedWidth, resolvedHeight)
        val exact = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(exact, exact)
    }

    /**
     * Compatibility bridge for the first production 3D rollout.
     *
     * LudoPawsBoardView intentionally keeps its authoritative board/touch layer
     * untouched. Its second child is the legacy cosmetic pawn overlay. We hide
     * only that child after the 3D EGL surface is confirmed operational; on any
     * renderer failure it is restored immediately. Legal-move halos are then
     * supplied by [pawn3DLegalHalo].
     */
    private fun setLegacyPawnOverlayVisible(
        visible: Boolean,
    ) {
        if (board.childCount < 2) {
            return
        }
        board.getChildAt(1).visibility =
            if (visible) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
    }

    private fun updateIdleClock(
        state: MatchSnapshot?,
        nowMillis: Long,
    ) {
        val nextKey =
            LudoPawsIdleReactionPolicy
                .meaningfulStateKey(state)

        if (
            state == null ||
            state.status != "ACTIVE"
        ) {
            removeCallbacks(idleReactionRunnable)
            meaningfulStateKey = nextKey
            lastMeaningfulChangeAtMillis = nowMillis
            lastIdleReactionKey = null
            return
        }

        if (nextKey != meaningfulStateKey) {
            meaningfulStateKey = nextKey
            lastMeaningfulChangeAtMillis = nowMillis
            lastIdleReactionKey = null
        }
    }

    private fun scheduleIdleReaction(
        state: MatchSnapshot?,
        nowMillis: Long,
    ) {
        removeCallbacks(idleReactionRunnable)
        val delay =
            LudoPawsIdleReactionPolicy
                .delayUntilEligibleMillis(
                    state = state,
                    lastMeaningfulChangeAtMillis =
                        lastMeaningfulChangeAtMillis,
                    nowMillis = nowMillis,
                )
                ?: return
        postDelayed(
            idleReactionRunnable,
            delay,
        )
    }

    private fun playIdleReactionIfEligible() {
        val state =
            currentSnapshot
                ?: return
        val nowMillis =
            monotonicMillis()
        val remaining =
            LudoPawsIdleReactionPolicy
                .delayUntilEligibleMillis(
                    state = state,
                    lastMeaningfulChangeAtMillis =
                        lastMeaningfulChangeAtMillis,
                    nowMillis = nowMillis,
                )
                ?: return
        if (remaining > 0L) {
            postDelayed(
                idleReactionRunnable,
                remaining,
            )
            return
        }

        val idleKey =
            LudoPawsIdleReactionPolicy
                .idleReactionKey(state)
                ?: return
        if (idleKey == lastIdleReactionKey) {
            return
        }

        val reactions =
            LudoPawsReactionEngine
                .deriveIdle(
                    current = state,
                    nowMillis = nowMillis,
                    lastMeaningfulChangeAtMillis =
                        lastMeaningfulChangeAtMillis,
                    thresholdMillis =
                        LudoPawsIdleReactionPolicy
                            .IDLE_THRESHOLD_MILLIS,
                )
        if (reactions.isEmpty()) {
            return
        }
        lastIdleReactionKey = idleKey

        playReactions(
            reactions = reactions,
            characterIdsBySeat =
                currentCharacterIdsBySeat,
            reducedMotion =
                settingsStore
                    .snapshot()
                    .reducedMotionEnabled,
        )
    }

    private fun playReactions(
        reactions: List<com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction>,
        characterIdsBySeat: List<String>,
        reducedMotion: Boolean,
    ) {
        if (reactions.isEmpty()) {
            return
        }
        gameFxOverlay.play(
            reactions = reactions,
            reducedMotion = reducedMotion,
        )
        characterReactionOverlay.play(
            reactions = reactions,
            reducedMotion = reducedMotion,
        )
        voicePlayer.playHighestPriority(
            reactions = reactions,
            characterIdsBySeat = characterIdsBySeat,
        )
    }

    private fun monotonicMillis(): Long =
        System.nanoTime() /
            1_000_000L

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density
}
