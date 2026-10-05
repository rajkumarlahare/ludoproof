package com.ludoproof.game

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.ludoproof.game.feature.characters.data.audio.LudoPawsVoicePlayer
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsIdleReactionPolicy
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Production Ludo Paws board shell.
 *
 * Character visuals are exclusively rendered by the shared 3D runtime. The old
 * drawable pawn, drawable capture-return and large drawable reaction layers have
 * been retired. The proven board remains authoritative for geometry/touch/game
 * state, while board FX, legal halos and animal audio stay presentation-only.
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
    private val gameFxOverlay =
        LudoPawsGameFxOverlayView(context)
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
            gameFxOverlay,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )

        // The authoritative board remains a safe classic-pawn fallback if a
        // device cannot create the ES3 surface. No legacy animal drawable layer
        // is re-enabled.
        pawn3DLegalHalo.visibility = View.GONE
        pawn3DScene.onOperationalChanged =
            { available ->
                pawn3DLegalHalo.visibility =
                    if (available) View.VISIBLE else View.GONE
            }
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String? = null,
        characterIdsBySeat: List<String> = emptyList(),
    ) {
        val nowMillis = monotonicMillis()
        updateIdleClock(
            state = state,
            nowMillis = nowMillis,
        )
        currentSnapshot = state
        currentCharacterIdsBySeat = characterIdsBySeat.take(4)

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
        gameFxOverlay.bind(
            previous = previous,
            current = state,
            perspectiveColor = perspectiveColor,
            reducedMotion = reducedMotion,
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
        gameFxOverlay.stop()
        voicePlayer.shutdown()
        previousSnapshot = null
        currentSnapshot = null
        currentCharacterIdsBySeat = emptyList()
        meaningfulStateKey = null
        lastIdleReactionKey = null
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
                    lastMeaningfulChangeAtMillis = lastMeaningfulChangeAtMillis,
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
        val nowMillis = monotonicMillis()
        val remaining =
            LudoPawsIdleReactionPolicy
                .delayUntilEligibleMillis(
                    state = state,
                    lastMeaningfulChangeAtMillis = lastMeaningfulChangeAtMillis,
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
                    lastMeaningfulChangeAtMillis = lastMeaningfulChangeAtMillis,
                    thresholdMillis = LudoPawsIdleReactionPolicy.IDLE_THRESHOLD_MILLIS,
                )
        if (reactions.isEmpty()) {
            return
        }
        lastIdleReactionKey = idleKey

        playReactions(
            reactions = reactions,
            characterIdsBySeat = currentCharacterIdsBySeat,
            reducedMotion =
                settingsStore
                    .snapshot()
                    .reducedMotionEnabled,
        )
    }

    private fun playReactions(
        reactions: List<LudoPawsReaction>,
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
        voicePlayer.playHighestPriority(
            reactions = reactions,
            characterIdsBySeat = characterIdsBySeat,
        )
    }

    private fun monotonicMillis(): Long =
        System.nanoTime() / 1_000_000L

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density
}
