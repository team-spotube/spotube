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

package org.spotube.spotube.core.server

import org.spotube.plugin_interfaces.plugin_apis.audio.AudioSource
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioFormat
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioQuality
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioStream
import org.spotube.plugin_interfaces.plugin_apis.audio.StreamProtocol
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.PlatformType
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.getPlatform
import org.spotube.spotube.modules.plugin.AudioPluginSource
import org.spotube.spotube.modules.settings.UserSettingsSource
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
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
    // The id of the audio plugin that produced this entry. Guards against a stale entry
    // (e.g. a YouTube video URL) being served after the user switches to a different
    // audio plugin, which would otherwise play back completely unrelated content.
    val pluginId: String)

private data class CachedAlternatives(
    val pluginId: String,
    val sources: List<AudioSource>)

/**
 * Stream preferences for callers other than playback. The downloader passes the user's *download*
 * format/quality and needs a file it can fetch with plain HTTP, so DASH/HLS manifests are excluded.
 * Such requests bypass the playback stream-URL cache in both directions.
 */
data class StreamRequest(
    val format: AudioFormat,
    val quality: AudioQuality,
    val progressiveOnly: Boolean)

data class StreamInfo(
    val url: String,
    val protocol: StreamProtocol,
    val codec: String,
    val container: String)

