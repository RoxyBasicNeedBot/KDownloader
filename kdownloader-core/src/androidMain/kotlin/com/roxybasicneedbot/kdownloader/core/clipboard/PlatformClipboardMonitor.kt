/* ktlint-disable */
package com.roxybasicneedbot.kdownloader.core.clipboard

import android.content.ClipboardManager
import android.content.Context
import com.roxybasicneedbot.kdownloader.core.context.AndroidContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.regex.Pattern

actual class PlatformClipboardMonitor actual constructor() {
    private val clipboardManager = AndroidContext.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val urlPattern = Pattern.compile(
        "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    )

    actual fun observeUrls(): Flow<String> = callbackFlow {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val text = getClipboardText()
            if (text != null && isUrl(text)) {
                trySend(text)
            }
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        
        // Emit initial value if any
        val initialText = getClipboardText()
        if (initialText != null && isUrl(initialText)) {
            trySend(initialText)
        }

        awaitClose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    actual fun getClipboardText(): String? {
        val clip = clipboardManager.primaryClip ?: return null
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).text
            return text?.toString()
        }
        return null
    }

    private fun isUrl(text: String): Boolean {
        return urlPattern.matcher(text).find()
    }
}
