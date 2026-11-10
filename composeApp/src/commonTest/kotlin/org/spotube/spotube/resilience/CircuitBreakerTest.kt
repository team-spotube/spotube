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
class CircuitBreakerTest {

    @Test
    fun `opens after threshold failures`() = runTest {
        val cb = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 3))

        repeat(2) { cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) }
        assertEquals(CircuitState.CLOSED, cb.state)
        assertTrue(cb.canProceed())

        cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) // 3rd failure
        assertEquals(CircuitState.OPEN, cb.state)
        assertFalse(cb.canProceed())
    }

    @Test
    fun `enters half-open after recovery timeout`() = runTest {
        val testClock = TestClock()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(
                failureThreshold = 2,
                recoveryTimeout = Duration.seconds(10)
            ),
            testClock
        )

        repeat(2) { cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) }
        assertEquals(CircuitState.OPEN, cb.state)

        // Advance past recovery timeout
        testClock.advanceTime(Duration.seconds(11))
        assertEquals(CircuitState.HALF_OPEN, cb.state)
        assertTrue(cb.canProceed())
    }

    @Test
    fun `closes after successful half-open probes`() = runTest {
        val testClock = TestClock()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(
                failureThreshold = 2,
                recoveryTimeout = Duration.seconds(10),
                halfOpenMaxAttempts = 2
            ),
            testClock
        )

        repeat(2) { cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) }
        testClock.advanceTime(Duration.seconds(11)) // → HALF_OPEN

        cb.recordSuccess() // 1
        assertEquals(CircuitState.HALF_OPEN, cb.state)

        cb.recordSuccess() // 2 → CLOSED
        assertEquals(CircuitState.CLOSED, cb.state)
        assertTrue(cb.canProceed())
    }

    @Test
    fun `failure in half-open reopens immediately`() = runTest {
        val testClock = TestClock()
        val cb = CircuitBreaker(
            CircuitBreakerConfig(
                failureThreshold = 2,
                recoveryTimeout = Duration.seconds(10),
                halfOpenMaxAttempts = 3
            ),
            testClock
        )

        repeat(2) { cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) }
        testClock.advanceTime(Duration.seconds(11)) // → HALF_OPEN

        cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) // Failure in half-open
        assertEquals(CircuitState.OPEN, cb.state)
        assertFalse(cb.canProceed())
    }

    @Test
    fun `reset closes circuit`() = runTest {
        val cb = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 2))

        repeat(2) { cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE) }
        assertEquals(CircuitState.OPEN, cb.state)

        cb.reset()
        assertEquals(CircuitState.CLOSED, cb.state)
        assertTrue(cb.canProceed())
        assertEquals(0, cb.consecutiveFailures)
    }

    @Test
    fun `success in closed resets failure count`() = runTest {
        val cb = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 3))

        cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE)
        cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE)
        assertEquals(2, cb.consecutiveFailures)

        cb.recordSuccess()
        assertEquals(0, cb.consecutiveFailures)
    }

    @Test
    fun `classifies error kind on failure`() = runTest {
        val cb = CircuitBreaker(CircuitBreakerConfig(failureThreshold = 2))

        cb.recordFailure(ErrorKind.OFFLINE)
        assertEquals(ErrorKind.OFFLINE, cb.lastErrorKind)

        cb.recordFailure(ErrorKind.SERVICE_UNAVAILABLE)
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, cb.lastErrorKind)
    }
}