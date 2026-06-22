@file:Suppress("LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught", "PrintStackTrace", "SwallowedException", "ReturnCount", "MaxLineLength", "MagicNumber")

package com.roxybasicneedbot.kdownloader.android.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.roxybasicneedbot.kdownloader.android.KDownloader
import com.roxybasicneedbot.kdownloader.android.persistence.KDownloaderDatabase
import com.roxybasicneedbot.kdownloader.android.persistence.entity.DownloadTaskEntity
import com.roxybasicneedbot.kdownloader.core.engine.DownloadEngine
import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import com.roxybasicneedbot.kdownloader.core.model.DownloadState
import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import com.roxybasicneedbot.kdownloader.core.network.HttpClientFactory
import com.roxybasicneedbot.kdownloader.core.network.PlatformNetworkMonitor
import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = KDownloaderDatabase.getInstance(context)
    private val taskDao = db.downloadTaskDao()

    companion object {
        const val KEY_DOWNLOAD_ID = "key_download_id"
        const val NOTIFICATION_ID = 9999
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val downloader = KDownloader.getInstance(applicationContext)
        val channelId = downloader.notificationConfig.channelId
        val icon = downloader.notificationConfig.smallIcon
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("KDownloader")
            .setContentText("Preparing background download...")
            .setSmallIcon(icon)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        
        // Mark worker as foreground first to avoid Background execution limits
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val taskEntity = taskDao.getTaskById(id) ?: return Result.failure()
        val request = taskEntity.toDownloadRequest()
        
        val fileStorage = PlatformFileStorage()
        val networkMonitor = PlatformNetworkMonitor()
        val httpClientFactory = HttpClientFactory()
        
        val config = DownloadConfig(
            connectionTimeoutMs = 15000L,
            readTimeoutMs = 15000L
        )
        
        val engine = DownloadEngine(config, fileStorage, networkMonitor, httpClientFactory)
        val notificationManager = KDownloader.getInstance(applicationContext).notificationManager

        var isDone = false
        var isFailed = false

        // Collect progress and states from engine, updating db and notifications
        val stateJob = CoroutineScope(Dispatchers.IO).launch {
            engine.states.collect { (stateId, state) ->
                if (stateId != id) return@collect
                
                // Update database
                val currentTask = taskDao.getTaskById(id) ?: return@collect
                val updatedTask = when (state) {
                    is DownloadState.Downloading -> {
                        currentTask.copy(
                            status = "DOWNLOADING",
                            downloadedBytes = state.progress.downloadedBytes,
                            totalBytes = state.progress.totalBytes
                        )
                    }
                    is DownloadState.Connecting -> currentTask.copy(status = "CONNECTING")
                    is DownloadState.Merging -> currentTask.copy(status = "MERGING")
                    is DownloadState.Verifying -> currentTask.copy(status = "VERIFYING")
                    is DownloadState.PostProcessing -> currentTask.copy(status = "POST_PROCESSING")
                    is DownloadState.Paused -> currentTask.copy(status = "PAUSED")
                    is DownloadState.WaitingForNetwork -> currentTask.copy(status = "WAITING_FOR_NETWORK")
                    is DownloadState.Done -> {
                        isDone = true
                        currentTask.copy(status = "DONE", completedAt = System.currentTimeMillis())
                    }
                    is DownloadState.Failed -> {
                        isFailed = true
                        currentTask.copy(status = "FAILED", errorMessage = state.error.message)
                    }
                    is DownloadState.Cancelled -> currentTask.copy(status = "CANCELLED")
                    else -> currentTask
                }
                taskDao.insertTask(updatedTask)

                // Update notification
                when (state) {
                    is DownloadState.Done -> {
                        notificationManager.cancelNotification(id)
                        notificationManager.showCompletedNotification(id, request.fileName)
                    }
                    is DownloadState.Failed -> {
                        notificationManager.cancelNotification(id)
                        notificationManager.showFailedNotification(id, request.fileName, state.error.message ?: "Unknown Error")
                    }
                    is DownloadState.Cancelled -> {
                        notificationManager.cancelNotification(id)
                    }
                    else -> {
                        notificationManager.showProgressNotification(id, request.fileName, state)
                    }
                }
            }
        }

        try {
            engine.start(request)
            
            // Wait for completion or failure
            while (!isDone && !isFailed && !isStopped) {
                delay(200)
            }
            
            if (isStopped) {
                engine.stop(id)
                val currentTask = taskDao.getTaskById(id)
                if (currentTask != null) {
                    taskDao.insertTask(currentTask.copy(status = "PAUSED"))
                }
                notificationManager.showProgressNotification(id, request.fileName, DownloadState.Paused)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val currentTask = taskDao.getTaskById(id)
            if (currentTask != null) {
                taskDao.insertTask(currentTask.copy(status = "FAILED", errorMessage = e.message))
            }
            notificationManager.showFailedNotification(id, request.fileName, e.message ?: "Unknown Error")
            isFailed = true
        } finally {
            stateJob.cancel()
        }

        return if (isDone) {
            Result.success()
        } else if (isFailed) {
            Result.failure()
        } else {
            Result.retry()
        }
    }
}

private fun DownloadTaskEntity.toDownloadRequest(): DownloadRequest {
    return DownloadRequest(
        id = id,
        url = url,
        destinationDir = destinationDir,
        fileName = fileName,
        priority = try { DownloadPriority.valueOf(priority) } catch (e: Exception) { DownloadPriority.NORMAL },
        chunkCount = chunkCount,
        headers = headers.map,
        wifiOnly = wifiOnly,
        speedLimit = speedLimit,
        mirrorUrls = mirrorUrls.list,
        hashAlgorithm = hashAlgorithm,
        expectedHash = expectedHash,
        scheduleAt = scheduleAt,
        groupTag = groupTag
    )
}

