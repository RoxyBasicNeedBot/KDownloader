package com.roxybasicneedbot.kdownloader.core.util

expect object HashVerifier {
    suspend fun verify(filePath: String, expectedHash: String, algorithm: String): Boolean
}
