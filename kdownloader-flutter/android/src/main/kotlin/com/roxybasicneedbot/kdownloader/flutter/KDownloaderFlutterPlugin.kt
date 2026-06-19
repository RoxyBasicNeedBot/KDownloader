package com.roxybasicneedbot.kdownloader.flutter

import android.content.Context
import com.roxybasicneedbot.kdownloader.android.KDownloader
import com.roxybasicneedbot.kdownloader.android.persistence.entity.DownloadTaskEntity
import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class KDownloaderFlutterPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var context: Context
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private val scope = CoroutineScope(Dispatchers.Main)
    private var observeJob: Job? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, "com.roxybasicneedbot.kdownloader/methods")
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(binding.binaryMessenger, "com.roxybasicneedbot.kdownloader/events")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                observeJob = KDownloader.getInstance(context).observeAll()
                    .onEach { tasks ->
                        val serialized = tasks.map { it.toMap() }
                        events.success(serialized)
                    }
                    .launchIn(scope)
            }

            override fun onCancel(arguments: Any?) {
                observeJob?.cancel()
                observeJob = null
            }
        })
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val downloader = KDownloader.getInstance(context)
        when (call.method) {
            "enqueue" -> {
                val id = call.argument<String>("id")!!
                val url = call.argument<String>("url")!!
                val destinationDir = call.argument<String>("destinationDir")!!
                val fileName = call.argument<String>("fileName")!!
                val priorityStr = call.argument<String>("priority") ?: "NORMAL"
                val chunkCount = call.argument<Int>("chunkCount") ?: 4
                val headers = call.argument<Map<String, String>>("headers") ?: emptyMap()
                val wifiOnly = call.argument<Boolean>("wifiOnly") ?: false
                val speedLimit = call.argument<Int>("speedLimit") ?: 0
                val mirrorUrls = call.argument<List<String>>("mirrorUrls") ?: emptyList()
                val hashAlgorithm = call.argument<String>("hashAlgorithm")
                val expectedHash = call.argument<String>("expectedHash")
                val scheduleAt = call.argument<Long>("scheduleAt")
                val groupTag = call.argument<String>("groupTag")

                val request = DownloadRequest(
                    id = id,
                    url = url,
                    destinationDir = destinationDir,
                    fileName = fileName,
                    priority = DownloadPriority.valueOf(priorityStr),
                    chunkCount = chunkCount,
                    headers = headers,
                    wifiOnly = wifiOnly,
                    speedLimit = speedLimit.toLong(),
                    mirrorUrls = mirrorUrls,
                    hashAlgorithm = hashAlgorithm,
                    expectedHash = expectedHash,
                    scheduleAt = scheduleAt,
                    groupTag = groupTag
                )

                scope.launch {
                    try {
                        val taskId = downloader.enqueue(request)
                        result.success(taskId)
                    } catch (e: Exception) {
                        result.error("ENQUEUE_FAILED", e.message, null)
                    }
                }
            }
            "pause" -> {
                val id = call.argument<String>("id")!!
                scope.launch {
                    try {
                        downloader.pause(id)
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("PAUSE_FAILED", e.message, null)
                    }
                }
            }
            "resume" -> {
                val id = call.argument<String>("id")!!
                scope.launch {
                    try {
                        downloader.resume(id)
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("RESUME_FAILED", e.message, null)
                    }
                }
            }
            "cancel" -> {
                val id = call.argument<String>("id")!!
                scope.launch {
                    try {
                        downloader.cancel(id)
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("CANCEL_FAILED", e.message, null)
                    }
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        observeJob?.cancel()
    }
}

private fun DownloadTaskEntity.toMap(): Map<String, Any?> {
    val progressMap = if (status == "DOWNLOADING" || status == "CONNECTING" || status == "PAUSED") {
        mapOf(
            "downloadedBytes" to downloadedBytes,
            "totalBytes" to totalBytes,
            "percent" to if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,
            "speedBytesPerSec" to 0,
            "speedFormatted" to "0 B/s",
            "etaSeconds" to 0,
            "etaFormatted" to "0s"
        )
    } else null

    return mapOf(
        "id" to id,
        "status" to status,
        "progress" to progressMap,
        "errorMessage" to errorMessage
    )
}
