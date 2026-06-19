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

    init {
        engine.states
            .onEach { (id, state) ->
                val current = _tasks.value.toMutableMap()
                current[id] = state
                _tasks.value = current
                notifyListeners()
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
        return id
    }

    fun pause(id: String) {
        scope.launch {
            engine.stop(id)
        }
    }

    fun resume(id: String) {
        val currentTask = _tasks.value[id]
        if (currentTask is DownloadState.Paused || currentTask is DownloadState.Failed) {
            val current = _tasks.value.toMutableMap()
            current[id] = DownloadState.Queued
            _tasks.value = current
            notifyListeners()
        }
    }

    fun cancel(id: String) {
        scope.launch {
            engine.stop(id)
            val current = _tasks.value.toMutableMap()
            current.remove(id)
            _tasks.value = current
            notifyListeners()
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
