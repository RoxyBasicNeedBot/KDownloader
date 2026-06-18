package com.roxybasicneedbot.kdownloader.core.network

import com.roxybasicneedbot.kdownloader.core.util.epochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BandwidthController(private val limitBytesPerSecond: Long) {
    private val mutex = Mutex()
    private var tokens: Long = limitBytesPerSecond
    private var lastRefillTime: Long = epochMillis()
    private val maxCapacity = limitBytesPerSecond

    suspend fun acquire(bytes: Long) {
        if (limitBytesPerSecond <= 0L) return // Unlimited

        var needed = bytes
        while (needed > 0L) {
            mutex.withLock {
                refill()
                val available = tokens
                if (available > 0L) {
                    val toTake = minOf(needed, available)
                    tokens -= toTake
                    needed -= toTake
                }
            }
            if (needed > 0L) {
                delay(50)
            }
        }
    }

    private fun refill() {
        val now = epochMillis()
        val elapsed = now - lastRefillTime
        if (elapsed > 0L) {
            val newTokens = (elapsed * limitBytesPerSecond) / 1000L
            tokens = minOf(maxCapacity, tokens + newTokens)
            lastRefillTime = now
        }
    }
}
