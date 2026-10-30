package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.retry.RetryOnException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Configuration for the complete resilience pipeline.
 */
@ExperimentalTime
data class ResiliencePipelineConfig(
    val rateLimiterConfig: TokenBucketConfig = TokenBucketConfig.DEFAULT_MUSICBRAINZ_CONFIG,
    val circuitBreakerConfig: CircuitBreakerConfig = CircuitBreakerConfig.DEFAULT_MUSICBRAINZ_CONFIG,
    val retryConfig: RetryConfig = RetryConfig.DEFAULT
)

/**
 * Retry configuration with exponential backoff and jitter.
 */
@ExperimentalTime
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = Duration.milliseconds(400),
    val multiplier: Double = 2.0,
    val maxDelay: Duration = Duration.seconds(10)
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(initialDelay > Duration.ZERO) { "initialDelay must be positive" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
        require(maxDelay >= initialDelay) { "maxDelay must be >= initialDelay" }
    }

    companion object {
        const val DEFAULT = RetryConfig()
    }
}

/**
 * Complete resilience pipeline combining:
 * 1. Rate Limiter (Token Bucket)
 * 2. Circuit Breaker
 * 3. Retry with Exponential Backoff + Jitter
 * 4. Error Classification
 *
 * Based on DuranMusic's dio_resilience pipeline in `musicbrainz_rate_limiter.dart`.
 * Usage:
 * ```kotlin
 * val pipeline = ResiliencePipeline(ResiliencePipelineConfig.DEFAULT_MUSICBRAINZ)
 * val result = pipeline.execute { apiCall() }
 * ```
 */
@ExperimentalTime
@ExperimentalCoroutinesApi
class ResiliencePipeline(
    private val config: ResiliencePipelineConfig = ResiliencePipelineConfig(),
    private val clock: Clock = SystemClock
) {

    private val rateLimiter = TokenBucketRateLimiter(config.rateLimiterConfig, clock)
    private val circuitBreaker = CircuitBreaker(config.circuitBreakerConfig, clock)

    /**
     * Executes a suspending function with full resilience pipeline.
     * - Rate limits the call
     * - Checks circuit breaker
     * - Retries with exponential backoff + jitter
     * - Records success/failure in circuit breaker
     */
    suspend fun <T> execute(block: suspend () -> T): T {
        // Rate limiting
        rateLimiter.acquire()

        // Circuit breaker check
        if (!circuitBreaker.canProceed()) {
            throw CircuitBreakerOpenException("Circuit breaker is OPEN")
        }

        // Retry with exponential backoff + jitter
        return retryWithBackoff(block)
    }

    /**
     * Executes without rate limiting (for ListenBrainz which has different limits).
     * Still applies circuit breaker and retry.
     */
    suspend fun <T> executeWithoutRateLimit(block: suspend () -> T): T {
        if (!circuitBreaker.canProceed()) {
            throw CircuitBreakerOpenException("Circuit breaker is OPEN")
        }
        return retryWithBackoff(block)
    }

    private suspend fun <T> retryWithBackoff(block: suspend () -> T): T {
        var attempt = 0
        var lastException: Throwable? = null

        while (true) {
            attempt++
            try {
                val result = block()
                circuitBreaker.recordSuccess()
                return result
            } catch (e: Exception) {
                lastException = e
                val errorKind = ErrorClassifier.classify(e)
                circuitBreaker.recordFailure(errorKind)

                if (attempt >= config.retryConfig.maxAttempts) {
                    throw e
                }

                // Exponential backoff with jitter
                val delay = calculateDelay(attempt)
                kotlinx.coroutines.delay(delay)
            }
        }
    }

    private fun calculateDelay(attempt: Int): Duration {
        val baseDelay = config.retryConfig.initialDelay * (config.retryConfig.multiplier.pow(attempt - 1))
        val cappedDelay = kotlin.math.min(baseDelay, config.retryConfig.maxDelay)

        // Add jitter: random between 0 and delay
        val jitter = (Math.random() * cappedDelay.toMilliseconds()).toLong()
        return Duration.milliseconds(jitter)
    }

    // Expose components for monitoring/testing
    val rateLimiterComponent: RateLimiter = rateLimiter
    val circuitBreakerComponent: CircuitBreaker = circuitBreaker
}

/**
 * Exception thrown when circuit breaker is open.
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

/**
 * Default configuration for MusicBrainz/ListenBrainz.
 */
@ExperimentalTime
object ResiliencePipelineConfig {
    const val DEFAULT_MUSICBRAINZ = ResiliencePipelineConfig(
        rateLimiterConfig = TokenBucketConfig.DEFAULT_MUSICBRAINZ_CONFIG,
        circuitBreakerConfig = CircuitBreakerConfig.DEFAULT_MUSICBRAINZ_CONFIG,
        retryConfig = RetryConfig.DEFAULT
    )

    const val DEFAULT_LISTENBRAINZ = ResiliencePipelineConfig(
        rateLimiterConfig = TokenBucketConfig(ratePerSecond = 10.0, burst = 20), // Higher limits for LB
        circuitBreakerConfig = CircuitBreakerConfig.DEFAULT_MUSICBRAINZ_CONFIG,
        retryConfig = RetryConfig.DEFAULT
    )
}