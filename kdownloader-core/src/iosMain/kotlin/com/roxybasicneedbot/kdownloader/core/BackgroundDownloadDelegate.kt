package com.roxybasicneedbot.kdownloader.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class BackgroundDownloadDelegate(
    private val onProgress: (id: String, downloaded: Long, total: Long) -> Unit,
    private val onCompleted: (id: String, tempFileUrl: NSURL) -> Unit,
    private val onError: (id: String, error: String) -> Unit
) : NSObject(), NSURLSessionDownloadDelegateProtocol {

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL
    ) {
        val id = downloadTask.taskDescription ?: ""
        onCompleted(id, didFinishDownloadingToURL)
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long
    ) {
        val id = downloadTask.taskDescription ?: ""
        onProgress(id, totalBytesWritten, totalBytesExpectedToWrite)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        if (didCompleteWithError != null) {
            val id = task.taskDescription ?: ""
            onError(id, didCompleteWithError.localizedDescription)
        }
    }
}
