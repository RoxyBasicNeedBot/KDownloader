package com.roxybasicneedbot.kdownloader.core.util

object UrlValidator {
    fun isValid(url: String): Boolean {
        if (url.isBlank()) return false
        val trimmed = url.trim()
        return trimmed.startsWith("http://", ignoreCase = true) || 
               trimmed.startsWith("https://", ignoreCase = true)
    }

    fun sanitize(url: String): String {
        return url.trim()
    }
}
