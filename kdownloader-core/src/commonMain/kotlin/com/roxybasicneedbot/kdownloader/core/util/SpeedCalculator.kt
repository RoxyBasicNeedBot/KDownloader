package com.roxybasicneedbot.kdownloader.core.util

class SpeedCalculator {
    private var lastUpdateTime: Long = 0L
    private var lastBytes: Long = 0L
    private var currentSpeed: Long = 0L
    private val alpha = 0.2 // Smoothing factor for EMA

    fun update(downloadedBytes: Long, currentTimeMs: Long): Long {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTimeMs
            lastBytes = downloadedBytes
            return 0L
        }

        val timeDiff = currentTimeMs - lastUpdateTime
        if (timeDiff >= 500) { // Update every 500ms
            val byteDiff = downloadedBytes - lastBytes
            val speedInstant = if (timeDiff > 0) (byteDiff * 1000) / timeDiff else 0L
            currentSpeed = if (currentSpeed == 0L) {
                speedInstant
            } else {
                (alpha * speedInstant + (1 - alpha) * currentSpeed).toLong()
            }
            lastUpdateTime = currentTimeMs
            lastBytes = downloadedBytes
        }
        return currentSpeed
    }

    fun getSpeed(): Long = currentSpeed

    fun reset() {
        lastUpdateTime = 0L
        lastBytes = 0L
        currentSpeed = 0L
    }
}
