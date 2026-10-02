package com.ludoproof.game

internal data class OfflineSetupUiState(
    val selectedPlayers: Int = 2,
    val selectedColor: String = "BLUE",
)

internal class OfflineSetupStateHolder(
    initial: OfflineSetupUiState = OfflineSetupUiState(),
) {
    var value: OfflineSetupUiState = initial
        private set

    fun update(transform: (OfflineSetupUiState) -> OfflineSetupUiState) {
        value = transform(value)
    }
}
