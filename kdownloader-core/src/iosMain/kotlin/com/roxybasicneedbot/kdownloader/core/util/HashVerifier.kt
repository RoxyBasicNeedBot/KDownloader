package com.roxybasicneedbot.kdownloader.core.util

import kotlinx.cinterop.*
import platform.posix.*
import platform.CoreCrypto.*

@OptIn(ExperimentalForeignApi::class)
actual object HashVerifier {
    actual suspend fun verify(filePath: String, expectedHash: String, algorithm: String): Boolean {
        val file = fopen(filePath, "rb") ?: return false
        try {
            val algo = algorithm.uppercase()
            val bufferSize = 8192
            val buffer = ByteArray(bufferSize)

            when (algo) {
                "MD5" -> {
                    memScoped {
                        val ctx = alloc<CC_MD5_CTX>()
                        CC_MD5_Init(ctx.ptr)
                        buffer.usePinned { pinned ->
                            while (true) {
                                val bytesRead = fread(pinned.addressOf(0), 1.convert(), bufferSize.convert(), file).toLong()
                                if (bytesRead <= 0) break
                                CC_MD5_Update(ctx.ptr, pinned.addressOf(0), bytesRead.convert())
                            }
                        }
                        val digest = ByteArray(16)
                        digest.usePinned { digestPinned ->
                            CC_MD5_Final(digestPinned.addressOf(0).reinterpret(), ctx.ptr)
                        }
                        val hashStr = digest.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
                        return hashStr.equals(expectedHash, ignoreCase = true)
                    }
                }
                "SHA-1", "SHA1" -> {
                    memScoped {
                        val ctx = alloc<CC_SHA1_CTX>()
                        CC_SHA1_Init(ctx.ptr)
                        buffer.usePinned { pinned ->
                            while (true) {
                                val bytesRead = fread(pinned.addressOf(0), 1.convert(), bufferSize.convert(), file).toLong()
                                if (bytesRead <= 0) break
                                CC_SHA1_Update(ctx.ptr, pinned.addressOf(0), bytesRead.convert())
                            }
                        }
                        val digest = ByteArray(20)
                        digest.usePinned { digestPinned ->
                            CC_SHA1_Final(digestPinned.addressOf(0).reinterpret(), ctx.ptr)
                        }
                        val hashStr = digest.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
                        return hashStr.equals(expectedHash, ignoreCase = true)
                    }
                }
                "SHA-256", "SHA256" -> {
                    memScoped {
                        val ctx = alloc<CC_SHA256_CTX>()
                        CC_SHA256_Init(ctx.ptr)
                        buffer.usePinned { pinned ->
                            while (true) {
                                val bytesRead = fread(pinned.addressOf(0), 1.convert(), bufferSize.convert(), file).toLong()
                                if (bytesRead <= 0) break
                                CC_SHA256_Update(ctx.ptr, pinned.addressOf(0), bytesRead.convert())
                            }
                        }
                        val digest = ByteArray(32)
                        digest.usePinned { digestPinned ->
                            CC_SHA256_Final(digestPinned.addressOf(0).reinterpret(), ctx.ptr)
                        }
                        val hashStr = digest.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
                        return hashStr.equals(expectedHash, ignoreCase = true)
                    }
                }
                else -> return false
            }
        } catch (e: Exception) {
            return false
        } finally {
            fclose(file)
        }
    }
}
