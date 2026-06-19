package com.roxybasicneedbot.kdownloader.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.net.InetAddress

actual class PlatformNetworkMonitor {
    actual fun observeConnectivity(): Flow<NetworkState> = flow {
        var lastState = getCurrentState()
        emit(lastState)
        while (true) {
            delay(3000)
            val currentState = getCurrentState()
            if (currentState != lastState) {
                emit(currentState)
                lastState = currentState
            }
        }
    }

    actual fun isConnected(): Boolean {
        return try {
            val address = InetAddress.getByName("8.8.8.8")
            address.isReachable(2000)
        } catch (e: Exception) {
            false
        }
    }

    actual fun isWifi(): Boolean {
        return isConnected()
    }

    private fun getCurrentState(): NetworkState {
        return if (isConnected()) NetworkState.CONNECTED_WIFI else NetworkState.DISCONNECTED
    }
}
