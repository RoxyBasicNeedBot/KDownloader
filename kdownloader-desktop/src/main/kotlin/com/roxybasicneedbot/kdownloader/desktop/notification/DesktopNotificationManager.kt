/* ktlint-disable */
@file:Suppress("UnusedParameter")

package com.roxybasicneedbot.kdownloader.desktop.notification

import com.roxybasicneedbot.kdownloader.core.model.DownloadState
import com.roxybasicneedbot.kdownloader.desktop.ui.SystemTrayManager

class DesktopNotificationManager {

    fun showProgressNotification(id: String, fileName: String, state: DownloadState) {
        // AWT System Tray does not support continuous progress updates well without spamming the user.
        // We will only show notifications for significant events (Done, Failed).
        when (state) {
            is DownloadState.Done -> {
                SystemTrayManager.showNotification(
                    title = "Download Complete",
                    message = fileName,
                    isError = false
                )
            }
            is DownloadState.Failed -> {
                SystemTrayManager.showNotification(
                    title = "Download Failed",
                    message = "$fileName: ${state.error.message}",
                    isError = true
                )
            }
            else -> {
                // Ignore continuous progress updates
            }
        }
    }

    fun cancelNotification(id: String) {
        // AWT tray doesn't let you cancel a specific popup notification easily.
    }
}
