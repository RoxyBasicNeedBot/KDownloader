package com.roxybasicneedbot.kdownloader.core.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual class PlatformNetworkMonitor {
    private val monitor = nw_path_monitor_create()
    private var isConnectedState = false
    private var isWifiState = false

    init {
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            isConnectedState = (status == nw_path_status_satisfied)
            isWifiState = nw_path_uses_interface_type(path, nw_interface_type_wifi)
        }
        nw_path_monitor_start(monitor)
    }

    actual fun observeConnectivity(): Flow<NetworkState> = callbackFlow {
        val pathMonitor = nw_path_monitor_create()
        
        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            val status = nw_path_get_status(path)
            val connected = status == nw_path_status_satisfied
            val wifi = nw_path_uses_interface_type(path, nw_interface_type_wifi)
            val cellular = nw_path_uses_interface_type(path, nw_interface_type_cellular)
            
            val state = when {
                !connected -> NetworkState.DISCONNECTED
                wifi -> NetworkState.CONNECTED_WIFI
                cellular -> NetworkState.CONNECTED_CELLULAR
                else -> NetworkState.CONNECTED_WIFI
            }
            trySend(state)
        }
        
        nw_path_monitor_set_queue(pathMonitor, dispatch_get_main_queue())
        nw_path_monitor_start(pathMonitor)
        
        trySend(if (isConnectedState) {
            if (isWifiState) NetworkState.CONNECTED_WIFI else NetworkState.CONNECTED_CELLULAR
        } else {
            NetworkState.DISCONNECTED
        })

        awaitClose {
            nw_path_monitor_cancel(pathMonitor)
        }
    }

    actual fun isConnected(): Boolean = isConnectedState

    actual fun isWifi(): Boolean = isWifiState
}
