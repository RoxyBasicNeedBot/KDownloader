package com.roxybasicneedbot.kdownloader.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roxybasicneedbot.kdownloader.android.persistence.KDownloaderDatabase
import com.roxybasicneedbot.kdownloader.android.worker.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = KDownloaderDatabase.getInstance(context)
            val taskDao = db.downloadTaskDao()
            val scheduler = WorkManagerScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val activeStatuses = listOf(
                    "DOWNLOADING", "QUEUED", "CONNECTING", 
                    "MERGING", "POST_PROCESSING", "WAITING_FOR_NETWORK"
                )
                
                activeStatuses.forEach { status ->
                    val tasks = taskDao.getTasksByStatus(status)
                    tasks.forEach { task ->
                        // Mark status as queued in DB, then reschedule
                        taskDao.insertTask(task.copy(status = "QUEUED"))
                        scheduler.scheduleDownload(task.id, task.wifiOnly)
                    }
                }
            }
        }
    }
}
