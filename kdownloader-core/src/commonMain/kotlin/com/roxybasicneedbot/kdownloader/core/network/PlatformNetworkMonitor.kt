package com.roxybasicneedbot.kdownloader.core.network

import kotlinx.coroutines.flow.Flow

enum class NetworkState {
    CONNECTED_WIFI,
    CONNECTED_CELLULAR,
    DISCONNECTED
}

expect class PlatformNetworkMonitor {
    fun observeConnectivity(): Flow<NetworkState>
    fun isConnected(): Boolean
    fun isWifi(): Boolean
}
