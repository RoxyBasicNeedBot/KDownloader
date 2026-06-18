package com.roxybasicneedbot.kdownloader.core.model

data class ChunkInfo(
    val downloadId: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long,
    val status: ChunkStatus,
    val mirrorUrl: String? = null,
    val tempFilePath: String,
    val speedBytesPerSec: Long = 0L,
    val retryCount: Int = 0
)

enum class ChunkStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
