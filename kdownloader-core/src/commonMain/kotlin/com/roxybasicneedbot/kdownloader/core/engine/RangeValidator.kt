package com.roxybasicneedbot.kdownloader.core.engine

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class RangeValidator(private val client: HttpClient) {
    data class ValidationResult(
        val supportsRange: Boolean,
        val contentLength: Long,
        val etag: String?,
        val lastModified: String?
    )

    suspend fun validate(url: String, headers: Map<String, String>): ValidationResult {
        return try {
            val response: HttpResponse = client.head(url) {
                headers {
                    headers.forEach { (k, v) -> append(k, v) }
                }
            }
            
            if (response.status.isSuccess()) {
                val acceptRanges = response.headers[HttpHeaders.AcceptRanges]
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                val etag = response.headers[HttpHeaders.ETag]
                val lastModified = response.headers[HttpHeaders.LastModified]
                
                val supportsRange = acceptRanges?.contains("bytes", ignoreCase = true) == true || 
                                    response.headers[HttpHeaders.ContentRange] != null
                
                ValidationResult(supportsRange, contentLength, etag, lastModified)
            } else {
                ValidationResult(false, -1L, null, null)
            }
        } catch (e: Exception) {
            ValidationResult(false, -1L, null, null)
        }
    }
}
