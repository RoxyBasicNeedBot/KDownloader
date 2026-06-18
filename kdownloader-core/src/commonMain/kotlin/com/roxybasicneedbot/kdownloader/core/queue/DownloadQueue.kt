package com.roxybasicneedbot.kdownloader.core.queue

import com.roxybasicneedbot.kdownloader.core.model.DownloadPriority
import com.roxybasicneedbot.kdownloader.core.model.DownloadRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadQueue(private val maxConcurrent: Int) {
    private val mutex = Mutex()
    private val _queued = MutableStateFlow<List<DownloadRequest>>(emptyList())
    val queued: StateFlow<List<DownloadRequest>> = _queued

    private val _active = MutableStateFlow<List<DownloadRequest>>(emptyList())
    val active: StateFlow<List<DownloadRequest>> = _active

    suspend fun enqueue(request: DownloadRequest) = mutex.withLock {
        val current = _queued.value.toMutableList()
        current.add(request)
        // Sort by priority (ordinal ascending, lower ordinal means higher priority if CRITICAL=0, HIGH=1, etc.)
        current.sortBy { it.priority.ordinal }
        _queued.value = current
    }

    suspend fun dequeue(): DownloadRequest? = mutex.withLock {
        if (_active.value.size >= maxConcurrent) return null

        val currentQueued = _queued.value.toMutableList()
        if (currentQueued.isEmpty()) return null

        val next = currentQueued.removeAt(0)
        _queued.value = currentQueued

        val currentActive = _active.value.toMutableList()
        currentActive.add(next)
        _active.value = currentActive

        return next
    }

    suspend fun remove(requestId: String) = mutex.withLock {
        _queued.value = _queued.value.filter { it.id != requestId }
        _active.value = _active.value.filter { it.id != requestId }
    }

    suspend fun complete(requestId: String) = mutex.withLock {
        _active.value = _active.value.filter { it.id != requestId }
    }

    fun getActiveCount(): Int = _active.value.size
}
