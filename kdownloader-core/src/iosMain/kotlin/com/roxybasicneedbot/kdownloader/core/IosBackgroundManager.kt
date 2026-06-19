package com.roxybasicneedbot.kdownloader.core

import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid

object IosBackgroundManager {
    private var backgroundTaskId: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid

    fun beginBackgroundTask() {
        if (backgroundTaskId != UIBackgroundTaskInvalid) {
            endBackgroundTask()
        }

        backgroundTaskId = UIApplication.sharedApplication.beginBackgroundTaskWithName(
            taskName = "com.roxybasicneedbot.kdownloader.background_task",
            expirationHandler = {
                endBackgroundTask()
            }
        )
    }

    fun endBackgroundTask() {
        if (backgroundTaskId != UIBackgroundTaskInvalid) {
            UIApplication.sharedApplication.endBackgroundTask(backgroundTaskId)
            backgroundTaskId = UIBackgroundTaskInvalid
        }
    }
}
