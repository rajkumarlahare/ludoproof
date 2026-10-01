package com.ludoproof.game

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View

class GamePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)

    var animations: Boolean
        get() = prefs.getBoolean("animations", true)
        set(value) { prefs.edit().putBoolean("animations", value).apply() }

    var haptics: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(value) { prefs.edit().putBoolean("haptics", value).apply() }

    var speed: String
        get() = prefs.getString("speed", "Normal")?.takeIf { it in SPEEDS } ?: "Normal"
        set(value) { if (value in SPEEDS) prefs.edit().putString("speed", value).apply() }

    val rollDurationMs: Long
        get() = if (!animations || !android.animation.ValueAnimator.areAnimatorsEnabled()) 0L else when (speed) {
            "Relaxed" -> 800L
            "Fast" -> 200L
            else -> 430L
        }

    fun feedback(view: View) {
        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    companion object {
        val SPEEDS = listOf("Relaxed", "Normal", "Fast")
    }
}
