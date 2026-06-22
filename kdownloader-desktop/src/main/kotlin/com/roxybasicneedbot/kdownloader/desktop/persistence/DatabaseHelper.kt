/* ktlint-disable */
package com.roxybasicneedbot.kdownloader.desktop.persistence

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object DatabaseHelper {
    private const val DB_DIR = ".kdownloader"
    private const val DB_NAME = "kdownloader.db"

    private fun getDatabaseFile(): File {
        val userHome = System.getProperty("user.home")
        val dir = File(userHome, DB_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, DB_NAME)
    }

    private val url: String by lazy {
        "jdbc:sqlite:${getDatabaseFile().absolutePath}"
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection(url)
    }

    fun initDatabase() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS download_tasks (
                        id TEXT PRIMARY KEY,
                        url TEXT NOT NULL,
                        destinationDir TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        chunkCount INTEGER NOT NULL,
                        headers TEXT NOT NULL,
                        wifiOnly INTEGER NOT NULL,
                        speedLimit INTEGER NOT NULL,
                        mirrorUrls TEXT NOT NULL,
                        hashAlgorithm TEXT,
                        expectedHash TEXT,
                        scheduleAt INTEGER,
                        groupTag TEXT,
                        status TEXT NOT NULL,
                        downloadedBytes INTEGER NOT NULL,
                        totalBytes INTEGER NOT NULL,
                        errorMessage TEXT,
                        errorCode INTEGER,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS chunk_states (
                        downloadId TEXT NOT NULL,
                        chunkIndex INTEGER NOT NULL,
                        startByte INTEGER NOT NULL,
                        endByte INTEGER NOT NULL,
                        downloadedBytes INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        mirrorUrl TEXT,
                        tempFilePath TEXT NOT NULL,
                        speedBytesPerSec INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        PRIMARY KEY (downloadId, chunkIndex),
                        FOREIGN KEY (downloadId) REFERENCES download_tasks(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Enable foreign key support in SQLite
                stmt.execute("PRAGMA foreign_keys = ON;")
            }
        }
    }
}
