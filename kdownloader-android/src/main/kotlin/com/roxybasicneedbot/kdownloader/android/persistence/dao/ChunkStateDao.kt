@file:Suppress("WildcardImport")

package com.roxybasicneedbot.kdownloader.android.persistence.dao

import androidx.room.*
import com.roxybasicneedbot.kdownloader.android.persistence.entity.ChunkStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkStateEntity>)

    @Update
    suspend fun updateChunk(chunk: ChunkStateEntity)

    @Query("SELECT * FROM chunk_states WHERE downloadId = :downloadId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDownload(downloadId: String): List<ChunkStateEntity>

    @Query("SELECT * FROM chunk_states WHERE downloadId = :downloadId ORDER BY chunkIndex ASC")
    fun observeChunksForDownload(downloadId: String): Flow<List<ChunkStateEntity>>

    @Query("DELETE FROM chunk_states WHERE downloadId = :downloadId")
    suspend fun deleteChunksForDownload(downloadId: String)
}
