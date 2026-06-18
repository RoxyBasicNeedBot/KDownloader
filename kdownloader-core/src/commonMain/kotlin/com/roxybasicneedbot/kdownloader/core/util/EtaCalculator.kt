package com.roxybasicneedbot.kdownloader.core.util

object EtaCalculator {
    fun calculate(downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long): Long {
        if (totalBytes <= 0 || downloadedBytes >= totalBytes || speedBytesPerSec <= 0) {
            return -1L
        }
        val remainingBytes = totalBytes - downloadedBytes
        return remainingBytes / speedBytesPerSec
    }

    fun format(etaSeconds: Long): String {
        if (etaSeconds < 0) return "Unknown"
        if (etaSeconds == 0L) return "0s"
        
        val hours = etaSeconds / 3600
        val minutes = (etaSeconds % 3600) / 60
        val seconds = etaSeconds % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
}
