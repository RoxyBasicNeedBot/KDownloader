package com.roxybasicneedbot.kdownloader.core.model

import kotlin.random.Random

data class DownloadRequest(
    val id: String,
    val url: String,
    val destinationDir: String,
    val fileName: String,
    val priority: DownloadPriority = DownloadPriority.NORMAL,
    val chunkCount: Int = 4,
    val headers: Map<String, String> = emptyMap(),
    val wifiOnly: Boolean = false,
    val speedLimit: Long = 0L, // 0 means unlimited
    val mirrorUrls: List<String> = emptyList(),
    val hashAlgorithm: String? = null,
    val expectedHash: String? = null,
    val scheduleAt: Long? = null,
    val groupTag: String? = null
) {
    class Builder(private val url: String, private val destinationDir: String, private val fileName: String) {
        private var id: String = generateUUID()
        private var priority: DownloadPriority = DownloadPriority.NORMAL
        private var chunkCount: Int = 4
        private var headers: MutableMap<String, String> = mutableMapOf()
        private var wifiOnly: Boolean = false
        private var speedLimit: Long = 0L
        private var mirrorUrls: MutableList<String> = mutableListOf()
        private var hashAlgorithm: String? = null
        private var expectedHash: String? = null
        private var scheduleAt: Long? = null
        private var groupTag: String? = null

        fun setId(id: String) = apply { this.id = id }
        fun setPriority(priority: DownloadPriority) = apply { this.priority = priority }
        fun setChunkCount(chunkCount: Int) = apply { this.chunkCount = chunkCount }
        fun addHeader(key: String, value: String) = apply { this.headers[key] = value }
        fun setHeaders(headers: Map<String, String>) = apply { 
            this.headers.clear()
            this.headers.putAll(headers)
        }
        fun setWifiOnly(wifiOnly: Boolean) = apply { this.wifiOnly = wifiOnly }
        fun setSpeedLimit(speedLimit: Long) = apply { this.speedLimit = speedLimit }
        fun addMirrorUrl(mirrorUrl: String) = apply { this.mirrorUrls.add(mirrorUrl) }
        fun setMirrorUrls(mirrorUrls: List<String>) = apply {
            this.mirrorUrls.clear()
            this.mirrorUrls.addAll(mirrorUrls)
        }
        fun setHashVerification(algorithm: String, expectedHash: String) = apply {
            this.hashAlgorithm = algorithm
            this.expectedHash = expectedHash
        }
        fun setScheduleAt(timestamp: Long?) = apply { this.scheduleAt = timestamp }
        fun setGroupTag(groupTag: String?) = apply { this.groupTag = groupTag }

        fun build(): DownloadRequest {
            return DownloadRequest(
                id = id,
                url = url,
                destinationDir = destinationDir,
                fileName = fileName,
                priority = priority,
                chunkCount = chunkCount,
                headers = headers,
                wifiOnly = wifiOnly,
                speedLimit = speedLimit,
                mirrorUrls = mirrorUrls,
                hashAlgorithm = hashAlgorithm,
                expectedHash = expectedHash,
                scheduleAt = scheduleAt,
                groupTag = groupTag
            )
        }

        private fun generateUUID(): String {
            // Simple multiplatform-friendly random UUID-like string
            val chars = "abcdef0123456789"
            return buildString {
                for (i in 0 until 36) {
                    when (i) {
                        8, 13, 18, 23 -> append('-')
                        else -> append(chars[Random.nextInt(chars.length)])
                    }
                }
            }
        }
    }
}
