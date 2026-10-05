package com.ludoproof.game

internal enum class OnlineStateSource {
    AUTHORITATIVE,
    CACHE,
}

internal enum class PendingRollSecretAction {
    KEEP,
    CLEAR,
}

/**
 * Fail-safe recovery policy for a locally encrypted client seed.
 *
 * Cached state is presentation-only and may lag the server, so it must never
 * destroy the seed needed to reveal an already committed roll. Only a fresh
 * authoritative server snapshot may decide that the local secret is stale.
 */
internal object OnlinePendingRollRecoveryPolicy {
    fun action(
        source: OnlineStateSource,
        secret: PendingRollSecret,
        state: MatchSnapshot,
    ): PendingRollSecretAction {
        if (source == OnlineStateSource.CACHE) {
            return PendingRollSecretAction.KEEP
        }

        if (secret.matchId != state.matchId) {
            return PendingRollSecretAction.CLEAR
        }

        if (state.status != "ACTIVE") {
            return PendingRollSecretAction.CLEAR
        }

        val pending = state.pendingRoll
            ?: return PendingRollSecretAction.CLEAR

        if (pending.status == "RESOLVED") {
            return PendingRollSecretAction.CLEAR
        }

        val remoteCommitment = pending.clientCommitment
        if (
            remoteCommitment != null &&
            remoteCommitment != secret.clientCommitment
        ) {
            return PendingRollSecretAction.CLEAR
        }

        return PendingRollSecretAction.KEEP
    }
}

internal object OnlineCachedStateRestorePolicy {
    fun shouldApply(
        sessionMatchId: String?,
        cachedMatchId: String?,
    ): Boolean =
        !sessionMatchId.isNullOrBlank() &&
            sessionMatchId == cachedMatchId
}
