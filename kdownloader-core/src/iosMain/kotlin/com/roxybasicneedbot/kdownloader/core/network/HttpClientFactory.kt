package com.roxybasicneedbot.kdownloader.core.network

import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual class HttpClientFactory actual constructor() {
    actual fun create(config: DownloadConfig): HttpClient {
        return HttpClient(Darwin) {
            engine {
                configureSession {
                    timeoutIntervalForRequest = config.connectionTimeoutMs.toDouble() / 1000.0
                    timeoutIntervalForResource = config.readTimeoutMs.toDouble() / 1000.0
                }
            }
        }
    }
}
