package com.roxybasicneedbot.kdownloader.core.clipboard

import kotlinx.coroutines.flow.Flow

expect class PlatformClipboardMonitor {
    constructor()
    fun observeUrls(): Flow<String>
    fun getClipboardText(): String?
}
