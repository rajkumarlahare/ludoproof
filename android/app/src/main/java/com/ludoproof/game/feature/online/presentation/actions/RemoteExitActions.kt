package com.ludoproof.game.feature.online

import android.os.Build
import android.window.OnBackInvokedDispatcher
import com.ludoproof.game.MainActivity

internal fun MainActivity.registerRemoteBackHandling() {
    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        onBackInvokedDispatcher
            .registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) {
                abandonRemoteSessionAndFinish()
            }
    }
}

internal fun MainActivity.abandonRemoteSessionAndFinish() {
    publicMatchmakingStore.clear()
    secureSessionStore.clear()
    pendingRollStore.clear()
    cachedMatchStore.clear()

    matchId = null
    playerToken = null
    playerId = null
    currentState = null
    pendingSecret = null

    realtimeConnected = false
    realtimeClient.disconnect()
    finish()
}
