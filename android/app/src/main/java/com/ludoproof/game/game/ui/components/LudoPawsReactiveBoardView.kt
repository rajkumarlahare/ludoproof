package com.ludoproof.game

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.ludoproof.game.feature.characters.data.audio.LudoPawsVoicePlayer
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsIdleReactionPolicy
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReaction
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Production Ludo Paws board shell.
 *
 * Character visuals are rendered by the shared 3D runtime. The proven board
 * remains authoritative for geometry/touch/game state, while 3D body language,
 * board FX and animal audio are presentation-only.
 */
class LudoPawsReactiveBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val board = LudoPawsBoardView(context)
    private val pawn3DScene = LudoPaws3DSceneView(context)
    private val legalPulse = LudoPaws3DLegalPulseView(context)
    private val gameFxOverlay = LudoPawsGameFxOverlayView(context)
    private val voicePlayer = LudoPawsVoicePlayer(context)

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

        // The approved board itself remains square and unchanged. Only the
        // transparent 3D pawn surface is allowed to extend above this container
        // so a large animal on the top row is not clipped at the board edge.
        clipChildren = false
        clipToPadding = false

        addView(
            board,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )

        // Normal gameplay is 3D-first. Keep the legacy 2D pawn layer hidden from
        // the first frame so its circular pawn art cannot appear beside, ahead of,
        // or behind an animal while the GL surface is starting or moving.
        board.setClassicPawnFallbackVisible(false)

        // Brighten only the transparent 3D pawn layer, not the board artwork.
        // RGB scaling preserves the existing material palette, highlights,
        // shadows and alpha while making all four animals easier to read.
        val pawnBrightness =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter =
                    ColorMatrixColorFilter(
                        ColorMatrix().apply {
                            setScale(
                                PAWN_BRIGHTNESS_SCALE,
                                PAWN_BRIGHTNESS_SCALE,
                                PAWN_BRIGHTNESS_SCALE,
                                1f,
                            )
                        },
                    )
            }
        pawn3DScene.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            pawnBrightness,
        )

        addView(
            pawn3DScene,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        // Legal choices use a pawn-local shimmer/sparkle instead of any circular
        // follow halo. The cue marks only selectable pawns and never previews the
        // destination square.
        addView(
            legalPulse,
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

        pawn3DScene.onOperationalChanged =
            { available ->
                if (available) {
                    board.setClassicPawnFallbackVisible(false)
                } else if (
                    isShown &&
                    windowVisibility == View.VISIBLE
                ) {
                    // Only expose the proven classic-pawn fallback when the visible
                    // 3D runtime actually becomes unavailable. Lifecycle teardown
                    // while the board is hidden must never flash legacy circles.
                    board.setClassicPawnFallbackVisible(true)
                }
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
            characterIdsBySeat = characterIdsBySeat,
        )
        legalPulse.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
        )
        gameFxOverlay.bind(
            previous = previous,
            current = state,
            perspectiveColor = perspectiveColor,
        )
        previousSnapshot = state

        playReactions(
            reactions = reactions,
            characterIdsBySeat = characterIdsBySeat,
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
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        val screenWidth = resources.displayMetrics.widthPixels
        val compactScreen =
            screenWidth <= density(600f).roundToInt()

        // On phones the board is intentionally allowed to outgrow the centered
        // content column and use the physical screen width. The board stage and
        // its ancestors already disable child clipping, so this expands the
        // square equally left/right instead of stretching it. Tablet/expanded
        // layouts keep the existing constrained content width.
        val requestedWidth =
            if (compactScreen) {
                screenWidth
            } else {
                parentWidth
            }
        val resolvedHeight =
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                requestedWidth
            } else {
                parentHeight
            }
        val size =
            min(
                requestedWidth,
                resolvedHeight,
            ).coerceAtLeast(1)

        configurePawnTopOverflow(size)

        val exact = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(exact, exact)
    }

    private fun configurePawnTopOverflow(boardSize: Int) {
        val topOverflow =
            (boardSize * PAWN_TOP_OVERFLOW_FRACTION)
                .roundToInt()
                .coerceAtLeast(0)
        val params = pawn3DScene.layoutParams as LayoutParams
        val targetHeight = boardSize + topOverflow
        if (
            params.height == targetHeight &&
            params.topMargin == -topOverflow
        ) {
            return
        }

        // Negative top margin + matching extra height keeps the bottom edge and
        // every board-space pawn coordinate exactly where they were. The added
        // pixels exist only above the board for heads/ears/horns to render into.
        params.width = LayoutParams.MATCH_PARENT
        params.height = targetHeight
        params.topMargin = -topOverflow
        pawn3DScene.layoutParams = params
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
        val state = currentSnapshot ?: return
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
        if (idleKey == lastIdleReactionKey) return

        val reactions =
            LudoPawsReactionEngine
                .deriveIdle(
                    current = state,
                    nowMillis = nowMillis,
                    lastMeaningfulChangeAtMillis = lastMeaningfulChangeAtMillis,
                    thresholdMillis = LudoPawsIdleReactionPolicy.IDLE_THRESHOLD_MILLIS,
                )
        if (reactions.isEmpty()) return
        lastIdleReactionKey = idleKey

        playReactions(
            reactions = reactions,
            characterIdsBySeat = currentCharacterIdsBySeat,
        )
    }

    private fun playReactions(
        reactions: List<LudoPawsReaction>,
        characterIdsBySeat: List<String>,
    ) {
        if (reactions.isEmpty()) return

        // Body language is separate from overlay FX. The scene itself decides
        // how Dog/Goat/Duck/Cat anatomy should express each animation cue.
        pawn3DScene.playReactions(reactions)
        gameFxOverlay.play(
            reactions = reactions,
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

    private companion object {
        // About 1.8 board cells on a 15x15 Ludo board. This is transparent
        // presentation space only; the approved square board remains unchanged.
        const val PAWN_TOP_OVERFLOW_FRACTION = 0.12f
        const val PAWN_BRIGHTNESS_SCALE = 1.10f
    }
}
