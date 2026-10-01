package com.ludoproof.game

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.RadioButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArcadeFlowTest {
    @Test fun localSetupNamesRollAndResumeKeepTheSameSavedEvent() {
        val controller = Robolectric.buildActivity(OfflineGameActivity::class.java).setup().visible()
        val activity = controller.get()
        capture(activity, "local-setup")
        val inputs = descendants(activity.window.decorView).filterIsInstance<EditText>().toList()
        inputs[0].setText("Asha")
        inputs[1].setText("Ravi")
        button(activity, "LET'S PLAY  ›").performClick()
        val initial = OfflineGameEngine(activity).snapshot()!!
        assertEquals(listOf("Asha", "Ravi"), initial.players.map { it.displayName })
        capture(activity, "local-game")
        button(activity, "ROLL DICE").performClick()
        val rolled = OfflineGameEngine(activity).snapshot()!!
        assertEquals(1, rolled.history.size)
        assertFalse(button(activity, "ROLLING…").isEnabled)
        controller.pause().stop().destroy()
        val resumed = Robolectric.buildActivity(OfflineGameActivity::class.java).setup().visible()
        assertEquals(rolled, OfflineGameEngine(resumed.get()).snapshot())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(1, OfflineGameEngine(resumed.get()).snapshot()!!.history.size)
        resumed.pause().stop().destroy()
    }

    @Test fun newGameRequiresExplicitReplacementAndCanBeCancelled() {
        val controller = Robolectric.buildActivity(OfflineGameActivity::class.java).setup().visible()
        val activity = controller.get()
        button(activity, "LET'S PLAY  ›").performClick()
        val original = OfflineGameEngine(activity).snapshot()
        button(activity, "New game").performClick()
        val dialog = ShadowDialog.getLatestDialog()
        captureView(dialog.window!!.decorView, "new-game-dialog", 340, 600)
        descendants(dialog.window!!.decorView).filterIsInstance<Button>().first { it.text == "Keep playing" }.performClick()
        assertEquals(original, OfflineGameEngine(activity).snapshot())
        button(activity, "New game").performClick()
        descendants(ShadowDialog.getLatestDialog().window!!.decorView).filterIsInstance<Button>().first { it.text == "New game" }.performClick()
        assertNull(OfflineGameEngine(activity).snapshot())
        assertNotNull(button(activity, "LET'S PLAY  ›"))
        controller.pause().stop().destroy()
    }

    @Test fun homeAndSettingsRenderAndPreferencesPersist() {
        val controller = Robolectric.buildActivity(HomeActivity::class.java).setup().visible()
        val activity = controller.get()
        capture(activity, "home")
        assertNotNull(button(activity, "Online"))
        assertNotNull(button(activity, "Local"))
        ArcadeDialogs.showSettings(activity)
        val dialog = ShadowDialog.getLatestDialog()
        captureView(dialog.window!!.decorView, "settings", 340, 650)
        assertTrue(descendants(dialog.window!!.decorView).filterIsInstance<RadioButton>().first { it.text == "Normal" }.isChecked)
        descendants(dialog.window!!.decorView).filterIsInstance<RadioButton>().first { it.text == "Fast" }.performClick()
        assertEquals("Fast", GamePreferences(activity).speed)
        descendants(dialog.window!!.decorView).filterIsInstance<Switch>().first { it.text == "Dice animation" }.performClick()
        assertFalse(GamePreferences(activity).animations)
        assertEquals(0L, GamePreferences(activity).rollDurationMs)
        dialog.dismiss()
        controller.pause().stop().destroy()
    }

    @Test fun onlineLobbyShowsErrorsOutsideTheHiddenGameControls() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        val activity = controller.get()
        capture(activity, "online-lobby")
        assertTrue(descendants(activity.window.decorView).filterIsInstance<EditText>().all { it.isShown })
        descendants(activity.window.decorView).filterIsInstance<EditText>().first().setText("")
        button(activity, "Create a room").apply { isEnabled = true; performClick() }
        assertTrue(descendants(activity.window.decorView).filterIsInstance<TextView>().any {
            it.isShown && it.text.toString() == "Player name must contain 2 to 24 characters"
        })
        controller.pause().stop().destroy()
    }

    @Test @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun narrowSetupKeepsAllControlsReachable() {
        val controller = Robolectric.buildActivity(OfflineGameActivity::class.java).setup().visible()
        val activity = controller.get()
        button(activity, "4 P").performClick()
        captureView(activity.window.decorView, "local-setup-small", 320, 640)
        assertEquals(4, descendants(activity.window.decorView).filterIsInstance<EditText>().count())
        assertTrue(button(activity, "LET'S PLAY  ›").isEnabled)
        controller.pause().stop().destroy()
    }

    @Test fun largerTextKeepsThePlayActionInTheViewport() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.5f)
        val controller = Robolectric.buildActivity(OfflineGameActivity::class.java).setup().visible()
        val activity = controller.get()
        capture(activity, "local-setup-large-text")
        val action = button(activity, "LET'S PLAY  ›")
        val position = IntArray(2)
        action.getLocationOnScreen(position)
        assertTrue(action.isShown)
        assertTrue(position[1] + action.height <= 800)
        controller.pause().stop().destroy()
    }

    private fun button(activity: Activity, text: String): Button =
        descendants(activity.window.decorView).filterIsInstance<Button>().first { it.text.toString() == text }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }

    private fun capture(activity: Activity, name: String) = captureView(activity.window.decorView, name, 360, 800)

    private fun captureView(view: View, name: String, width: Int, height: Int) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File("build/reports/ui/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
