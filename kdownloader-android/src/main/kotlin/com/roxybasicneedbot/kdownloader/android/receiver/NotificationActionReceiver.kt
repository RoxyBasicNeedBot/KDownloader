@file:Suppress("TooGenericExceptionCaught")

package com.roxybasicneedbot.kdownloader.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roxybasicneedbot.kdownloader.android.KDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val downloader = KDownloader.getInstance(context)
                when (intent.action) {
                    ACTION_PAUSE -> downloader.pause(downloadId)
                    ACTION_RESUME -> downloader.resume(downloadId)
                    ACTION_CANCEL -> downloader.cancel(downloadId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.roxybasicneedbot.kdownloader.ACTION_PAUSE"
        const val ACTION_RESUME = "com.roxybasicneedbot.kdownloader.ACTION_RESUME"
        const val ACTION_CANCEL = "com.roxybasicneedbot.kdownloader.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}
