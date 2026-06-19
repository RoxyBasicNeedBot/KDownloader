package com.roxybasicneedbot.kdownloader.core.network

import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual class HttpClientFactory actual constructor() {
    actual fun create(config: DownloadConfig): HttpClient {
        return HttpClient(CIO) {
            engine {
                requestTimeout = config.readTimeoutMs
                endpoint {
                    connectTimeout = config.connectionTimeoutMs
                }
            }
        }
    }
}
