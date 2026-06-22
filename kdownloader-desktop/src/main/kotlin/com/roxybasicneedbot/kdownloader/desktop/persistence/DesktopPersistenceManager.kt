@file:Suppress("MagicNumber")

package com.roxybasicneedbot.kdownloader.desktop.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.sql.ResultSet

class DesktopPersistenceManager {

    private val taskUpdates = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val allTasksUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        DatabaseHelper.initDatabase()
    }

    suspend fun insertTask(task: DownloadTaskEntity) = withContext(Dispatchers.IO) {
        DatabaseHelper.getConnection().use { conn ->
            val sql = """
                INSERT OR REPLACE INTO download_tasks (
                    id, url, destinationDir, fileName, priority, chunkCount, headers, wifiOnly, speedLimit, mirrorUrls,
                    hashAlgorithm, expectedHash, scheduleAt, groupTag, status, downloadedBytes, totalBytes, errorMessage,
                    errorCode, completedAt, createdAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, task.id)
                stmt.setString(2, task.url)
                stmt.setString(3, task.destinationDir)
                stmt.setString(4, task.fileName)
                stmt.setString(5, task.priority)
                stmt.setInt(6, task.chunkCount)
                stmt.setString(7, task.headers.toString())
                stmt.setInt(8, if (task.wifiOnly) 1 else 0)
                stmt.setLong(9, task.speedLimit)
                stmt.setString(10, task.mirrorUrls.toString())
                stmt.setString(11, task.hashAlgorithm)
                stmt.setString(12, task.expectedHash)
                stmt.setObject(13, task.scheduleAt)
                stmt.setString(14, task.groupTag)
                stmt.setString(15, task.status)
                stmt.setLong(16, task.downloadedBytes)
                stmt.setLong(17, task.totalBytes)
                stmt.setString(18, task.errorMessage)
                stmt.setObject(19, task.errorCode)
                stmt.setObject(20, task.completedAt)
                stmt.setLong(21, task.createdAt)
                stmt.executeUpdate()
            }
        }
        taskUpdates.tryEmit(task.id)
        allTasksUpdates.tryEmit(Unit)
    }

    suspend fun getTaskById(id: String): DownloadTaskEntity? = withContext(Dispatchers.IO) {
        DatabaseHelper.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM download_tasks WHERE id = ?").use { stmt ->
                stmt.setString(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return@withContext mapToDownloadTask(rs)
                }
            }
        }
        null
    }

    suspend fun getAllTasks(): List<DownloadTaskEntity> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<DownloadTaskEntity>()
        DatabaseHelper.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM download_tasks")
                while (rs.next()) {
                    tasks.add(mapToDownloadTask(rs))
                }
            }
        }
        tasks
    }

    suspend fun deleteTaskById(id: String) = withContext(Dispatchers.IO) {
        DatabaseHelper.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM download_tasks WHERE id = ?").use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate()
            }
        }
        taskUpdates.tryEmit(id)
        allTasksUpdates.tryEmit(Unit)
    }

    fun observeTaskById(id: String): Flow<DownloadTaskEntity?> {
        return taskUpdates
            .onStart { emit(id) }
            .map { updatedId ->
                if (updatedId == id) {
                    getTaskById(id)
                } else null
            }
    }

    fun observeAllTasks(): Flow<List<DownloadTaskEntity>> {
        return allTasksUpdates
            .onStart { emit(Unit) }
            .map { getAllTasks() }
    }

    private fun mapToDownloadTask(rs: ResultSet): DownloadTaskEntity {
        return DownloadTaskEntity(
            id = rs.getString("id"),
            url = rs.getString("url"),
            destinationDir = rs.getString("destinationDir"),
            fileName = rs.getString("fileName"),
            priority = rs.getString("priority"),
            chunkCount = rs.getInt("chunkCount"),
            headers = HeadersWrapper.fromString(rs.getString("headers")),
            wifiOnly = rs.getInt("wifiOnly") == 1,
            speedLimit = rs.getLong("speedLimit"),
            mirrorUrls = MirrorUrlsWrapper.fromString(rs.getString("mirrorUrls")),
            hashAlgorithm = rs.getString("hashAlgorithm"),
            expectedHash = rs.getString("expectedHash"),
            scheduleAt = rs.getObject("scheduleAt") as? Long,
            groupTag = rs.getString("groupTag"),
            status = rs.getString("status"),
            downloadedBytes = rs.getLong("downloadedBytes"),
            totalBytes = rs.getLong("totalBytes"),
            errorMessage = rs.getString("errorMessage"),
            errorCode = rs.getObject("errorCode") as? Int,
            completedAt = rs.getObject("completedAt") as? Long,
            createdAt = rs.getLong("createdAt")
        )
    }
}
