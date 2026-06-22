package com.roxybasicneedbot.kdownloader.desktop

import com.roxybasicneedbot.kdownloader.core.engine.DownloadEngine
import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage
import com.roxybasicneedbot.kdownloader.core.network.PlatformNetworkMonitor
import com.roxybasicneedbot.kdownloader.core.network.HttpClientFactory
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import com.roxybasicneedbot.kdownloader.core.model.DownloadState
import com.roxybasicneedbot.kdownloader.desktop.notification.DesktopNotificationManager
import com.roxybasicneedbot.kdownloader.desktop.persistence.DesktopPersistenceManager
import com.roxybasicneedbot.kdownloader.desktop.persistence.DownloadTaskEntity
import com.roxybasicneedbot.kdownloader.desktop.persistence.HeadersWrapper
import com.roxybasicneedbot.kdownloader.desktop.persistence.MirrorUrlsWrapper
import com.roxybasicneedbot.kdownloader.desktop.ui.SystemTrayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class KDownloaderDesktop private constructor() {

    private val persistenceManager = DesktopPersistenceManager()
    private val notificationManager = DesktopNotificationManager()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeTasks = ConcurrentHashMap<String, Job>()

    init {
        SystemTrayManager.initialize()
        SystemTrayManager.onPauseAllClicked = {
            scope.launch {
                persistenceManager.getAllTasks().forEach { task ->
                    if (task.status == "DOWNLOADING" || task.status == "QUEUED") {
                        pause(task.id)
                    }
                }
            }
        }
        SystemTrayManager.onResumeAllClicked = {
            scope.launch {
                persistenceManager.getAllTasks().forEach { task ->
                    if (task.status == "PAUSED") {
                        resume(task.id)
                    }
                }
            }
        }
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
        persistenceManager.insertTask(entity)
        startDownload(request)
        return request.id
    }

    private fun startDownload(request: DownloadRequest) {
        val engine = DownloadEngine(
            DownloadConfig(),
            PlatformFileStorage(),
            PlatformNetworkMonitor(),
            HttpClientFactory()
        )
        
        val job = scope.launch {
            engine.states.collect { (_, state) ->
                notificationManager.showProgressNotification(request.id, request.fileName, state)
                // In a real implementation, you would also update the SQLite database with the current state and progress
                if (state is DownloadState.Done || state is DownloadState.Failed || state is DownloadState.Cancelled) {
                    activeTasks.remove(request.id)
                }
            }
        }
        activeTasks[request.id] = job
        scope.launch { engine.start(request) }
    }

    suspend fun pause(id: String) {
        activeTasks[id]?.cancel()
        activeTasks.remove(id)
        val task = persistenceManager.getTaskById(id)
        if (task != null) {
            persistenceManager.insertTask(task.copy(status = "PAUSED"))
        }
    }

    suspend fun resume(id: String) {
        val task = persistenceManager.getTaskById(id)
        if (task != null) {
            persistenceManager.insertTask(task.copy(status = "QUEUED"))
            // We need to map DownloadTaskEntity back to DownloadRequest to restart
            val request = DownloadRequest(
                id = task.id,
                url = task.url,
                destinationDir = task.destinationDir,
                fileName = task.fileName,
                chunkCount = task.chunkCount,
                wifiOnly = task.wifiOnly,
                speedLimit = task.speedLimit,
                hashAlgorithm = task.hashAlgorithm,
                expectedHash = task.expectedHash,
                scheduleAt = task.scheduleAt,
                groupTag = task.groupTag
            )
            startDownload(request)
        }
    }

    suspend fun cancel(id: String) {
        activeTasks[id]?.cancel()
        activeTasks.remove(id)
        val task = persistenceManager.getTaskById(id)
        if (task != null) {
            persistenceManager.deleteTaskById(id)
            notificationManager.cancelNotification(id)
        }
    }

    fun observeAll(): Flow<List<DownloadTaskEntity>> {
        return persistenceManager.observeAllTasks()
    }

    companion object {
        @Volatile
        private var INSTANCE: KDownloaderDesktop? = null

        fun getInstance(): KDownloaderDesktop {
            return INSTANCE ?: synchronized(this) {
                val instance = KDownloaderDesktop()
                INSTANCE = instance
                instance
            }
        }
    }
}
