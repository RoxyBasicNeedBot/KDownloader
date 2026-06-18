package com.roxybasicneedbot.kdownloader.core.engine

import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage

class ChunkMerger(private val fileStorage: PlatformFileStorage) {
    suspend fun merge(chunkPaths: List<String>, finalPath: String) {
        fileStorage.mergeFiles(chunkPaths, finalPath)
        chunkPaths.forEach { path ->
            try {
                fileStorage.delete(path)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
