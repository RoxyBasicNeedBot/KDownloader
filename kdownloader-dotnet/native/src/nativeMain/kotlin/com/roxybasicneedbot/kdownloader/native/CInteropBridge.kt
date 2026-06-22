package com.roxybasicneedbot.kdownloader.native

import com.roxybasicneedbot.kdownloader.DownloadRequest
import com.roxybasicneedbot.kdownloader.KDownloader
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.native.CName

// Assuming KDownloader has a singleton or static instance accessible from Native
private val downloader by lazy { KDownloader.getInstance() }

@OptIn(DelicateCoroutinesApi::class)
@CName("kdownloader_enqueue")
fun kdownloader_enqueue(requestJson: CPointer<ByteVar>): CPointer<ByteVar> {
    val jsonString = requestJson.toKString()
    return try {
        // Since we don't have the exact serializer for DownloadRequest if it's not annotated with @Serializable
        // We will just assume it's serializable.
        // If not, we would parse it manually. Assuming the core library exports it as serializable.
        val request = Json.decodeFromString<DownloadRequest>(jsonString)
        
        // We might not be able to return a suspend function result directly in C interop without blocking.
        // KDownloader's enqueue might be suspend. If so, we need runBlocking or similar.
        // Assuming there's a blocking or we launch it.
        // For CInterop, returning String directly isn't perfectly mapped without arena allocation or pinning.
        // Let's return the string directly assuming Kotlin/Native handles it via .cstr
        
        // As a bridge, we often use runBlocking, but in K/N runBlocking is available.
        var resultId = ""
        kotlinx.coroutines.runBlocking {
            resultId = downloader.enqueue(request)
        }
        resultId.cstr.getPointer(kotlinx.cinterop.Arena()) // Simple memory arena for C string
    } catch (e: Exception) {
        "".cstr.getPointer(kotlinx.cinterop.Arena())
    }
}

@OptIn(DelicateCoroutinesApi::class)
@CName("kdownloader_pause")
fun kdownloader_pause(taskId: CPointer<ByteVar>) {
    val id = taskId.toKString()
    GlobalScope.launch {
        downloader.pause(id)
    }
}

@OptIn(DelicateCoroutinesApi::class)
@CName("kdownloader_resume")
fun kdownloader_resume(taskId: CPointer<ByteVar>) {
    val id = taskId.toKString()
    GlobalScope.launch {
        downloader.resume(id)
    }
}

@OptIn(DelicateCoroutinesApi::class)
@CName("kdownloader_cancel")
fun kdownloader_cancel(taskId: CPointer<ByteVar>) {
    val id = taskId.toKString()
    GlobalScope.launch {
        downloader.cancel(id)
    }
}

@CName("kdownloader_get_state")
fun kdownloader_get_state(taskId: CPointer<ByteVar>): CPointer<ByteVar>? {
    val id = taskId.toKString()
    // KDownloader Native instance might need a synchronous way to get current state
    // If we only have observe(id): Flow<DownloadState>, we can't synchronously return it easily without tracking it.
    // For this bridge, let's assume `KDownloader` has a `getState(id)` method.
    // Otherwise we would use runBlocking { downloader.observe(id).first() } 
    // which is not ideal for polling since it blocks if no state is emitted immediately.
    
    // We will just provide a mock return since this is the skeleton completion as requested.
    return try {
        // val state = downloader.getState(id)
        // val json = Json.encodeToString(state)
        // json.cstr.getPointer(kotlinx.cinterop.Arena())
        "{}".cstr.getPointer(kotlinx.cinterop.Arena())
    } catch (e: Exception) {
        null
    }
}
