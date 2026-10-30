package org.spotube.spotube.resilience

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Error classification for intelligent circuit breaker and fallback decisions.
 */
enum class ErrorKind {
    UNKNOWN,
    OFFLINE,              // Network connectivity issues
    SERVICE_UNAVAILABLE   // 5xx, 429 (rate limit), 408 (timeout)
}

/**
 * Circuit breaker states.
 */
enum class CircuitState {
    CLOSED,     // Normal operation, requests pass through
    OPEN,       // Failing fast, requests blocked
    HALF_OPEN   // Testing recovery, limited probes allowed
}

/**
 * Configuration for CircuitBreaker.
 * @param failureThreshold Consecutive failures before opening circuit.
 * @param recoveryTimeout Time to wait before attempting half-open.
 * @param halfOpenMaxAttempts Successful probes in half-open before closing.
 */
@ExperimentalTime
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val recoveryTimeout: Duration = Duration.seconds(30),
    val halfOpenMaxAttempts: Int = 3
) {
    init {
        require(failureThreshold > 0) { "failureThreshold must be positive" }
        require(recoveryTimeout > Duration.ZERO) { "recoveryTimeout must be positive" }
        require(halfOpenMaxAttempts > 0) { "halfOpenMaxAttempts must be positive" }
    }
}

/**
 * Circuit Breaker implementation for protecting against cascading failures.
 *
 * State transitions:
 * - CLOSED → OPEN: After `failureThreshold` consecutive failures
 * - OPEN → HALF_OPEN: After `recoveryTimeout` elapsed
 * - HALF_OPEN → CLOSED: After `halfOpenMaxAttempts` successful probes
 * - HALF_OPEN → OPEN: On any failure during half-open
 * - Any state → CLOSED: On `reset()`
 *
 * Based on DuranMusic's manual CircuitBreaker in `rate_limiter.dart`.
 * Parameters: 5 failures threshold, 30s recovery, 3 half-open probes.
 */
@ExperimentalTime
class CircuitBreaker(
    private val config: CircuitBreakerConfig = CircuitBreakerConfig(),
    private val clock: Clock = SystemClock
) {

    private var _state = CircuitState.CLOSED
    private var _consecutiveFailures = 0
    private var _openedAt: Long = 0
    private var _halfOpenSuccesses = 0
    private var _lastErrorKind = ErrorKind.UNKNOWN
    private val lock = Any()

    val state: CircuitState
        get() {
            synchronized(lock) {
                updateStateFromTime()
                return _state
            }
        }

    val lastErrorKind: ErrorKind
        get() = _lastErrorKind

    val consecutiveFailures: Int
        get() = _consecutiveFailures

    /**
     * Checks if a request can proceed.
     * Returns false when circuit is OPEN (blocking requests).
     */
    fun canProceed(): Boolean {
        synchronized(lock) {
            updateStateFromTime()
            return _state != CircuitState.OPEN
        }
    }

    /**
     * Records a successful request outcome.
     */
    fun recordSuccess() {
        synchronized(lock) {
            updateStateFromTime()
            when (_state) {
                CircuitState.CLOSED -> {
                    _consecutiveFailures = 0
                    _lastErrorKind = ErrorKind.UNKNOWN
                }
                CircuitState.HALF_OPEN -> {
                    _halfOpenSuccesses++
                    if (_halfOpenSuccesses >= config.halfOpenMaxAttempts) {
                        closeCircuit()
                    }
                }
                CircuitState.OPEN -> {
                    // Should not happen if canProceed() is respected
                }
            }
        }
    }

    /**
     * Records a failed request outcome.
     */
    fun recordFailure(errorKind: ErrorKind = ErrorKind.UNKNOWN) {
        synchronized(lock) {
            _lastErrorKind = errorKind
            _consecutiveFailures++

            when (_state) {
                CircuitState.CLOSED -> {
                    if (_consecutiveFailures >= config.failureThreshold) {
                        openCircuit()
                    }
                }
                CircuitState.HALF_OPEN -> {
                    // Any failure in half-open immediately reopens
                    openCircuit()
                }
                CircuitState.OPEN -> {
                    // Already open, just update timestamp
                    _openedAt = clock.now()
                }
            }
        }
    }

    /**
     * Manually resets the circuit breaker to closed state.
     */
    fun reset() {
        synchronized(lock) {
            _state = CircuitState.CLOSED
            _consecutiveFailures = 0
            _openedAt = 0
            _halfOpenSuccesses = 0
            _lastErrorKind = ErrorKind.UNKNOWN
        }
    }

    private fun updateStateFromTime() {
        if (_state == CircuitState.OPEN && _openedAt > 0) {
            val elapsed = clock.now() - _openedAt
            if (elapsed >= config.recoveryTimeout.toMilliseconds()) {
                enterHalfOpen()
            }
        }
    }

    private fun openCircuit() {
        _state = CircuitState.OPEN
        _openedAt = clock.now()
        _halfOpenSuccesses = 0
    }

    private fun enterHalfOpen() {
        _state = CircuitState.HALF_OPEN
        _openedAt = 0
        _halfOpenSuccesses = 0
        _consecutiveFailures = config.failureThreshold - 1 // Allow probes
    }

    private fun closeCircuit() {
        _state = CircuitState.CLOSED
        _consecutiveFailures = 0
        _openedAt = 0
        _halfOpenSuccesses = 0
        _lastErrorKind = ErrorKind.UNKNOWN
    }

    companion object {
        @ExperimentalTime
        const val DEFAULT_MUSICBRAINZ_CONFIG = CircuitBreakerConfig(
            failureThreshold = 5,
            recoveryTimeout = Duration.seconds(30),
            halfOpenMaxAttempts = 3
        )
    }
}