package com.roxybasicneedbot.kdownloader.core.model

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Queued : DownloadState()
    data class Scheduled(val startAt: Long) : DownloadState()
    data object Connecting : DownloadState()
    data class Downloading(val progress: DownloadProgress) : DownloadState()
    data object Paused : DownloadState()
    data object WaitingForNetwork : DownloadState()
    data object Merging : DownloadState()
    data class PostProcessing(val step: String) : DownloadState()
    data class Verifying(val algorithm: String) : DownloadState()
    data class Done(val result: DownloadResult) : DownloadState()
    data class Failed(val error: DownloadError, val retryCount: Int) : DownloadState()
    data object Cancelled : DownloadState()
}

data class DownloadError(
    val code: ErrorCode,
    val message: String,
    val throwable: Throwable? = null
)

enum class ErrorCode {
    UNKNOWN,
    NETWORK_ERROR,
    SERVER_ERROR,
    DISK_FULL,
    WRITE_ERROR,
    INTEGRITY_MISMATCH,
    MERGE_ERROR,
    USER_CANCELLED,
    POST_PROCESSING_ERROR,
    PROXY_ERROR,
    AUTH_ERROR
}
