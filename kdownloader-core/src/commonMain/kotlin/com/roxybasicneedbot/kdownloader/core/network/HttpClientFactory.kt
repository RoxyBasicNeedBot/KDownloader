package com.roxybasicneedbot.kdownloader.core.network

import com.roxybasicneedbot.kdownloader.core.model.DownloadConfig
import io.ktor.client.HttpClient

expect class HttpClientFactory {
    constructor()
    fun create(config: DownloadConfig): HttpClient
}
