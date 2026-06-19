package com.roxybasicneedbot.kdownloader.android.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chunk_states",
    primaryKeys = ["downloadId", "chunkIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DownloadTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["downloadId"])]
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

