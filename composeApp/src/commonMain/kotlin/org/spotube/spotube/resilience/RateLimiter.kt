package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Interface for rate limiting implementations.
 * Allows different rate limiting strategies (token bucket, fixed window, sliding window, etc.)
 */
@ExperimentalCoroutinesApi
interface RateLimiter {

    /**
     * Attempts to acquire a permit. Suspends until a permit is available.
     * Returns true when a permit was acquired.
     */
    suspend fun acquire(): Boolean

    /**
     * Attempts to acquire a permit without suspending.
     * Returns true if a permit was immediately available and acquired.
     */
    fun tryAcquire(): Boolean

    /**
     * Current number of available permits (for monitoring/debugging).
     */
    val availablePermits: Double

    /**
     * Resets the rate limiter to its initial state.
     */
    fun reset()
}

/**
 * Configuration for TokenBucketRateLimiter.
 * @param ratePerSecond Tokens added per second (sustained rate).
 * @param burst Maximum bucket capacity (burst allowance).
 */
data class TokenBucketConfig(
    val ratePerSecond: Double = 8.0,
    val burst: Int = 16
) {
    init {
        require(ratePerSecond > 0) { "ratePerSecond must be positive" }
        require(burst > 0) { "burst must be positive" }
    }
}

/**
 * Clock abstraction for testability.
 */
interface Clock {
    fun now(): Long // epoch milliseconds
}

object SystemClock : Clock {
    override fun now() = System.currentTimeMillis()
}

class TestClock(private var _time: Long = 0) : Clock {
    override fun now() = _time
    fun advance(millis: Long) { _time += millis }
    fun setTime(millis: Long) { _time = millis }
}