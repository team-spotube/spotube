package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Thread-safe Token Bucket Rate Limiter implementation.
 *
 * Algorithm:
 * - Bucket starts full with `burst` tokens
 * - Tokens are refilled at `ratePerSecond` tokens/second
 * - Each `acquire()` consumes 1 token
 * - If bucket is empty, caller suspends until a token is available
 *
 * Based on DuranMusic's `musicbrainz_rate_limiter.dart` using dio_resilience.
 * Parameters: 8 req/s sustained, burst 16 (shared singleton for MusicBrainz/ListenBrainz).
 */
@ExperimentalCoroutinesApi
class TokenBucketRateLimiter(
    private val config: TokenBucketConfig,
    private val clock: Clock = SystemClock
) : RateLimiter {

    private var _tokens: Double = config.burst.toDouble()
    private var _lastRefill: Long = clock.now()
    private val lock = Any()

    override suspend fun acquire(): Boolean {
        synchronized(lock) {
            refill()
            if (_tokens >= 1.0) {
                _tokens -= 1.0
                return true
            }
        }

        // Wait for next token refill
        val waitTime = calculateWaitTime()
        delay(waitTime)
        return acquire() // Tail recursion, will succeed after refill
    }

    override fun tryAcquire(): Boolean {
        synchronized(lock) {
            refill()
            if (_tokens >= 1.0) {
                _tokens -= 1.0
                return true
            }
            return false
        }
    }

    override val availablePermits: Double
        get() {
            synchronized(lock) {
                refill()
                return _tokens
            }
        }

    override fun reset() {
        synchronized(lock) {
            _tokens = config.burst.toDouble()
            _lastRefill = clock.now()
        }
    }

    private fun refill() {
        val now = clock.now()
        val elapsedSeconds = (now - _lastRefill) / 1000.0
        if (elapsedSeconds > 0) {
            val newTokens = _tokens + elapsedSeconds * config.ratePerSecond
            _tokens = kotlin.math.min(newTokens, config.burst.toDouble())
            _lastRefill = now
        }
    }

    private fun calculateWaitTime(): Long {
        synchronized(lock) {
            // Time until 1 token is available
            val tokensNeeded = 1.0 - _tokens
            val secondsNeeded = tokensNeeded / config.ratePerSecond
            return (secondsNeeded * 1000).toLong().coerceAtLeast(1)
        }
    }

    companion object {
        /**
         * Default configuration matching DuranMusic's MusicBrainz rate limiter:
         * 8 requests/second sustained, burst of 16.
         */
        const val DEFAULT_MUSICBRAINZ_CONFIG = TokenBucketConfig(ratePerSecond = 8.0, burst = 16)
    }
}