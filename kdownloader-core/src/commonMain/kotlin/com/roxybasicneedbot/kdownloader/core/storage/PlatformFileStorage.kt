package com.roxybasicneedbot.kdownloader.core.storage

expect class PlatformFileStorage {
    constructor()
    suspend fun write(path: String, offset: Long, data: ByteArray)
    suspend fun read(path: String, offset: Long, length: Int): ByteArray
    suspend fun delete(path: String)
    suspend fun exists(path: String): Boolean
    suspend fun size(path: String): Long
    suspend fun createTempFile(prefix: String, suffix: String): String
    suspend fun mergeFiles(sourcePaths: List<String>, destPath: String)
}
