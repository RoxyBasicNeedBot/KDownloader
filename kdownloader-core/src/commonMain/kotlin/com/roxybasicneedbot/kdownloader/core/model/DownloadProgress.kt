package com.roxybasicneedbot.kdownloader.core.model

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long, // -1 if unknown
    val percent: Int, // 0 to 100
    val speedBytesPerSec: Long,
    val speedFormatted: String,
    val etaSeconds: Long, // -1 if unknown
    val etaFormatted: String,
    val activeChunks: Int,
    val totalChunks: Int,
    val chunkProgress: List<ChunkProgress>
)

data class ChunkProgress(
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percent: Int,
    val speedBytesPerSec: Long
)
