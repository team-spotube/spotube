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

import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioSource
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioFormat
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioQuality
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.AudioStream
import dev.krtirtho.plugin_interfaces.plugin_apis.audio.StreamProtocol
import dev.krtirtho.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import dev.krtirtho.spotube.core.audioplayer.AudioPlayerQueue
import dev.krtirtho.spotube.core.audioplayer.QueueEntry
import dev.krtirtho.spotube.core.di.injectLogger
import dev.krtirtho.spotube.modules.plugin.PluginManager
import dev.krtirtho.spotube.modules.settings.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

data class CachedStreamUrl(
    val url: String,
    val protocol: StreamProtocol,
    val expiresAtMs: Long,
    val container: String,
    val codec: String,
    val source: AudioSource.Streamed? = null,
    val selectedStream: AudioStream? = null,
    val preferredFormat: AudioFormat,
    val preferredQuality: AudioQuality,
)

data class StreamInfo(
    val url: String,
    val protocol: StreamProtocol,
    val codec: String,
    val container: String,
)

class StreamingUrlRepository(
    private val pluginManager: PluginManager,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val matchedTracksRepository: MatchedTracksRepository,
    private val settingsRepository: SettingsRepository,
) : KoinComponent {

    companion object {
        private val STREAM_URL_CACHE_TTL_MS = 30.seconds.inWholeMilliseconds
    }

    private val logger by injectLogger<StreamingUrlRepository>()
    private val streamUrlCacheMutex = Mutex()
    private val streamUrlCache = mutableMapOf<String, CachedStreamUrl>()
    private val alternativesCacheMutex = Mutex()
    private val alternativesCache = mutableMapOf<String, List<AudioSource>>()

    suspend fun resolveStreamInfo(
        trackId: String,
        forceRefresh: Boolean = false
    ): StreamInfo? {
        val queueEntry = audioPlayerQueue.queueFlow.value.firstOrNull {
            it is QueueEntry.StreamingTrack && it.track.id == trackId
        } as QueueEntry.StreamingTrack?

        if (queueEntry == null) {
            logger.v { "Track $trackId is not present in current queue" }
            return null
        }
        return resolveStreamInfo(queueEntry.track, forceRefresh)
    }

    suspend fun resolveStreamInfo(
        track: MetadataTrack,
        forceRefresh: Boolean = false
    ): StreamInfo? {
        val trackId = track.id
        val settings = settingsRepository.userSettings.value
        val preferredFormat = settings.streamingMusicFormat
        val preferredQuality = settings.streamingMusicQuality

        if (!forceRefresh) {
            getCachedStreamUrl(trackId, preferredFormat, preferredQuality)?.let { cached ->
                logger.v { "Using cached stream URL for track $trackId" }
                return cached
            }
        }
        val audioPlugin = pluginManager.selectedAudioPlugin.value
        if (audioPlugin == null) {
            logger.w { "No audio plugin selected while resolving stream for track $trackId" }
            return null
        }

        val source = matchedTracksRepository.getTrackSource(track)

        if (source != null) {
            logger.d { "Attempting stream resolution for track $trackId using cached source" }
            val stream = runCatching {
                audioPlugin.use {
                    audioAPI.getStreamsOfAudioSource(source)
                        .firstOrNull()
                }
            }.getOrElse { throwable ->
                logger.w(throwable) { "Failed to resolve audio stream for track $trackId using matched source" }
                null
            }

            if (stream != null) {
                logger.d { "Resolved stream for track $trackId using cached source" }
                val resolvedStream = selectPreferredAudioStream(
                    stream.streams,
                    preferredFormat,
                    preferredQuality,
                )
                if (resolvedStream != null) {
                    val info = resolvedStream.toStreamInfo()
                    cacheStreamInfo(trackId, info, preferredFormat, preferredQuality, stream, resolvedStream)
                    return info
                }
            }

            logger.d { "Cached source did not provide a stream for track $trackId; falling back to track lookup" }
        }

        logger.d { "Attempting stream resolution for track $trackId using track lookup" }
        val sources = runCatching {
            audioPlugin.use {
                audioAPI.getStreamsByTrack(track)
            }
        }.getOrElse { throwable ->
            logger.w(throwable) { "Failed to resolve audio stream for track $trackId" }
            return null
        }

        if (sources.isEmpty()) {
            logger.w { "No stream candidates found for track $trackId" }
            return null
        }

        cacheAlternatives(trackId, sources)

        val stream = runCatching {
            audioPlugin.use {
                sources.firstNotNullOfOrNull { src ->
                    when (src) {
                        is AudioSource.Streamed -> {
                            matchedTracksRepository.saveTrackSource(track, src.toBasic())
                            src
                        }

                        is AudioSource.Basic -> {
                            matchedTracksRepository.saveTrackSource(track, src)
                            audioAPI.getStreamsOfAudioSource(src)
                                .firstOrNull()
                        }
                    }
                }
            }
        }.getOrElse { throwable ->
            logger.w(throwable) { "Failed to resolve audio stream for track $trackId" }
            return null
        }

        if (stream == null) {
            logger.w { "No stream candidates found for track $trackId" }
            return null
        }

        logger.d { "Resolved stream for track $trackId via track lookup" }
        val resolvedStream = selectPreferredAudioStream(
            stream.streams,
            preferredFormat,
            preferredQuality,
        )
        if (resolvedStream != null) {
            val info = resolvedStream.toStreamInfo()
            cacheStreamInfo(trackId, info, preferredFormat, preferredQuality, stream, resolvedStream)
            return info
        }
        return null
    }

    private fun AudioStream.toStreamInfo() = StreamInfo(
        url = normalizeManifestUrl(url, protocol),
        protocol = protocol,
        codec = codec,
        container = container,
    )

    private suspend fun getCachedStreamUrl(
        trackId: String,
        preferredFormat: AudioFormat,
        preferredQuality: AudioQuality,
    ): StreamInfo? {
        val now = Clock.System.now().toEpochMilliseconds()
        return streamUrlCacheMutex.withLock {
            val cached = streamUrlCache[trackId] ?: return@withLock null
            if (cached.preferredFormat != preferredFormat || cached.preferredQuality != preferredQuality) {
                return@withLock null
            }
            if (cached.expiresAtMs <= now) {
                logger.v { "Cached stream URL expired for track $trackId" }
                return@withLock null
            }
            StreamInfo(cached.url, cached.protocol, cached.codec, cached.container)
        }
    }

    private suspend fun cacheStreamInfo(
        trackId: String,
        info: StreamInfo,
        preferredFormat: AudioFormat,
        preferredQuality: AudioQuality,
        source: AudioSource.Streamed,
        selectedStream: AudioStream,
    ) {
        val expiresAtMs = Clock.System.now().toEpochMilliseconds() + STREAM_URL_CACHE_TTL_MS
        streamUrlCacheMutex.withLock {
            streamUrlCache[trackId] = CachedStreamUrl(
                url = info.url,
                protocol = info.protocol,
                expiresAtMs = expiresAtMs,
                container = info.container,
                codec = info.codec,
                source = source,
                selectedStream = selectedStream,
                preferredFormat = preferredFormat,
                preferredQuality = preferredQuality,
            )
        }
    }

    suspend fun invalidateCachedStreamUrl(trackId: String, url: String? = null) {
        streamUrlCacheMutex.withLock {
            val cached = streamUrlCache[trackId] ?: return@withLock
            if (url == null || cached.url == url) {
                streamUrlCache.remove(trackId)
                logger.v { "Invalidated cached stream URL for track $trackId" }
            }
        }
        // Stream metadata is part of the same URL-cache entry and is removed with it.
    }

    suspend fun getCachedAlternatives(trackId: String): List<AudioSource>? {
        return alternativesCacheMutex.withLock {
            alternativesCache[trackId]
        }
    }

    suspend fun cacheAlternatives(trackId: String, sources: List<AudioSource>) {
        alternativesCacheMutex.withLock {
            alternativesCache[trackId] = sources
            logger.v { "Cached ${sources.size} alternative sources for track $trackId" }
        }
    }

    /** Returns stream details from the existing playback URL cache; never fetches them. */
    suspend fun getCachedStreamUrlEntry(trackId: String): CachedStreamUrl? {
        val now = Clock.System.now().toEpochMilliseconds()
        return streamUrlCacheMutex.withLock {
            val entry = streamUrlCache[trackId] ?: return@withLock null
            if (entry.expiresAtMs <= now) {
                logger.v { "Returning expired stream details for track $trackId" }
            }
            entry
        }
    }

    suspend fun invalidateCachedAlternatives(trackId: String) {
        alternativesCacheMutex.withLock {
            alternativesCache.remove(trackId)
        }
    }

}

