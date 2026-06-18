package com.roxybasicneedbot.kdownloader.core.model

data class DownloadConfig(
    val maxConcurrentDownloads: Int = 3,
    val defaultChunkCount: Int = 4,
    val globalSpeedLimit: Long = 0L, // 0 means unlimited
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000L,
    val connectionTimeoutMs: Long = 15000L,
    val readTimeoutMs: Long = 15000L
)
