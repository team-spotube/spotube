package org.spotube.spotube.resilience

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ErrorClassifierTest {

    @Test
    fun `classifies offline errors`() {
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("SocketException: Connection failed")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("Failed host lookup")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("Connection refused")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("Connection reset")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("Network is unreachable")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("connection error")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("Network error")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("timeout")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("TimeoutException")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("ConnectException")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("UnknownHostException")))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classify(Exception("SSLHandshakeException")))
    }

    @Test
    fun `classifies service unavailable errors`() {
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classify(Exception("HTTP 500")))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classify(Exception("HTTP 502")))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classify(Exception("HTTP 503")))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classify(Exception("HTTP 504")))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classify(Exception("status code of 429")))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classify(Exception("status code of 408")))
    }

    @Test
    fun `classifies unknown errors`() {
        assertEquals(ErrorKind.UNKNOWN, ErrorClassifier.classify(Exception("Some random error")))
        assertEquals(ErrorKind.UNKNOWN, ErrorClassifier.classify(Exception("HTTP 400")))
        assertEquals(ErrorKind.UNKNOWN, ErrorClassifier.classify(Exception("HTTP 404")))
    }

    @Test
    fun `classifies status codes`() {
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classifyStatusCode(500))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classifyStatusCode(503))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classifyStatusCode(429))
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ErrorClassifier.classifyStatusCode(408))
        assertEquals(ErrorKind.OFFLINE, ErrorClassifier.classifyStatusCode(0))
        assertEquals(ErrorKind.UNKNOWN, ErrorClassifier.classifyStatusCode(400))
        assertEquals(ErrorKind.UNKNOWN, ErrorClassifier.classifyStatusCode(404))
    }
}