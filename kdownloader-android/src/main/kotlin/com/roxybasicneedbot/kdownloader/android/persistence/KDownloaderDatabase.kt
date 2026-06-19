package com.roxybasicneedbot.kdownloader.android.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roxybasicneedbot.kdownloader.android.persistence.converter.TypeConverters as AppTypeConverters
import com.roxybasicneedbot.kdownloader.android.persistence.dao.ChunkStateDao
import com.roxybasicneedbot.kdownloader.android.persistence.dao.DownloadTaskDao
import com.roxybasicneedbot.kdownloader.android.persistence.entity.ChunkStateEntity
import com.roxybasicneedbot.kdownloader.android.persistence.entity.DownloadTaskEntity

@Database(
    entities = [DownloadTaskEntity::class, ChunkStateEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class KDownloaderDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun chunkStateDao(): ChunkStateDao

    companion object {
        @Volatile
        private var INSTANCE: KDownloaderDatabase? = null

        fun getInstance(context: Context): KDownloaderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KDownloaderDatabase::class.java,
                    "kdownloader_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
