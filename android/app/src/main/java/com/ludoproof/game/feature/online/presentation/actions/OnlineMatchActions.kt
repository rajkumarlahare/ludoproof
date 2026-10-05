package com.ludoproof.game.feature.online

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.concurrent.Executors
import com.ludoproof.game.*
import com.ludoproof.game.ui.online.*
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardCredential
import com.ludoproof.game.feature.leaderboard.data.local.LeaderboardIdentityStore
import com.ludoproof.game.feature.leaderboard.data.remote.LeaderboardApi
import com.ludoproof.game.feature.profile.data.local.ProfileStore

internal fun MainActivity.createMatch() {
    val displayName =
        runCatching {
            playerName()
        }.getOrElse {
            showStatus(
                it.message
                    ?: "Invalid player name",
            )
            return
        }
    runCatching {
        ProfileStore(
            this,
        ).updateDisplayName(
            displayName,
        )
    }
    val operationKey =
        "create:" + displayName
    val requestId =
        runCatching {
            pendingOperationStore
                .getOrCreate(
                    "create",
                    operationKey,
                )
        }.getOrElse {
            showStatus(
                it.message
                    ?: "Could not prepare match request",
            )
            return
        }

    runNetwork(
        action = {
            val credential =
                ensureLeaderboardCredential()
            api.createMatch(
                profileToken =
                    credential.profileToken,
                displayName =
                    displayName,
                clientRequestId =
                    requestId,
            )
        },
        onSuccess = {
            pendingOperationStore.clear(
                "create",
                operationKey,
            )
            captureSession(it)
            applyResponse(it)
        },
    )
}

internal fun MainActivity.joinMatch() {
    val displayName =
        runCatching {
            playerName()
        }.getOrElse {
            showStatus(
                it.message
                    ?: "Invalid player name",
            )
            return
        }

    runCatching {
        ProfileStore(
            this,
        ).updateDisplayName(
            displayName,
        )
    }
    val code =
        matchInput.text
            .toString()
            .trim()
            .uppercase()

    if (
        !Regex(
            "^LP[A-Z2-9]{8}$",
        ).matches(code)
    ) {
        showStatus(
            "Enter a valid Ludo Paws match code.",
        )
        return
    }

    val operationKey =
        "join:" +
            code +
            ":" +
            displayName
    val requestId =
        runCatching {
            pendingOperationStore
                .getOrCreate(
                    "join",
                    operationKey,
                )
        }.getOrElse {
            showStatus(
                it.message
                    ?: "Could not prepare join request",
            )
            return
        }

    runNetwork(
        action = {
            val credential =
                ensureLeaderboardCredential()
            api.joinMatch(
                matchId =
                    code,
                displayName =
                    displayName,
                clientRequestId =
                    requestId,
                profileToken =
                    credential.profileToken,
            )
        },
        onSuccess = {
            pendingOperationStore.clear(
                "join",
                operationKey,
            )
            captureSession(it)
            applyResponse(it)
        },
    )
}

internal fun MainActivity.refreshState(
    silent: Boolean = false,
) {
    withSession {
            code,
            token,
        ->
        runNetwork(
            action = {
                api.state(
                    code,
                    token,
                )
            },
            onSuccess = {
                applyResponse(
                    it,
                    announce = !silent,
                )
            },
            showWorking = !silent,
        )
    }
}

internal fun MainActivity.rollVerifiedDice() {
    val state =
        currentState
            ?: run {
                showStatus("Sync the active match before rolling.")
                return
            }
    val decision = onlineRollActionDecision(state)
    if (!decision.enabled) {
        showStatus(
            when (decision.kind) {
                OnlineRollActionKind.WAIT_FOR_RECOVERY ->
                    "This committed roll is locked to its original secret • waiting for recovery or server timeout."
                OnlineRollActionKind.MOVE_REQUIRED ->
                    "Move a highlighted token before rolling again."
                else ->
                    if (isOnline) "Verified roll is not available right now."
                    else "Offline — verified rolls resume when internet returns."
            },
        )
        updateControls(state)
        return
    }

    withSession {
            code,
            token,
        ->
        var secret =
            pendingSecret
                ?.takeIf {
                    it.matchId == code
                }

        if (decision.kind == OnlineRollActionKind.NEW_ROLL) {
            if (secret != null) {
                pendingRollStore.clear()
                pendingSecret = null
            }
            val prepared = SeedCommitment.prepare()
            secret =
                PendingRollSecret(
                    matchId = code,
                    clientSeed = prepared.clientSeed,
                    clientCommitment = prepared.clientCommitment,
                )
            pendingRollStore.save(requireNotNull(secret))
            pendingSecret = secret
        }

        val resumableSecret =
            secret
                ?: run {
                    showStatus("Roll recovery secret is unavailable • waiting for server recovery.")
                    currentState?.let(::updateControls)
                    return@withSession
                }

        diceView.startRolling()
        verificationText.text =
            if (decision.kind == OnlineRollActionKind.NEW_ROLL) {
                "Verifying committed EntroNex roll…"
            } else {
                "Resuming committed EntroNex roll…"
            }

        runNetwork(
            action = {
                when (decision.kind) {
                    OnlineRollActionKind.NEW_ROLL,
                    OnlineRollActionKind.RESUME_COMMIT_THEN_REVEAL,
                    ->
                        api.commitRoll(
                            code,
                            token,
                            resumableSecret.clientCommitment,
                        )

                    OnlineRollActionKind.RESUME_REVEAL_ONLY -> Unit
                    else -> error("Online roll action changed before submission")
                }

                val revealed =
                    api.revealRoll(
                        code,
                        token,
                        resumableSecret.clientSeed,
                    )

                pendingRollStore.clear()
                pendingSecret = null
                revealed
            },
        )
    }
}

internal fun MainActivity.moveToken(
    index: Int,
) {
    val eventIndex =
        currentState
            ?.pendingRoll
            ?.eventIndex
            ?.takeIf {
                it >= 0
            }
            ?: run {
                showStatus(
                    "No verified roll is available for this move.",
                )
                return
            }

    withSession {
            code,
            token,
        ->
        runNetwork(
            action = {
                api.move(
                    code,
                    token,
                    index,
                    eventIndex,
                )
            },
        )
    }
}

internal fun MainActivity.ensureLeaderboardCredential():
    LeaderboardCredential {
    val store =
        LeaderboardIdentityStore(
            this,
        )
    store.load()
        ?.let {
            return it
        }

    val requestId =
        store.registrationRequestId()
    val response =
        LeaderboardApi()
            .registerProfile(
                clientRequestId =
                    requestId,
            )
    val credential =
        LeaderboardCredential(
            profileId =
                response.getString(
                    "profileId",
                ),
            profileToken =
                response.getString(
                    "profileToken",
                ),
            registrationRequestId =
                requestId,
        )
    store.save(
        credential,
    )
    return credential
}

internal fun MainActivity.captureSession(
    response: JSONObject,
) {
    val code =
        response.getString(
            "matchId",
        )
    val token =
        response.getString(
            "playerToken",
        )
    val id =
        response.getString(
            "playerId",
        )

    if (
        matchId !=
        code
    ) {
        lastRealtimeRevision =
            -1
    }

    if (
        pendingSecret != null &&
        pendingSecret?.matchId != code
    ) {
        pendingRollStore.clear()
        pendingSecret = null
    }

    matchId = code
    playerToken = token
    playerId = id
    matchInput.setText(code)

    persistSessionSecurely(
        code = code,
        id = id,
        token = token,
    )
    connectRealtimeIfPossible()
}
