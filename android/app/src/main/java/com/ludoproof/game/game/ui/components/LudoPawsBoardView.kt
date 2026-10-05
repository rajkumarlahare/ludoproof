package com.ludoproof.game

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Production Ludo Paws board shell.
 *
 * The authoritative [LudoBoardView] remains solely responsible for board geometry,
 * touch hit-testing and gameplay-facing token selection. Animal pawn rendering is
 * now owned by the production 3D scene in [LudoPawsReactiveBoardView].
 *
 * BOARD GEOMETRY LOCK: do not move, resize or reinterpret the approved board here.
 */
class LudoPawsBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val baseBoard =
        LudoBoardView(context)

    var onTokenSelected: ((Int) -> Unit)?
        get() = baseBoard.onTokenSelected
        set(value) {
            baseBoard.onTokenSelected = value
        }

    init {
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(
                    view: View,
                    outline: Outline,
                ) {
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        density(3f),
                    )
                }
            }
        clipToOutline = true

        addView(
            baseBoard,
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
        // characterIdsBySeat is intentionally retained in this public UI contract so
        // callers do not need a gameplay-facing migration. Character visuals are 3D.
        @Suppress("UNUSED_VARIABLE")
        val retainedCharacterContract = characterIdsBySeat

        baseBoard.bind(
            state = state,
            playerId = playerId,
            perspectiveColor = perspectiveColor,
        )
    }

    fun reloadStyle() {
        baseBoard.reloadStyle()
    }

    // BOARD SIZE LOCK: exactly mirror the approved LudoBoardView square.
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
