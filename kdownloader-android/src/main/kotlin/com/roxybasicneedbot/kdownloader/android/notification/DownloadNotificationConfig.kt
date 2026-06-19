package com.roxybasicneedbot.kdownloader.android.notification

data class DownloadNotificationConfig(
    val channelId: String = "kdownloader_channel",
    val channelName: String = "Downloads",
    val channelDescription: String = "Notifications for active downloads",
    val smallIcon: Int = android.R.drawable.stat_sys_download, // default fallback
    val enableProgress: Boolean = true,
    val enableSpeed: Boolean = true
)
