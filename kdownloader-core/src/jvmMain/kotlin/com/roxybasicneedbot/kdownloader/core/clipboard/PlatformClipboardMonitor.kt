package com.roxybasicneedbot.kdownloader.core.clipboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

actual class PlatformClipboardMonitor actual constructor() {
    actual fun observeUrls(): Flow<String> = flow {
        var lastText: String? = null
        while (true) {
            val currentText = getClipboardText()
            if (currentText != null && currentText != lastText) {
                if (currentText.startsWith("http://", ignoreCase = true) || 
                    currentText.startsWith("https://", ignoreCase = true)) {
                    emit(currentText)
                }
                lastText = currentText
            }
            delay(1000)
        }
    }

    actual fun getClipboardText(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
