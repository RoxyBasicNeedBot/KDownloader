package com.roxybasicneedbot.kdownloader.native

import com.roxybasicneedbot.kdownloader.core.engine.DownloadEngine
import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage
import com.roxybasicneedbot.kdownloader.core.network.PlatformNetworkMonitor
import com.roxybasicneedbot.kdownloader.core.network.HttpClientFactory
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import com.roxybasicneedbot.kdownloader.core.model.DownloadState
import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.experimental.ExperimentalNativeApi

// Request matching C# JSON payload
@Serializable
data class NativeDownloadRequest(
    val id: String,
    val url: String,
    val destinationDir: String,
    val fileName: String,
    val chunkCount: Int = 8
)

@Serializable
data class NativeDownloadState(
    val status: String,
    val percent: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speedFormatted: String = "",
    val errorMessage: String? = null
)

@OptIn(ExperimentalNativeApi::class)
object NativeDownloadManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeEngines = mutableMapOf<String, Pair<DownloadEngine, Job>>()
    private val suspendedRequests = mutableMapOf<String, DownloadRequest>()
    private val latestStates = mutableMapOf<String, NativeDownloadState>()
    
    private val json = Json { ignoreUnknownKeys = true }

    @CName("kdownloader_free_string")
    fun freeString(ptr: CPointer<ByteVar>?) {
        if (ptr != null) {
            nativeHeap.free(ptr)
        }
    }

    @CName("kdownloader_enqueue")
    fun enqueue(requestJsonPtr: CPointer<ByteVar>?): CPointer<ByteVar>? {
        if (requestJsonPtr == null) return null
        
        try {
            val requestJson = requestJsonPtr.toKString()
            val nativeReq = json.decodeFromString<NativeDownloadRequest>(requestJson)
            
            val request = DownloadRequest(
                id = nativeReq.id,
                url = nativeReq.url,
                destinationDir = nativeReq.destinationDir,
                fileName = nativeReq.fileName,
                chunkCount = nativeReq.chunkCount,
                priority = DownloadPriority.NORMAL,
                headers = emptyMap(),
                wifiOnly = false,
                speedLimit = 0L,
                mirrorUrls = emptyList(),
                hashAlgorithm = null,
                expectedHash = null,
                scheduleAt = null,
                groupTag = null
            )
            
            val engine = DownloadEngine(
                DownloadConfig(),
                PlatformFileStorage(),
                PlatformNetworkMonitor(),
                HttpClientFactory()
            )
            val job = scope.launch {
                engine.states.collect { (_, state) ->
                    val nativeState = when (state) {
                        is DownloadState.Downloading -> NativeDownloadState("RUNNING", state.progress.percent, state.progress.downloadedBytes, state.progress.totalBytes, state.progress.speedFormatted, null)
                        is DownloadState.Done -> NativeDownloadState("DONE", 100, state.result.totalBytes, state.result.totalBytes, "", null)
                        is DownloadState.Failed -> NativeDownloadState("FAILED", 0, 0, 0, "", state.error.message)
                        is DownloadState.Paused -> NativeDownloadState("PAUSED")
                        is DownloadState.Cancelled -> NativeDownloadState("CANCELLED")
                        else -> NativeDownloadState(state::class.simpleName ?: "UNKNOWN")
                    }
                    latestStates[nativeReq.id] = nativeState

                    if (state is DownloadState.Done || state is DownloadState.Failed || state is DownloadState.Cancelled) {
                        activeEngines.remove(nativeReq.id)
                        suspendedRequests.remove(nativeReq.id)
                    }
                }
            }
            activeEngines[nativeReq.id] = Pair(engine, job)
            suspendedRequests[nativeReq.id] = request
            
            scope.launch { engine.start(request) }
            
            return nativeReq.id.cstr.getPointer(nativeHeap)
        } catch (e: Exception) {
            println("kdownloader_enqueue error: ${e.message}")
            return null
        }
    }

    @CName("kdownloader_pause")
    fun pause(taskIdPtr: CPointer<ByteVar>?) {
        val taskId = taskIdPtr?.toKString() ?: return
        val pair = activeEngines[taskId]
        pair?.second?.cancel()
        activeEngines.remove(taskId)
        scope.launch { pair?.first?.stop(taskId) }
        latestStates[taskId] = NativeDownloadState("PAUSED")
    }

    @CName("kdownloader_resume")
    fun resume(taskIdPtr: CPointer<ByteVar>?) {
        val taskId = taskIdPtr?.toKString() ?: return
        if (activeEngines.containsKey(taskId)) return // already running
        
        val request = suspendedRequests[taskId] ?: return
        val engine = DownloadEngine(
            DownloadConfig(),
            PlatformFileStorage(),
            PlatformNetworkMonitor(),
            HttpClientFactory()
        )
        val job = scope.launch {
            engine.states.collect { (_, state) ->
                val nativeState = when (state) {
                    is DownloadState.Downloading -> NativeDownloadState("RUNNING", state.progress.percent, state.progress.downloadedBytes, state.progress.totalBytes, state.progress.speedFormatted, null)
                    is DownloadState.Done -> NativeDownloadState("DONE", 100, state.result.totalBytes, state.result.totalBytes, "", null)
                    is DownloadState.Failed -> NativeDownloadState("FAILED", 0, 0, 0, "", state.error.message)
                    is DownloadState.Paused -> NativeDownloadState("PAUSED")
                    is DownloadState.Cancelled -> NativeDownloadState("CANCELLED")
                    else -> NativeDownloadState(state::class.simpleName ?: "UNKNOWN")
                }
                latestStates[taskId] = nativeState

                if (state is DownloadState.Done || state is DownloadState.Failed || state is DownloadState.Cancelled) {
                    activeEngines.remove(taskId)
                    suspendedRequests.remove(taskId)
                }
            }
        }
        activeEngines[taskId] = Pair(engine, job)
        scope.launch { engine.start(request) }
    }

    @CName("kdownloader_cancel")
    fun cancel(taskIdPtr: CPointer<ByteVar>?) {
        val taskId = taskIdPtr?.toKString() ?: return
        val pair = activeEngines[taskId]
        pair?.second?.cancel()
        activeEngines.remove(taskId)
        suspendedRequests.remove(taskId)
        scope.launch { pair?.first?.stop(taskId) }
        latestStates[taskId] = NativeDownloadState("CANCELLED")
    }

    @CName("kdownloader_get_state")
    fun getState(taskIdPtr: CPointer<ByteVar>?): CPointer<ByteVar>? {
        val taskId = taskIdPtr?.toKString() ?: return null
        val nativeState = latestStates[taskId] ?: NativeDownloadState("NOT_FOUND")
        val jsonString = json.encodeToString(nativeState)
        return jsonString.cstr.getPointer(nativeHeap)
    }
}
