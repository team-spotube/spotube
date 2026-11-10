package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class ResiliencePipelineTest {

    @Test
    fun `executes successfully on first attempt`() = runTest {
        val testClock = TestClock()
        val pipeline = ResiliencePipeline(
            ResiliencePipelineConfig.DEFAULT_MUSICBRAINZ,
            testClock
        )

        var callCount = 0
        val result = pipeline.execute {
            callCount++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `retries on failure and succeeds`() = runTest {
        val testClock = TestClock()
        val pipeline = ResiliencePipeline(
            ResiliencePipelineConfig(
                rateLimiterConfig = TokenBucketConfig(ratePerSecond = 1000.0, burst = 1000),
                circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 10),
                retryConfig = RetryConfig(maxAttempts = 3, initialDelay = Duration.milliseconds(10))
            ),
            testClock
        )

        var callCount = 0
        val result = pipeline.execute {
            callCount++
            if (callCount < 2) throw Exception("HTTP 503")
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `throws after max retries exhausted`() = runTest {
        val testClock = TestClock()
        val pipeline = ResiliencePipeline(
            ResiliencePipelineConfig(
                rateLimiterConfig = TokenBucketConfig(ratePerSecond = 1000.0, burst = 1000),
                circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 10),
                retryConfig = RetryConfig(maxAttempts = 3, initialDelay = Duration.milliseconds(10))
            ),
            testClock
        )

        var callCount = 0
        var caught = false
        try {
            pipeline.execute {
                callCount++
                throw Exception("HTTP 503")
            }
        } catch (e: Exception) {
            caught = true
        }

        assertTrue(caught)
        assertEquals(3, callCount)
    }

    @Test
    fun `rate limits calls`() = runTest {
        val testClock = TestClock()
        val pipeline = ResiliencePipeline(
            ResiliencePipelineConfig(
                rateLimiterConfig = TokenBucketConfig(ratePerSecond = 10.0, burst = 2),
                circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 10),
                retryConfig = RetryConfig(maxAttempts = 1)
            ),
            testClock
        )

        var callCount = 0
        repeat(3) {
            pipeline.executeWithoutRateLimit {
                callCount++
                "ok"
            }
        }

        assertEquals(3, callCount)
    }

    @Test
    fun `opens circuit breaker on repeated failures`() = runTest {
        val testClock = TestClock()
        val pipeline = ResiliencePipeline(
            ResiliencePipelineConfig(
                rateLimiterConfig = TokenBucketConfig(ratePerSecond = 1000.0, burst = 1000),
                circuitBreakerConfig = CircuitBreakerConfig(failureThreshold = 2),
                retryConfig = RetryConfig(maxAttempts = 1)
            ),
            testClock
        )

        repeat(2) {
            try { pipeline.execute { throw Exception("HTTP 503") } } catch (_) {}
        }

        assertEquals(CircuitState.OPEN, pipeline.circuitBreakerComponent.state)

        var caught = false
        try {
            pipeline.execute { "should not reach" }
        } catch (e: CircuitBreakerOpenException) {
            caught = true
        }
        assertTrue(caught)
    }

    @Test
    fun `half-open allows probes after recovery timeout`() = runTest {
        val testClock = TestClock()
        val pipeline = ResiliencePipeline(
            ResiliencePipelineConfig(
                rateLimiterConfig = TokenBucketConfig(ratePerSecond = 1000.0, burst = 1000),
                circuitBreakerConfig = CircuitBreakerConfig(
                    failureThreshold = 2,
                    recoveryTimeout = Duration.seconds(1)
                ),
                retryConfig = RetryConfig(maxAttempts = 1)
            ),
            testClock
        )

        repeat(2) {
            try { pipeline.execute { throw Exception("HTTP 503") } } catch (_) {}
        }
        assertEquals(CircuitState.OPEN, pipeline.circuitBreakerComponent.state)

        testClock.advanceTime(Duration.seconds(2))
        assertEquals(CircuitState.HALF_OPEN, pipeline.circuitBreakerComponent.state)

        pipeline.execute { "success" }
        repeat(2) { pipeline.execute { "success" } }
        assertEquals(CircuitState.CLOSED, pipeline.circuitBreakerComponent.state)
    }
}