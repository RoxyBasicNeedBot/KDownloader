package com.roxybasicneedbot.kdownloader.core.engine

class MirrorCoordinator(private val mirrorUrls: List<String>) {
    private var currentMirrorIndex = 0

    fun getNextUrl(primaryUrl: String): String {
        if (mirrorUrls.isEmpty()) return primaryUrl
        val url = mirrorUrls[currentMirrorIndex]
        currentMirrorIndex = (currentMirrorIndex + 1) % mirrorUrls.size
        return url
    }

    fun getMirrorUrlForChunk(primaryUrl: String, chunkIndex: Int): String {
        if (mirrorUrls.isEmpty()) return primaryUrl
        val index = chunkIndex % (mirrorUrls.size + 1)
        return if (index == 0) primaryUrl else mirrorUrls[index - 1]
    }
}
