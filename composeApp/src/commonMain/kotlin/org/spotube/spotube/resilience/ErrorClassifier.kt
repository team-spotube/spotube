package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Classifies errors for intelligent fallback and circuit breaker decisions.
 *
 * Based on DuranMusic's ErrorClassifier in `rate_limiter.dart`.
 * Classifies: OFFLINE (network), SERVICE_UNAVAILABLE (5xx, 429, 408), UNKNOWN
 */
@ExperimentalCoroutinesApi
object ErrorClassifier {

    private val offlineHints = setOf(
        "SocketException",
        "Failed host lookup",
        "Connection refused",
        "Connection reset",
        "Network is unreachable",
        "connection error",
        "Network error",
        "timeout",
        "TimeoutException",
        "ConnectException",
        "UnknownHostException",
        "SSLHandshakeException"
    )

    private val serviceUnavailableHints = setOf(
        "500", "501", "502", "503", "504", "505", "506", "507", "508", "509", "510", "511",
        "429", "408"
    )

    /**
     * Classifies a throwable into an ErrorKind.
     */
    fun classify(throwable: Throwable): ErrorKind {
        val message = throwable.message?.lowercase() ?: ""
        val className = throwable.javaClass.simpleName.lowercase()
        val fullText = "$message $className ${throwable.toString().lowercase()}"

        // Check for offline indicators
        for (hint in offlineHints) {
            if (fullText.contains(hint.lowercase())) {
                return ErrorKind.OFFLINE
            }
        }

        // Check for service unavailable HTTP status codes in message
        for (hint in serviceUnavailableHints) {
            if (fullText.contains(hint)) {
                return ErrorKind.SERVICE_UNAVAILABLE
            }
        }

        // Check for Ktor HttpResponseException
        if (throwable is HttpResponseException) {
            return when {
                throwable.status.value >= 500 -> ErrorKind.SERVICE_UNAVAILABLE
                throwable.status.value == 429 -> ErrorKind.SERVICE_UNAVAILABLE
                throwable.status.value == 408 -> ErrorKind.SERVICE_UNAVAILABLE
                else -> ErrorKind.UNKNOWN
            }
        }

        // Check for Ktor ClientRequestException (network errors)
        if (throwable is ClientRequestException) {
            return ErrorKind.OFFLINE
        }

        return ErrorKind.UNKNOWN
    }

    /**
     * Classifies an HTTP status code.
     */
    fun classifyStatusCode(statusCode: Int): ErrorKind {
        return when {
            statusCode >= 500 -> ErrorKind.SERVICE_UNAVAILABLE
            statusCode == 429 -> ErrorKind.SERVICE_UNAVAILABLE
            statusCode == 408 -> ErrorKind.SERVICE_UNAVAILABLE
            statusCode == 0 -> ErrorKind.OFFLINE // Connection failed
            else -> ErrorKind.UNKNOWN
        }
    }
}

// Type aliases for Ktor exceptions (to avoid direct dependency in commonMain)
@Suppress("UNUSED_PARAMETER")
private class HttpResponseException(
    val status: HttpStatusCode,
    val response: Any?
) : Exception("HTTP ${status.value}")

@Suppress("UNUSED_PARAMETER")
private class ClientRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Suppress("UNUSED_PARAMETER")
private class HttpStatusCode(val value: Int)