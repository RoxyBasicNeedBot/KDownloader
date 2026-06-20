package com.roxybasicneedbot.kdownloader.react

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.roxybasicneedbot.kdownloader.android.KDownloader
import com.roxybasicneedbot.kdownloader.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class KDownloaderModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var observeJob: Job? = null

    override fun getName() = "KDownloader"

    @ReactMethod
    fun enqueue(requestMap: ReadableMap, promise: Promise) {
        val request = DownloadRequest(
            id = requestMap.getString("id")!!,
            url = requestMap.getString("url")!!,
            destinationDir = requestMap.getString("destinationDir")!!,
            fileName = requestMap.getString("fileName")!!,
            priority = DownloadPriority.valueOf(requestMap.getString("priority") ?: "NORMAL"),
            chunkCount = if (requestMap.hasKey("chunkCount")) requestMap.getInt("chunkCount") else 8,
            headers = emptyMap(),
            wifiOnly = if (requestMap.hasKey("wifiOnly")) requestMap.getBoolean("wifiOnly") else false,
            speedLimit = 0L,
            mirrorUrls = emptyList()
        )
        
        scope.launch {
            try {
                val taskId = KDownloader.getInstance(reactApplicationContext).enqueue(request)
                promise.resolve(taskId)
            } catch(e: Exception) {
                promise.reject("ENQUEUE_FAILED", e.message)
            }
        }
    }

    @ReactMethod
    fun pause(id: String, promise: Promise) {
        KDownloader.getInstance(reactApplicationContext).pause(id)
        promise.resolve(null)
    }

    @ReactMethod
    fun resume(id: String, promise: Promise) {
        KDownloader.getInstance(reactApplicationContext).resume(id)
        promise.resolve(null)
    }

    @ReactMethod
    fun cancel(id: String, promise: Promise) {
        KDownloader.getInstance(reactApplicationContext).cancel(id)
        promise.resolve(null)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        if (eventName == "onDownloadStateChange" && observeJob == null) {
            observeJob = KDownloader.getInstance(reactApplicationContext).observeAll()
                .onEach { states ->
                    val array = Arguments.createArray()
                    // States mapping logic...
                    reactApplicationContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("onDownloadStateChange", array)
                }.launchIn(scope)
        }
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        if (count == 0) {
            observeJob?.cancel()
            observeJob = null
        }
    }
}
