package com.roxybasicneedbot.kdownloader.core.engine

import com.roxybasicneedbot.kdownloader.core.model.ChunkInfo
import com.roxybasicneedbot.kdownloader.core.model.ChunkStatus

object DynamicChunkSplitter {
    fun split(slowChunk: ChunkInfo, bytesDownloaded: Long): Pair<ChunkInfo, ChunkInfo>? {
        if (slowChunk.status != ChunkStatus.DOWNLOADING) return null
        
        val slowStart = slowChunk.startByte + bytesDownloaded
        val slowEnd = slowChunk.endByte
        val remainingBytes = slowEnd - slowStart

        val minSplitSize = 2 * 1024 * 1024L // 2MB minimum split size
        if (remainingBytes < minSplitSize) return null

        val splitPoint = slowStart + (remainingBytes / 2)

        val updatedSlow = slowChunk.copy(
            endByte = splitPoint
        )

        val newChunk = ChunkInfo(
            downloadId = slowChunk.downloadId,
            chunkIndex = slowChunk.chunkIndex + 100,
            startByte = splitPoint + 1,
            endByte = slowEnd,
            downloadedBytes = 0L,
            status = ChunkStatus.PENDING,
            tempFilePath = "${slowChunk.tempFilePath}_split_${slowChunk.chunkIndex}"
        )

        return Pair(updatedSlow, newChunk)
    }
}
