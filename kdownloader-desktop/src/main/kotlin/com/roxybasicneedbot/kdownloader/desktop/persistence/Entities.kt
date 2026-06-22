/* ktlint-disable */
package com.roxybasicneedbot.kdownloader.desktop.persistence

data class HeadersWrapper(val map: Map<String, String>) {
    // Basic serialization/deserialization for SQLite
    override fun toString(): String {
        return map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }
    companion object {
        fun fromString(str: String): HeadersWrapper {
            if (str.isBlank()) return HeadersWrapper(emptyMap())
            val map = str.split(";").associate { 
                val parts = it.split("=")
                parts[0] to (if (parts.size > 1) parts[1] else "")
            }
            return HeadersWrapper(map)
        }
    }
}

data class MirrorUrlsWrapper(val list: List<String>) {
    override fun toString(): String = list.joinToString(";")
    companion object {
        fun fromString(str: String): MirrorUrlsWrapper {
            if (str.isBlank()) return MirrorUrlsWrapper(emptyList())
            return MirrorUrlsWrapper(str.split(";"))
        }
    }
}

data class DownloadTaskEntity(
    val id: String,
    val url: String,
    val destinationDir: String,
    val fileName: String,
    val priority: String,
    val chunkCount: Int,
    val headers: HeadersWrapper,
    val wifiOnly: Boolean,
    val speedLimit: Long,
    val mirrorUrls: MirrorUrlsWrapper,
    val hashAlgorithm: String?,
    val expectedHash: String?,
    val scheduleAt: Long?,
    val groupTag: String?,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val errorCode: Int?,
    val completedAt: Long?,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChunkStateEntity(
    val downloadId: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long,
    val status: String,
    val mirrorUrl: String?,
    val tempFilePath: String,
    val speedBytesPerSec: Long,
    val retryCount: Int
)
