package com.ludoproof.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class BoardGestureTest {
    private val state = MatchSnapshot(
        "test", "ACTIVE", "red",
        listOf(PlayerSnapshot("red", "Red", "RED", 0, listOf(-1, -1, -1, -1))),
        0, 1,
        PendingRollSnapshot("RESOLVED", 0, 1, null, null, null, null, null, null, 6, setOf(0)),
        null, "v1", emptyList(),
    )

    @Test fun onlyAnUninterruptedTapSelectsAToken() {
        val board = LudoBoardView(RuntimeEnvironment.getApplication())
        val selected = mutableListOf<Int>()
        board.onTokenSelected = { selected += it }
        board.layout(0, 0, 360, 360)
        board.bind(state, "red")
        val bitmap = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
        board.draw(Canvas(bitmap))
        fun touch(action: Int, x: Float = 48f, y: Float = 48f) {
            val event = MotionEvent.obtain(0, 10, action, x, y, 0)
            board.onTouchEvent(event)
            event.recycle()
        }
        touch(MotionEvent.ACTION_DOWN)
        touch(MotionEvent.ACTION_UP)
        assertEquals(listOf(0), selected)
        selected.clear()

        touch(MotionEvent.ACTION_DOWN)
        touch(MotionEvent.ACTION_MOVE, 100f, 100f)
        touch(MotionEvent.ACTION_UP)
        touch(MotionEvent.ACTION_DOWN)
        touch(MotionEvent.ACTION_CANCEL)
        touch(MotionEvent.ACTION_UP)
        touch(MotionEvent.ACTION_DOWN)
        touch(MotionEvent.ACTION_POINTER_DOWN)
        touch(MotionEvent.ACTION_UP)
        touch(MotionEvent.ACTION_DOWN)
        board.bind(state, "red")
        board.draw(Canvas(bitmap))
        touch(MotionEvent.ACTION_UP)
        board.isEnabled = false
        touch(MotionEvent.ACTION_DOWN)
        touch(MotionEvent.ACTION_UP)
        assertEquals(emptyList<Int>(), selected)
        bitmap.recycle()
    }
}
