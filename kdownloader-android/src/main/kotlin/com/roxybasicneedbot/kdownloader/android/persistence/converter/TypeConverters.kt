package com.roxybasicneedbot.kdownloader.android.persistence.converter

import androidx.room.TypeConverter
import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import com.roxybasicneedbot.kdownloader.core.model.ChunkStatus
import com.roxybasicneedbot.kdownloader.android.persistence.entity.HeadersWrapper
import com.roxybasicneedbot.kdownloader.android.persistence.entity.MirrorUrlsWrapper
import org.json.JSONArray
import org.json.JSONObject

object TypeConverters {
    @TypeConverter
    fun fromHeadersWrapper(wrapper: HeadersWrapper?): String {
        if (wrapper == null) return "{}"
        val json = JSONObject()
        wrapper.map.forEach { (key, value) ->
            json.put(key, value)
        }
        return json.toString()
    }

    @TypeConverter
    fun toHeadersWrapper(value: String?): HeadersWrapper {
        if (value.isNullOrEmpty()) return HeadersWrapper(emptyMap())
        val map = mutableMapOf<String, String>()
        val json = JSONObject(value)
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.getString(key)
        }
        return HeadersWrapper(map)
    }

    @TypeConverter
    fun fromMirrorUrlsWrapper(wrapper: MirrorUrlsWrapper?): String {
        if (wrapper == null) return "[]"
        val array = JSONArray()
        wrapper.list.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toMirrorUrlsWrapper(value: String?): MirrorUrlsWrapper {
        if (value.isNullOrEmpty()) return MirrorUrlsWrapper(emptyList())
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return MirrorUrlsWrapper(list)
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

