package com.roxybasicneedbot.kdownloader.core.storage

import java.io.File
import java.io.RandomAccessFile

actual class PlatformFileStorage {
    actual constructor()

    actual suspend fun write(path: String, offset: Long, data: ByteArray) {
        val file = File(path)
        file.parentFile?.mkdirs()
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(offset)
            raf.write(data)
        }
    }

    actual suspend fun read(path: String, offset: Long, length: Int): ByteArray {
        val file = File(path)
        if (!file.exists()) return ByteArray(0)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val buffer = ByteArray(length)
            val read = raf.read(buffer)
            return if (read < length) buffer.copyOf(read) else buffer
        }
    }

    actual suspend fun delete(path: String) {
        File(path).delete()
    }

    actual suspend fun exists(path: String): Boolean {
        return File(path).exists()
    }

    actual suspend fun size(path: String): Long {
        return File(path).length()
    }

    actual suspend fun createTempFile(prefix: String, suffix: String): String {
        val temp = File.createTempFile(prefix, suffix)
        return temp.absolutePath
    }

    actual suspend fun mergeFiles(sourcePaths: List<String>, destPath: String) {
        val destFile = File(destPath)
        destFile.parentFile?.mkdirs()
        destFile.outputStream().use { output ->
            sourcePaths.forEach { srcPath ->
                val srcFile = File(srcPath)
                if (srcFile.exists()) {
                    srcFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
