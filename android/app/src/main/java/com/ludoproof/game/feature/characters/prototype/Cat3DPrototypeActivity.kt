package com.ludoproof.game.feature.characters.prototype

import android.app.Activity
import android.app.ActivityManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class Cat3DPrototypeActivity : Activity() {
    private var catView: Cat3DPrototypeView? = null

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        if (!supportsOpenGlEs3()) {
            setContentView(
                TextView(this).apply {
                    text = "This device does not support the OpenGL ES 3.0 cat prototype."
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.rgb(14, 19, 28))
                    textSize = 18f
                    setPadding(dp(24), dp(24), dp(24), dp(24))
                },
            )
            return
        }

        val root =
            FrameLayout(this).apply {
                setBackgroundColor(Color.rgb(14, 19, 28))
            }
        val prototype = Cat3DPrototypeView(this)
        catView = prototype
        root.addView(
            prototype,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val title =
            TextView(this).apply {
                text = "3D Kitty Prototype"
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(Color.argb(150, 8, 12, 18))
            }
        root.addView(
            title,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
                Gravity.TOP,
            ),
        )

        val controls =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(14))
                setBackgroundColor(Color.argb(175, 8, 12, 18))
            }

        controls.addView(
            controlButton("Idle") {
                prototype.playIdle()
            },
            weightedButtonLayoutParams(),
        )
        controls.addView(
            controlButton("Hop") {
                prototype.playHop()
            },
            weightedButtonLayoutParams(),
        )
        controls.addView(
            controlButton("Home") {
                prototype.playHomeCelebration()
            },
            weightedButtonLayoutParams(),
        )

        root.addView(
            controls,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72),
                Gravity.BOTTOM,
            ),
        )

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        catView?.onResume()
    }

    override fun onPause() {
        catView?.onPause()
        super.onPause()
    }

    private fun controlButton(
        label: String,
        action: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener {
                action()
            }
        }

    private fun weightedButtonLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f,
        ).apply {
            marginStart = dp(4)
            marginEnd = dp(4)
        }

    private fun supportsOpenGlEs3(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
    }

    private fun dp(
        value: Int,
    ): Int =
        (value * resources.displayMetrics.density).toInt()
}
