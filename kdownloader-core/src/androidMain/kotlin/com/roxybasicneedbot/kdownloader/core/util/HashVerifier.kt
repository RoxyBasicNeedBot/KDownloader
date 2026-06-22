package com.roxybasicneedbot.kdownloader.core.util

import java.io.File
import java.security.MessageDigest

actual object HashVerifier {
    actual suspend fun verify(
        filePath: String,
        expectedHash: String,
        algorithm: String,
    ): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            val digest = MessageDigest.getInstance(algorithm)
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read = input.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            val hashBytes = digest.digest()
            val hashStr = hashBytes.joinToString("") { "%02x".format(it) }
            hashStr.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
