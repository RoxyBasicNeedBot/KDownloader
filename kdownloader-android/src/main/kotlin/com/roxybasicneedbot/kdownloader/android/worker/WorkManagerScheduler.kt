package com.roxybasicneedbot.kdownloader.android.worker

import android.content.Context
import androidx.work.*

class WorkManagerScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleDownload(id: String, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_DOWNLOAD_ID to id))
            .setConstraints(constraints)
            .addTag(id)
            .build()

        workManager.enqueueUniqueWork(
            id,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelDownload(id: String) {
        workManager.cancelUniqueWork(id)
    }
}