/**
 * Resolves a stream URL to an absolute URL. Relative manifest paths (DASH/HLS) are
 * resolved against the YouTube origin; progressive URLs are returned unchanged.
 */
internal fun normalizeManifestUrl(url: String, protocol: StreamProtocol): String {
    if (url.startsWith("http")) return url
    return when (protocol) {
        StreamProtocol.DASH, StreamProtocol.HLS -> "https://www.youtube.com$url"
        StreamProtocol.PROGRESSIVE -> url
    }
}

/** Selects the available stream closest to the user's preferred format and quality. */
internal fun selectPreferredAudioStream(
    streams: List<AudioStream>,
    preferredFormat: AudioFormat,
    preferredQuality: AudioQuality,
): AudioStream? = streams.minWithOrNull(
    compareBy<AudioStream> { stream ->
        formatMismatchCount(stream, preferredFormat)
    }.thenBy { stream ->
        if (stream.matchesQualityType(preferredQuality)) 0 else 1
    }.thenBy { stream ->
        stream.qualityDistance(preferredQuality)
    }.thenBy { stream ->
        stream.channelDistance(preferredQuality)
    }
)

private fun formatMismatchCount(stream: AudioStream, preferredFormat: AudioFormat): Int =
    (if (stream.codec.equals(preferredFormat.codec, ignoreCase = true)) 0 else 1) +
        (if (stream.container.equals(preferredFormat.container, ignoreCase = true)) 0 else 1)

private fun AudioStream.matchesQualityType(preferredQuality: AudioQuality): Boolean = when (this) {
    is AudioStream.Lossy -> preferredQuality is AudioQuality.Lossy
    is AudioStream.Lossless -> preferredQuality is AudioQuality.Lossless
}

private fun AudioStream.qualityDistance(preferredQuality: AudioQuality): Long = when {
    this is AudioStream.Lossy && preferredQuality is AudioQuality.Lossy ->
        abs(bitrate.toLong() - preferredQuality.bitrate.toLong())

    this is AudioStream.Lossless && preferredQuality is AudioQuality.Lossless ->
        abs(sampleRate.toLong() - preferredQuality.sampleRate.toLong())

    else -> Long.MAX_VALUE
}

private fun AudioStream.channelDistance(preferredQuality: AudioQuality): Int = when {
    this is AudioStream.Lossless && preferredQuality is AudioQuality.Lossless ->
        abs(channels - preferredQuality.channels)

    else -> 0
}
