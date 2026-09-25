/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.krtirtho.spotube.core.server

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.di.injectLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import org.koin.core.component.KoinComponent
import kotlin.time.Duration.Companion.milliseconds

internal class StreamProxy(
    private val httpClient: HttpClient,
    private val streamingUrlRepository: StreamingUrlRepository,
    private val cacheManager: CacheManager,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val isCachingEnabled: () -> Boolean,
    private val activePort: () -> Int?,
    private val scope: CoroutineScope,
): KoinComponent {
    val logger by injectLogger<StreamProxy>()

    companion object {
        private const val HOST = "127.0.0.1"
    }

    private val fileSystem = FileSystem.SYSTEM
    private val streamingInProgress = mutableSetOf<String>()
    private val streamingMutex = Mutex()

    private fun getTrackMetadata(trackId: String): MetadataTrack? {
        return audioPlayerQueue.queueFlow.value
            .filterIsInstance<QueueEntry.StreamingTrack>()
            .firstOrNull { it.track.id == trackId }
            ?.track
    }

    suspend fun handleStreamRequest(call: ApplicationCall, requestMethod: HttpMethod) {
        val trackId = call.parameters["trackId"]?.trim().orEmpty()
        if (trackId.isBlank()) {
            logger.w { "Rejecting /stream request with missing track id" }
            call.respond(HttpStatusCode.BadRequest, "Missing track id")
            return
        }

        if (isCachingEnabled()) {
            val cachedEntry = cacheManager.findCachedEntry(trackId)
            if (cachedEntry != null) {
                val (filePath, entry) = cachedEntry
                logger.d { "Serving track $trackId from cache" }
                when (requestMethod) {
                    HttpMethod.Head -> cacheManager.serveCacheHead(call, entry)
                    HttpMethod.Get -> cacheManager.serveCacheGet(call, filePath, entry)
                    else -> call.respond(HttpStatusCode.MethodNotAllowed)
                }
                return
            }

            val isFirstRequest = streamingMutex.withLock {
                if (trackId in streamingInProgress) {
                    false
                } else {
                    streamingInProgress.add(trackId)
                    true
                }
            }

            if (!isFirstRequest) {
                logger.d { "Track $trackId is being cached, waiting for cache" }
                var waited = 0
                while (waited < 30) {
                    delay(200.milliseconds)
                    waited++
                    val entry = cacheManager.findCachedEntry(trackId)
                    if (entry != null) {
                        val (filePath, e) = entry
                        logger.d { "Track $trackId became available from cache" }
                        when (requestMethod) {
                            HttpMethod.Head -> cacheManager.serveCacheHead(call, e)
                            HttpMethod.Get -> cacheManager.serveCacheGet(call, filePath, e)
                            else -> call.respond(HttpStatusCode.MethodNotAllowed)
                        }
                        return
                    }
                }
                logger.w { "Timeout waiting for cache of track $trackId, falling through to upstream" }
            }

            try {
                doStreamFromUpstream(call, trackId, requestMethod)
            } finally {
                if (isFirstRequest) {
                    streamingMutex.withLock { streamingInProgress.remove(trackId) }
                }
            }
            return
        }

        doStreamFromUpstream(call, trackId, requestMethod)
    }

    private suspend fun doStreamFromUpstream(call: ApplicationCall, trackId: String, requestMethod: HttpMethod) {
        val streamInfo = streamingUrlRepository.resolveStreamInfo(trackId)
        if (streamInfo == null) {
            logger.w { "Unable to resolve stream for track $trackId" }
            call.respond(HttpStatusCode.NotFound, "Unable to resolve stream for track $trackId")
            return
        }

        if (streamInfo.protocol != StreamProtocol.PROGRESSIVE) {
            val port = activePort() ?: 8080
            val manifestUrl = "http://$HOST:$port/manifest/${trackId}"
            logger.d { "Redirecting $trackId to manifest URL: $manifestUrl" }
            call.response.headers.append(HttpHeaders.Location, manifestUrl)
            call.respond(HttpStatusCode.Found)
            return
        }

        proxyStream(call, streamInfo.url, trackId, requestMethod)
    }

    suspend fun handleManifestRequest(call: ApplicationCall) {
        val trackId = call.parameters["trackId"]?.trim().orEmpty()
        if (trackId.isBlank()) {
            logger.w { "Rejecting /manifest request with missing track id" }
            call.respond(HttpStatusCode.BadRequest, "Missing track id")
            return
        }

        val streamInfo = streamingUrlRepository.resolveStreamInfo(trackId)
        if (streamInfo == null) {
            logger.w { "Unable to resolve stream for track $trackId" }
            call.respond(HttpStatusCode.NotFound, "Unable to resolve stream for track $trackId")
            return
        }

        if (streamInfo.protocol == StreamProtocol.PROGRESSIVE) {
            logger.w { "Track $trackId is not a manifest stream" }
            call.respond(HttpStatusCode.BadRequest, "Not a manifest stream")
            return
        }

        proxyManifest(call, streamInfo.url, trackId, streamInfo.protocol)
    }

    suspend fun handleSegmentRequest(call: ApplicationCall) {
        val trackId = call.parameters["trackId"]?.trim().orEmpty()
        val segmentUrl = call.parameters["url"]?.let { decodeUrl(it) }

        if (trackId.isBlank() || segmentUrl.isNullOrBlank()) {
            logger.w { "Rejecting /segment request with missing parameters" }
            call.respond(HttpStatusCode.BadRequest, "Missing track id or segment url")
            return
        }

        proxySegment(call, segmentUrl, trackId)
    }

    private suspend fun proxyManifest(
        call: ApplicationCall,
        manifestUrl: String,
        trackId: String,
        protocol: StreamProtocol
    ) {
        logger.d { "Proxying manifest for track $trackId ($protocol): $manifestUrl" }

        val response = runCatching {
            httpClient.request(manifestUrl) {
                method = HttpMethod.Get
                call.forwardRequestHeaderIfPresent(headers, HttpHeaders.Accept)
                call.forwardRequestHeaderIfPresent(headers, HttpHeaders.UserAgent)
            }
        }.getOrElse { throwable ->
            logger.w(throwable) { "Failed to fetch manifest for track $trackId" }
            call.respond(HttpStatusCode.BadGateway, "Failed to fetch manifest")
            return
        }

        if (response.status.value >= 400) {
            logger.w { "Manifest request failed for track $trackId with status ${response.status.value}" }
            call.respond(HttpStatusCode.BadGateway, "Failed to fetch manifest")
            return
        }

        val manifestText = response.bodyAsText()
        val port = activePort() ?: 8080
        val serverUrl = "http://$HOST:$port"
        val rewrittenManifest = when (protocol) {
            StreamProtocol.HLS -> rewriteHlsManifest(manifestText, manifestUrl, serverUrl, trackId)
            StreamProtocol.DASH -> rewriteDashManifest(manifestText, manifestUrl, serverUrl, trackId)
            else -> manifestText
        }

        val contentType = when (protocol) {
            StreamProtocol.HLS -> ContentType("application", "vnd.apple.mpegurl")
            StreamProtocol.DASH -> ContentType("application", "dash+xml")
            else -> ContentType.Application.OctetStream
        }

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val status: HttpStatusCode = HttpStatusCode.OK
            override val contentType: ContentType = contentType
            override val contentLength: Long = rewrittenManifest.encodeToByteArray().size.toLong()

            override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                channel.writeFully(rewrittenManifest.encodeToByteArray())
            }
        })
    }

    private suspend fun proxySegment(
        call: ApplicationCall,
        segmentUrl: String,
        trackId: String
    ) {
        logger.d { "Proxying segment for track $trackId" }

        val response = runCatching {
            httpClient.request(segmentUrl) {
                method = HttpMethod.Get
                call.forwardRequestHeaderIfPresent(headers, HttpHeaders.Range)
                call.forwardRequestHeaderIfPresent(headers, HttpHeaders.Accept)
                call.forwardRequestHeaderIfPresent(headers, HttpHeaders.UserAgent)
            }
        }.getOrElse { throwable ->
            logger.w(throwable) { "Failed to fetch segment for track $trackId" }
            call.respond(HttpStatusCode.BadGateway, "Failed to fetch segment")
            return
        }

        if (response.status.value >= 400) {
            logger.w { "Segment request failed for track $trackId with status ${response.status.value}" }
            call.respond(HttpStatusCode.BadGateway, "Failed to fetch segment")
            return
        }

        val downstreamHeaders = upstreamHeadersToForward(response)
        val downstreamContentLength = downstreamHeaders[HttpHeaders.ContentLength]?.toLongOrNull()

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val status: HttpStatusCode = response.status
            override val headers: Headers = downstreamHeaders
            override val contentLength: Long? = downstreamContentLength
            override val contentType: ContentType? =
                downstreamHeaders[HttpHeaders.ContentType]?.let(ContentType.Companion::parse)

            override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                response.bodyAsChannel().copyTo(channel)
            }
        })
    }

    private suspend fun proxyStream(
        call: ApplicationCall,
        streamUrl: String,
        trackId: String,
        requestMethod: HttpMethod
    ) {
        logger.d { "Proxying stream for track $trackId using $requestMethod" }

        var currentUrl = streamUrl
        var attemptedRefresh = false
        var upstream: HttpResponse?

        while (true) {
            val response = runCatching {
                httpClient.request(currentUrl) {
                    method = requestMethod
                    call.forwardRequestHeaderIfPresent(headers, HttpHeaders.Range)
                    call.forwardRequestHeaderIfPresent(headers, HttpHeaders.IfRange)
                    call.forwardRequestHeaderIfPresent(headers, HttpHeaders.Accept)
                    call.forwardRequestHeaderIfPresent(headers, HttpHeaders.UserAgent)
                }
            }.getOrElse { throwable ->
                logger.w(throwable) { "Failed to fetch upstream stream for track $trackId" }
                call.respond(HttpStatusCode.BadGateway, "Failed to fetch upstream stream")
                return
            }

            if (response.status.value < 400) {
                upstream = response
                break
            }

            logger.w { "Upstream stream request failed for track $trackId with status ${response.status.value}" }
            streamingUrlRepository.invalidateCachedStreamUrl(trackId, currentUrl)

            if (attemptedRefresh) {
                upstream = response
                break
            }

            val refreshedUrl =
                streamingUrlRepository.resolveStreamInfo(trackId, forceRefresh = true)?.url
            if (refreshedUrl.isNullOrBlank() || refreshedUrl == currentUrl) {
                upstream = response
                break
            }

            logger.d { "Retrying stream for track $trackId using refreshed URL" }
            currentUrl = refreshedUrl
            attemptedRefresh = true
        }

        if (upstream.status.value >= 400 && isCachingEnabled()) {
            val cachedEntry = cacheManager.findCachedEntry(trackId)
            if (cachedEntry != null) {
                val (filePath, entry) = cachedEntry
                logger.d { "Falling back to cache for track $trackId after upstream error" }
                if (requestMethod == HttpMethod.Get) {
                    cacheManager.serveCacheGet(call, filePath, entry)
                } else {
                    cacheManager.serveCacheHead(call, entry)
                }
                return
            }
        }

        val downstreamHeaders = upstreamHeadersToForward(upstream)
        val downstreamContentLength = downstreamHeaders[HttpHeaders.ContentLength]?.toLongOrNull()
        logger.v {
            "Proxy response headers for $trackId status=${upstream.status.value} " +
                    "contentType=${downstreamHeaders[HttpHeaders.ContentType]} " +
                    "contentLength=${downstreamHeaders[HttpHeaders.ContentLength]} " +
                    "contentRange=${downstreamHeaders[HttpHeaders.ContentRange]} " +
                    "acceptRanges=${downstreamHeaders[HttpHeaders.AcceptRanges]}"
        }

        if (requestMethod == HttpMethod.Head) {
            call.respond(object : OutgoingContent.NoContent() {
                override val status: HttpStatusCode = upstream.status
                override val headers: Headers = downstreamHeaders
                override val contentLength: Long? = downstreamContentLength
            })
            return
        }

        val doCache = requestMethod == HttpMethod.Get && isCachingEnabled()
        val cacheTrack = if (doCache) getTrackMetadata(trackId) else null
        val cacheFilename = if (cacheTrack != null) {
            cacheManager.resolveCacheFilename(cacheTrack, downstreamHeaders[HttpHeaders.ContentType])
        } else {
            null
        }
        val trackIdForCache = trackId
        val cacheContentType = downstreamHeaders[HttpHeaders.ContentType] ?: "application/octet-stream"

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val status: HttpStatusCode = upstream.status
            override val headers: Headers = downstreamHeaders
            override val contentLength: Long? = downstreamContentLength
            override val contentType: ContentType? =
                downstreamHeaders[HttpHeaders.ContentType]?.let(ContentType.Companion::parse)

            override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                if (cacheFilename != null) {
                    val cacheDir = cacheManager.resolveCacheDir()
                    val cacheFile = cacheDir / cacheFilename.toPath()
                    fileSystem.createDirectories(cacheDir)
                    try {
                        val packet = upstream.bodyAsChannel().readRemaining()
                        val allBytes = packet.readByteArray()
                        channel.writeFully(allBytes)
                        fileSystem.sink(cacheFile).buffer().use { sink ->
                            sink.write(allBytes)
                        }
                        cacheManager.commitCacheEntry(
                            trackId = trackIdForCache,
                            filename = cacheFilename,
                            allBytes = allBytes,
                            contentType = cacheContentType,
                        )
                        scope.launch {
                            cacheManager.evictIfNeeded()
                        }
                    } catch (e: Exception) {
                        if (fileSystem.exists(cacheFile)) {
                            fileSystem.delete(cacheFile)
                        }
                        throw e
                    }
                } else {
                    upstream.bodyAsChannel().copyTo(channel)
                }
            }
        })
    }

    private fun rewriteHlsManifest(
        manifest: String,
        baseUrl: String,
        serverUrl: String,
        trackId: String
    ): String {
        val manifestBase = baseUrl.substringBeforeLast('/')
        val lines = manifest.lines()
        val rewritten = lines.map { line ->
            when {
                line.isBlank() || line.startsWith("#") -> line
                line.startsWith("http") -> {
                    "${serverUrl}/segment/${trackId}?url=${encodeUrl(line)}"
                }
                else -> {
                    val absoluteUrl = if (line.startsWith("/")) {
                        val urlBase =
                            baseUrl.substringBefore("://") + "://" + baseUrl.substringAfter("://")
                                .substringBefore('/')
                        "$urlBase$line"
                    } else {
                        "$manifestBase/$line"
                    }
                    "${serverUrl}/segment/${trackId}?url=${encodeUrl(absoluteUrl)}"
                }
            }
        }
        return rewritten.joinToString("\n")
    }

    private fun rewriteDashManifest(
        manifest: String,
        baseUrl: String,
        serverUrl: String,
        trackId: String
    ): String {
        val manifestBase = baseUrl.substringBeforeLast('/')
        var rewritten = manifest

        val urlRegex = Regex("""(BaseURL|SegmentURL|Location)>([^<]+)""")
        rewritten = urlRegex.replace(rewritten) { match ->
            val tag = match.groupValues[1]
            val url = match.groupValues[2]
            val absoluteUrl = if (url.startsWith("http")) url else "$manifestBase/$url"
            "$tag>${serverUrl}/segment/${trackId}?url=${encodeUrl(absoluteUrl)}"
        }

        val srcRegex = Regex("""src="([^"]+)"""")
        rewritten = srcRegex.replace(rewritten) { match ->
            val url = match.groupValues[1]
            val absoluteUrl = if (url.startsWith("http")) url else "$manifestBase/$url"
            """src="${serverUrl}/segment/${trackId}?url=${encodeUrl(absoluteUrl)}"""
        }

        return rewritten
    }

    private fun encodeUrl(url: String): String = url.encodeURLParameter()
    private fun decodeUrl(encoded: String): String = encoded.decodeURLQueryComponent()

    private fun ApplicationCall.forwardRequestHeaderIfPresent(
        builder: HeadersBuilder,
        headerName: String
    ) {
        request.headers.getAll(headerName)?.forEach { value ->
            builder.append(headerName, value)
        }
    }

    private fun upstreamHeadersToForward(upstream: HttpResponse): Headers {
        return HeadersBuilder().apply {
            upstream.headers.forEach { name, values ->
                values.forEach { value -> append(name, value) }
            }
        }.build()
    }
}
