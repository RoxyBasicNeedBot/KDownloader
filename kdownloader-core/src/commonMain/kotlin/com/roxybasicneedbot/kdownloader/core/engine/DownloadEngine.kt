package com.roxybasicneedbot.kdownloader.core.engine

import com.roxybasicneedbot.kdownloader.core.model.*
import com.roxybasicneedbot.kdownloader.core.network.BandwidthController
import com.roxybasicneedbot.kdownloader.core.network.HttpClientFactory
import com.roxybasicneedbot.kdownloader.core.network.PlatformNetworkMonitor
import com.roxybasicneedbot.kdownloader.core.storage.PlatformFileStorage
import com.roxybasicneedbot.kdownloader.core.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadEngine(
    private val config: DownloadConfig,
    private val fileStorage: PlatformFileStorage,
    private val networkMonitor: PlatformNetworkMonitor,
    private val httpClientFactory: HttpClientFactory
) {
    private val client = httpClientFactory.create(config)
    private val rangeValidator = RangeValidator(client)
    private val activeJobs = mutableMapOf<String, Job>()
    private val jobsMutex = Mutex()
    
    private val _states = MutableSharedFlow<Pair<String, DownloadState>>(replay = 1)
    val states: SharedFlow<Pair<String, DownloadState>> = _states.asSharedFlow()

    suspend fun start(request: DownloadRequest) {
        jobsMutex.withLock {
            if (activeJobs.containsKey(request.id)) return
            val job = CoroutineScope(Dispatchers.Default).launch {
                runDownload(request)
            }
            activeJobs[request.id] = job
        }
    }

    suspend fun stop(requestId: String) {
        jobsMutex.withLock {
            val job = activeJobs.remove(requestId)
            job?.cancel()
            _states.emit(requestId to DownloadState.Paused)
        }
    }

    private suspend fun runDownload(request: DownloadRequest) {
        val id = request.id
        try {
            _states.emit(id to DownloadState.Connecting)
            
            val validation = rangeValidator.validate(request.url, request.headers)
            val totalBytes = validation.contentLength
            val supportsRange = validation.supportsRange
            
            val finalDest = "${request.destinationDir}/${request.fileName}"
            
            val chunkCount = if (supportsRange && totalBytes > 0) request.chunkCount else 1
            val chunks = mutableListOf<ChunkInfo>()
            val chunkPaths = mutableListOf<String>()

            val limit = request.speedLimit.coerceAtLeast(0)
            val bandwidthController = if (limit > 0) BandwidthController(limit) else null

            if (chunkCount > 1) {
                val chunkSize = totalBytes / chunkCount
                for (i in 0 until chunkCount) {
                    val start = i * chunkSize
                    val end = if (i == chunkCount - 1) totalBytes - 1 else (i + 1) * chunkSize - 1
                    val tempPath = "${finalDest}.chunk_$i"
                    chunkPaths.add(tempPath)
                    chunks.add(
                        ChunkInfo(
                            downloadId = id,
                            chunkIndex = i,
                            startByte = start,
                            endByte = end,
                            downloadedBytes = 0L,
                            status = ChunkStatus.PENDING,
                            tempFilePath = tempPath
                        )
                    )
                }
            } else {
                val tempPath = "${finalDest}.temp"
                chunkPaths.add(tempPath)
                chunks.add(
                    ChunkInfo(
                        downloadId = id,
                        chunkIndex = 0,
                        startByte = 0L,
                        endByte = totalBytes,
                        downloadedBytes = 0L,
                        status = ChunkStatus.PENDING,
                        tempFilePath = tempPath
                    )
                )
            }

            _states.emit(id to DownloadState.Downloading(
                DownloadProgress(0, totalBytes, 0, 0, "0 B/s", -1, "Unknown", chunkCount, chunkCount, emptyList())
            ))

            val speedCalculator = SpeedCalculator()
            val chunkDownloader = ChunkDownloader(client, fileStorage, bandwidthController)
            val mirrorCoordinator = MirrorCoordinator(request.mirrorUrls)

            coroutineScope {
                val deferreds = chunks.mapIndexed { index, chunk ->
                    val urlToUse = mirrorCoordinator.getMirrorUrlForChunk(request.url, index)
                    async {
                        var currentChunk = chunk
                        chunkDownloader.download(
                            url = urlToUse,
                            tempFilePath = currentChunk.tempFilePath,
                            startByte = currentChunk.startByte,
                            endByte = currentChunk.endByte,
                            downloadedBytes = currentChunk.downloadedBytes
                        ) { progress ->
                            currentChunk = currentChunk.copy(
                                downloadedBytes = currentChunk.downloadedBytes + progress
                            )
                            val totalDownloaded = chunks.sumOf { it.downloadedBytes }
                            val percent = if (totalBytes > 0) ((totalDownloaded * 100) / totalBytes).toInt() else 0
                            val speed = speedCalculator.update(totalDownloaded, epochMillis())
                            val speedFormatted = SizeFormatter.formatSpeed(speed)
                            val eta = EtaCalculator.calculate(totalDownloaded, totalBytes, speed)
                            val etaFormatted = EtaCalculator.format(eta)

                            launch {
                                _states.emit(id to DownloadState.Downloading(
                                    DownloadProgress(
                                        downloadedBytes = totalDownloaded,
                                        totalBytes = totalBytes,
                                        percent = percent,
                                        speedBytesPerSec = speed,
                                        speedFormatted = speedFormatted,
                                        etaSeconds = eta,
                                        etaFormatted = etaFormatted,
                                        activeChunks = chunkCount,
                                        totalChunks = chunkCount,
                                        chunkProgress = emptyList()
                                    )
                                ))
                            }
                        }
                    }
                }
                deferreds.awaitAll()
            }

            _states.emit(id to DownloadState.Merging)
            val merger = ChunkMerger(fileStorage)
            merger.merge(chunkPaths, finalDest)

            var hashVerified = true
            if (request.hashAlgorithm != null && request.expectedHash != null) {
                _states.emit(id to DownloadState.Verifying(request.hashAlgorithm))
                hashVerified = HashVerifier.verify(finalDest, request.expectedHash, request.hashAlgorithm)
                if (!hashVerified) {
                    throw Exception("Hash mismatch. Expected ${request.expectedHash}")
                }
            }

            val result = DownloadResult(
                id = id,
                filePath = finalDest,
                totalBytes = totalBytes,
                downloadTimeMs = 0L,
                averageSpeedBytesPerSec = 0L,
                hashVerified = hashVerified
            )
            _states.emit(id to DownloadState.Done(result))

        } catch (e: CancellationException) {
            _states.emit(id to DownloadState.Paused)
            throw e
        } catch (e: Exception) {
            _states.emit(id to DownloadState.Failed(
                DownloadError(ErrorCode.NETWORK_ERROR, e.message ?: "Download failed", e),
                0
            ))
        } finally {
            jobsMutex.withLock {
                activeJobs.remove(id)
            }
        }
    }
}
