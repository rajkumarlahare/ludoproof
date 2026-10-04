package com.ludoproof.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class DiceVisibilityInstrumentedTest {
    @Test
    fun neutralDiceFaceStaysBrightAndOpaqueOverDarkBlueGameplayBackground() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()
        val size = 192

        val host =
            FrameLayout(context).apply {
                setBackgroundColor(
                    0xFF071A47.toInt(),
                )
            }
        val dice =
            DiceView(context).apply {
                isEnabled = false
                alpha = 1f
                showOutcome(6)
            }
        host.addView(
            dice,
            FrameLayout.LayoutParams(
                size,
                size,
            ),
        )

        val exact =
            View.MeasureSpec.makeMeasureSpec(
                size,
                View.MeasureSpec.EXACTLY,
            )
        host.measure(exact, exact)
        host.layout(0, 0, size, size)

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888,
            )
        host.draw(
            Canvas(bitmap),
        )

        // Sample the upper-middle of the face: safely inside the rounded
        // rectangle and away from all pip locations.
        val pixel =
            bitmap.getPixel(
                size / 2,
                (size * .20f).toInt(),
            )
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)

        assertTrue(
            "dice face must remain bright",
            red >= 225 && green >= 225 && blue >= 225,
        )
        assertTrue(
            "dice face must remain neutral, not blue-tinted",
            abs(red - blue) <= 18,
        )
        assertEquals(
            "dice view must not be dimmed",
            1f,
            dice.alpha,
            0f,
        )
    }
}
