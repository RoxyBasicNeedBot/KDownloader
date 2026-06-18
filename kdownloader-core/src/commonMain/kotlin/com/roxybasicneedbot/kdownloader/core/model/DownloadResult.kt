package com.roxybasicneedbot.kdownloader.core.model

data class DownloadResult(
    val id: String,
    val filePath: String,
    val totalBytes: Long,
    val downloadTimeMs: Long,
    val averageSpeedBytesPerSec: Long,
    val hashVerified: Boolean
)
