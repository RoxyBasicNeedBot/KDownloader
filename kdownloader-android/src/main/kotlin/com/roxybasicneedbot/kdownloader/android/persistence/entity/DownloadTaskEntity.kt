package com.roxybasicneedbot.kdownloader.android.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val url: String,
    val destinationDir: String,
    val fileName: String,
    val priority: DownloadPriority,
    val chunkCount: Int,
    val headers: Map<String, String>,
    val wifiOnly: Boolean,
    val speedLimit: Long,
    val mirrorUrls: List<String>,
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
