package com.roxybasicneedbot.kdownloader.core.storage

import kotlinx.cinterop.*
import platform.posix.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileStorage actual constructor() {

    private fun createParentDirectories(path: String) {
        val fileManager = NSFileManager.defaultManager
        val url = NSURL.fileURLWithPath(path)
        val parentUrl = url.URLByDeletingLastPathComponent
        if (parentUrl != null) {
            fileManager.createDirectoryAtURL(parentUrl, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }

    actual suspend fun write(path: String, offset: Long, data: ByteArray) {
        createParentDirectories(path)
        val file = fopen(path, "r+b") ?: fopen(path, "w+b") ?: throw Exception("Failed to open file: $path")
        try {
            fseek(file, offset, SEEK_SET)
            if (data.isNotEmpty()) {
                data.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1.convert(), data.size.convert(), file)
                }
            }
        } finally {
            fclose(file)
        }
    }

    actual suspend fun read(path: String, offset: Long, length: Int): ByteArray {
        val file = fopen(path, "rb") ?: return ByteArray(0)
        try {
            fseek(file, offset, SEEK_SET)
            val buffer = ByteArray(length)
            if (length > 0) {
                val bytesRead = buffer.usePinned { pinned ->
                    fread(pinned.addressOf(0), 1.convert(), length.convert(), file).toLong()
                }
                return if (bytesRead < length) buffer.copyOf(bytesRead.toInt()) else buffer
            }
            return ByteArray(0)
        } finally {
            fclose(file)
        }
    }

    actual suspend fun delete(path: String) {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }

    actual suspend fun exists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }

    actual suspend fun size(path: String): Long {
        val file = fopen(path, "rb") ?: return 0L
        try {
            fseek(file, 0, SEEK_END)
            return ftell(file).toLong()
        } finally {
            fclose(file)
        }
    }

    actual suspend fun createTempFile(prefix: String, suffix: String): String {
        val tempDir = NSTemporaryDirectory()
        val uniqueName = "$prefix${NSUUID.UUID().UUIDString()}$suffix"
        val tempPath = "$tempDir/$uniqueName"
        val file = fopen(tempPath, "wb") ?: throw Exception("Failed to create temp file: $tempPath")
        fclose(file)
        return tempPath
    }

    actual suspend fun mergeFiles(sourcePaths: List<String>, destPath: String) {
        createParentDirectories(destPath)
        val destFile = fopen(destPath, "wb") ?: throw Exception("Failed to open destination: $destPath")
        try {
            val bufferSize = 64 * 1024
            val buffer = ByteArray(bufferSize)
            buffer.usePinned { pinned ->
                sourcePaths.forEach { srcPath ->
                    val srcFile = fopen(srcPath, "rb") ?: return@forEach
                    try {
                        while (true) {
                            val bytesRead = fread(pinned.addressOf(0), 1.convert(), bufferSize.convert(), srcFile).toLong()
                            if (bytesRead <= 0) break
                            fwrite(pinned.addressOf(0), 1.convert(), bytesRead.convert(), destFile)
                        }
                    } finally {
                        fclose(srcFile)
                    }
                }
            }
        } finally {
            fclose(destFile)
        }
    }
}
