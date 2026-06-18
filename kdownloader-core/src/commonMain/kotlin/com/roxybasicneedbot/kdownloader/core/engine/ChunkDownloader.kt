package com.roxybasicneedbot.kdownloader.core.engine

import com.roxybasicneedbot.kdownloader.core.network.BandwidthController
import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class ChunkDownloader(
    private val client: HttpClient,
    private val fileStorage: PlatformFileStorage,
    private val bandwidthController: BandwidthController?
) {
    suspend fun download(
        url: String,
        tempFilePath: String,
        startByte: Long,
        endByte: Long,
        downloadedBytes: Long,
        onProgress: (bytesDownloadedThisChunk: Long) -> Unit
    ) {
        val currentStart = startByte + downloadedBytes
        if (endByte in 1..currentStart) {
            return // Chunk already complete
        }

        val rangeHeaderValue = if (endByte > 0L) "bytes=$currentStart-$endByte" else "bytes=$currentStart-"
        
        client.prepareGet(url) {
            headers {
                append(HttpHeaders.Range, rangeHeaderValue)
            }
        }.execute { response ->
            val channel = response.bodyAsChannel()
            var offset = downloadedBytes
            val buffer = ByteArray(8192)

            while (!channel.isClosedForRead && coroutineContext.isActive) {
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read <= 0) break

                bandwidthController?.acquire(read.toLong())

                val dataToWrite = if (read < buffer.size) buffer.copyOf(read) else buffer
                fileStorage.write(tempFilePath, offset, dataToWrite)
                
                offset += read
                onProgress(read.toLong())
            }
        }
    }
}
