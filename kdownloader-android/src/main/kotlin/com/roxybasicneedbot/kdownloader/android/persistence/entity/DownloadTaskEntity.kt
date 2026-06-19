package com.roxybasicneedbot.kdownloader.android.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

data class HeadersWrapper(val map: Map<String, String>)
data class MirrorUrlsWrapper(val list: List<String>)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
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
    val status: String, // String representation of DownloadState name
    val downloadedBytes: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val errorCode: Int?,
    val completedAt: Long?,
    val createdAt: Long = System.currentTimeMillis()
)

