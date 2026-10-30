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

package org.spotube.spotube.modules.downloads

import org.spotube.spotube.core.network.HttpClientFactory
import co.touchlab.kermit.Logger
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.paths.Paths
import org.spotube.spotube.core.server.StreamInfo
import org.spotube.spotube.core.server.StreamRequest
import org.spotube.spotube.core.server.StreamingUrlRepository
import org.spotube.spotube.core.server.urlSummary
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import org.spotube.spotube.modules.settings.SettingsRepository
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.discard
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use
import kotlin.time.Clock
import org.jetbrains.compose.resources.getString
import spotube.composeapp.generated.resources.*

data class DownloadItem(
    val id: String,
    val title: String,
    val artists: String,
    val album: String?,
    val status: DownloadStatus = DownloadStatus.Queued,
    val progress: Float = 0f,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val errorMessage: String? = null,
    val track: MetadataTrack? = null)

sealed interface DownloadStatus {
    data object Queued : DownloadStatus
    data object Downloading : DownloadStatus
    data object Completed : DownloadStatus
    data class Failed(val error: String) : DownloadStatus
    data object Cancelled : DownloadStatus
}

class DownloadManager(
    private val settingsRepository: SettingsRepository,
    private val paths: Paths,
    private val streamingUrlRepository: StreamingUrlRepository,
    private val httpClientFactory: HttpClientFactory
) {
    companion object {
        const val MAX_CONCURRENT_DOWNLOADS = 4
        private const val SEGMENT_COUNT = 4
        private const val CHUNK_SIZE = 256 * 1024
        private const val YOUTUBE_REFERER = "https://www.youtube.com/"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val ANDROID_USER_AGENT =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip"
        private const val IOS_USER_AGENT =
            "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
        private const val VISIONOS_USER_AGENT =
            "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; US)"
    }

    private val logger = Logger.withTag("DownloadManager")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fileSystem = FileSystem.SYSTEM
    private val queueMutex = Mutex()

    private val httpClient = httpClientFactory.create {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
            connectTimeoutMillis = 30_000
        }
        expectSuccess = false
    }

    private val downloadsMap = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())

    val downloadsFlow: StateFlow<List<DownloadItem>> = downloadsMap
        .map { map -> map.values.sortedByDescending { it.id } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cachedDownloadFolder = MutableStateFlow<String?>(null)

    /** Where downloads are saved right now: the user's override, else the platform default. */
    val downloadFolder: StateFlow<String> = cachedDownloadFolder
        .map { it?.takeIf(String::isNotBlank) ?: paths.getUserDownloadsDirPath() }
        .stateIn(scope, SharingStarted.Eagerly, paths.getUserDownloadsDirPath())

    private val activeJobs = mutableMapOf<String, Job>()

    init {
        scope.launch {
            settingsRepository.userSettings.collect { settings ->
                cachedDownloadFolder.value = settings.overloadedDownloadFolder
            }
        }
    }

    fun enqueue(track: MetadataTrack) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val id = "${track.id}_$timestamp"
        val artistNames = track.artists.joinToString(", ") { it.name }

        val item = DownloadItem(
            id = id,
            title = track.title,
            artists = artistNames,
            album = track.album?.title,
            track = track)

        downloadsMap.update { it + (id to item) }
        val settings = settingsRepository.userSettings.value
        logger.i {
            "[Download $id] enqueued '${track.title}' by $artistNames (track=${track.id}) " +
                "downloadPrefs=${settings.downloadMusicFormat.codec}/${settings.downloadMusicFormat.container} " +
                "${settings.downloadMusicQuality} streamingPrefs=${settings.streamingMusicFormat.codec}/" +
                "${settings.streamingMusicFormat.container} ${settings.streamingMusicQuality} folder=${downloadFolder.value}"
        }
        scope.launch { processQueue() }
    }

    fun cancel(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        downloadsMap.update { map ->
            map[id]?.let { item ->
                map + (id to item.copy(status = DownloadStatus.Cancelled))
            } ?: map
        }
        cleanupTempFiles(id)
    }

    fun retry(id: String) {
        val item = downloadsMap.value[id] ?: return
        if (item.status !is DownloadStatus.Failed && item.status != DownloadStatus.Cancelled) return

        downloadsMap.update { map ->
            map + (id to item.copy(
                status = DownloadStatus.Queued,
                progress = 0f,
                downloadedBytes = 0L,
                errorMessage = null))
        }
        scope.launch { processQueue() }
    }

    fun remove(id: String) {
        cancel(id)
        downloadsMap.update { it - id }
    }

    fun clearCompleted() {
        downloadsMap.update { map ->
            map.filter { it.value.status != DownloadStatus.Completed }
        }
    }

    private suspend fun processQueue() {
        queueMutex.withLock {
            while (true) {
                val activeCount = activeJobs.values.count { it.isActive }
                if (activeCount >= MAX_CONCURRENT_DOWNLOADS) break

                val nextItem =
                    downloadsMap.value.values.firstOrNull { it.status == DownloadStatus.Queued }
                        ?: break

                val id = nextItem.id
                downloadsMap.update { map ->
                    map + (id to nextItem.copy(status = DownloadStatus.Downloading))
                }

                val job = scope.launch { executeDownload(nextItem) }
                activeJobs[id] = job
            }
        }
    }

    private suspend fun executeDownload(item: DownloadItem) {
        val track = item.track ?: run {
            logger.e { "Download item has no track metadata" }
            val noMetadataMessage = getString(Res.string.downloads_error_no_metadata)
            downloadsMap.update { map ->
                map[item.id]?.let {
                    map + (item.id to it.copy(
                        status = DownloadStatus.Failed(noMetadataMessage),
                        errorMessage = noMetadataMessage))
                } ?: map
            }
            return
        }

        try {
            logger.i { "[Download ${item.id}] resolving stream for '${track.title}'" }
            val settings = settingsRepository.userSettings.value
            val streamInfo = streamingUrlRepository.resolveStreamInfo(
                track,
                request = StreamRequest(
                    format = settings.downloadMusicFormat,
                    quality = settings.downloadMusicQuality,
                    // A manifest URL would be saved as-is instead of the audio it points to.
                    progressiveOnly = true))
            if (streamInfo == null) {
                logger.w { "Could not resolve stream URL for ${track.title}" }
                val unresolvedStatus = getString(Res.string.downloads_error_stream_unresolved_short)
                val unresolvedMessage = getString(Res.string.downloads_error_stream_unresolved)
                downloadsMap.update { map ->
                    map[item.id]?.let {
                        map + (item.id to it.copy(
                            status = DownloadStatus.Failed(unresolvedStatus),
                            errorMessage = unresolvedMessage))
                    } ?: map
                }
                return
            }

            val url = streamInfo.url
            logResolvedStream(item, streamInfo)

            val filename = buildFilename(item, streamInfo)

            val downloadDir = resolveDownloadDir()
            fileSystem.createDirectories(downloadDir)

            val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "spotube_downloads".toPath()
            fileSystem.createDirectories(tempDir)

            val (totalSize, supportsRanges) = probeUrl(item.id, url)

            if (totalSize > 0 && supportsRanges) {
                logger.i { "[Download ${item.id}] multi-segment download ($SEGMENT_COUNT segments, $totalSize bytes) -> ${downloadDir / filename}" }
                executeMultiSegmentDownload(url, filename, item, tempDir, downloadDir, totalSize)
            } else {
                logger.i { "[Download ${item.id}] single-connection download (size=$totalSize ranges=$supportsRanges) -> ${downloadDir / filename}" }
                executeSingleConnectionDownload(url, filename, item, tempDir, downloadDir)
            }

            val finalPath = downloadDir / filename.toPath()
            val writtenBytes = fileSystem.metadataOrNull(finalPath)?.size
            logger.i { "[Download ${item.id}] wrote $finalPath ($writtenBytes bytes, expected $totalSize)" }
            if (totalSize > 0 && writtenBytes != null && writtenBytes != totalSize) {
                logger.w { "[Download ${item.id}] SIZE MISMATCH: wrote $writtenBytes bytes, server reported $totalSize" }
            }

            applyMetadata(item, filename, downloadDir)
            // Without this Android music players and the Files app's Audio view don't see the
            // file until the next full media scan.
            addToMediaLibrary((downloadDir / filename.toPath()).toString())
            logger.i { "[Download ${item.id}] completed '${item.title}'" }

            downloadsMap.update { map ->
                map[item.id]?.let {
                    map + (item.id to it.copy(status = DownloadStatus.Completed, progress = 1f))
                } ?: map
            }
        } catch (e: CancellationException) {
            logger.i { "[Download ${item.id}] cancelled" }
            downloadsMap.update { map ->
                map[item.id]?.let {
                    map + (item.id to it.copy(status = DownloadStatus.Cancelled))
                } ?: map
            }
            throw e
        } catch (e: Exception) {
            logger.e(e) { "[Download ${item.id}] FAILED '${item.title}': ${e.message}" }
            val failureMessage = e.message ?: getString(Res.string.common_unknown_error)
            downloadsMap.update { map ->
                map[item.id]?.let {
                    map + (item.id to it.copy(
                        status = DownloadStatus.Failed(failureMessage),
                        errorMessage = e.message))
                } ?: map
            }
        } finally {
            activeJobs.remove(item.id)
            processQueue()
        }
    }

    private suspend fun executeMultiSegmentDownload(
        url: String,
        filename: String,
        item: DownloadItem,
        tempDir: Path,
        downloadDir: Path,
        totalSize: Long) {
        val segmentSize = totalSize / SEGMENT_COUNT
        val segments = (0 until SEGMENT_COUNT).map { i ->
            val start = i * segmentSize
            val end = if (i == SEGMENT_COUNT - 1) totalSize - 1 else (i + 1) * segmentSize - 1
            Segment(index = i, start = start, end = end)
        }

        val segmentFiles = segments.map { segment ->
            tempDir / "${item.id}_seg${segment.index}.tmp"
        }

        coroutineScope {
            segments.mapIndexed { index, segment ->
                async {
                    downloadSegment(url, item, segment, segmentFiles[index], totalSize)
                }
            }.awaitAll()
        }

        val finalPath = downloadDir / filename.toPath()
        fileSystem.sink(finalPath).buffer().use { sink ->
            for (segmentFile in segmentFiles) {
                fileSystem.source(segmentFile).buffer().use { source ->
                    sink.writeAll(source)
                }
            }
        }

        segmentFiles.forEach { fileSystem.delete(it) }
    }

    private suspend fun downloadSegment(
        url: String,
        item: DownloadItem,
        segment: Segment,
        outputPath: Path,
        totalSize: Long) {
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.Range, "bytes=${segment.start}-${segment.end}")
            }
            applyClientHeaders(url)
        }
        logger.d {
            "[Download ${item.id}] segment ${segment.index} bytes=${segment.start}-${segment.end} -> " +
                "HTTP ${response.status.value} type=${response.headers[HttpHeaders.ContentType]} " +
                "range=${response.headers[HttpHeaders.ContentRange]}"
        }
        // expectSuccess = false, so without these checks an error page, or the whole file once per
        // segment (a 200 to a range request), would be written into the "audio" file.
        if (!response.status.isSuccess()) {
            logger.w { "[Download ${item.id}] segment ${segment.index} got HTTP ${response.status}; failing download" }
            throw DownloadException(getString(Res.string.downloads_error_http, response.status.value))
        }
        if (response.status != HttpStatusCode.PartialContent) {
            logger.w { "[Download ${item.id}] segment ${segment.index} got ${response.status} instead of 206; range ignored" }
            throw DownloadException(getString(Res.string.downloads_error_range_ignored))
        }
        val channel = response.bodyAsChannel()

        fileSystem.sink(outputPath).buffer().use { sink ->
            val packet = channel.readRemaining()
            val bytes = packet.readByteArray()
            packet.close()
            sink.write(bytes)

            updateProgress(item.id, bytes.size.toLong(), totalSize)
        }
    }

    private suspend fun executeSingleConnectionDownload(
        url: String,
        filename: String,
        item: DownloadItem,
        tempDir: Path,
        downloadDir: Path) {
        val tempFile = tempDir / "${item.id}.tmp"

        val response = httpClient.get(url) {
            applyClientHeaders(url)
        }
        val totalSize = response.contentLength() ?: 0L
        logger.i {
            "[Download ${item.id}] GET -> HTTP ${response.status.value} " +
                "type=${response.headers[HttpHeaders.ContentType]} length=$totalSize"
        }
        if (!response.status.isSuccess()) {
            logger.w { "[Download ${item.id}] GET got HTTP ${response.status}; failing download" }
            throw DownloadException(getString(Res.string.downloads_error_http, response.status.value))
        }
        rejectVideo(item.id, response.headers[HttpHeaders.ContentType])
        val channel = response.bodyAsChannel()

        fileSystem.sink(tempFile).buffer().use { sink ->
            val packet = channel.readRemaining()
            val bytes = packet.readByteArray()
            packet.close()
            sink.write(bytes)

            updateProgress(item.id, bytes.size.toLong(), totalSize)
        }

        val finalPath = downloadDir / filename.toPath()
        fileSystem.copy(tempFile, finalPath)
        fileSystem.delete(tempFile)
    }

    private fun updateProgress(id: String, bytesJustRead: Long, totalSize: Long) {
        downloadsMap.update { map ->
            map[id]?.let { current ->
                val newDownloaded = current.downloadedBytes + bytesJustRead
                val progress = if (totalSize > 0) {
                    (newDownloaded.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                } else 0f
                map + (id to current.copy(
                    progress = progress,
                    downloadedBytes = newDownloaded,
                    totalBytes = totalSize))
            } ?: map
        }
    }

    private suspend fun probeUrl(id: String, url: String): Pair<Long, Boolean> {
        return try {
            val response = httpClient.get(url) {
                headers {
                    append(HttpHeaders.Range, "bytes=0-0")
                }
                applyClientHeaders(url)
            }
            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                logger.w { "[Download $id] PROBE returned HTTP ${response.status.value}; failing download early" }
                throw DownloadException(getString(Res.string.downloads_error_http, response.status.value))
            }
            val contentRange = response.headers[HttpHeaders.ContentRange]
            val totalFromContentRange = contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()
            val contentLength = totalFromContentRange ?: response.contentLength() ?: 0L
            val acceptRanges = response.headers[HttpHeaders.AcceptRanges]
            val supportsRanges = response.status == HttpStatusCode.PartialContent ||
                acceptRanges?.equals("bytes", ignoreCase = true) == true
            val contentType = response.headers[HttpHeaders.ContentType]
            logger.i {
                "[Download $id] PROBE (bytes=0-0) -> HTTP ${response.status.value} type=$contentType " +
                    "length=$contentLength contentRange=$contentRange acceptRanges=$acceptRanges"
            }
            runCatching { response.bodyAsChannel().discard() }
            rejectVideo(id, contentType)
            Pair(contentLength, supportsRanges)
        } catch (e: DownloadException) {
            throw e
        } catch (e: Exception) {
            logger.w(e) { "[Download $id] PROBE request failed, falling back to single connection" }
            Pair(0L, false)
        }
    }

    /**
     * Some sources only offer muxed video (e.g. NewPipe falls back to video streams when a video
     * has no audio-only ones). Saving that as a "song" is worse than failing with a clear reason.
     */
    private suspend fun rejectVideo(id: String, contentType: String?) {
        if (contentType?.startsWith("video/", ignoreCase = true) == true) {
            logger.w { "[Download $id] server says this is VIDEO ($contentType), not audio; failing download" }
            throw DownloadException(getString(Res.string.downloads_error_video_only))
        }
    }

    private fun isYouTubeOrGoogleVideo(url: String): Boolean {
        val host = runCatching { Url(url).host.lowercase() }.getOrNull() ?: ""
        return host.endsWith("googlevideo.com") ||
            host.endsWith("youtube.com") ||
            host.endsWith("youtu.be") ||
            url.contains("googlevideo.com", ignoreCase = true) ||
            url.contains("youtube.com", ignoreCase = true) ||
            url.contains("youtu.be", ignoreCase = true)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyClientHeaders(targetUrl: String) {
        if (isYouTubeOrGoogleVideo(targetUrl)) {
            val clientParam = runCatching { Url(targetUrl).parameters["c"]?.uppercase() }.getOrNull()
                ?: Regex("""[?&]c=([^&#]+)""").find(targetUrl)?.groupValues?.get(1)?.uppercase()

            when (clientParam) {
                "ANDROID" -> {
                    headers[HttpHeaders.UserAgent] = ANDROID_USER_AGENT
                    headers.remove(HttpHeaders.Referrer)
                    headers.remove("Referer")
                    headers.remove(HttpHeaders.Origin)
                    headers.remove("Origin")
                }
                "IOS" -> {
                    headers[HttpHeaders.UserAgent] = IOS_USER_AGENT
                    headers.remove(HttpHeaders.Referrer)
                    headers.remove("Referer")
                    headers.remove(HttpHeaders.Origin)
                    headers.remove("Origin")
                }
                "VISIONOS" -> {
                    headers[HttpHeaders.UserAgent] = VISIONOS_USER_AGENT
                    headers.remove(HttpHeaders.Referrer)
                    headers.remove("Referer")
                    headers.remove(HttpHeaders.Origin)
                    headers.remove("Origin")
                }
                else -> {
                    headers[HttpHeaders.Referrer] = YOUTUBE_REFERER
                    headers[HttpHeaders.Origin] = "https://www.youtube.com"
                    headers[HttpHeaders.UserAgent] = BROWSER_USER_AGENT
                }
            }
        }
    }

    private fun logResolvedStream(item: DownloadItem, info: StreamInfo) {
        val settings = settingsRepository.userSettings.value
        val wanted = settings.downloadMusicFormat
        logger.i {
            "[Download ${item.id}] resolved stream codec=${info.codec} container=${info.container} " +
                "protocol=${info.protocol} ${urlSummary(info.url)}"
        }
        if (info.protocol != StreamProtocol.PROGRESSIVE) {
            logger.w {
                "[Download ${item.id}] stream is a ${info.protocol} MANIFEST; a plain GET saves the manifest, not audio"
            }
        }
        if (!info.codec.equals(wanted.codec, ignoreCase = true) || !info.container.equals(wanted.container, ignoreCase = true)) {
            logger.w {
                "[Download ${item.id}] FORMAT FALLBACK: download setting is ${wanted.codec}/${wanted.container} " +
                    "${settings.downloadMusicQuality}, but the closest downloadable stream offered is ${info.codec}/${info.container}"
            }
        }
    }

    private suspend fun applyMetadata(item: DownloadItem, filename: String, downloadDir: Path) {
        val filePath = downloadDir / filename.toPath()
        if (!fileSystem.exists(filePath)) {
            logger.w { "[Download ${item.id}] skipping metadata: $filePath does not exist" }
            return
        }
        logger.d { "[Download ${item.id}] writing metadata to $filePath" }

        val track = item.track ?: return

        val coverBytes = try {
            val thumbnail = track.thumbnails?.maxByOrNull { it.width * it.height }
            if (thumbnail != null) {
                val response = httpClient.get(thumbnail.url)
                val channel = response.bodyAsChannel()
                val packet = channel.readRemaining()
                val bytes = packet.readByteArray()
                packet.close()
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to download cover art for ${item.title}" }
            null
        }

        try {
            uniffi.compose_app.writeAudioMetadata(
                filePath = filePath.toString(),
                title = item.title,
                artists = item.artists,
                album = item.album,
                trackNumber = track.trackNumber,
                discNumber = track.discNumber,
                coverBytes = coverBytes)
        } catch (e: Exception) {
            logger.w(e) { "[Download ${item.id}] failed to write audio metadata for ${item.title}" }
        }
    }

    private fun resolveDownloadDir(): Path = downloadFolder.value.toPath()

    private fun cleanupTempFiles(id: String) {
        try {
            val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "spotube_downloads".toPath()
            if (fileSystem.exists(tempDir)) {
                fileSystem.list(tempDir).forEach { file ->
                    if (file.name.startsWith(id)) {
                        fileSystem.delete(file)
                    }
                }
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cleanup temp files for $id" }
        }
    }

    private fun buildFilename(downloadItem: DownloadItem, streamInfo: StreamInfo): String {
        val safeTitle = downloadItem.title.sanitizeFilename()
        val safeArtist = downloadItem.artists.sanitizeFilename()
        val extension = fileExtension(streamInfo)
        return "$safeArtist - $safeTitle.$extension"
    }

    /** AAC in an MP4 container is audio-only, so give it the audio extension players expect. */
    private fun fileExtension(info: StreamInfo): String =
        if (info.codec.equals("aac", ignoreCase = true) && info.container.equals("mp4", ignoreCase = true)) "m4a"
        else info.container

    private fun String.sanitizeFilename(): String {
        return this.replace(Regex("[^a-zA-Z0-9.\\-_' ]"), "_").take(200)
    }

    private data class Segment(val index: Int, val start: Long, val end: Long)
}

/** A download failure whose [message] is already localized and safe to show the user. */
private class DownloadException(message: String) : Exception(message)
