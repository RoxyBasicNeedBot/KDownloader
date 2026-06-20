package com.roxybasicneedbot.kdownloader.sample

import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import com.roxybasicneedbot.kdownloader.core.model.DownloadState
import com.roxybasicneedbot.kdownloader.desktop.KDownloaderDesktop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.io.File

fun main() = runBlocking {
    println("🚀 KDownloader Desktop Engine Starting...")

    val downloader = KDownloaderDesktop.getInstance()
    
    // Setup download request (50MB sample file)
    val url = "https://speed.hetzner.de/100MB.bin" 
    val destination = System.getProperty("user.dir") + "/downloads"
    File(destination).mkdirs()
    
    val request = DownloadRequest(
        id = "sample-task-1",
        url = url,
        destinationDir = destination,
        fileName = "100MB_Test_File.bin",
        priority = DownloadPriority.HIGH,
        chunkCount = 8,
        headers = emptyMap(),
        wifiOnly = false,
        speedLimit = 0,
        mirrorUrls = emptyList(),
        hashAlgorithm = null,
        expectedHash = null,
        scheduleAt = null,
        groupTag = null
    )

    // Observe State
    downloader.observe(request.id).onEach { state ->
        when (state) {
            is DownloadState.Downloading -> {
                val p = state.progress
                val bar = buildProgressBar(p.percent)
                print("\rDownloading: [$bar] ${p.percent}% | ${p.speedFormatted} | ETA: ${p.etaFormatted}     ")
            }
            is DownloadState.Done -> {
                println("\n✅ Download Complete! File saved to: ${request.destinationDir}/${request.fileName}")
            }
            is DownloadState.Failed -> {
                println("\n❌ Download Failed: ${state.error}")
            }
            else -> {}
        }
    }.launchIn(this)

    println("Enqueuing Download: $url")
    downloader.enqueue(request)

    // Keep alive until done or failed
    var isFinished = false
    downloader.observe(request.id).onEach { state ->
        if (state is DownloadState.Done || state is DownloadState.Failed || state is DownloadState.Cancelled) {
            isFinished = true
        }
    }.launchIn(this)
    
    while (!isFinished) {
        delay(100)
    }
    
    println("Sample completed. Shutting down.")
    // Wait for a bit for terminal to flush
    delay(500)
}

fun buildProgressBar(percent: Int): String {
    val length = 40
    val filled = (percent * length) / 100
    val empty = length - filled
    return "█".repeat(filled) + "░".repeat(empty)
}
