package com.roxybasicneedbot.kdownloader.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.roxybasicneedbot.kdownloader.core.context.AndroidContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class PlatformNetworkMonitor {
    private val cm =
        AndroidContext.appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    actual fun observeConnectivity(): Flow<NetworkState> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(getCurrentState())
                    }

                    override fun onLost(network: Network) {
                        trySend(NetworkState.DISCONNECTED)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        trySend(getCurrentState())
                    }
                }

            val request =
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            cm.registerNetworkCallback(request, callback)
            trySend(getCurrentState())

            awaitClose {
                cm.unregisterNetworkCallback(callback)
            }
        }

    actual fun isConnected(): Boolean {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    actual fun isWifi(): Boolean {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun getCurrentState(): NetworkState {
        val capabilities =
            cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetworkState.DISCONNECTED
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkState.DISCONNECTED
        }
        return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            NetworkState.CONNECTED_WIFI
        } else {
            NetworkState.CONNECTED_CELLULAR
        }
    }
}
