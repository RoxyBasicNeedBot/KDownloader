package com.roxybasicneedbot.kdownloader.reactnative

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import com.roxybasicneedbot.kdownloader.android.KDownloader
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
                tasks.forEach { task ->
                    val map = Arguments.createMap()
                    map.putString("id", task.id)
                    map.putString("url", task.url)
                    map.putString("destinationDir", task.destinationDir)
                    map.putString("fileName", task.fileName)
                    map.putString("status", task.status)
                    map.putDouble("downloadedBytes", task.downloadedBytes.toDouble())
                    map.putDouble("totalBytes", task.totalBytes.toDouble())
                    map.putString("errorMessage", task.errorMessage)
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
            
            if (requestMap.hasKey("id")) builder.setId(requestMap.getString("id")!!)
            if (requestMap.hasKey("chunkCount")) builder.setChunkCount(requestMap.getInt("chunkCount"))
            if (requestMap.hasKey("wifiOnly")) builder.setWifiOnly(requestMap.getBoolean("wifiOnly"))
            if (requestMap.hasKey("speedLimit")) builder.setSpeedLimit(requestMap.getDouble("speedLimit").toLong())
            if (requestMap.hasKey("priority")) {
                val p = requestMap.getString("priority")
                if (p == "HIGH") builder.setPriority(com.roxybasicneedbot.kdownloader.core.model.DownloadPriority.HIGH)
                else if (p == "LOW") builder.setPriority(com.roxybasicneedbot.kdownloader.core.model.DownloadPriority.LOW)
            }
            if (requestMap.hasKey("headers")) {
                val headersMap = requestMap.getMap("headers")?.toHashMap()
                headersMap?.forEach { (k, v) -> builder.addHeader(k, v.toString()) }
            }
            if (requestMap.hasKey("groupTag")) builder.setGroupTag(requestMap.getString("groupTag"))
            
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
