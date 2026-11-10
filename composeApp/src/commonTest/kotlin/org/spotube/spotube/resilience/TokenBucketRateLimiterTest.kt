package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class TokenBucketRateLimiterTest {

    @Test
    fun `allows burst up to capacity`() = runTest {
        val limiter = TokenBucketRateLimiter(TokenBucketConfig(burst = 5, ratePerSecond = 1.0))

        repeat(5) { assertTrue(limiter.tryAcquire()) }
        assertFalse(limiter.tryAcquire()) // 6th should fail
    }

    @Test
    fun `refills tokens over time`() = runTest {
        val testClock = TestClock()
        val limiter = TokenBucketRateLimiter(
            TokenBucketConfig(burst = 2, ratePerSecond = 10.0),
            testClock
        )

        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire()) // burst exhausted

        // Advance 1 second → +10 tokens, clamped to burst (2)
        testClock.advanceTime(Duration.seconds(1))
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun `clamps at burst capacity`() = runTest {
        val testClock = TestClock()
        val limiter = TokenBucketRateLimiter(
            TokenBucketConfig(burst = 3, ratePerSecond = 100.0),
            testClock
        )

        // Advance 10 seconds → would be 1000 tokens
        testClock.advanceTime(Duration.seconds(10))
        assertEquals(3.0, limiter.availablePermits) // Clamped to burst
    }

    @Test
    fun `acquire suspends when empty and resumes after refill`() = runTest {
        val testClock = TestClock()
        val limiter = TokenBucketRateLimiter(
            TokenBucketConfig(burst = 1, ratePerSecond = 10.0),
            testClock
        )

        assertTrue(limiter.tryAcquire()) // Take the only token
        assertFalse(limiter.tryAcquire()) // Empty

        // Start acquire in background - it should suspend
        val deferred = kotlinx.coroutines.async { limiter.acquire() }
        assertFalse(deferred.isCompleted) // Still suspended

        // Advance time to allow refill
        testClock.advanceTime(Duration.milliseconds(200))
        assertTrue(deferred.await()) // Should complete now
    }

    @Test
    fun `reset restores initial state`() = runTest {
        val limiter = TokenBucketRateLimiter(TokenBucketConfig(burst = 2, ratePerSecond = 1.0))

        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())

        limiter.reset()
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
    }
}