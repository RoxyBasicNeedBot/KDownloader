@file:Suppress("WildcardImport")

package com.roxybasicneedbot.kdownloader.android.persistence.dao

import androidx.room.*
import com.roxybasicneedbot.kdownloader.android.persistence.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    suspend fun getAllTasks(): List<DownloadTaskEntity>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    fun observeTaskById(id: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = :status")
    suspend fun getTasksByStatus(status: String): List<DownloadTaskEntity>
}
