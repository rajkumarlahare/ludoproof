package com.ludoproof.game

internal data class OnlineGameUiState(
    val matchId: String? = null,
    val playerToken: String? = null,
    val playerId: String? = null,
    val currentState: MatchSnapshot? = null,
    val pendingSecret: PendingRollSecret? = null,
    val isOnline: Boolean = false,
    val presentedDiceEventKey: String? = null,
)

internal class OnlineGameStateHolder(
    initial: OnlineGameUiState = OnlineGameUiState(),
) {
    var value: OnlineGameUiState = initial
        private set

    fun update(transform: (OnlineGameUiState) -> OnlineGameUiState) {
        value = transform(value)
    }
}
