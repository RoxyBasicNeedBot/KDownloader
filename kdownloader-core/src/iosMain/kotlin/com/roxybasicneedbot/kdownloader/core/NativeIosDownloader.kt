package com.roxybasicneedbot.kdownloader.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
object NativeIosDownloader {
    private val delegate = BackgroundDownloadDelegate(
        onProgress = { id, downloaded, total ->
            KDownloader.instance.updateProgress(id, downloaded, total)
        },
        onCompleted = { id, tempUrl ->
            KDownloader.instance.completeDownload(id, tempUrl)
        },
        onError = { id, error ->
            KDownloader.instance.failDownload(id, error)
        }
    )

    private val session: NSURLSession by lazy {
        val config = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier("com.roxybasicneedbot.kdownloader.background")
        config.sessionSendsLaunchEvents = true
        NSURLSession.sessionWithConfiguration(config, delegate, null)
    }

    fun startDownload(id: String, urlString: String): String {
        val url = NSURL.URLWithString(urlString) ?: return ""
        val task = session.downloadTaskWithURL(url)
        task.taskDescription = id
        task.resume()
        return id
    }

    fun cancelDownload(id: String) {
        session.getTasksWithCompletionHandler { _, _, downloadTasks ->
            val task = downloadTasks?.filterIsInstance<NSURLSessionDownloadTask>()?.firstOrNull { it.taskDescription == id }
            task?.cancel()
        }
    }
}
