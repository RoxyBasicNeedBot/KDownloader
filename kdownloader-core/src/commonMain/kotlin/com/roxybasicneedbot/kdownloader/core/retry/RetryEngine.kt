package com.roxybasicneedbot.kdownloader.core.retry

import kotlinx.coroutines.delay

data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000L,
    val multiplier: Double = 2.0
)

class RetryEngine(private val policy: RetryPolicy) {
    suspend fun <T> execute(
        onRetry: suspend (attempt: Int, error: Throwable) -> Unit = { _, _ -> },
        block: suspend () -> T
    ): T {
        var attempt = 0
        var currentDelay = policy.initialDelayMs
        while (true) {
            try {
                return block()
            } catch (e: Throwable) {
                attempt++
                if (attempt > policy.maxRetries) {
                    throw e
                }
                onRetry(attempt, e)
                delay(currentDelay)
                currentDelay = (currentDelay * policy.multiplier).toLong()
            }
        }
    }
}
