package com.ludoproof.game

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView

/**
 * Shared full-screen nature backdrop.
 *
 * Every regular screen uses the grass/flower artwork. As soon as the actual
 * Ludo board is visible, the backdrop switches to the dedicated match garden
 * artwork. The board remains the only signal: waiting/setup/lobby UI therefore
 * keeps the regular background, while ACTIVE and FINISHED board presentation
 * uses the match background without coupling this view to game rules/state.
 */
class ArcadeBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ImageView(context, attrs) {
    private enum class BackdropKind(
        val resourceName: String,
        val fallbackColor: Int,
    ) {
        UI(
            resourceName = "ludo_paws_ui_background",
            fallbackColor = 0xFF275B19.toInt(),
        ),
        MATCH(
            resourceName = "ludo_paws_match_background",
            fallbackColor = 0xFF315B1F.toInt(),
        ),
    }

    private var appliedKind: BackdropKind? = null

    private val globalLayoutListener =
        ViewTreeObserver.OnGlobalLayoutListener {
            syncBackdropWithVisibleBoard()
        }

    init {
        scaleType = ScaleType.CENTER_CROP
        adjustViewBounds = false
        isClickable = false
        isFocusable = false
        contentDescription = null
        importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_NO
        applyBackdrop(BackdropKind.UI)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewTreeObserver.addOnGlobalLayoutListener(
            globalLayoutListener,
        )
        post {
            syncBackdropWithVisibleBoard()
        }
    }

    override fun onDetachedFromWindow() {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnGlobalLayoutListener(
                globalLayoutListener,
            )
        }
        super.onDetachedFromWindow()
    }

    private fun syncBackdropWithVisibleBoard() {
        val root = rootView ?: return
        val nextKind =
            if (containsVisibleLudoBoard(root)) {
                BackdropKind.MATCH
            } else {
                BackdropKind.UI
            }
        applyBackdrop(nextKind)
    }

    private fun containsVisibleLudoBoard(
        view: View,
    ): Boolean {
        if (view is LudoPawsReactiveBoardView) {
            return view.isShown
        }
        if (view !is ViewGroup) {
            return false
        }

        for (index in 0 until view.childCount) {
            if (
                containsVisibleLudoBoard(
                    view.getChildAt(index),
                )
            ) {
                return true
            }
        }
        return false
    }

    @Suppress("DiscouragedApi")
    private fun applyBackdrop(kind: BackdropKind) {
        if (appliedKind == kind) {
            return
        }
        appliedKind = kind

        // Names are resolved dynamically so this presentation layer remains
        // isolated from authoritative gameplay code. Release resource shrinking
        // is paired with res/raw/ludo_paws_background_keep.xml.
        val drawableId =
            resources.getIdentifier(
                kind.resourceName,
                "drawable",
                context.packageName,
            )
        if (drawableId != 0) {
            setBackgroundColor(0x00000000)
            setImageResource(drawableId)
        } else {
            // Keeps development builds usable if artwork has not been copied
            // into drawable-nodpi yet; production/release builds include both.
            setImageDrawable(null)
            setBackgroundColor(kind.fallbackColor)
        }
    }
}
