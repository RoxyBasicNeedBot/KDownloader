package com.roxybasicneedbot.kdownloader.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roxybasicneedbot.kdownloader.android.receiver.NotificationActionReceiver
import com.roxybasicneedbot.kdownloader.core.model.DownloadProgress
import com.roxybasicneedbot.kdownloader.core.model.DownloadState

class DownloadNotificationManager(
    private val context: Context,
    private val config: DownloadNotificationConfig
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = config.channelDescription
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(id: String, fileName: String, state: DownloadState) {
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(config.smallIcon)
            .setContentTitle(fileName)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        // Add actions
        when (state) {
            is DownloadState.Downloading -> {
                val progress = state.progress
                val speedText = if (config.enableSpeed) " | ${progress.speedFormatted}" else ""
                val etaText = " | ETA: ${progress.etaFormatted}"
                
                builder.setContentText("${progress.percent}%$speedText$etaText")
                builder.setProgress(100, progress.percent, progress.totalBytes <= 0)
                
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    createActionPendingIntent(id, NotificationActionReceiver.ACTION_PAUSE)
                )
            }
            is DownloadState.Connecting -> {
                builder.setContentText("Connecting...")
                builder.setProgress(100, 0, true)
            }
            is DownloadState.Merging -> {
                builder.setContentText("Merging files...")
                builder.setProgress(100, 0, true)
            }
            is DownloadState.Verifying -> {
                builder.setContentText("Verifying checksum...")
                builder.setProgress(100, 0, true)
            }
            is DownloadState.PostProcessing -> {
                builder.setContentText("Post processing...")
                builder.setProgress(100, 0, true)
            }
            is DownloadState.Paused -> {
                builder.setContentText("Paused")
                builder.setProgress(100, 0, false)
                builder.setOngoing(false)
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    "Resume",
                    createActionPendingIntent(id, NotificationActionReceiver.ACTION_RESUME)
                )
            }
            is DownloadState.WaitingForNetwork -> {
                builder.setContentText("Waiting for network...")
                builder.setProgress(100, 0, true)
            }
            else -> return
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Cancel",
            createActionPendingIntent(id, NotificationActionReceiver.ACTION_CANCEL)
        )

        try {
            notificationManager.notify(id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Handle missing notification permission on API 33+
        }
    }

    fun showCompletedNotification(id: String, fileName: String) {
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(config.smallIcon)
            .setContentTitle(fileName)
            .setContentText("Download completed")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)

        try {
            notificationManager.notify(id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Handle missing notification permission
        }
    }

    fun showFailedNotification(id: String, fileName: String, errorMsg: String) {
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(config.smallIcon)
            .setContentTitle(fileName)
            .setContentText("Download failed: $errorMsg")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Retry",
                createActionPendingIntent(id, NotificationActionReceiver.ACTION_RESUME)
            )

        try {
            notificationManager.notify(id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Handle missing notification permission
        }
    }

    fun cancelNotification(id: String) {
        notificationManager.cancel(id.hashCode())
    }

    private fun createActionPendingIntent(id: String, actionStr: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = actionStr
            putExtra(NotificationActionReceiver.EXTRA_DOWNLOAD_ID, id)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            (id + actionStr).hashCode(),
            intent,
            flags
        )
    }
}
