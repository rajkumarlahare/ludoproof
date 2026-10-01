package com.ludoproof.game

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class ConnectivityMonitor(
    context: Context,
    private val onChanged: (Boolean) -> Unit,
) {
    private val manager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile private var registered = false

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (registered) onChanged(isOnline())
            }

            override fun onLost(network: Network) {
                if (registered) onChanged(isOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                if (!registered) return
                onChanged(
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET,
                    ) &&
                        networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                        ),
                )
            }
        }

    fun isOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities =
            manager.getNetworkCapabilities(network)
                ?: return false
        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
        ) &&
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            )
    }

    fun start() {
        if (registered) return
        registered = true
        runCatching {
            manager.registerDefaultNetworkCallback(callback)
        }.onFailure {
            registered = false
        }
        onChanged(isOnline())
    }

    fun stop() {
        if (!registered) return
        registered = false
        runCatching {
            manager.unregisterNetworkCallback(callback)
        }
    }
}
