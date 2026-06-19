package com.roxybasicneedbot.kdownloader.core.network

import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual class HttpClientFactory actual constructor() {
    actual fun create(config: DownloadConfig): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
                    readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
                }
            }
        }
    }
}
