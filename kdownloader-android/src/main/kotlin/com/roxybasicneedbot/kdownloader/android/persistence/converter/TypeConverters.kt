package com.roxybasicneedbot.kdownloader.android.persistence.converter

import androidx.room.TypeConverter
import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import com.roxybasicneedbot.kdownloader.core.model.ChunkStatus
import org.json.JSONArray
import org.json.JSONObject

class TypeConverters {
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        if (map == null) return "{}"
        val json = JSONObject()
        map.forEach { (key, value) ->
            json.put(key, value)
        }
        return json.toString()
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        val json = JSONObject(value)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getString(key)
        }
        return map
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null) return "[]"
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    @TypeConverter
    fun fromDownloadPriority(priority: DownloadPriority?): String {
        return priority?.name ?: DownloadPriority.NORMAL.name
    }

    @TypeConverter
    fun toDownloadPriority(value: String?): DownloadPriority {
        return try {
            DownloadPriority.valueOf(value ?: DownloadPriority.NORMAL.name)
        } catch (e: Exception) {
            DownloadPriority.NORMAL
        }
    }

    @TypeConverter
    fun fromChunkStatus(status: ChunkStatus?): String {
        return status?.name ?: ChunkStatus.PENDING.name
    }

    @TypeConverter
    fun toChunkStatus(value: String?): ChunkStatus {
        return try {
            ChunkStatus.valueOf(value ?: ChunkStatus.PENDING.name)
        } catch (e: Exception) {
            ChunkStatus.PENDING
        }
    }
}
