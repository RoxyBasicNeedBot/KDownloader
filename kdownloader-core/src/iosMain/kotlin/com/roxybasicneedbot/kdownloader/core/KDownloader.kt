package com.roxybasicneedbot.kdownloader.core

import com.roxybasicneedbot.kdownloader.core.engine.DownloadEngine
import com.roxybasicneedbot.kdownloader.core.model.*
import com.roxybasicneedbot.kdownloader.core.network.HttpClientFactory
import com.roxybasicneedbot.kdownloader.core.network.PlatformNetworkMonitor
import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.objc.objc_sync_enter
import platform.objc.objc_sync_exit
import platform.Foundation.NSURL
import platform.Foundation.NSFileManager

interface DownloadListener {
    fun onTasksUpdated(tasks: List<Map<String, Any?>>)
}

class KDownloader private constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val config = DownloadConfig()
    private val fileStorage = PlatformFileStorage()
    private val networkMonitor = PlatformNetworkMonitor()
    private val httpClientFactory = HttpClientFactory()

    private val engine = DownloadEngine(
        config = config,
        fileStorage = fileStorage,
        networkMonitor = networkMonitor,
        httpClientFactory = httpClientFactory
    )

    private val _tasks = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val tasks: StateFlow<Map<String, DownloadState>> = _tasks.asStateFlow()

    private val listeners = mutableListOf<DownloadListener>()
    private val pendingRequests = mutableMapOf<String, DownloadRequest>()

    init {
        engine.states
            .onEach { (id, state) ->
                val current = _tasks.value.toMutableMap()
                current[id] = state
                _tasks.value = current
                notifyListeners()

                val hasActive = current.values.any {
                    it is DownloadState.Downloading ||
                    it is DownloadState.Connecting ||
                    it is DownloadState.Merging
                }
                if (hasActive) {
                    IosBackgroundManager.beginBackgroundTask()
                } else {
                    IosBackgroundManager.endBackgroundTask()
                }
            }
            .launchIn(scope)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun registerListener(listener: DownloadListener) {
        objc_sync_enter(listeners)
        try {
            listeners.add(listener)
        } finally {
            objc_sync_exit(listeners)
        }
        listener.onTasksUpdated(getSerializedTasks())
    }

    @OptIn(ExperimentalForeignApi::class)
    fun unregisterListener(listener: DownloadListener) {
        objc_sync_enter(listeners)
        try {
            listeners.remove(listener)
        } finally {
            objc_sync_exit(listeners)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun notifyListeners() {
        val list = getSerializedTasks()
        objc_sync_enter(listeners)
        try {
            listeners.forEach { it.onTasksUpdated(list) }
        } finally {
            objc_sync_exit(listeners)
        }
    }

    private fun getSerializedTasks(): List<Map<String, Any?>> {
        return _tasks.value.map { (id, state) -> state.toMap(id) }
    }

    fun enqueue(
        id: String,
        url: String,
        destinationDir: String,
        fileName: String,
        priority: String,
        chunkCount: Int,
        headers: Map<String, String>,
        wifiOnly: Boolean,
        speedLimit: Long,
        mirrorUrls: List<String>,
        hashAlgorithm: String?,
        expectedHash: String?,
        scheduleAt: Long?,
        groupTag: String?
    ): String {
        val request = DownloadRequest(
            id = id,
            url = url,
            destinationDir = destinationDir,
            fileName = fileName,
            priority = DownloadPriority.entries.firstOrNull { it.name.equals(priority, ignoreCase = true) } ?: DownloadPriority.NORMAL,
            chunkCount = chunkCount,
            headers = headers,
            wifiOnly = wifiOnly,
            speedLimit = speedLimit,
            mirrorUrls = mirrorUrls,
            hashAlgorithm = hashAlgorithm,
            expectedHash = expectedHash,
            scheduleAt = scheduleAt,
            groupTag = groupTag
        )

        val current = _tasks.value.toMutableMap()
        current[id] = DownloadState.Queued
        _tasks.value = current
        notifyListeners()

        pendingRequests[id] = request

        if (groupTag == "background") {
            NativeIosDownloader.startDownload(id, url)
        } else {
            scope.launch {
                try {
                    engine.start(request)
                } catch (e: Exception) {
                    val fail = _tasks.value.toMutableMap()
                    fail[id] = DownloadState.Failed(DownloadError(ErrorCode.UNKNOWN, e.message ?: "Failed"), 0)
                    _tasks.value = fail
                    notifyListeners()
                }
            }
        }
        return id
    }

    internal fun updateProgress(id: String, downloaded: Long, total: Long) {
        val current = _tasks.value.toMutableMap()
        current[id] = DownloadState.Downloading(
            DownloadProgress(
                downloadedBytes = downloaded,
                totalBytes = total,
                percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0,
                speedBytesPerSec = 0,
                speedFormatted = "0 B/s",
                etaSeconds = 0,
                etaFormatted = "0s",
                activeChunks = 1,
                totalChunks = 1,
                chunkProgress = emptyList()
            )
        )
        _tasks.value = current
        notifyListeners()
    }

    @OptIn(ExperimentalForeignApi::class)
    internal fun completeDownload(id: String, tempUrl: NSURL) {
        val request = pendingRequests[id]
        if (request != null) {
            val fileManager = NSFileManager.defaultManager()
            val destPath = "${request.destinationDir}/${request.fileName}"
            val destUrl = NSURL.fileURLWithPath(destPath)
            if (fileManager.fileExistsAtPath(destPath)) {
                fileManager.removeItemAtURL(destUrl, null)
            }
            val success = fileManager.moveItemAtURL(tempUrl, destUrl, null)
            val current = _tasks.value.toMutableMap()
            if (success) {
                current[id] = DownloadState.Done(
                    DownloadResult(
                        id = id,
                        filePath = destPath,
                        totalBytes = request.speedLimit,
                        downloadTimeMs = 0L,
                        averageSpeedBytesPerSec = 0L,
                        hashVerified = false
                    )
                )
            } else {
                current[id] = DownloadState.Failed(DownloadError(ErrorCode.WRITE_ERROR, "Failed to move native downloaded file"), 0)
            }
            _tasks.value = current
            notifyListeners()
        }
    }

    internal fun failDownload(id: String, error: String) {
        val current = _tasks.value.toMutableMap()
        current[id] = DownloadState.Failed(DownloadError(ErrorCode.UNKNOWN, error), 0)
        _tasks.value = current
        notifyListeners()
    }

    fun pause(id: String) {
        val request = pendingRequests[id]
        if (request?.groupTag == "background") {
            NativeIosDownloader.cancelDownload(id)
            val current = _tasks.value.toMutableMap()
            current[id] = DownloadState.Paused
            _tasks.value = current
            notifyListeners()
        } else {
            scope.launch {
                engine.stop(id)
            }
        }
    }

    fun resume(id: String) {
        val currentTask = _tasks.value[id]
        if (currentTask is DownloadState.Paused || currentTask is DownloadState.Failed) {
            val request = pendingRequests[id]
            val current = _tasks.value.toMutableMap()
            current[id] = DownloadState.Queued
            _tasks.value = current
            notifyListeners()

            if (request != null) {
                if (request.groupTag == "background") {
                    NativeIosDownloader.startDownload(id, request.url)
                } else {
                    scope.launch {
                        try {
                            engine.start(request)
                        } catch (e: Exception) {
                            val fail = _tasks.value.toMutableMap()
                            fail[id] = DownloadState.Failed(DownloadError(ErrorCode.UNKNOWN, e.message ?: "Failed"), 0)
                            _tasks.value = fail
                            notifyListeners()
                        }
                    }
                }
            }
        }
    }

    fun cancel(id: String) {
        val request = pendingRequests[id]
        if (request?.groupTag == "background") {
            NativeIosDownloader.cancelDownload(id)
            val current = _tasks.value.toMutableMap()
            current.remove(id)
            _tasks.value = current
            notifyListeners()
        } else {
            scope.launch {
                engine.stop(id)
                val current = _tasks.value.toMutableMap()
                current.remove(id)
                _tasks.value = current
                notifyListeners()
            }
        }
    }

    companion object {
        val instance = KDownloader()
    }
}

private fun DownloadState.toMap(id: String): Map<String, Any?> {
    val statusStr = when (this) {
        is DownloadState.Idle -> "IDLE"
        is DownloadState.Queued -> "QUEUED"
        is DownloadState.Scheduled -> "SCHEDULED"
        is DownloadState.Connecting -> "CONNECTING"
        is DownloadState.Downloading -> "DOWNLOADING"
        is DownloadState.Paused -> "PAUSED"
        is DownloadState.WaitingForNetwork -> "WAITING_FOR_NETWORK"
        is DownloadState.Merging -> "MERGING"
        is DownloadState.PostProcessing -> "POST_PROCESSING"
        is DownloadState.Verifying -> "VERIFYING"
        is DownloadState.Done -> "DONE"
        is DownloadState.Failed -> "FAILED"
        is DownloadState.Cancelled -> "CANCELLED"
    }

    val progressMap = if (this is DownloadState.Downloading) {
        val p = this.progress
        mapOf(
            "downloadedBytes" to p.downloadedBytes,
            "totalBytes" to p.totalBytes,
            "percent" to p.percent,
            "speedBytesPerSec" to p.speedBytesPerSec,
            "speedFormatted" to p.speedFormatted,
            "etaSeconds" to p.etaSeconds,
            "etaFormatted" to p.etaFormatted
        )
    } else null

    val errorMsg = if (this is DownloadState.Failed) {
        this.error.message
    } else null

    return mapOf(
        "id" to id,
        "status" to statusStr,
        "progress" to progressMap,
        "errorMessage" to errorMsg
    )
}
