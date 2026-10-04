package com.ludoproof.game

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.ludoproof.game.feature.characters.data.audio.LudoPawsVoicePlayer
import com.ludoproof.game.feature.characters.domain.reaction.LudoPawsReactionEngine
import com.ludoproof.game.feature.settings.data.local.GameSettingsStore
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared Ludo Paws board shell.
 *
 * Authoritative gameplay remains inside the existing board/engine stack. Phase
 * 10 adds two presentation-only layers above it: one for board particles and
 * token-transition FX, and one for larger character personality reactions.
 */
class LudoPawsReactiveBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val board =
        LudoPawsBoardView(context)
    private val gameFxOverlay =
        LudoPawsGameFxOverlayView(context)
    private val characterReactionOverlay =
        LudoPawsCharacterReactionOverlayView(context)
    private val voicePlayer =
        LudoPawsVoicePlayer(context)
    private val settingsStore =
        GameSettingsStore(context)
    private var previousSnapshot: MatchSnapshot? = null

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
    }

    fun bind(
        state: MatchSnapshot?,
        playerId: String?,
        perspectiveColor: String? = null,
        characterIdsBySeat: List<String> = emptyList(),
    ) {
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

        if (reactions.isNotEmpty()) {
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
    }

    fun reloadStyle() {
        board.reloadStyle()
    }

    override fun onDetachedFromWindow() {
        gameFxOverlay.stop()
        characterReactionOverlay.stop()
        voicePlayer.shutdown()
        previousSnapshot = null
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

    private fun density(value: Float): Float =
        value * resources.displayMetrics.density
}
