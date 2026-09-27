package com.ludoproof.game

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val api = GameApi()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var nameInput: EditText
    private lateinit var matchInput: EditText
    private lateinit var statusText: TextView

    private var matchId: String? = null
    private var playerToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "ludoproof_session",
            MODE_PRIVATE,
        )
        matchId = prefs.getString("matchId", null)
        playerToken = prefs.getString("playerToken", null)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        content.addView(
            TextView(this).apply {
                text = "LudoProof"
                textSize = 30f
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        content.addView(
            TextView(this).apply {
                text =
                    "Every dice roll is bound to a verifiable EntroNex event."
                textSize = 15f
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        nameInput = EditText(this).apply {
            hint = "Player name"
            setText("Player")
            inputType = InputType.TYPE_CLASS_TEXT
        }
        content.addView(nameInput)

        matchInput = EditText(this).apply {
            hint = "Match code, e.g. LPABCDEFGH"
            setText(matchId.orEmpty())
            inputType =
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        content.addView(matchInput)

        content.addView(
            row(
                button("Create Match") {
                    runNetwork {
                        val response =
                            api.createMatch(playerName())
                        saveSession(response)
                        response
                    }
                },
                button("Join Match") {
                    runNetwork {
                        val code = matchInput.text
                            .toString()
                            .trim()
                            .uppercase()
                        val response =
                            api.joinMatch(
                                code,
                                playerName(),
                            )
                        saveSession(response)
                        response
                    }
                },
            ),
        )

        content.addView(
            row(
                button("Refresh") {
                    withSession { code, token ->
                        runNetwork {
                            api.state(code, token)
                        }
                    }
                },
                button("Start") {
                    withSession { code, token ->
                        runNetwork {
                            api.start(code, token)
                        }
                    }
                },
            ),
        )

        content.addView(
            button("ROLL VERIFIED DICE") {
                withSession { code, token ->
                    runNetwork {
                        val prepared =
                            SeedCommitment.prepare()

                        // Commit is completed and received before
                        // the client seed is revealed.
                        api.commitRoll(
                            code,
                            token,
                            prepared.clientCommitment,
                        )

                        api.revealRoll(
                            code,
                            token,
                            prepared.clientSeed,
                        )
                    }
                }
            }.apply {
                textSize = 20f
                minHeight = 130
            },
        )

        content.addView(
            TextView(this).apply {
                text = "Move a token after a verified roll:"
                textSize = 16f
            },
        )

        content.addView(
            row(
                button("Token 1") { moveToken(0) },
                button("Token 2") { moveToken(1) },
            ),
        )
        content.addView(
            row(
                button("Token 3") { moveToken(2) },
                button("Token 4") { moveToken(3) },
            ),
        )

        statusText = TextView(this).apply {
            textSize = 13f
            setPadding(0, 24, 0, 48)
            setTextIsSelectable(true)
            text =
                "Create or join a match.\n" +
                    "API: ${BuildConfig.LUDOPROOF_API_BASE_URL}"
        }
        content.addView(statusText)

        setContentView(
            ScrollView(this).apply {
                addView(content)
            },
        )

        if (matchId != null && playerToken != null) {
            matchInput.setText(matchId)
            withSession { code, token ->
                runNetwork { api.state(code, token) }
            }
        }
    }

    private fun moveToken(index: Int) {
        withSession { code, token ->
            runNetwork {
                api.move(code, token, index)
            }
        }
    }

    private fun playerName(): String {
        val value =
            nameInput.text.toString().trim()
        require(value.length in 2..24) {
            "Player name must contain 2 to 24 characters"
        }
        return value
    }

    private fun saveSession(response: org.json.JSONObject) {
        val code = response.getString("matchId")
        val token = response.getString("playerToken")
        matchId = code
        playerToken = token
        matchInput.setText(code)

        getSharedPreferences(
            "ludoproof_session",
            MODE_PRIVATE,
        )
            .edit()
            .putString("matchId", code)
            .putString("playerToken", token)
            .apply()
    }

    private fun withSession(
        action: (String, String) -> Unit,
    ) {
        val code = matchId
        val token = playerToken

        if (code == null || token == null) {
            showStatus(
                "Create or join a match first.",
            )
            return
        }

        action(code, token)
    }

    private fun runNetwork(
        action: () -> org.json.JSONObject,
    ) {
        showStatus("Working…")
        executor.execute {
            try {
                val response = action()
                mainHandler.post {
                    showStatus(
                        response.toString(2),
                    )
                }
            } catch (error: Exception) {
                mainHandler.post {
                    showStatus(
                        "Error: " +
                            (error.message ?: error.toString()),
                    )
                }
            }
        }
    }

    private fun showStatus(value: String) {
        statusText.text = value
    }

    private fun button(
        label: String,
        action: () -> Unit,
    ): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun row(
        vararg views: View,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for (view in views) {
                addView(
                    view,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ),
                )
            }
        }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