class StreamingUrlRepository(
    private val pluginManager: AudioPluginSource,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val matchedTracksRepository: TrackSourceRepository,
    private val settingsRepository: UserSettingsSource,
    private val streamUrlCacheTtlMs: Long = 30.seconds.inWholeMilliseconds,
    private val platform: PlatformType = currentPlatform) : KoinComponent {

    private val logger by injectLogger<StreamingUrlRepository>()
    private val streamUrlCacheMutex = Mutex()
    private val streamUrlCache = mutableMapOf<String, CachedStreamUrl>()
    private val alternativesCacheMutex = Mutex()
    private val alternativesCache = mutableMapOf<String, CachedAlternatives>()

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
        forceRefresh: Boolean = false,
        request: StreamRequest? = null
    ): StreamInfo? {
        val trackId = track.id
        val settings = settingsRepository.userSettings.value
        val preferredFormat = request?.format ?: settings.streamingMusicFormat
        val preferredQuality = request?.quality ?: settings.streamingMusicQuality
        // The URL cache holds what *playback* resolved; a download with other prefs must not
        // read it, nor replace it with a stream playback didn't ask for.
        val useUrlCache = request == null

        // A built-in audio plugin is always selected, so null means plugins are still loading.
        // The player requests the restored queue's track right at startup; failing that request
        // leaves the item permanently failed (AVPlayer), so wait for the plugin instead.
        val audioPlugin = pluginManager.selectedAudioPlugin.value
            ?: withTimeoutOrNull(AUDIO_PLUGIN_LOAD_TIMEOUT) {
                logger.i { "[Resolve $trackId] waiting for the audio plugin to load" }
                pluginManager.selectedAudioPlugin.filterNotNull().first()
            }
        if (audioPlugin == null) {
            logger.w { "No audio plugin selected while resolving stream for track $trackId" }
            return null
        }
        logger.i {
            "[Resolve $trackId] '${track.title}' plugin=${audioPlugin.pluginId} forceRefresh=$forceRefresh " +
                "prefs: format=${preferredFormat.codec}/${preferredFormat.container} quality=$preferredQuality " +
                if (request == null) "(streaming settings)" else "(caller request, progressiveOnly=${request.progressiveOnly})"
        }

        fun select(stream: AudioSource.Streamed, origin: String): AudioStream? {
            val candidates = if (request?.progressiveOnly == true) {
                stream.streams.filter { it.protocol == StreamProtocol.PROGRESSIVE }.also { kept ->
                    if (kept.size != stream.streams.size) logger.i {
                        "[Resolve $trackId] dropped ${stream.streams.size - kept.size} manifest (DASH/HLS) candidates; " +
                            "caller needs a directly downloadable file"
                    }
                }
            } else {
                stream.streams
            }
            return selectPreferredAudioStream(candidates, preferredFormat, preferredQuality, platform)
                .also { logSelection(trackId, origin, candidates, preferredFormat, preferredQuality, it) }
        }

        if (useUrlCache && !forceRefresh) {
            getCachedStreamUrl(trackId, preferredFormat, preferredQuality, audioPlugin.pluginId)?.let { cached ->
                logger.i {
                    "[Resolve $trackId] cache hit: codec=${cached.codec} container=${cached.container} " +
                        "protocol=${cached.protocol} ${urlSummary(cached.url)}"
                }
                return cached
            }
        }

        val source = matchedTracksRepository.getTrackSource(track, audioPlugin.pluginId)

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
                val resolvedStream = select(stream, "matched source ${source.id}")
                if (resolvedStream != null) {
                    val info = resolvedStream.toStreamInfo()
                    if (useUrlCache) {
                        cacheStreamInfo(trackId, info, preferredFormat, preferredQuality, stream, resolvedStream, audioPlugin.pluginId)
                    }
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

        cacheAlternatives(trackId, sources, audioPlugin.pluginId)

        val stream = runCatching {
            audioPlugin.use {
                sources.firstNotNullOfOrNull { src ->
                    when (src) {
                        is AudioSource.Streamed -> {
                            matchedTracksRepository.saveTrackSource(track, src.toBasic(), audioPlugin.pluginId)
                            src
                        }

                        is AudioSource.Basic -> {
                            matchedTracksRepository.saveTrackSource(track, src, audioPlugin.pluginId)
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
        val resolvedStream = select(stream, "track lookup -> ${stream.id}")
        if (resolvedStream != null) {
            val info = resolvedStream.toStreamInfo()
            if (useUrlCache) {
                cacheStreamInfo(trackId, info, preferredFormat, preferredQuality, stream, resolvedStream, audioPlugin.pluginId)
            }
            return info
        }
        return null
    }

    /** Logs every candidate with its ranking key so it's obvious *why* a stream won. */
    private fun logSelection(
        trackId: String,
        origin: String,
        streams: List<AudioStream>,
        preferredFormat: AudioFormat,
        preferredQuality: AudioQuality,
        selected: AudioStream?) {
        logger.i {
            "[Resolve $trackId] selecting from ${streams.size} candidates ($origin), " +
                "want ${preferredFormat.codec}/${preferredFormat.container} $preferredQuality " +
                "(rank key = unplayable, formatMismatch, qualityTypeMismatch, qualityDistance, channelDistance; " +
                "lowest wins):\n" +
                streams.joinToString("\n") { stream ->
                    val marker = if (stream === selected) "  -> " else "     "
                    "$marker${stream.describeForLog()} key=(${if (stream.isPlayableOn(platform)) 0 else 1}, " +
                        "${formatMismatchCount(stream, preferredFormat)}, " +
                        "${if (stream.matchesQualityType(preferredQuality)) 0 else 1}, " +
                        "${stream.qualityDistance(preferredQuality).let { if (it == Long.MAX_VALUE) "inf" else "$it" }}, " +
                        "${stream.channelDistance(preferredQuality)}) ${urlSummary(stream.url)}"
                }
        }
        when {
            selected == null -> logger.w { "[Resolve $trackId] no stream selected" }
            !selected.isPlayableOn(platform) -> logger.w {
                "[Resolve $trackId] NO PLAYABLE STREAM on $platform; falling back to " +
                    "${selected.describeForLog()}, which the player will likely reject"
            }
            formatMismatchCount(selected, preferredFormat) > 0 -> logger.w {
                "[Resolve $trackId] FORMAT FALLBACK: wanted ${preferredFormat.codec}/${preferredFormat.container}, " +
                    "got ${selected.codec}/${selected.container} (${selected.protocol})"
            }
            selected.qualityDistance(preferredQuality) != 0L -> logger.w {
                "[Resolve $trackId] QUALITY FALLBACK: wanted $preferredQuality, got ${selected.describeForLog()}"
            }
        }
    }

    private fun AudioStream.toStreamInfo() = StreamInfo(
        url = normalizeManifestUrl(url, protocol),
        protocol = protocol,
        codec = codec,
        container = container)

    private suspend fun getCachedStreamUrl(
        trackId: String,
        preferredFormat: AudioFormat,
        preferredQuality: AudioQuality,
        pluginId: String): StreamInfo? {
        val now = Clock.System.now().toEpochMilliseconds()
        return streamUrlCacheMutex.withLock {
            val cached = streamUrlCache[trackId] ?: return@withLock null
            if (cached.pluginId != pluginId) {
                logger.v { "Cached stream URL for track $trackId belongs to a different plugin; ignoring" }
                return@withLock null
            }
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
        pluginId: String) {
        val expiresAtMs = Clock.System.now().toEpochMilliseconds() + streamUrlCacheTtlMs
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
                pluginId = pluginId)
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

    /** Returns cached alternatives only if they were produced by [pluginId]. */
    suspend fun getCachedAlternatives(trackId: String, pluginId: String): List<AudioSource>? {
        return alternativesCacheMutex.withLock {
            val cached = alternativesCache[trackId] ?: return@withLock null
            if (cached.pluginId != pluginId) {
                logger.v { "Cached alternatives for track $trackId belong to a different plugin; ignoring" }
                return@withLock null
            }
            cached.sources
        }
    }

    suspend fun cacheAlternatives(trackId: String, sources: List<AudioSource>, pluginId: String) {
        alternativesCacheMutex.withLock {
            alternativesCache[trackId] = CachedAlternatives(pluginId, sources)
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
private val AUDIO_PLUGIN_LOAD_TIMEOUT = 30.seconds

internal fun normalizeManifestUrl(url: String, protocol: StreamProtocol): String {
    if (url.startsWith("http")) return url
    return when (protocol) {
        StreamProtocol.DASH, StreamProtocol.HLS -> "https://www.youtube.com$url"
        StreamProtocol.PROGRESSIVE -> url
    }
}

internal val currentPlatform: PlatformType by lazy { getPlatform().type }

/**
 * Selects the available stream closest to the user's preferred format and quality.
 * Streams the [platform]'s player can decode always win over ones it can't, whatever the
 * preferences say: e.g. opus/webm is unplayable on iOS.
 */
internal fun selectPreferredAudioStream(
    streams: List<AudioStream>,
    preferredFormat: AudioFormat,
    preferredQuality: AudioQuality,
    platform: PlatformType = currentPlatform): AudioStream? = streams.minWithOrNull(
    compareBy<AudioStream> { stream ->
        if (stream.isPlayableOn(platform)) 0 else 1
    }.thenBy { stream ->
        formatMismatchCount(stream, preferredFormat)
    }.thenBy { stream ->
        if (stream.matchesQualityType(preferredQuality)) 0 else 1
    }.thenBy { stream ->
        stream.qualityDistance(preferredQuality)
    }.thenBy { stream ->
        stream.channelDistance(preferredQuality)
    }
)

private val MATROSKA_CONTAINERS = setOf("webm", "mkv", "matroska")

// Containers a platform's player can't open, so e.g. YouTube's opus/webm streams fail to load there.
private val UNSUPPORTED_CONTAINERS: Map<PlatformType, Set<String>> = mapOf(
    // AVPlayer has no WebM/Ogg/Matroska demuxer.
    PlatformType.IOS to MATROSKA_CONTAINERS + setOf("ogg", "oga"))

/**
 * Whether [platform]'s player can play this stream. ExoPlayer (Android) and VLC elsewhere handle
 * everything plugins return; AVPlayer (iOS) can't demux WebM/Ogg and can't play DASH.
 */
internal fun AudioStream.isPlayableOn(platform: PlatformType): Boolean =
    isContainerPlayableOn(container, platform) &&
        !(platform == PlatformType.IOS && protocol == StreamProtocol.DASH)

/** Whether [platform]'s player can open [container] at all; see [AudioStream.isPlayableOn]. */
internal fun isContainerPlayableOn(container: String, platform: PlatformType): Boolean =
    container.lowercase() !in UNSUPPORTED_CONTAINERS[platform].orEmpty()

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

internal fun AudioStream.describeForLog(): String = when (this) {
    is AudioStream.Lossy -> "lossy codec=$codec container=$container bitrate=$bitrate protocol=$protocol"
    is AudioStream.Lossless ->
        "lossless codec=$codec container=$container sampleRate=$sampleRate channels=$channels protocol=$protocol"
}

/**
 * Host plus the query params that identify a YouTube stream (itag, mime, clen = byte size, dur),
 * instead of the full signed URL.
 */
internal fun urlSummary(url: String): String = runCatching {
    val parsed = io.ktor.http.Url(url)
    val keys = listOf("itag", "mime", "clen", "dur")
    val params = keys.mapNotNull { key -> parsed.parameters[key]?.let { "$key=$it" } }
    "url=${parsed.host}${parsed.encodedPath.take(40)}" + if (params.isEmpty()) "" else " ${params.joinToString(" ")}"
}.getOrDefault("url=<unparseable ${url.take(60)}>")
