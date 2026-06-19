package com.roxybasicneedbot.kdownloader.android

import android.content.Context
import com.roxybasicneedbot.kdownloader.android.notification.DownloadNotificationConfig
import com.roxybasicneedbot.kdownloader.android.notification.DownloadNotificationManager
import com.roxybasicneedbot.kdownloader.android.persistence.KDownloaderDatabase
import com.roxybasicneedbot.kdownloader.android.persistence.entity.DownloadTaskEntity
import com.roxybasicneedbot.kdownloader.android.persistence.entity.HeadersWrapper
import com.roxybasicneedbot.kdownloader.android.persistence.entity.MirrorUrlsWrapper
import com.roxybasicneedbot.kdownloader.android.worker.WorkManagerScheduler
import com.roxybasicneedbot.kdownloader.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KDownloader private constructor(private val context: Context) {
    private val db = KDownloaderDatabase.getInstance(context)
    private val taskDao = db.downloadTaskDao()
    private val scheduler = WorkManagerScheduler(context)

    var notificationConfig = DownloadNotificationConfig()
        private set

    internal var notificationManager = DownloadNotificationManager(context, notificationConfig)
        private set

    fun setNotificationConfig(config: DownloadNotificationConfig) {
        notificationConfig = config
        notificationManager = DownloadNotificationManager(context, config)
    }

    suspend fun enqueue(request: DownloadRequest): String {
        val entity = DownloadTaskEntity(
            id = request.id,
            url = request.url,
            destinationDir = request.destinationDir,
            fileName = request.fileName,
            priority = request.priority.name,
            chunkCount = request.chunkCount,
            headers = HeadersWrapper(request.headers),
            wifiOnly = request.wifiOnly,
            speedLimit = request.speedLimit,
            mirrorUrls = MirrorUrlsWrapper(request.mirrorUrls),
            hashAlgorithm = request.hashAlgorithm,
            expectedHash = request.expectedHash,
            scheduleAt = request.scheduleAt,
            groupTag = request.groupTag,
            status = "QUEUED",
            downloadedBytes = 0L,
            totalBytes = -1L,
            errorMessage = null,
            errorCode = null,
            completedAt = null
        )
        taskDao.insertTask(entity)
        scheduler.scheduleDownload(request.id, request.wifiOnly)
        return request.id
    }

    suspend fun pause(id: String) {
        scheduler.cancelDownload(id)
        val task = taskDao.getTaskById(id)
        if (task != null) {
            taskDao.insertTask(task.copy(status = "PAUSED"))
            notificationManager.showProgressNotification(id, task.fileName, DownloadState.Paused)
        }
    }

    suspend fun resume(id: String) {
        val task = taskDao.getTaskById(id)
        if (task != null) {
            taskDao.insertTask(task.copy(status = "QUEUED"))
            scheduler.scheduleDownload(id, task.wifiOnly)
        }
    }

    suspend fun cancel(id: String) {
        scheduler.cancelDownload(id)
        val task = taskDao.getTaskById(id)
        if (task != null) {
            taskDao.deleteTaskById(id)
            notificationManager.cancelNotification(id)
        }
    }

    fun observe(id: String): Flow<DownloadState> {
        return taskDao.observeTaskById(id).map { entity ->
            entity?.toDownloadState() ?: DownloadState.Idle
        }
    }

    fun observeAll(): Flow<List<DownloadTaskEntity>> {
        return taskDao.observeAllTasks()
    }

    companion object {
        @Volatile
        private var INSTANCE: KDownloader? = null

        fun getInstance(context: Context): KDownloader {
            return INSTANCE ?: synchronized(this) {
                val instance = KDownloader(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

private fun DownloadTaskEntity.toDownloadState(): DownloadState {
    return when (status) {
        "IDLE" -> DownloadState.Idle
        "QUEUED" -> DownloadState.Queued
        "CONNECTING" -> DownloadState.Connecting
        "DOWNLOADING" -> DownloadState.Downloading(
            DownloadProgress(
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,
                speedBytesPerSec = 0,
                speedFormatted = "0 B/s",
                etaSeconds = 0,
                etaFormatted = "0s",
                activeChunks = chunkCount,
                totalChunks = chunkCount,
                chunkProgress = emptyList()
            )
        )
        "PAUSED" -> DownloadState.Paused
        "WAITING_FOR_NETWORK" -> DownloadState.WaitingForNetwork
        "MERGING" -> DownloadState.Merging
        "POST_PROCESSING" -> DownloadState.PostProcessing("Post processing...")
        "VERIFYING" -> DownloadState.Verifying(hashAlgorithm ?: "SHA-256")
        "DONE" -> DownloadState.Done(
            DownloadResult(
                id = id,
                filePath = "$destinationDir/$fileName",
                totalBytes = totalBytes,
                downloadTimeMs = completedAt?.minus(createdAt) ?: 0L,
                averageSpeedBytesPerSec = 0L,
                hashVerified = expectedHash != null
            )
        )
        "FAILED" -> {
            val errCode = when (errorCode) {
                1 -> ErrorCode.NETWORK_ERROR
                2 -> ErrorCode.SERVER_ERROR
                3 -> ErrorCode.DISK_FULL
                4 -> ErrorCode.WRITE_ERROR
                else -> ErrorCode.UNKNOWN
            }
            DownloadState.Failed(DownloadError(errCode, errorMessage ?: "Download failed"), 0)
        }
        "CANCELLED" -> DownloadState.Cancelled
        else -> DownloadState.Idle
    }
}
