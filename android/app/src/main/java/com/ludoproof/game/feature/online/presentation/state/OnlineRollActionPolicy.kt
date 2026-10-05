package com.ludoproof.game.feature.online

enum class OnlineRollActionKind {
    NEW_ROLL,
    RESUME_COMMIT_THEN_REVEAL,
    RESUME_REVEAL_ONLY,
    WAIT_FOR_RECOVERY,
    MOVE_REQUIRED,
    DISABLED,
}

data class OnlineRollActionDecision(
    val kind: OnlineRollActionKind,
    val enabled: Boolean,
    val label: String,
)

/**
 * Fail-closed presentation/action policy for EntroNex commit/reveal recovery.
 *
 * A server-side pending roll belongs to the exact client commitment that opened
 * it. Android may resume that roll only when the locally protected seed matches
 * the authoritative commitment. Otherwise it waits for server recovery/timeout
 * instead of attempting a second commitment for the same turn.
 */
object OnlineRollActionPolicy {
    fun resolve(
        isOnline: Boolean,
        matchStatus: String,
        myTurn: Boolean,
        pendingStatus: String?,
        remoteClientCommitment: String?,
        currentMatchId: String?,
        localSecretMatchId: String?,
        localClientCommitment: String?,
    ): OnlineRollActionDecision {
        if (matchStatus != "ACTIVE" || !myTurn) {
            return disabled()
        }
        if (!isOnline) {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.DISABLED,
                enabled = false,
                label = "OFFLINE • WAITING",
            )
        }

        if (pendingStatus == null) {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.NEW_ROLL,
                enabled = true,
                label = "ROLL VERIFIED DICE",
            )
        }

        if (pendingStatus == "RESOLVED") {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.MOVE_REQUIRED,
                enabled = false,
                label = "MOVE A TOKEN",
            )
        }

        val matchingSecret =
            currentMatchId != null &&
                localSecretMatchId == currentMatchId &&
                !localClientCommitment.isNullOrBlank() &&
                !remoteClientCommitment.isNullOrBlank() &&
                localClientCommitment == remoteClientCommitment

        if (!matchingSecret) {
            return OnlineRollActionDecision(
                kind = OnlineRollActionKind.WAIT_FOR_RECOVERY,
                enabled = false,
                label = "WAITING FOR ROLL RECOVERY",
            )
        }

        return when (pendingStatus) {
            "CREATING" ->
                OnlineRollActionDecision(
                    kind = OnlineRollActionKind.RESUME_COMMIT_THEN_REVEAL,
                    enabled = true,
                    label = "RESUME VERIFIED ROLL",
                )

            "COMMITTED",
            "RESOLVING",
            ->
                OnlineRollActionDecision(
                    kind = OnlineRollActionKind.RESUME_REVEAL_ONLY,
                    enabled = true,
                    label = "RESUME VERIFIED ROLL",
                )

            else ->
                OnlineRollActionDecision(
                    kind = OnlineRollActionKind.WAIT_FOR_RECOVERY,
                    enabled = false,
                    label = "WAITING FOR ROLL RECOVERY",
                )
        }
    }

    private fun disabled() =
        OnlineRollActionDecision(
            kind = OnlineRollActionKind.DISABLED,
            enabled = false,
            label = "ROLL VERIFIED DICE",
        )
}
