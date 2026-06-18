package com.roxybasicneedbot.kdownloader.core.util

import kotlin.math.ln
import kotlin.math.pow

object SizeFormatter {
    fun format(bytes: Long): String {
        if (bytes < 0) return "Unknown"
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val unit = "KMGTPE"[exp - 1] + "B"
        val value = bytes / 1024.0.pow(exp.toDouble())
        return formatDouble(value) + " " + unit
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 B/s"
        return format(bytesPerSec) + "/s"
    }

    private fun formatDouble(value: Double): String {
        // Simple multiplatform double formatting (e.g. 2.45)
        val intPart = value.toLong()
        val fracPart = ((value - intPart) * 100).toLong()
        val absFrac = if (fracPart < 0) -fracPart else fracPart
        val fracStr = if (absFrac < 10) "0$absFrac" else absFrac.toString()
        return "$intPart.$fracStr"
    }
}
