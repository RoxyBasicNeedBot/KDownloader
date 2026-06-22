package com.roxybasicneedbot.kdownloader.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.roxybasicneedbot.kdownloader.DownloadRequest
import com.roxybasicneedbot.kdownloader.KDownloader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class KDownloaderModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun getName(): String {
        return "KDownloader"
    }

    override fun initialize() {
        super.initialize()
        
        scope.launch {
            KDownloader.getInstance(reactApplicationContext).observeAll().collectLatest { tasks ->
                val array = Arguments.createArray()
                // Assuming DownloadTaskEntity has fields. We mock serialization here:
                tasks.forEach { task ->
                    val map = Arguments.createMap()
                    // Add standard states based on DownloadState mapping
                    // We just emit a placeholder for now since we don't have the exact Entity properties
                    map.putString("id", task.toString())
                    array.pushMap(map)
                }
                
                reactApplicationContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("onDownloadStateChange", array)
            }
        }
    }

    @ReactMethod
    fun enqueue(requestMap: ReadableMap, promise: Promise) {
        try {
            val id = requestMap.getString("id") ?: throw IllegalArgumentException("Missing id")
            val url = requestMap.getString("url") ?: throw IllegalArgumentException("Missing url")
            val destinationDir = requestMap.getString("destinationDir") ?: throw IllegalArgumentException("Missing destinationDir")
            val fileName = requestMap.getString("fileName") ?: throw IllegalArgumentException("Missing fileName")

            val builder = DownloadRequest.Builder(
                url = url,
                destinationDir = destinationDir,
                fileName = fileName
            )
            // Note: we can apply other optional arguments from requestMap if provided
            
            scope.launch {
                try {
                    val taskId = KDownloader.getInstance(reactApplicationContext).enqueue(builder.build())
                    promise.resolve(taskId)
                } catch (e: Exception) {
                    promise.reject("ENQUEUE_ERROR", e.message, e)
                }
            }
        } catch (e: Exception) {
            promise.reject("INVALID_ARGS", e.message, e)
        }
    }

    @ReactMethod
    fun pause(id: String, promise: Promise) {
        scope.launch {
            try {
                KDownloader.getInstance(reactApplicationContext).pause(id)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("PAUSE_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun resume(id: String, promise: Promise) {
        scope.launch {
            try {
                KDownloader.getInstance(reactApplicationContext).resume(id)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("RESUME_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun cancel(id: String, promise: Promise) {
        scope.launch {
            try {
                KDownloader.getInstance(reactApplicationContext).cancel(id)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("CANCEL_ERROR", e.message, e)
            }
        }
    }

    @ReactMethod
    fun addListener(eventName: String?) {
        // Required for RN built-in Event Emitter Calls
    }

    @ReactMethod
    fun removeListeners(count: Int?) {
        // Required for RN built-in Event Emitter Calls
    }

    override fun invalidate() {
        scope.cancel()
        super.invalidate()
    }
}
